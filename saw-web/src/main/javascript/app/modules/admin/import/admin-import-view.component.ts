import { Component, Input, ViewChild, ElementRef } from '@angular/core';
import * as fpPipe from 'lodash/fp/pipe';
import * as fpFilter from 'lodash/fp/filter';
import * as fpMap from 'lodash/fp/map';
import * as map from 'lodash/map';
import * as reduce from 'lodash/reduce';
import * as forEach from 'lodash/forEach';
import * as filter from 'lodash/filter';
import * as toString from 'lodash/toString';
import * as get from 'lodash/get';
import * as isEmpty from 'lodash/isEmpty';
import * as fpFlatMap from 'lodash/fp/flatMap';
import * as json2Csv from 'json-2-csv';
import * as FileSaver from 'file-saver';
import * as moment from 'moment';
import { JwtService } from '../../../../login/services/jwt.service';
import { ImportService } from './import.service';
import { CategoryService } from '../category/category.service';
import { SidenavMenuService } from '../../../common/components/sidenav';
import { executeAllPromises } from '../../../common/utils/executeAllPromises';
import { getFileContents } from '../../../common/utils/fileManager';
import { AdminMenuData } from '../consts';
import { Analysis } from '../../../models';
import { ExportService } from '../export/export.service';

const template = require('./admin-import-view.component.html');
require('./admin-import-view.component.scss');

type FileInfo = {name: string, count: number};
type FileContent = {name: string, count: number, analyses: Array<Analysis>};
@Component({
  selector: 'admin-import-view',
  template
})
export class AdminImportViewComponent {
  files: Array<FileInfo>;
  fileContents: Array<FileContent>
  selectedCategory;
  categories$;
  metricsMap: Object = {};
  analyses: Array<Analysis>;
  analysesFromBEMap: Object = {};
  userCanExportErrors = false;
  atLeast1AnalysisIsSelected = false;

  @Input() columns: any[];

  constructor(
    private _importService: ImportService,
    private _exportService: ExportService,
    private _categoryService: CategoryService,
    private _sidenav: SidenavMenuService,
    private _jwtService: JwtService
  ) {}

  ngOnInit() {
    this._sidenav.updateMenu(AdminMenuData, 'ADMIN');
    this.categories$ = this._categoryService.getList().then(fpFilter(category => category.moduleName === 'ANALYZE'));
    this.getMetrics();
  }

  getMetrics() {
    this._exportService.getMetricList().then(metrics => {
      this.metricsMap = reduce(metrics, (acc, metric) => {
        acc[metric.metricName] = metric;
        return acc;
      }, {});
    })
  }

  onRemoveFile(fileName) {
    this.fileContents = filter(this.fileContents, ({name}) => fileName !== name);
    this.splitFileContents(this.fileContents);
  }

  splitFileContents(contents) {
    this.atLeast1AnalysisIsSelected = false;
    this.files = map(contents, ({name, count}) => ({name, count}));
    this.analyses = fpPipe(
      fpFlatMap(({analyses}) => analyses),
      fpMap(analysis => this.getAnalysisObjectForGrid(analysis))
    )(contents);
  }

  readFiles(event) {
    const files = event.target.files;

    const contentPromises = fpPipe(
      fpFilter(file => file.type === 'application/json'),
      fpMap(file => getFileContents(file)
        .then(content => {
          const analyses = JSON.parse(content);
          return {
            name: file.name,
            count: analyses.length,
            analyses
          };
        })
      )
    )(files);

    Promise.all(contentPromises).then(contents => {
      this.fileContents = contents;
      this.splitFileContents(contents);
    });
  }

  onCategoryChange(categoryId) {
    this.selectedCategory = categoryId;
    this._importService.getAnalysesFor(toString(categoryId)).then(analyses => {
      this.analysesFromBEMap = reduce(analyses, (acc, analysis) => {
        acc[`${analysis.name}:${analysis.metricName}:${analysis.type}`] = analysis;
        return acc;
      }, {});
      this.splitFileContents(this.fileContents);
    });
  }

  getAnalysisObjectForGrid(analysis) {
    const metric = this.metricsMap[analysis.metricName];
    if (metric) {
      analysis.semanticId = metric.id;
    }
    const analysisFromBE = this.analysesFromBEMap[`${analysis.name}:${analysis.metricName}:${analysis.type}`];

    const possibilitySelector = metric ? (analysisFromBE ? 'duplicate' : 'normal') : 'noMetric'

    const possibility = this.getPossibleGridObjects(possibilitySelector, analysis, analysisFromBE);

    return {
      ...possibility,
      selection: false
    }
  }

  getPossibleGridObjects(selector: 'noMetric' | 'duplicate' | 'normal', analysis, analysisFromBE) {
    switch (selector) {
    case 'noMetric':
      return {
        logColor: 'red',
        log: 'Metric doesn\'t exists.',
        errorMsg: `${analysis.metricName}: Metric does not exists.`,
        duplicateAnalysisInd: false,
        errorInd: true,
        noMetricInd: true,
        analysis
      };
    case 'duplicate':
      const modifiedAnalysis = this.getModifiedAnalysis(analysis, analysisFromBE);
      return {
        logColor: 'brown',
        log: 'Analysis exists. Please Override to delete existing data.',
        errorMsg: 'Analysis exists. Please Override to delete existing data.',
        duplicateAnalysisInd: true,
        errorInd: true,
        noMetricInd: false,
        analysis: modifiedAnalysis
      };
    case 'normal':
      return {
        logColor: 'transparent',
        log: '',
        errorMsg: '',
        duplicateAnalysisInd: false,
        errorInd: false,
        noMetricInd: false,
        analysis
      };
    }
  }

  getModifiedAnalysis(analysis, analysisFromBE) {
    const {
      isScheduled,
      scheduled,
      createdTimestamp,
      esRepository,
      id,
      repository
    } = analysisFromBE;
    const {
      userFullName,
      userId
    } = this._jwtService.getTokenObj().ticket;

    return {
      ...analysis,
      isScheduled,
      scheduled,
      createdTimestamp,
      userFullName,
      id,
      userId,
      esRepository,
      repository
    }
  }

  import() {
    const importPromises = fpPipe(
      fpFilter('selection'),
      fpMap(gridObj => {
        const {duplicateAnalysisInd, analysis} = gridObj;
        if (duplicateAnalysisInd) {
          return this.importExistingAnalysis(analysis);
        } else {
          return this.importNewAnalysis(analysis).then(addedAnalysis => {
            gridObj.analysis.id = addedAnalysis.id;
            return addedAnalysis;
          });
        }
      })
    )(this.analyses);

    executeAllPromises(importPromises).then((results) => {
      const selectedAnalyses = filter(this.analyses, 'selection');

      const updatedAnalysesMap = reduce(results, (acc, result, index) => {

        if (result.result) {
          const analysis = result.result;
          acc[analysis.id] = {analysis};
        } else {
          const error = result.error;
          const gridObj = selectedAnalyses[index];
          acc[gridObj.analysis.id] = {error};
        }

        return acc;
      }, {});

      let hasErrors = false;
      // update the logs
      forEach(this.analyses, gridObj => {
        if (gridObj.selection) {
          const id = gridObj.analysis.id;
          const container = updatedAnalysesMap[id];
          // if analysis was updated
          if (container && container.analysis) {
            gridObj.logColor = 'green';
            gridObj.log = 'Successfully Imported';
            gridObj.errorInd = false;

          } else {
            hasErrors = true;
            const error = container.error;
            gridObj.logColor = 'red';
            gridObj.log = 'Error While Importing'
            gridObj.errorMsg = get(error, 'error.error.message');
            gridObj.errorInd = true;
          }
        }
      });

      this.userCanExportErrors = hasErrors;
    });
  }


  exportErrors() {
    const logMessages = fpPipe(
      fpFilter('selection'),
      fpMap(gridObj => {
        const { analysis, errorMsg } = gridObj;
        const { metricName, name, type } = analysis;
        return {
          analysisName: name,
          analysisType: type,
          metricName,
          errorLog: errorMsg
        };
      })
    )(this.analyses);

    if (!isEmpty(logMessages)) {
      json2Csv.json2csv(logMessages, (err, csv) => {
        if (err) {
          throw err;
        }
        const logFileName = this.getLogFileName();
        const newData = new Blob([csv], {type: 'text/csv;charset=utf-8'});
        FileSaver.saveAs(newData, logFileName);
      });
    }
  }

  getLogFileName() {
    const formatedDate = moment().format('YYYYMMDDHHmmss');
    return `log${formatedDate}.csv`;
  }

  importNewAnalysis(analysis: Analysis) {
    const {
      semanticId,
      type
    } = analysis;
    return new Promise<Analysis>((resolve, reject) => {

      this._importService.createAnalysis(semanticId, type).then((initializedAnalysis: Analysis) => {
        const {
          isScheduled,
          scheduled,
          createdTimestamp,
          id,
          userFullName,
          userId,
          esRepository,
          repository
        } = initializedAnalysis;

        this.importExistingAnalysis({
          ...analysis,
          isScheduled,
          scheduled,
          createdTimestamp,
          id,
          userFullName,
          userId,
          esRepository,
          repository
        }).then(updatedAnalysis => resolve(updatedAnalysis), err => reject(err));
      });
    });
  }

  importExistingAnalysis(analysis): Promise<Analysis> {
    analysis.categoryId = toString(this.selectedCategory);
    return this._importService.updateAnalysis(analysis);
  }

  onAnalysesValiditychange(atLeast1AnalysisIsSelected) {
    this.atLeast1AnalysisIsSelected = atLeast1AnalysisIsSelected;
  }
}
