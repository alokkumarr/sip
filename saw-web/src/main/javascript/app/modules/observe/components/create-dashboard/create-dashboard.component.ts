import { Component, Inject, ViewChild } from '@angular/core';
import { UIRouter } from '@uirouter/angular';
import { MdDialogRef, MD_DIALOG_DATA, MdDialog } from '@angular/material'; import { SaveDashboardComponent } from '../save-dashboard/save-dashboard.component';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { GridsterConfig, GridsterItem, GridsterComponent } from 'angular-gridster2';
import { MenuService } from '../../../../common/services/menu.service';
import { Dashboard } from '../../models/dashboard.interface';
import {
  trigger,
  state,
  style,
  animate,
  transition
} from '@angular/animations';

import * as forEach from 'lodash/forEach';
import * as find from 'lodash/find';
import * as map from 'lodash/map';
import * as get from 'lodash/get';

import { AnalysisChoiceComponent } from '../analysis-choice/analysis-choice.component';

const template = require('./create-dashboard.component.html');
require('./create-dashboard.component.scss');

const MARGIN_BETWEEN_TILES = 10;

@Component({
  selector: 'create-dashboard',
  template,
  animations: [
    trigger('moveButton', [
      state('empty', style({
        bottom: '50%',
        right: '50%',
        transform: 'translateX(50%)'
      })),
      state('filled', style({
        bottom: '30px',
        right: '30px',
        transform: 'translateX(0%)'
      })),
      transition('* => *', animate('500ms ease-out'))
    ]),
    trigger('hideHelp', [
      state('empty', style({
        opacity: 1
      })),
      state('filled', style({
        opacity: 0
      })),
      transition('* => *', animate('250ms ease-out'))
    ])
  ]
})
export class CreateDashboardComponent {
  @ViewChild('gridster') gridster: GridsterComponent;

  public fillState = 'empty';
  public columns = 4;
  public options: GridsterConfig;
  public dashboard: Array<GridsterItem>;
  public chartUpdater = new BehaviorSubject({});

  constructor(public dialogRef: MdDialogRef<CreateDashboardComponent>,
    private dialog: MdDialog,
    private router: UIRouter,
    private menu: MenuService,
    @Inject(MD_DIALOG_DATA) public layout: any
  ) { }

  checkEmpty() {
    this.fillState = this.dashboard.length > 0 ? 'filled' : 'empty';
  }

  itemChange(item, itemComponent) {
    setTimeout(() => {
      if (this.gridster.columns !== this.columns) {
        this.refreshAllTiles();
      } else {
        this.refreshTile(item);
      }
      this.columns = this.gridster.columns;
    }, 500)
  }

  getDimensions(item) {
    return {
      width: this.gridster.curColWidth * item.cols - MARGIN_BETWEEN_TILES,
      height: this.gridster.curRowHeight * item.rows - MARGIN_BETWEEN_TILES
    };
  }

  refreshTile(item) {
    const dimensions = this.getDimensions(item);
    item.updater.next([
      {path: 'chart.height', data: dimensions.height},
      {path: 'chart.width', data: dimensions.width}
    ])
  }

  refreshAllTiles() {
    forEach(this.dashboard, this.refreshTile.bind(this));
  }

  ngOnInit() {
    this.options = {
      gridType: 'scrollVertical',
      minCols: this.columns,
      maxCols: 100,
      margin: MARGIN_BETWEEN_TILES,
      minRows: 4,
      maxRows: 100,
      itemChangeCallback: this.itemChange.bind(this),
      draggable: {
        enabled: true
      },
      resizable: {
        enabled: true
      }
    };

    this.dashboard = [];
  }

  initializeDashboard() {
  }

  removeTile(item: GridsterItem) {
    this.dashboard.splice(this.dashboard.indexOf(item), 1);
    this.checkEmpty();
  }

  exitCreator(data) {
    this.dialogRef.close(data);
  }

  chooseAnalysis() {
    const dialogRef = this.dialog.open(AnalysisChoiceComponent);

    dialogRef.afterClosed().subscribe(analysis => {
      if (!analysis) {
        return;
      }

      const item = { cols: 1, rows: 1, analysis, updater: new BehaviorSubject({}) };
      this.dashboard.push(item);
      this.checkEmpty();
    });
  }

  prepareDashboardForSave(): Dashboard {
    return {
      entityId: '',
      categoryId: '',
      name: '',
      description: '',
      tiles: map(this.dashboard, tile => ({
        type: 'analysis',
        id: get(tile, 'analysis.id', ''),
        x: tile.x,
        y: tile.y,
        cols: tile.cols,
        rows: tile.rows
      })),
      filters: []
    }
  }

  openSaveDialog() {
    const dialogRef = this.dialog.open(SaveDashboardComponent, {
      data: {
        dashboard: this.prepareDashboardForSave()
      }
    });

    dialogRef.afterClosed().subscribe((result: Dashboard) => {
      if (result) {
        this.dialogRef.afterClosed().subscribe(() => {
          this.updateSideMenu(result);
          this.router.stateService.go('observe.dashboard', {
            dashboard: result.entityId,
            subCategory: result.categoryId
          });
        });
        this.dialogRef.close();
      }
    });
  }

  updateSideMenu(dashboard: Dashboard) {
    const menu = this.menu.getCachedMenu('OBSERVE') || [];
    let subCategory;
    forEach(menu, category => {
      subCategory = find(category.children, subCat => subCat.id.toString() === dashboard.categoryId.toString());
    });

    if (subCategory) {
      subCategory.children = subCategory.children || [];
      subCategory.children.push({
        id: dashboard.entityId,
        name: dashboard.name,
        url: `#!/observe/${dashboard.categoryId}?dashboard=${dashboard.entityId}`,
        data: dashboard
      });
    }

    this.menu.updateMenu(menu, 'OBSERVE');
  }
}
