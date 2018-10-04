import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  ElementRef
} from '@angular/core';
import { MatDialog, MatSidenav } from '@angular/material';
import * as html2pdf from 'html2pdf.js';
import { ActivatedRoute, Router, NavigationEnd } from '@angular/router';

import { saveAs } from 'file-saver/FileSaver';
import { Dashboard } from '../../models/dashboard.interface';
import { ConfirmDialogComponent } from '../dialogs/confirm-dialog/confirm-dialog.component';
import { CreateDashboardComponent } from '../create-dashboard/create-dashboard.component';
import { DashboardService } from '../../services/dashboard.service';
import { GlobalFilterService } from '../../services/global-filter.service';
import { ObserveService } from '../../services/observe.service';
import { JwtService } from '../../../../../login/services/jwt.service';
import {
  ConfigService,
  PREFERENCES
} from '../../../../common/services/configuration.service';
import { dataURItoBlob } from '../../../../common/utils/dataURItoBlob';

import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';
import { flatMap } from 'rxjs/operators';
import 'rxjs/add/observable/of';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/catch';
import * as get from 'lodash/get';
import * as filter from 'lodash/filter';

const template = require('./observe-view.component.html');

function downloadDataUrlFromJavascript(filename, dataUrl) {
  const blob = dataURItoBlob(dataUrl);
  saveAs(blob, filename);
}

@Component({
  selector: 'observe-view',
  template,
  providers: [DashboardService, GlobalFilterService]
})
export class ObserveViewComponent implements OnInit, OnDestroy {
  private dashboardId: string;
  private subCategoryId: string;
  private dashboard: Dashboard;
  public requester = new BehaviorSubject({});
  private listeners: Array<Subscription> = [];
  private hasAutoRefresh: boolean = false;
  private shouldAutoRefresh: boolean = true;
  private isDefault = false;
  private privileges = {
    create: false,
    delete: false,
    edit: false
  };
  @ViewChild('filterSidenav') sidenav: MatSidenav;
  @ViewChild('downloadContainer') downloadContainer: ElementRef;
  hasKPIs: boolean = false;

  constructor(
    public dialog: MatDialog,
    private observe: ObserveService,
    private dashboardService: DashboardService,
    private router: Router,
    private filters: GlobalFilterService,
    private configService: ConfigService,
    private jwt: JwtService,
    private _route: ActivatedRoute
  ) {
    const navigationListener = this.router.events.subscribe((e: any) => {
      if (e instanceof NavigationEnd) {
        this.initialise();
      }
    });

    this.listeners.push(navigationListener);
  }

  ngOnInit() {}

  initialise() {
    const snapshot = this._route.snapshot;
    const { subCategory } = snapshot.params;
    const { dashboard } = snapshot.queryParams;
    this.dashboardId = dashboard;
    this.subCategoryId = subCategory;
    this.dashboard = null;

    this.loadPrivileges();
    if (this.dashboardId) {
      this.loadDashboard().subscribe(() => {
        this.startAutoRefresh();
      });
    }

    this.checkDefaultDashboard();
  }

  ngOnDestroy() {
    this.dashboardService.unsetAutoRefresh(
      (this.dashboard || { entityId: null }).entityId
    );
    this.listeners.forEach(l => l.unsubscribe());
  }

  toggleDefault() {
    this.isDefault = !this.isDefault;
    const payload = [
      {
        key: PREFERENCES.DEFAULT_DASHBOARD,
        value: this.dashboardId
      },
      {
        key: PREFERENCES.DEFAULT_DASHBOARD_CAT,
        value: this.subCategoryId
      }
    ];

    const action = this.isDefault
      ? this.configService.saveConfig(payload)
      : this.configService.deleteConfig(payload, false);

    action.subscribe(
      () => this.checkDefaultDashboard(),
      () => this.checkDefaultDashboard()
    );
  }

  /**
   * Checks if current dashboard is set as default dashboard
   *
   * @returns {undefined}
   */
  checkDefaultDashboard() {
    this.isDefault =
      this.configService.getPreference(PREFERENCES.DEFAULT_DASHBOARD) ===
      this.dashboardId;
  }

  stopAutoRefresh() {
    this.dashboardService.unsetAutoRefresh(this.dashboard.entityId);
  }

  modifyAutoRefresh({ checked }) {
    if (checked) {
      this.startAutoRefresh();
      this.refreshDashboard();
    } else {
      this.stopAutoRefresh();
    }
  }

  startAutoRefresh() {
    if (!this.dashboard.autoRefreshEnabled) {
      this.hasAutoRefresh = false;
      this.shouldAutoRefresh = false;
      this.dashboardService.unsetAutoRefresh(this.dashboard.entityId);
      return;
    }

    this.hasAutoRefresh = true;
    this.shouldAutoRefresh = true;
    this.dashboardService.setAutoRefresh(this.dashboard);
    const autoRefreshListener = this.dashboardService
      .getAutoRefreshSubject(this.dashboard.entityId)
      .subscribe(({ dashboardId }) => {
        if (dashboardId !== this.dashboard.entityId) return;
        this.refreshDashboard();
      });
    this.listeners.push(autoRefreshListener);
  }

  refreshDashboard() {
    this.requester.next({ action: 'refresh' });
  }

  confirmDelete() {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        message: 'Are you sure you want to delete this dashboard?',
        actionButton: 'Delete',
        actionColor: 'warn'
      }
    });

    dialogRef.afterClosed().subscribe((result: Boolean) => {
      if (!result) {
        return;
      }

      this.deleteDashboard();
    });
  }

  deleteDashboard(): void {
    const dashboardId = this.dashboard.entityId;
    this.observe
      .deleteDashboard(this.dashboard)
      .pipe(
        flatMap(() => {
          if (
            this.configService.getPreference(PREFERENCES.DEFAULT_DASHBOARD) ===
            dashboardId
          ) {
            return this.configService.deleteConfig(
              [
                {
                  key: PREFERENCES.DEFAULT_DASHBOARD,
                  value: dashboardId
                }
              ],
              true
            );
          } else {
            return Observable.of(true);
          }
        })
      )
      .subscribe(() => {
        this.observe.reloadMenu().subscribe(menu => {
          this.observe.updateSidebar(menu);
          this.observe.redirectToFirstDash(menu, true);
        });
      });
  }

  downloadDashboard() {
    const FILE_NAME = this.dashboard.name;
    const elem = this.downloadContainer.nativeElement.getElementsByTagName(
      'gridster'
    )[0];

    /* Set overflow to visible manually to fix safari's bug. Without this,
     * safari downloads a blank image */
    const overflow = elem.style.overflow;
    elem.style.overflow = 'visible';

    html2pdf()
      .from(elem)
      .set({
        margin: 1,
        filename: `${FILE_NAME}.pdf`,
        image: { type: 'jpeg', quality: 1 },
        html2canvas: {
          backgroundColor: '#f4f5f4',
          scale: 2,
          width: elem.scrollWidth,
          height: elem.scrollHeight,
          windowWidth: elem.scrollWidth,
          windowHeight: elem.scrollHeight
        },
        jsPDF: {
          unit: 'px',
          orientation: 'landscape',
          format: [elem.scrollHeight + 50, elem.scrollWidth]
        }
      })
      // .save(); // comment this and uncomment following lines if png is needed instead of pdf
      .outputImg('datauristring')
      .then(uri => downloadDataUrlFromJavascript(`${FILE_NAME}.png`, uri))
      .then(() => {
        elem.style.overflow = overflow;
      });
  }

  editDashboard(): void {
    this.dialog.open(CreateDashboardComponent, {
      panelClass: 'full-screen-dialog',
      data: {
        dashboard: this.dashboard,
        mode: 'edit'
      },
      maxWidth: '1600px'
    });
  }

  createDashboard(): void {
    this.dialog.open(CreateDashboardComponent, {
      panelClass: 'full-screen-dialog',
      data: {
        mode: 'create'
      },
      maxWidth: '1600px'
    });
  }

  /**
   * Pushes a new event to global filter service which
   * individual analyses are listening to. This triggers
   * updates in those analyses.
   *
   * Closes the sidebar.
   *
   * @param {any} globalFilters
   * @returns {void}
   * @memberof ObserveViewComponent
   */
  onApplyGlobalFilter(data): void {
    if (!data) {
      this.sidenav.close();
      return;
    }

    this.filters.onApplyFilter.next(data.analysisFilters);
    this.filters.onApplyKPIFilter.next(data.kpiFilters);
    this.sidenav.close();
  }

  loadDashboard(): Observable<Dashboard> {
    return this.observe
      .getDashboard(this.dashboardId)
      .map((data: Dashboard) => {
        this.dashboard = data;
        this.loadPrivileges();
        this.checkForKPIs();
        return data;
      })
      .catch(error => {
        return Observable.of(error);
      });
  }

  /**
   * checkForKPIs - Checks if dashboard has any KPI tiles
   *
   * @returns {void}
   */
  checkForKPIs(): void {
    const tiles = get(this.dashboard, 'tiles', []);
    const kpis = filter(tiles, t => t.type === 'kpi');
    this.hasKPIs = kpis && kpis.length > 0;
  }

  filterSidenavStateChange(data) {
    this.filters.onSidenavStateChange.next(data);
  }

  loadPrivileges(): void {
    if (!this.subCategoryId) {
      return;
    }

    const moduleName = 'OBSERVE';

    this.privileges.create = this.jwt.hasPrivilege('CREATE', {
      module: moduleName,
      subCategoryId: this.subCategoryId
    });

    if (!this.dashboard) {
      return;
    }

    this.privileges.edit = this.jwt.hasPrivilege('EDIT', {
      module: moduleName,
      subCategoryId: this.subCategoryId,
      creatorId: this.dashboard.createdBy
    });

    this.privileges.delete = this.jwt.hasPrivilege('DELETE', {
      module: moduleName,
      subCategoryId: this.subCategoryId,
      creatorId: this.dashboard.createdBy
    });
  }
}
