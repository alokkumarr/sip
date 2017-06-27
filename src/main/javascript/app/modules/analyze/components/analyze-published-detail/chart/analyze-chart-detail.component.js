import get from 'lodash/get';
import values from 'lodash/values';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import template from './analyze-chart-detail.component.html';

export const AnalyzeChartDetailComponent = {
  template,
  bindings: {
    analysis: '<',
    requester: '<'
  },
  controller: class AnalyzeChartDetailController {
    constructor(ChartService, FilterService, $timeout) {
      'ngInject';

      this._ChartService = ChartService;
      this._FilterService = FilterService;
      this._$timeout = $timeout;
      this.chartUpdater = new BehaviorSubject({});
      this.labels = {
        y: '', x: ''
      };

    }

    $onInit() {
      this.settings = this._ChartService.fillSettings(this.analysis.artifacts, this.analysis);
      this.subscription = this.requester.subscribe(options => this.request(options));

      this.labels.x = get(this.analysis, 'xAxis.title', null);
      this.labels.y = get(this.analysis, 'yAxis.title', null);

      this.legend = {
        align: get(this.analysis, 'legend.align', 'right'),
        layout: get(this.analysis, 'legend.layout', 'vertical'),
        options: {
          align: values(this._ChartService.LEGEND_POSITIONING),
          layout: values(this._ChartService.LAYOUT_POSITIONS)
        }
      };
      this.chartOptions = this._ChartService.getChartConfigFor(this.analysis.chartType, {legend: this.legend});

      this._$timeout(() => {
        this.updateChart();
      });
    }

    $onDestroy() {
      this.subscription.unsubscribe();
    }

    updateChart() {
      const changes = this._ChartService.dataToChangeConfig(
        this.analysis.chartType,
        this.settings,
        this.filteredData,
        {labels: this.labels}
      );

      this.chartUpdater.next(changes);
    }

    request({data}) {
      /* eslint-disable no-unused-expressions */
      if (!data) {
        return;
      }

      this.filteredData = data;
      this.updateChart();
      /* eslint-disable no-unused-expressions */
    }

    onExport() {
      this.chartUpdater.next({
        export: true
      });
    }
  }
};
