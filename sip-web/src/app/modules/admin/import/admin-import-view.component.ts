import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { Store, Select } from '@ngxs/store';
import {
  LoadAllAnalyzeCategories,
  SelectAnalysisGlobalCategory,
  LoadMetrics,
  LoadAnalysesForCategory,
  ClearImport,
  RefreshAllCategories
} from './actions/import-page.actions';
import { Observable } from 'rxjs';
import * as fpPipe from 'lodash/fp/pipe';
import * as fpFilter from 'lodash/fp/filter';
import * as fpMap from 'lodash/fp/map';
import * as map from 'lodash/map';
import * as reduce from 'lodash/reduce';
import * as forEach from 'lodash/forEach';
import * as filter from 'lodash/filter';
import * as toString from 'lodash/toString';
import * as pick from 'lodash/pick';
import * as get from 'lodash/get';
import * as isEmpty from 'lodash/isEmpty';
import * as fpFlatMap from 'lodash/fp/flatMap';
import * as json2Csv from 'json-2-csv';
import * as FileSaver from 'file-saver';
import * as moment from 'moment';
import { JwtService } from '../../../common/services';
import { ImportService } from './import.service';
import { CategoryService } from '../category/category.service';
import { SidenavMenuService } from '../../../common/components/sidenav';
import { executeAllPromises } from '../../../common/utils/executeAllPromises';
import { getFileContents } from '../../../common/utils/fileManager';
import { AdminMenuData } from '../consts';
import { Analysis, AnalysisDSL } from '../../../models';
import { ExportService } from '../export/export.service';
import { DSL_ANALYSIS_TYPES } from '../../analyze/consts';
import { isDSLAnalysis } from 'src/app/common/types';

const DUPLICATE_GRID_OBJECT_PROPS = {
  logColor: 'brown',
  log: 'Analysis exists. Please Overwrite to delete existing data.',
  errorMsg: 'Analysis exists. Please Overwrite to delete existing data.',
  duplicateAnalysisInd: true,
  errorInd: false,
  noMetricInd: false
};

const NORMAL_GRID_OBJECT_PROPS = {
  logColor: 'transparent',
  log: '',
  errorMsg: '',
  duplicateAnalysisInd: false,
  errorInd: false,
  noMetricInd: false
};

const LEGACY_GRID_OBJECT_PROPS = {
  logColor: 'red',
  log: 'Invalid analysis structure. Missing sipQuery property.',
  errorMsg: 'Invalid analysis structure. Missing sipQuery property.',
  duplicateAnalysisInd: false,
  errorInd: true,
  legacyInd: true,
  noMetricInd: false
};

interface FileInfo {
  name: string;
  count: number;
}

interface FileContent {
  name: string;
  count: number;
  analyses: Array<Analysis | AnalysisDSL>;
}

interface AnalysisGridObject {
  selection?: boolean;
  logColor: string;
  log: string;
  errorMsg: string;
  duplicateAnalysisInd: boolean;
  errorInd: boolean;
  noMetricInd: boolean;
  analysis: Analysis | AnalysisDSL;
}

@Component({
  selector: 'admin-import-view',
  templateUrl: './admin-import-view.component.html',
  styleUrls: ['./admin-import-view.component.scss']
})
export class AdminImportViewComponent implements OnInit, OnDestroy {
  @Select(state => state.admin.importPage.categories.analyze)
  categories$: Observable<any[]>;

  @Select(state => state.admin.importPage.metrics) metricMap$: Observable<any>;

  files: Array<FileInfo>;
  fileContents: Array<FileContent>;
  selectedCategory: string | number;
  analyses: Array<AnalysisGridObject>;
  userCanExportErrors = false;
  atLeast1AnalysisIsSelected = false;

  @Input() columns: any[];

  constructor(
    public _importService: ImportService,
    public _exportService: ExportService,
    public _categoryService: CategoryService,
    public _sidenav: SidenavMenuService,
    public _jwtService: JwtService,
    private store: Store
  ) {}

  ngOnInit() {
    this._sidenav.updateMenu(AdminMenuData, 'ADMIN');
    this.store.dispatch([new LoadAllAnalyzeCategories(), new LoadMetrics()]);
  }

  ngOnDestroy() {
    this.store.dispatch(new ClearImport());
  }

  /**
   * Reads a file that user just added. Tries to parse it
   * and add analyses to list for selection.
   *
   * @param {*} event
   * @memberof AdminImportViewComponent
   */
  readFiles(event) {
    const files = event.target.files;

    /* Filter out non-json files */
    const contentPromises = <Promise<FileContent>[]>fpPipe(
      fpFilter(file => file.type === 'application/json'),
      fpMap(file =>
        getFileContents(file).then(content => {
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

      // clear the file input
      event.target.value = '';
    });
  }

  splitFileContents(contents: FileContent[]) {
    let hasErrors = false;
    this.atLeast1AnalysisIsSelected = false;
    this.files = map(contents, ({ name, count }) => ({ name, count }));
    this.analyses = fpPipe(
      fpFlatMap(({ analyses }) => analyses),
      fpMap((analysis: Analysis | AnalysisDSL) => {
        if (isDSLAnalysis(analysis)) {
          analysis.category = this.selectedCategory || '';
        } else {
          analysis.categoryId = +(this.selectedCategory || '');
        }
        const gridObj = this.getAnalysisObjectForGrid(analysis);
        if (gridObj.errorInd) {
          hasErrors = true;
        }
        return gridObj;
      })
    )(contents);
    this.userCanExportErrors = hasErrors;
  }

  onRemoveFile(fileName: string) {
    this.fileContents = filter(
      this.fileContents,
      ({ name }) => fileName !== name
    );
    this.splitFileContents(this.fileContents);
  }

  updateGridObject(analysisId: string) {
    const id = this.analyses.findIndex(
      ({ analysis }) => analysis.id === analysisId
    );
    const newGridObject = this.getAnalysisObjectForGrid(
      this.analyses[id].analysis,
      this.analyses[id].selection
    );
    this.analyses.splice(id, 1, newGridObject);
  }

  onCategoryChange(categoryId: string) {
    this.selectedCategory = categoryId;
    this.store
      .dispatch(new SelectAnalysisGlobalCategory(categoryId))
      .subscribe(() => {
        this.splitFileContents(this.fileContents);
      });
  }

  onAnalysisCategoryChange({ categoryId, analysisId }) {
    this.setAnalysisCategory(categoryId, analysisId);
    this.store
      .dispatch(new LoadAnalysesForCategory(categoryId))
      .subscribe(() => {
        this.updateGridObject(analysisId);
      });
  }

  setAnalysisCategory(categoryId: string | number, analysisId: string) {
    const a = this.analyses.find(({ analysis }) => analysis.id === analysisId);
    if (a && a.analysis) {
      if (isDSLAnalysis(a.analysis)) {
        a.analysis.category = toString(categoryId);
      } else {
        a.analysis.categoryId = toString(categoryId);
      }
    }
  }

  getAnalysisObjectForGrid(
    analysis: Analysis | AnalysisDSL,
    selection = false
  ): AnalysisGridObject {
    const { metrics, referenceAnalyses } = this.store.selectSnapshot(
      state => state.admin.importPage
    );
    const metric = metrics[analysis.metricName];
    if (metric) {
      analysis.semanticId = metric.id;
    }
    const categoryId = isDSLAnalysis(analysis)
      ? analysis.category
      : analysis.categoryId;
    const analysisCategory = referenceAnalyses[categoryId.toString()] || {};
    const analysisFromBE =
      analysisCategory[
        `${analysis.name}:${analysis.metricName}:${analysis.type}`
      ];

    const possibilitySelector =
      !isDSLAnalysis(analysis) && DSL_ANALYSIS_TYPES.includes(analysis.type)
        ? 'legacy'
        : metric
        ? analysisFromBE
          ? 'duplicate'
          : 'normal'
        : 'noMetric';

    const possibility = this.getPossibleGridObjects(
      possibilitySelector,
      analysis,
      analysisFromBE
    );

    return {
      ...possibility,
      selection
    };
  }

  getPossibleGridObjects(
    selector: 'noMetric' | 'duplicate' | 'normal' | 'legacy',
    analysis: Analysis | AnalysisDSL,
    analysisFromBE: Analysis | AnalysisDSL
  ): AnalysisGridObject {
    switch (selector) {
      case 'noMetric':
        return {
          logColor: 'red',
          log: `Metric doesn't exists.`,
          errorMsg: `${analysis.metricName}: Metric does not exists.`,
          duplicateAnalysisInd: false,
          errorInd: true,
          noMetricInd: true,
          analysis
        };
      case 'legacy':
        return {
          analysis,
          ...LEGACY_GRID_OBJECT_PROPS
        };
      case 'duplicate':
        const modifiedAnalysis = this.getModifiedAnalysis(
          analysis,
          analysisFromBE
        );
        return {
          ...DUPLICATE_GRID_OBJECT_PROPS,
          analysis: modifiedAnalysis
        };
      case 'normal':
        return {
          ...NORMAL_GRID_OBJECT_PROPS,
          analysis
        };
    }
  }

  getModifiedAnalysis(
    analysis: Analysis | AnalysisDSL,
    analysisFromBE: Analysis | AnalysisDSL
  ): Analysis | AnalysisDSL {
    let fields: Partial<Analysis | AnalysisDSL>;
    if (isDSLAnalysis(analysisFromBE)) {
      const { id, createdTime, schedule } = analysisFromBE;
      fields = { id, createdTime, schedule };
    } else {
      const {
        isScheduled,
        scheduled,
        createdTimestamp,
        esRepository,
        id,
        repository
      } = analysisFromBE;
      fields = {
        isScheduled,
        scheduled,
        createdTimestamp,
        esRepository,
        id,
        repository
      };
    }
    const { userFullName, userId } = this._jwtService.getTokenObj().ticket;

    return {
      ...analysis,
      ...fields,
      userFullName,
      userId
    } as Analysis | AnalysisDSL;
  }

  import() {
    const importPromises = fpPipe(
      fpFilter('selection'),
      fpMap((gridObj: AnalysisGridObject) => {
        const { duplicateAnalysisInd, analysis } = gridObj;
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

    executeAllPromises(importPromises).then(
      results => {
        const selectedAnalyses: AnalysisGridObject[] = filter(
          this.analyses,
          'selection'
        );

        const updatedAnalysesMap = reduce(
          results,
          (acc, result, index) => {
            if (result.result) {
              const analysis = result.result;
              acc[analysis.id] = { analysis };
            } else {
              const error = result.error;
              const gridObj = selectedAnalyses[index];
              acc[gridObj.analysis.id] = { error };
            }

            return acc;
          },
          {}
        );

        let hasErrors = false;
        let someImportsWereSuccesful = false;
        // update the logs
        forEach(this.analyses, (gridObj: AnalysisGridObject) => {
          if (gridObj.selection) {
            const id = gridObj.analysis.id;
            const container = updatedAnalysesMap[id];
            // if analysis was updated
            if (container && container.analysis) {
              gridObj.logColor = 'green';
              gridObj.log = 'Successfully Imported';
              gridObj.errorInd = false;
              gridObj.duplicateAnalysisInd = true;
              gridObj.selection = false;
              someImportsWereSuccesful = true;
            } else {
              hasErrors = true;
              const error = container.error;
              gridObj.logColor = 'red';
              gridObj.log = 'Error While Importing';
              gridObj.errorMsg = get(error, 'error.error.message');
              gridObj.errorInd = true;
            }
          }
        });

        this.userCanExportErrors = hasErrors;

        if (someImportsWereSuccesful) {
          this.analyses = [...this.analyses];
        }
        this.atLeast1AnalysisIsSelected = false;
        this.store.dispatch(new RefreshAllCategories());
      },
      () => {
        this.store.dispatch(new RefreshAllCategories());
      }
    );
  }

  exportErrors() {
    const logMessages = fpPipe(
      fpFilter('errorInd'),
      fpMap((gridObj: AnalysisGridObject) => {
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
        const newData = new Blob([csv], { type: 'text/csv;charset=utf-8' });
        FileSaver.saveAs(newData, logFileName);
      });
    }
  }

  getLogFileName(): string {
    const formatedDate = moment().format('YYYYMMDDHHmmss');
    return `log${formatedDate}.csv`;
  }

  importNewAnalysis(analysis: Analysis | AnalysisDSL) {
    const { semanticId, type } = analysis;
    return new Promise<Analysis | AnalysisDSL>((resolve, reject) => {
      this._importService
        .createAnalysis(semanticId, type)
        .then((initializedAnalysis: AnalysisDSL) => {
          let fields: Partial<Analysis | AnalysisDSL>;
          if (isDSLAnalysis(initializedAnalysis)) {
            fields = pick(initializedAnalysis, [
              'id',
              'createdBy',
              'createdTime',
              'schedule'
            ]);
          } else {
            fields = pick(initializedAnalysis, [
              'isScheduled',
              'scheduled',
              'createdTimestamp',
              'id',
              'userFullName',
              'userId',
              'esRepository',
              'repository'
            ]);
          }

          this.importExistingAnalysis({
            ...analysis,
            ...fields
          } as Analysis | AnalysisDSL).then(
            updatedAnalysis => resolve(updatedAnalysis),
            err => reject(err)
          );
        });
    });
  }

  canImport() {
    const selectedAnalyses: AnalysisGridObject[] =
      filter(this.analyses, 'selection') || [];
    return (
      selectedAnalyses.length &&
      selectedAnalyses.every(({ analysis }) =>
        Boolean(
          isDSLAnalysis(analysis) ? analysis.category : analysis.categoryId
        )
      )
    );
  }

  importExistingAnalysis(
    analysis: Analysis | AnalysisDSL
  ): Promise<Analysis | AnalysisDSL> {
    return this._importService.updateAnalysis(analysis);
  }

  onAnalysesValiditychange(atLeast1AnalysisIsSelected: boolean) {
    this.atLeast1AnalysisIsSelected = atLeast1AnalysisIsSelected;
  }
}
