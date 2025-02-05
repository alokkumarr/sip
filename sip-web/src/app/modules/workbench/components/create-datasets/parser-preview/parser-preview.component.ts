import {
  Component,
  Input,
  OnInit,
  OnDestroy,
  AfterViewInit,
  ViewChild,
  EventEmitter,
  Output
} from '@angular/core';
import { BehaviorSubject } from 'rxjs';

import * as set from 'lodash/set';
import * as get from 'lodash/get';
import * as map from 'lodash/map';
import * as replace from 'lodash/replace';
import * as cloneDeep from 'lodash/cloneDeep';
import * as assign from 'lodash/assign';
import * as has from 'lodash/has';
import * as take from 'lodash/take';
import * as isUndefined from 'lodash/isUndefined';

import { MatDialog } from '@angular/material';
import { DxDataGridComponent } from 'devextreme-angular/ui/data-grid';

import { DxDataGridService } from '../../../../../common/services/dxDataGrid.service';
import { DateformatDialogComponent } from '../dateformat-dialog/dateformat-dialog.component';
import { WorkbenchService } from '../../../services/workbench.service';

@Component({
  selector: 'parser-preview',
  templateUrl: './parser-preview.component.html',
  styleUrls: ['./parser-preview.component.scss']
})
export class ParserPreviewComponent
  implements OnInit, OnDestroy, AfterViewInit {
  @Input() previewObj: BehaviorSubject<any>;
  public gridListInstance: any;
  public previewgridConfig: Array<any>;
  public updaterSubscribtion: any;
  public fieldInfo = [];
  public parserData: any;
  public rawFile: any;
  public inspectError = false;
  public errMsg = '';
  private dateDialogRef;

  constructor(
    public dxDataGrid: DxDataGridService,
    public dialog: MatDialog,
    public workBench: WorkbenchService
  ) {}

  @Output() parserConfig: EventEmitter<any> = new EventEmitter<any>();

  @ViewChild(DxDataGridComponent, { static: false })
  dataGrid: DxDataGridComponent;

  ngOnInit() {
    this.previewgridConfig = this.getPreviewGridConfig();
    setTimeout(() => {
      this.updaterSubscribtion = this.previewObj.subscribe(data => {
        this.onUpdate(data);
      });
    }, 100);
  }

  ngOnDestroy() {
    this.updaterSubscribtion.unsubscribe();
  }

  onUpdate(data) {
    if (data.length === 2 && !isUndefined(data[0].samplesParsed)) {
      this.parserData = cloneDeep(data[0]);
      const parsedData = data[0].samplesParsed;
      this.fieldInfo = cloneDeep(data[0].fields);
      setTimeout(() => {
        this.dataGrid.instance.beginCustomLoading('Loading...');
        this.reloadDataGrid(parsedData);
      });
    } else if (data.length !== 0 && !isUndefined(data[0].error)) {
      this.inspectError = true;
      this.errMsg = data[0].error.message;
    } else if (data.length === 0) {
      setTimeout(() => {
        this.dataGrid.instance.beginCustomLoading('Loading...');
        this.reloadDataGrid([]);
      });
    }
    if (data.length === 2 && !isUndefined(data[1])) {
      this.rawPreview(data[1]);
    }
  }

  ngAfterViewInit() {
    this.dataGrid.instance.option(this.previewgridConfig);
  }

  getPreviewGridConfig() {
    const dataSource = [];
    return this.dxDataGrid.mergeWithDefaultConfig({
      dataSource,
      columnAutoWidth: false,
      wordWrapEnabled: false,
      searchPanel: {
        visible: false,
        width: 240,
        placeholder: 'Search...'
      },
      height: '100%',
      width: '100%',
      filterRow: {
        visible: true,
        applyFilter: 'auto'
      },
      headerFilter: {
        visible: false
      },
      sorting: {
        mode: 'none'
      },
      export: {
        fileName: 'Parsed_Sample'
      },
      scrolling: {
        showScrollbar: 'always',
        mode: 'virtual',
        useNative: false
      },
      showRowLines: false,
      showBorders: false,
      rowAlternationEnabled: true,
      showColumnLines: true,
      selection: {
        mode: 'none'
      },
      customizeColumns: columns => {
        for (const column of columns) {
          column.headerCellTemplate = 'headerTemplate';
        }
      }
    });
  }

  reloadDataGrid(data) {
    this.dataGrid.instance.option('dataSource', data);
    this.dataGrid.instance.refresh();
    this.dataGrid.instance.endCustomLoading();
  }

  checkDate(event) {
    const ftype = event.srcElement.value;
    if (ftype === 'date') {
      this.openDateFormatDialog(event);
    } else {
      const formatEdit = event.currentTarget.nextElementSibling;
      formatEdit.style.visibility = 'hidden';
      const index = event.srcElement.id;
      if (has(this.parserData.fields[index], 'format')) {
        set(
          this.fieldInfo[index],
          'format',
          get(this.parserData.fields[index], 'format')
        );
      }
    }
  }
  /*set the user provided date format for the Field. If the format is empty revert back the field type to original. */
  openDateFormatDialog(event) {
    let index: number;
    let dateformat = '';
    let formatArr = [];
    if (event.type === 'click') {
      index = replace(event.target.id, 'edit_', '');
    } else {
      index = event.srcElement.id;
    }
    if (has(this.fieldInfo[index], 'format')) {
      if (this.fieldInfo[index].format.length > 1) {
        formatArr = [this.fieldInfo[index].format]; // Setting the formaArr as array. Added as part of SIP-9427.
      } else {
        dateformat = get(this.fieldInfo[index], 'format[0]');
      }
    }

    // Making sure multiple dialogs are not opened when edit icon is clicked.
    if (this.dateDialogRef) {
      return;
    }
    this.dateDialogRef = this.dialog.open(DateformatDialogComponent, {
      hasBackdrop: false,
      data: {
        placeholder: 'Enter date format',
        format: dateformat,
        formatArr: formatArr
      }
    });
    this.dateDialogRef.afterClosed().subscribe(format => {
      let id = -1;
      if (event.type === 'click') {
        id = replace(event.target.id, 'edit_', '');
      } else {
        id = event.srcElement.id;
      }
      if (format === '' && has(this.parserData.fields[id], 'format')) {
        if (
          this.parserData.fields[id].format.length === 0 ||
          this.parserData.fields[id].format.length > 1
        ) {
          event.srcElement.value = get(this.parserData.fields[id], 'type');
        } else if (this.parserData.fields[id].format.length === 1) {
          set(
            this.fieldInfo[id],
            'format[0]',
            get(this.parserData.fields[id], 'format[0]')
          );
        }
      } else if (format === '' && !has(this.parserData.fields[id], 'format')) {
        event.srcElement.value = get(this.parserData.fields[id], 'type');
      } else if (format !== '') {
        set(this.fieldInfo[id], 'format', [format]);
      }
      this.dateDialogRef = null;
    });
  }

  toAdd() {
    const fieldsObj = map(this.fieldInfo, obj => {
      if (obj.format) {
        obj.format = obj.format[0]; // Parser component expects format to be a string.
      }
      return obj;
    });
    const config = assign(this.parserData, { fields: fieldsObj });
    this.parserConfig.emit(config);
  }

  errorInfoVisibility(index) {
    if (get(this.fieldInfo[index], 'format').length > 1) {
      return 'visible';
    }
    return 'hidden';
  }

  rawPreview(filePath) {
    this.workBench.getRawPreviewData(filePath).subscribe(data => {
      this.rawFile = take(data.data, 50);
    });
  }
}
