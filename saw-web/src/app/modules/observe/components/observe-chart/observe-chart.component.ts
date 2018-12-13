import {
  Component,
  Input,
  Output,
  OnInit,
  OnDestroy,
  AfterViewInit,
  EventEmitter,
  ViewChild
} from '@angular/core';
import { DxDataGridComponent } from 'devextreme-angular/ui/data-grid';
import { BehaviorSubject, Subscription } from 'rxjs';
import { ChartService } from '../../../analyze/services/chart.service';
import { AnalyzeService } from '../../../analyze/services/analyze.service';
import { FilterService } from '../../../analyze/services/filter.service';
import { ChartComponent } from '../../../../common/components/charts/chart.component';
import { flattenChartData } from '../../../../common/utils/dataFlattener';
import * as isUndefined from 'lodash/isUndefined';
import * as get from 'lodash/get';
import * as set from 'lodash/set';
import * as isEmpty from 'lodash/isEmpty';
import * as clone from 'lodash/clone';
import * as deepClone from 'lodash/cloneDeep';
import * as filter from 'lodash/filter';
import * as map from 'lodash/map';
import * as orderBy from 'lodash/orderBy';
import * as find from 'lodash/find';
import * as forEach from 'lodash/forEach';
import * as remove from 'lodash/remove';
import * as concat from 'lodash/concat';
import * as moment from 'moment';

import { EXECUTION_MODES } from '../../../analyze/services/analyze.service';
import { ArtifactColumnReport } from '../../../analyze/types';

interface ReportGridField {
  caption: string;
  dataField: string;
  dataType: string;
  type: string;
  visibleIndex: number;
  payload: ArtifactColumnReport;
  visible: boolean;
  allowSorting?: boolean;
  alignment?: 'center' | 'left' | 'right';
  format?: string | object;
  sortOrder?: 'asc' | 'desc';
  sortIndex?: number;
  changeColumnProp: Function;
  headerCellTemplate: string;
}

@Component({
  selector: 'observe-chart',
  templateUrl: './observe-chart.component.html',
  styleUrls: ['./observe-chart.component.scss'],
  providers: [ChartService]
})

export class ObserveChartComponent implements OnInit, OnDestroy, AfterViewInit {
  public columns: ReportGridField[];
  @Input() analysis: any;
  @Input() item: any;
  @Input() enableChartDownload: boolean;
  @Input() updater: BehaviorSubject<Array<any>>;
  @Input() ViewMode: boolean;
  @Output() onRefresh = new EventEmitter<any>();
  @ViewChild(ChartComponent) chartComponent: ChartComponent;
  @ViewChild(DxDataGridComponent) dataGrid: DxDataGridComponent;

  public chartUpdater = new BehaviorSubject([]);
  public requesterSubscription: Subscription;
  public legend: any;
  public chartOptions: any;
  public settings: any;
  public labels: any;
  public gridData: Array<any>;
  public sorts: Array<any>;
  public filters: Array<any>;
  public isStockChart: boolean;
  public chartToggleData: any;
  public toggleToGrid: boolean;

  constructor(
    public chartService: ChartService,
    public analyzeService: AnalyzeService,
    public filterService: FilterService
  ) {
    this.customizeColumns = this.customizeColumns.bind(this);
  }

  ngOnInit() {
    this.legend = this.chartService.initLegend(this.analysis);

    this.chartOptions = this.chartService.getChartConfigFor(
      this.analysis.chartType,
      { legend: this.legend }
    );
    this.isStockChart = isUndefined(this.analysis.isStockChart)
      ? false
      : this.analysis.isStockChart;
    this.subscribeToRequester();
    this.toggleToGrid = false;
  }

  ngOnDestroy() {
    this.requesterSubscription.unsubscribe();
  }

  /* Accept changes from parent component and pass those on to chart.
     Having separate requester and chartUpdater allows transforming
     changes coming from parent before passing them on. */
  subscribeToRequester() {
    this.requesterSubscription = this.updater.subscribe(data => {
      let changes = this.getChangeConfig(
        this.settings,
        this.gridData,
        this.labels
      );
      changes = changes.concat(data);
      this.chartUpdater.next(changes);
    });
  }

  ngAfterViewInit() {
    this.initChart();
  }

  initChart() {
    this.labels = { x: null, y: null, tempX: null, tempY: null };
    this.labels.tempX = this.labels.x = get(this.analysis, 'xAxis.title', null);
    this.labels.tempY = this.labels.y = get(this.analysis, 'yAxis.title', null);
    this.sorts = this.analysis.sqlBuilder.sorts;

    this.filters = map(
      get(this.analysis, 'sqlBuilder.filters', []),
      this.filterService.backend2FrontendFilter(this.analysis.artifacts)
    );

    this.legend = {
      align: get(this.analysis, 'legend.align'),
      layout: get(this.analysis, 'legend.layout')
    };

    this.settings = this.chartService.fillSettings(
      this.analysis.artifacts,
      this.analysis
    );
    this.chartService.updateAnalysisModel(this.analysis);

    if (isUndefined(this.analysis._executeTile) || this.analysis._executeTile) {
      this.onRefreshData().then(data => {
        this.gridData = data;
        this.reloadChart(this.settings, this.gridData, this.labels);
        this.item && this.onRefresh.emit(this.item);
      });
    }
  }

  isNodeField(field) {
    const nodeFieldAreas = ['x', 'g'];
    return (
      field &&
      (nodeFieldAreas.includes(field.checked) ||
        nodeFieldAreas.includes(field.area))
    );
  }

  isDataField(field) {
    const nodeFieldAreas = ['y', 'z'];
    return (
      field &&
      (nodeFieldAreas.includes(field.checked) ||
        nodeFieldAreas.includes(field.area))
    );
  }

  reloadChart(settings, gridData, labels) {
    const changes = this.getChangeConfig(settings, gridData, labels);
    this.chartUpdater.next(changes);
  }

  getChangeConfig(settings, gridData, labels): Array<any> {
    if (isEmpty(gridData)) {
      /* Making sure empty data refreshes chart and shows no data there.  */
      return [{ path: 'series', data: [] }];
    }

    if (!isEmpty(this.sorts)) {
      gridData = orderBy(
        gridData,
        map(this.sorts, 'columnName'),
        map(this.sorts, 'order')
      );
    }

    let changes = this.chartService.dataToChangeConfig(
      this.analysis.chartType,
      settings,
      this.analysis.sqlBuilder,
      deepClone(gridData),
      { labels, labelOptions: this.analysis.labelOptions, sorts: this.sorts }
    );

    changes = changes.concat(this.getLegend());
    changes = changes.concat([{ path: 'title.y', data: -10 }]);

    changes.push({
      path: 'chart.inverted',
      data: get(this.analysis, 'isInverted', false)
    });

    return changes;
  }

  fetchColumnData(axisName, value) {
    let aliasName = axisName;
    forEach(this.analysis.artifacts[0].columns, column => {
      if (axisName === column.name) {
        aliasName = column.aliasName || column.displayName;
        value =
          column.type === 'date'
            ? moment
                .utc(value)
                .format(
                  column.dateFormat === 'MMM d YYYY'
                    ? 'MMM DD YYYY'
                    : column.dateFormat === 'MMMM d YYYY, h:mm:ss a'
                      ? 'MMMM DD YYYY, h:mm:ss a'
                      : column.dateFormat
                )
            : value;
        if (
          value &&
          (column.aggregate === 'percentage' || column.aggregate === 'avg')
        ) {
          value =
            value.toFixed(2) + (column.aggregate === 'percentage' ? '%' : '');
        }
        value = value === 'Undefined' ? '' : value;
      }
    });
    return { aliasName, value };
  }

  trimKeyword(data) {
    const trimData = data.map(row => {
      const obj = {};
      for (const key of Object.keys(row)) {
        const trimKey = this.fetchColumnData(key.split('.')[0], row[key]);
        obj[trimKey.aliasName] = trimKey.value;
      }
      return obj;
    });
    return trimData;
  }

  onRefreshData() {
    const payload = this.generatePayload(this.analysis);
    return this.analyzeService
      .getDataBySettings(payload, EXECUTION_MODES.LIVE)
      .then(
        ({ data }) => {
          const parsedData = flattenChartData(data, payload.sqlBuilder);
          if (this.ViewMode) {
            this.chartToggleData = this.trimKeyword(parsedData);
          }
          return parsedData || [];
        },
        err => {
          throw err;
        }
      );
  }

  generatePayload(source) {
    const payload = clone(source);

    set(
      payload,
      'sqlBuilder.filters',
      map(this.filters, this.filterService.frontend2BackendFilter())
    );

    const g = find(
      this.settings.groupBy,
      gf => (gf.area || gf.checked) === 'g'
    );
    const x = find(this.settings.xaxis, xf => (xf.area || xf.checked) === 'x');
    const y = filter(
      this.settings.yaxis,
      yf => (yf.area || yf.checked) === 'y'
    );
    const z = find(this.settings.zaxis, zf => (zf.area || zf.checked) === 'z');

    const allFields = [x, g, ...y, z];

    let nodeFields = filter(allFields, this.isNodeField);
    const dataFields = filter(allFields, this.isDataField);

    if (payload.chartType === 'scatter') {
      const xFields = remove(
        dataFields,
        ({ checked, area }) => (area || checked) === 'x'
      );
      nodeFields = concat(xFields, nodeFields);
    }

    forEach(dataFields, field => {
      if (!field.aggregate) {
        field.aggregate = 'sum';
      }
    });

    set(payload, 'sqlBuilder.dataFields', dataFields);
    set(payload, 'sqlBuilder.nodeFields', nodeFields);

    delete payload.supports;
    set(payload, 'sqlBuilder.sorts', this.sorts);
    set(
      payload,
      'sqlBuilder.booleanCriteria',
      this.analysis.sqlBuilder.booleanCriteria
    );
    set(payload, 'xAxis', { title: this.labels.x });
    set(payload, 'yAxis', { title: this.labels.y });
    set(payload, 'legend', {
      align: this.legend.align,
      layout: this.legend.layout
    });

    return payload;
  }

  getLegend(): Array<any> {
    const align = this.chartService.LEGEND_POSITIONING[this.legend.align];
    const layout = this.chartService.LAYOUT_POSITIONS[this.legend.layout];

    if (!align || !layout) {
      return [];
    }

    return [
      {
        path: 'legend.align',
        data: align.align
      },
      {
        path: 'legend.verticalAlign',
        data: align.verticalAlign
      },
      {
        path: 'legend.layout',
        data: layout.layout
      }
    ];
  }

  exportGridToExcel() {
    if (!isEmpty(this.chartToggleData)) {
      this.dataGrid.instance.exportToExcel(false);
    }

  }

  customizeColumns(columns) {
    forEach(columns, (col: ReportGridField) => {
      col.alignment = 'left';
    });
  }
}
