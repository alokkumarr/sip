import {
  Component,
  Inject,
  ViewChild,
  OnDestroy,
  AfterContentInit
} from '@angular/core';
import {
  MatDialogRef,
  MAT_DIALOG_DATA,
  MatDialog,
  MatSidenav
} from '@angular/material';
import { Router } from '@angular/router';
import { SaveDashboardComponent } from '../save-dashboard/save-dashboard.component';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { MenuService } from '../../../../common/services/menu.service';
import { WIDGET_ACTIONS } from '../add-widget/widget.model';
import { ObserveService } from '../../services/observe.service';
import { DashboardService } from '../../services/dashboard.service';
import { Dashboard } from '../../models/dashboard.interface';
import { animations } from './create-dashboard.animations';

import { Subscription } from 'rxjs/Subscription';
import * as forEach from 'lodash/forEach';
import * as isEmpty from 'lodash/isEmpty';
import * as get from 'lodash/get';
import { GlobalFilterService } from '../../services/global-filter.service';

const template = require('./create-dashboard.component.html');
const style = require('./create-dashboard.component.scss');

const MARGIN_BETWEEN_TILES = 10;

@Component({
  selector: 'create-dashboard',
  template,
  styles: [style],
  animations,
  providers: [DashboardService, GlobalFilterService]
})
export class CreateDashboardComponent implements OnDestroy, AfterContentInit {
  public fillState = 'empty';
  public dashboard: Dashboard;
  public requester = new BehaviorSubject({});
  public mode = 'create';
  public sidebarWidget: string;
  public editItem: any;

  editSubscription: Subscription;

  @ViewChild('widgetChoice') widgetSidenav: MatSidenav;

  constructor(
    public dialogRef: MatDialogRef<CreateDashboardComponent>,
    private dialog: MatDialog,
    private router: Router,
    private menu: MenuService,
    private globalFilterService: GlobalFilterService,
    private dashboardService: DashboardService,
    private observe: ObserveService,
    @Inject(MAT_DIALOG_DATA) public dialogData: any
  ) {
    this.dashboard = get(this.dialogData, 'dashboard');
    this.mode = get(this.dialogData, 'mode');
    this.checkEmpty(this.dashboard);
  }

  ngOnDestroy() {
    this.editSubscription && this.editSubscription.unsubscribe();
  }

  ngAfterContentInit() {
    this.subscribeToEdits();
  }

  subscribeToEdits() {
    this.editSubscription = this.dashboardService.onEditItem.subscribe(data => {
      if (isEmpty(data)) { return; }

      this.sidebarWidget = 'edit';
      this.editItem = data;

      this.widgetSidenav.open();
    });
  }

  openFilters() {
    this.sidebarWidget = 'filter';
    this.widgetSidenav.open();
  }

  checkEmpty(dashboard) {
    this.fillState =
      get(dashboard, 'tiles', []).length > 0 ? 'filled' : 'empty';
  }

  onDashboardChange(data) {
    if (data.changed) {
      this.checkEmpty(data.dashboard);
      this.updateWidgetLog(data.dashboard);
    } else if (data.save) {
      this.openSaveDialog(data.dashboard);
    }
  }

  sidenavStateChange(data) {
    if (this.sidebarWidget === 'filter') {
      this.globalFilterService.onSidenavStateChange.next(data);
    }
  }

  onApplyGlobalFilter(data): void {
    if (!data) {
      this.widgetSidenav.close();
      return;
    }

    this.globalFilterService.onApplyFilter.next(data.analysisFilters);
    this.globalFilterService.onApplyKPIFilter.next(data.kpiFilters);
    this.widgetSidenav.close();
  }

  updateWidgetLog(dashboard) {
    const newLog = {};
    forEach(dashboard.tiles, tile => {
      newLog[tile.id] = { type: tile.type };
    });

    this.dashboardService.dashboardWidgets.next(newLog);
  }

  ngOnInit() {}

  exitCreator(data) {
    this.dialogRef.close(data);
  }

  chooseAnalysis() {
    this.sidebarWidget = 'add';
    this.widgetSidenav.open();
  }

  onAnalysisAction(action, data) {
    /* prettier-ignore */
    switch (action) {
    case 'ADD':
      if (!data) { return; }
      const item = {
        cols: 16,
        rows: 16,
        analysis: data,
        updater: new BehaviorSubject({})
      };
      this.requester.next({ action: 'add', data: item });
      break;
    case 'REMOVE':
      if (!data) { return; }
      this.requester.next({ action: 'remove', data });
      break;
    }
  }

  onKPIAction(action, data) {
    /* prettier-ignore */
    switch (action) {
    case WIDGET_ACTIONS.ADD:
      if (!data) { return; }

      const item = {
        cols: 13,
        rows: 6,
        kpi: data
      };
      this.requester.next({action: 'add', data: item});
      break;
    }
  }

  onBulletAction(action, data) {
    /* prettier-ignore */
    switch (action) {
    case WIDGET_ACTIONS.ADD:
      if (!data) { return; }

      const item = {
        cols: 20,
        rows: 6,
        bullet: data,
        updater: new BehaviorSubject({})
      };
      this.requester.next({action: 'add', data: item});
      break;
    }
  }

  onWidgetAction({ widget, action, data }) {
    /* prettier-ignore */
    switch (widget) {
    case 'ANALYSIS':
      this.onAnalysisAction(action, data);
      break;
    case 'KPI':
      this.onKPIAction(action, data);
      break;
    case 'BULLET':
      this.onBulletAction(action, data);
      break;
    }
  }

  saveDashboard() {
    this.requester.next({ action: 'get' });
  }

  openSaveDialog(dashboard: Dashboard) {
    const dialogRef = this.dialog.open(SaveDashboardComponent, {
      data: {
        dashboard,
        mode: this.mode
      }
    });

    dialogRef.afterClosed().subscribe((result: Dashboard) => {
      if (result) {
        this.dialogRef.afterClosed().subscribe(() => {
          this.updateSideMenu(result);
          this.router.navigate(
            ['observe', result.categoryId],
            {queryParams: {dashboard: result.entityId}}
          );
        });
        this.dialogRef.close();
      }
    });
  }

  /* After successful save, update the sidemenu with the dashboard. This saves a network
     request because we already have all the data available to us. */
  updateSideMenu(dashboard: Dashboard) {
    this.observe.reloadMenu().subscribe(menu => {
      this.observe.updateSidebar(menu);
    });
  }
}
