import {
  Component,
  Input,
  Output,
  OnInit,
  OnDestroy,
  EventEmitter,
  ViewChild,
  ElementRef
} from '@angular/core';
import { DxDataGridComponent } from 'devextreme-angular/ui/data-grid';
import * as fpPipe from 'lodash/fp/pipe';
import * as fpFlatMap from 'lodash/fp/flatMap';
import * as fpMap from 'lodash/fp/map';
import * as reduce from 'lodash/reduce';
import * as isUndefined from 'lodash/isUndefined';
import * as forEach from 'lodash/forEach';
import * as split from 'lodash/split';
import * as isFunction from 'lodash/isFunction';
import * as isEmpty from 'lodash/isEmpty';
import { MatDialog, MatDialogConfig } from '@angular/material';
import CustomStore from 'devextreme/data/custom_store';
import { DateFormatDialogComponent } from '../date-format-dialog';
import { DataFormatDialogComponent } from '../data-format-dialog';
import { AliasRenameDialogComponent } from '../alias-rename-dialog';
import { getFormatter } from '../../utils/numberFormatter';
import { Subscription, BehaviorSubject } from 'rxjs';
import * as filter from 'lodash/filter';
import * as map from 'lodash/map';

import { AGGREGATE_TYPES, AGGREGATE_TYPES_OBJ } from '../../consts';

import {
  ArtifactColumnReport,
  Artifact,
  Sort,
  ReportGridChangeEvent
} from './types';
import { DATE_TYPES, NUMBER_TYPES } from '../../../modules/analyze/consts';
import { DEFAULT_PRECISION } from '../data-format-dialog/data-format-dialog.component';

import { flattenReportData } from '../../../common/utils/dataFlattener';

interface ReportGridSort {
  order: 'asc' | 'desc';
  index: number;
}

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

const DEFAULT_PAGE_SIZE = 25;
const LOAD_PANEL_POSITION_SELECTOR = '.report-dx-grid';

@Component({
  selector: 'report-grid-upgraded',
  templateUrl: './report-grid.component.html',
  styleUrls: ['./report-grid.component.scss']
})
export class ReportGridComponent implements OnInit, OnDestroy {
  public columns: ReportGridField[];
  public data;
  private listeners: Array<Subscription> = [];
  @Output() change: EventEmitter<ReportGridChangeEvent> = new EventEmitter();
  @ViewChild(DxDataGridComponent) dataGrid: DxDataGridComponent;
  @Input() query: string;
  @Input() isInQueryMode;
  @Input() analysis;
  @Input() dimensionChanged: BehaviorSubject<any>;
  @Input('sorts')
  set setSorts(sorts: Sort[]) {
    this.sorts = reduce(
      sorts,
      (acc, sort, index) => {
        const reportGirdSort: ReportGridSort = {
          order: sort.order,
          index
        };
        acc[sort.columnName] = reportGirdSort;
        return acc;
      },
      {}
    );
  }
  @Input('artifacts')
  set setArtifactColumns(artifacts: Artifact[]) {
    if (!artifacts) {
      this.artifacts = null;
      this.columns = null;
      return;
    }
    this.artifacts = artifacts;
    this.columns = this.artifacts2Columns(this.artifacts);
    // if there are less then 5 columns, divide the grid up into equal slices for the columns
    if (this.columns.length > 5) {
      this.columnAutoWidth = true;
    }

    if (isEmpty(this.columns)) {
      this.columns = null;
    }
  }
  @Input('queryColumns')
  set setQueryColumns(queryColumns) {
    // TODO merge with SAW - 2002 for queryColumns
    // for query mode
    this.columns = this.queryColumns2Columns(queryColumns);
  }
  @Input('data')
  set setData(data: any[]) {
    if (data || data.length < 7) {
      this.gridHeight = 'auto';
    } else {
      this.gridHeight = '100%';
    }

    if (!this.isInQueryMode) {
      const artifact = this.fetchColumsUponCheck();
      if (!this.artifacts) {
        return;
      }
      this.columns = this.artifacts2Columns(artifact);
    }
    this.data = flattenReportData(data, this.analysis);
  }
  @Input('dataLoader')
  set setDataLoader(
    dataLoader: (options: {}) => Promise<{ data: any[]; totalCount: number }>
  ) {
    // setup pagination for paginated data
    if (isFunction(dataLoader)) {
      this.dataLoader = dataLoader;
      this.data = new CustomStore({
        load: options => this.dataLoader(options)
      });
      this.remoteOperations = { paging: true };
      /* Reset pager after a new dataLoader is set */
      this.paging = { pageSize: DEFAULT_PAGE_SIZE, pageIndex: 0 };
    } else {
      throw new Error('Data loader requires a Function');
    }
  }
  @Input() isEditable = false;
  @Input() columnHeaders;

  public dataLoader: (options: {}) => Promise<{
    data: any[];
    totalCount: number;
  }>;

  public sorts: {};
  public artifacts: Artifact[];
  public pageSize: number = DEFAULT_PAGE_SIZE;
  public isColumnChooserListenerSet = false;
  public onLoadPanelShowing: Function;

  // grid settings
  public columnAutoWidth = false;
  public columnMinWidth = 172;
  public columnResizingMode = 'widget';
  public allowColumnReordering = true;
  public allowColumnResizing = true;
  public showColumnHeaders = true;
  public showColumnLines = false;
  public showRowLines = false;
  public showBorders = false;
  public rowAlternationEnabled = true;
  public hoverStateEnabled = true;
  public wordWrapEnabled = false;
  public scrolling = { mode: 'Virtual' };
  public sorting = { mode: 'multiple' };
  public columnChooser;
  public gridWidth = '100%';
  public gridHeight: '100%' | 'auto' = '100%';
  public remoteOperations;
  public paging;
  public pager = {
    showNavigationButtons: true,
    allowedPageSizes: [DEFAULT_PAGE_SIZE, 50, 75, 100],
    showPageSizeSelector: true
  };
  public loadPanel;
  public AGGREGATE_TYPES_OBJ = AGGREGATE_TYPES_OBJ;
  public aggregates;
  public isQueryMode;

  constructor(private _dialog: MatDialog, public _elemRef: ElementRef) {
    this.onLoadPanelShowing = ({ component }) => {
      const instance = this.dataGrid.instance;
      if (instance) {
        const elem =
          this.pageSize > DEFAULT_PAGE_SIZE
            ? window
            : this._elemRef.nativeElement.querySelector(
                LOAD_PANEL_POSITION_SELECTOR
              );
        component.option('position.of', elem);
        this.pageSize = instance.pageSize();
      }
    };
    this.customizeColumns = this.customizeColumns.bind(this);
  }

  ngOnInit() {
    if (this.dimensionChanged) {
      this.listeners.push(this.subscribeForRepaint());
    }
    // disable editing if needed
    if (!this.isEditable) {
      this.columnChooser = {
        enabled: isUndefined(this.columnHeaders) ? true : this.columnHeaders,
        mode: 'select'
      };

      // paging is used in situations where the grid is not editable
      this.loadPanel = {
        onShowing: this.onLoadPanelShowing,
        position: {
          of: this._elemRef.nativeElement.querySelector(
            LOAD_PANEL_POSITION_SELECTOR
          ),
          at: 'center',
          my: 'center'
        }
      };
    }
  }

  ngOnDestroy() {
    this.listeners.forEach(sub => sub.unsubscribe());
  }

  isAggregateEligible() {
    return filter(AGGREGATE_TYPES, aggregate => {
      if (aggregate.valid.includes(this.analysis.type)) {
        return true;
      }
    });
  }

  onContentReady({ component }) {
    if (this.isEditable) {
      this.updateVisibleIndices(component);
    } else {
      if (!this.isColumnChooserListenerSet) {
        this.setColumnChooserOptions(component);
        this.isColumnChooserListenerSet = true;
      }
    }
  }

  subscribeForRepaint() {
    return this.dimensionChanged.subscribe(() => {
      this.dataGrid &&
        this.dataGrid.instance &&
        this.dataGrid.instance.repaint();
    });
  }

  customizeColumns(columns) {
    forEach(columns, (col: ReportGridField) => {
      col.allowSorting = !this.isEditable;
      col.alignment = 'left';
    });
  }

  /** Update the visible indices when the column order changes */
  updateVisibleIndices(component) {
    if (!this.columns) {
      return;
    }
    const cols = component.getVisibleColumns();
    let isVisibleIndexChanged = false;
    forEach(cols, (col: ReportGridField) => {
      if (col.visibleIndex !== col.payload.visibleIndex) {
        col.changeColumnProp('visibleIndex', col.visibleIndex);
        isVisibleIndexChanged = true;
      }
      if (isVisibleIndexChanged) {
        // disabled this event so that refreshing data does not fire this event and triggers draft mode
        // TODO find a better way to trigger this, and not on onContentReady
        // currently devextreme has no event for when the columns of the grid get reordered
        // so the onContentReady event was used for that
        // this.change.emit({ subject: 'visibleIndex' });
      }
    });
  }

  /** Column chooser should be closed when a click outside of it appears */
  setColumnChooserOptions(component) {
    const columnChooserView = component.getView('columnChooserView');
    if (!columnChooserView._popupContainer) {
      columnChooserView._initializePopupContainer();
      columnChooserView.render();
      columnChooserView._popupContainer._options.closeOnOutsideClick = true;
    }
  }

  fetchAggregation(type) {
    if (NUMBER_TYPES.includes(type)) {
      this.aggregates = this.isAggregateEligible();
    } else {
      this.aggregates = filter(AGGREGATE_TYPES, t => {
        return t.value === 'count' || t.value.toLowerCase() === 'distinctcount';
      });
    }
  }

  checkFormatDataCondition(type) {
    if (NUMBER_TYPES.includes(type) || DATE_TYPES.includes(type)) {
      return true;
    } else {
      return false;
    }
  }

  aggregateColumn(payload, value) {
    payload.aggregate = value === 'distinctcount' ? 'distinctCount' : value;
    if (value === 'clear') {
      delete payload.aggregate;
    }
    this.change.emit({
      subject: 'aggregate',
      column: payload
    });
  }

  removeColumn({ changeColumnProp, payload }: ReportGridField) {
    changeColumnProp('checked', false);
    this.change.emit({
      subject: 'removeColumn',
      column: payload
    });
  }

  renameColumn({ payload, changeColumnProp }: ReportGridField) {
    this.getNewDataThroughDialog(
      AliasRenameDialogComponent,
      { alias: payload.alias || '' },
      alias => {
        changeColumnProp('alias', alias);
        this.change.emit({ subject: 'alias' });
      }
    );
  }

  formatColumn({ type, changeColumnProp, payload }: ReportGridField) {
    let component;
    if (NUMBER_TYPES.includes(type)) {
      component = DataFormatDialogComponent;
    } else if (DATE_TYPES.includes(type)) {
      component = DateFormatDialogComponent;
    }

    this.getNewDataThroughDialog(
      component,
      { format: payload.format, type },
      format => {
        changeColumnProp('format', format);
        this.change.emit({ subject: 'format' });
      }
    );
  }

  getNewDataThroughDialog(component, currentData, actionFn: Function) {
    this._dialog
      .open(component, {
        width: 'auto',
        height: 'auto',
        data: currentData
      } as MatDialogConfig)
      .afterClosed()
      .subscribe(newValue => {
        if (!isUndefined(newValue)) {
          actionFn(newValue);
        }
      });
  }

  artifacts2Columns(artifacts: Artifact[]): ReportGridField[] {
    return fpPipe(
      fpFlatMap((artifact: Artifact) => artifact.columns || [artifact]),
      fpMap((column: ArtifactColumnReport) => {
        let isNumberType = NUMBER_TYPES.includes(column.type);

        const aggregate =
          AGGREGATE_TYPES_OBJ[
            column.aggregate && column.aggregate.toLowerCase()
          ];
        let type = column.type;
        if (
          aggregate &&
          ['count', 'distinctcount'].includes(
            aggregate.value && aggregate.value.toLowerCase()
          )
        ) {
          type = aggregate.type || column.type;
          isNumberType = true;
        }

        const preprocessedFormat = this.preprocessFormatIfNeeded(
          column.format,
          type,
          column.aggregate
        );
        const format = isNumberType
          ? { formatter: getFormatter(preprocessedFormat) }
          : column.format;
        const field: ReportGridField = {
          caption: column.alias || column.displayName,
          dataField: this.getDataField(column),
          dataType: isNumberType ? 'number' : type,
          type,
          visibleIndex: column.visibleIndex,
          visible: isUndefined(column.visible) ? true : column.visible,
          payload: column,
          format,
          headerCellTemplate: 'headerCellTemplate',
          changeColumnProp: (prop, value) => {
            column[prop] = value;
          },
          ...this.getSortingPart(column)
        };

        if (
          DATE_TYPES.includes(column.type) &&
          isUndefined(column.format) &&
          !aggregate
        ) {
          field.format = 'yyyy-MM-dd';
        }
        return field;
      })
    )(artifacts);
  }

  preprocessFormatIfNeeded(format, type, aggregate) {
    const isPercentage = aggregate === 'percentage';
    const isAVG = aggregate === 'avg';
    if ((!isPercentage && !isAVG) || !NUMBER_TYPES.includes(type)) {
      return format;
    }

    return {
      ...format,
      precision: DEFAULT_PRECISION,
      percentage: isPercentage
    };
  }

  getDataField(column: ArtifactColumnReport) {
    const parts = split(column.columnName, '.');
    return parts[0];
  }

  queryColumns2Columns(queryColumns): ReportGridField[] {
    return [];
  }

  getSortingPart(column: ArtifactColumnReport) {
    const sort = this.sorts[column.columnName];
    if (sort) {
      return {
        sortIndex: sort.index,
        sortOrder: sort.order
      };
    }
    return {};
  }

  fetchColumsUponCheck() {
    return map(this.analysis.artifacts, artifact => {
      const columns = filter(artifact.columns, 'checked');
      return {
        ...artifact,
        columns
      };
    });
  }
}
