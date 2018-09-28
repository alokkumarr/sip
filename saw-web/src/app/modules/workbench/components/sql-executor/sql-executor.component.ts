
import { Component, ViewChild, OnInit, OnDestroy } from '@angular/core';
import { MatDialog } from '@angular/material';
import { Router } from '@angular/router';

import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { ToastService } from '../../../../common/services/toastMessage.service';

import * as endsWith from 'lodash/endsWith';

import { SQL_AQCTIONS } from '../../sample-data';

import { SqlScriptComponent } from './query/sql-script.component';
import { DetailsDialogComponent } from './dataset-details-dialog/details-dialog.component';
import { WorkbenchService } from '../../services/workbench.service';

const template = require('./sql-executor.component.html');
const style = require('./sql-executor.component.scss');

@Component({
  selector: 'sql-executor',
  template,
  styles: [
    `:host {
      'class': 'sql-executor'
    }`,
    style
  ]
})
export class SqlExecutorComponent implements OnInit, OnDestroy {
  private artifacts = [];
  private gridConfig: Array<any>;
  private gridData = new BehaviorSubject([]);
  private dsMetadata: any;
  private datasetDetails: Array<any>;
  private appliedActions: Array<any> = SQL_AQCTIONS;
  private scriptHeight = 100;
  private previewHeight = 0;
  private query = '';

  constructor(
    private router: Router,
    public dialog: MatDialog,
    private workBench: WorkbenchService,
    private notify: ToastService
  ) { }

  @ViewChild('sqlscript') private scriptComponent: SqlScriptComponent;

  ngOnInit() {
    this.getPageData();
  }

  ngOnDestroy() {
    this.workBench.removeDataFromLS('dsMetadata');
  }

  getPageData(): void {
    this.dsMetadata = this.workBench.getDataFromLS('dsMetadata');
    this.constructArtifactForEditor();
    // this.workBench.getDatasetDetails('this.datasetID').subscribe(data => {
    //   this.artifacts = data.artifacts;
    // });
  }

  constructArtifactForEditor() {
    const table = {
      artifactName: this.dsMetadata.system.name,
      columns: this.dsMetadata.schema.fields
    };
    this.artifacts.push(table);
  }

  runScript(): void {
    this.scriptComponent.executeQuery();
  }

  sendDataToPreview(data) {
    this.gridData.next(data);
  }

  getQuery(data) {
    this.query = data;
  }

  openSaveDialog() {
    const detailsDialogRef = this.dialog.open(DetailsDialogComponent, {
      hasBackdrop: false,
      width: '400px',
      height: '300px'
    });

    detailsDialogRef
      .afterClosed()
      .subscribe(data => {
        if (data !== false) {
          this.datasetDetails = data;
          this.scriptComponent.onCreateEmitter();
          this.triggerSQL(data);
        }
      });
  }

  toggleViewMode(fullScreenPreview) {
    this.previewHeight = fullScreenPreview ? 100 : 60;
    this.scriptHeight = fullScreenPreview ? 0 : 40;
  }


  /**
   * Constructs the payload for SQL executor component.
   *
   * @param {any} data
   * @memberof SqlExecutorComponent
   */
  triggerSQL(data) {
    /**
     * Temporary workaround to construct user friendly SQL script.
     * Will be handled in BE in upcoming release
     */
    const appendedScript = `CREATE TABLE ${data.name} AS ${this.query}`;
    const script = endsWith(appendedScript, ';') === true ? `${appendedScript}` : `${appendedScript};`;
    const payload = {
      'name': data.name,
      'input': this.dsMetadata.system.name,
      'component': 'sql',
      'configuration': {
        'script': script
      }
    };
    this.workBench.triggerParser(payload).subscribe(data => {
      this.notify.info('SQL_Executor_triggered_successfully', 'Creating Dataset', { hideDelay: 9000 });
    });
    this.router.navigate(['workbench' , 'dataobjects']);
  }

  previewAction(action) {
    this.scriptComponent.viewAction(action);
  }

  backToDS() {
    this.router.navigate(['workbench', 'dataobjects']);
  }
}
