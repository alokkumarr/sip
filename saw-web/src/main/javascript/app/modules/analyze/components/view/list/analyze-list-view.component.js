import * as template from './analyze-list-view.component.html';
import style from './analyze-list-view.component.scss';
import * as forEach from 'lodash/forEach';
import cronstrue from 'cronstrue';
import * as moment from 'moment';
import * as isEmpty from 'lodash/isEmpty';
import {Events} from '../../../consts';

export const AnalyzeListViewComponent = {
  template,
  styles: [style],
  bindings: {
    analyses: '<',
    analysisType: '<',
    filter: '<',
    onAction: '&',
    searchTerm: '<',
    updater: '<',
    cronJobs: '<'
  },
  controller: class AnalyzeListViewController {
    constructor($mdDialog, dxDataGridService, AnalyzeService, AnalyzeActionsService, JwtService, $eventEmitter) {
      'ngInject';
      this._$mdDialog = $mdDialog;
      this._dxDataGridService = dxDataGridService;
      this._AnalyzeService = AnalyzeService;
      this._AnalyzeActionsService = AnalyzeActionsService;
      this._JwtService = JwtService;
      this._$eventEmitter = $eventEmitter;

      this._gridListInstance = null;

      this.canUserFork = false;
    }

    $onInit() {
      this.gridConfig = this.getGridConfig();
      this.updaterSubscribtion = this.updater.subscribe(update => this.onUpdate(update));

      this.canUserFork = this._JwtService.hasPrivilege('FORK', {
        subCategoryId: this.analyses[0].categoryId
      });
    }

    $onChanges(changes) {
      if (changes.cronJobs && this._gridListInstance) {
        this.onUpdateAnalysisType(this.analysisType);
      }
    }

    showExecutingFlag(analysisId) {
      return analysisId && this._AnalyzeService.isExecuting(analysisId);
    }

    $onDestroy() {
      this.updaterSubscribtion.unsubscribe();
    }

    onUpdate({analysisType, analyses}) {
      /* eslint-disable */
      analyses && this.reloadDataGrid(analyses);
      analysisType && this.onUpdateAnalysisType(analysisType);
      /* eslint-enable */
    }

    onUpdateAnalysisType(analysisType) {
      let scheduleState;
      this.analysisType = analysisType;
      if (analysisType === 'all') {
        this._gridListInstance.clearFilter();
      } else if (analysisType === 'scheduled') {
        this._gridListInstance.filter(itemData => {
          scheduleState = false;
          forEach(this.cronJobs, cron => {
            if (cron.jobDetails.analysisID === itemData.id) {
              scheduleState = true;
            }
          });
          return scheduleState;
        });
      } else {
        this._gridListInstance.filter(['type', '=', analysisType]);
      }
    }

    onGridInitialized(e) {
      this._gridListInstance = e.component;
      this.onUpdateAnalysisType(this.analysisType);
    }

    fork(analysis) {
      this._AnalyzeActionsService.fork(analysis).then(result => {
        if (!result.isSaveSuccessful) {
          return result;
        }
        this._$eventEmitter.emit(Events.AnalysesRefresh);
      });
    }

    onSuccessfulDeletion(analysis) {
      this.onAction({
        type: 'onSuccessfulDeletion',
        model: analysis
      });
    }

    onSuccessfulExecution(analysis) {
      this.onAction({
        type: 'onSuccessfulExecution',
        model: analysis
      });
    }

    onSuccessfulPublish(analysis) {
      this.onAction({
        type: 'onSuccessfulPublish',
        model: analysis
      });
    }

    reloadDataGrid(analyses) {
      this._gridListInstance.option('dataSource', analyses);
      this._gridListInstance.refresh();
    }

    getGridConfig() {
      const dataSource = this.analyses || [];
      const columns = [{
        caption: 'NAME',
        dataField: 'name',
        allowSorting: true,
        alignment: 'left',
        width: '36%',
        cellTemplate: 'nameCellTemplate',
        cssClass: 'branded-column-name'
      }, {
        caption: 'METRICS',
        dataField: 'metrics',
        allowSorting: true,
        alignment: 'left',
        width: '21%',
        calculateCellValue: rowData => {
          return rowData.metricName ||
            (rowData.metrics || []).join(', ');
        },
        cellTemplate: 'metricsCellTemplate'
      }, {
        caption: 'SCHEDULED',
        dataField: 'scheduled',
        calculateCellValue: rowData => {
          return this.generateSchedule(rowData);
        },
        allowSorting: true,
        alignment: 'left',
        width: '12%'
      }, {
        caption: 'TYPE',
        dataField: 'type',
        allowSorting: true,
        alignment: 'left',
        width: '8%',
        calculateCellValue: rowData => {
          return this.checkRowType(rowData);
        },
        cellTemplate: 'typeCellTemplate'
      }, {
        caption: 'CREATOR',
        dataField: 'userFullName',
        allowSorting: true,
        alignment: 'left',
        width: '20%',
        calculateCellValue: rowData => {
          return (rowData.userFullName || '').toUpperCase();
        },
        cellTemplate: 'creatorCellTemplate'
      }, {
        caption: 'CREATED',
        allowSorting: true,
        dataField: 'createdTimestamp',
        alignment: 'left',
        width: '8%',
        cellTemplate: 'timecreatedCellTemplate'
      }, {
        caption: '',
        cellTemplate: 'actionCellTemplate'
      }];
      return this._dxDataGridService.mergeWithDefaultConfig({
        onInitialized: this.onGridInitialized.bind(this),
        columns,
        dataSource,
        paging: {
          pageSize: 10
        },
        pager: {
          showPageSizeSelector: true,
          showInfo: true
        }
      });
    }

    generateSchedule(rowData) {
      let scheduleHuman = '';
      forEach(this.cronJobs, cron => {
        if (cron.jobDetails.analysisID === rowData.id && !isEmpty(cron.jobDetails.cronExpression)) {
          if (cron.jobDetails.activeTab === 'hourly') {
            // there is no time stamp in hourly cron hence converting to utc and local is not required.
            const localMinuteCron = this.extractMinute(cron.jobDetails.cronExpression);
            scheduleHuman = cronstrue.toString(localMinuteCron);

          } else {
            const localCron = this.convertToLocal(cron.jobDetails.cronExpression);
            scheduleHuman = cronstrue.toString(localCron);
          }
        }
      });
      return scheduleHuman;
    }

    extractMinute(CronUTC) {
      if (CronUTC.match(/\d+ 0\/\d+ \* 1\/1 \* \? \*/)) {
        return CronUTC;
      }
      const splitArray = CronUTC.split(' ');
      const date = new Date();
      date.setUTCHours(moment().format('HH'), splitArray[1]);
      const UtcTime = moment.utc(date).local().format('mm').split(' ');
      splitArray[1] = UtcTime[0];
      return splitArray.join(' ');
    }

    convertToLocal(CronUTC) {
      const splitArray = CronUTC.split(' ');
      const date = new Date();
      date.setUTCHours(splitArray[2], splitArray[1]);
      const UtcTime = moment.utc(date).local().format('mm HH').split(' ');
      splitArray[1] = UtcTime[0];
      splitArray[2] = UtcTime[1];
      return splitArray.join(' ');

    }

    checkRowType(rowData) {
      let analysisType = rowData.type;
      if (rowData.type === 'esReport') {
        analysisType = 'REPORT';
      }
      return analysisType.toUpperCase();
    }
  }
};
