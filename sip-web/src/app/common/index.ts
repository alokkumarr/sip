import 'devextreme/localization';
import 'devextreme/localization/messages/en.json';
import 'devextreme/ui/data_grid';
import 'mottle';

import { CommonModule as CommonModuleAngular4 } from '@angular/common';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { ClickOutsideModule } from 'ng-click-outside';
import { AngularSplitModule } from 'angular-split';

import {
  NgModule,
  CUSTOM_ELEMENTS_SCHEMA,
  ModuleWithProviders
} from '@angular/core';
import { NgxPopperModule } from 'ngx-popper';
import { FlexLayoutModule } from '@angular/flex-layout';
import { NgxsModule } from '@ngxs/store';
import { CommonState } from './state/common.state';
import { RouterModule } from '@angular/router';
import {
  DxPivotGridComponent,
  DxPivotGridModule
} from 'devextreme-angular/ui/pivot-grid';
import {
  DxDataGridComponent,
  DxDataGridModule
} from 'devextreme-angular/ui/data-grid';
import {
  DxTextBoxModule,
  DxButtonModule,
  DxSliderModule,
  DxTooltipModule,
  DxCheckBoxModule
} from 'devextreme-angular';
import { DxTemplateModule } from 'devextreme-angular/core/template';
import { OwlDateTimeModule, OwlNativeDateTimeModule } from 'ng-pick-datetime';
import { ColorPickerModule } from 'ngx-color-picker';

import { DndModule } from './dnd';
import {
  AddTokenInterceptor,
  HandleErrorInterceptor,
  RefreshTokenInterceptor,
  ProgressIndicatorInterceptor
} from './interceptor';
import { SearchBoxComponent } from './components/search-box';
import {
  IsUserLoggedInGuard,
  DefaultModuleGuard,
  SSOAuthGuard
} from './guards';
import { MaterialModule } from '../material.module';
import { CommonPipesModule } from './pipes/common-pipes.module';
import { PivotGridComponent } from './components/pivot-grid/pivot-grid.component';
import { FieldDetailsComponent } from './components/field-details/field-details.component';
import {
  AccordionMenuComponent,
  AccordionMenuLinkComponent
} from './components/accordionMenu';
import { SidenavComponent, SidenavMenuService } from './components/sidenav';
import { BrandingLogoComponent } from './components/branding-logo/branding-logo.component';
import { ErrorDetailComponent } from './components/error-detail';
import { DataFormatDialogComponent } from './components/data-format-dialog';
import { ConfirmDialogComponent } from './components/confirm-dialog';
import { ReportGridComponent } from './components/report-grid';
import {
  JsPlumbConnectorComponent,
  JsPlumbCanvasComponent,
  JsPlumbTableComponent,
  JsPlumbJoinLabelComponent,
  JoinDialogComponent,
  JsPlumbEndpointDirective
} from './components/js-plumb';
import { AliasRenameDialogComponent } from './components/alias-rename-dialog';
import { DateFormatDialogComponent } from './components/date-format-dialog';
import { ChoiceGroupComponent } from './components/choice-group';
import { AggregateChooserComponent } from './components/aggregate-chooser';
import { PasswordToggleComponent } from './components/password-toggle';
import { ClickToCopyDirective, E2eDirective } from './directives';
import { CronJobSchedularComponent } from './components/cron-scheduler/cron-job-schedular';
import { CronDatePickerComponent } from './components/cron-scheduler/cron-date-picker';
import { ChartGridComponent } from './components/chart-grid';
import { SSOAuthComponent } from './components/sso-auth/sso-auth.component';
import { EmailListComponent } from './email-list';
import { CustomColorPickerComponent } from './components/custom-color-picker';

import { UChartModule } from './components/charts';
import { MapBoxModule } from './map-box/map-box.module';

import { DskFilterGroupComponent } from './dsk-filter-group/dsk-filter-group.component';
import { DskFilterDialogComponent } from './dsk-filter-dialog/dsk-filter-dialog.component';
import { DskFilterGroupViewComponent } from './dsk-filter-group-view/dsk-filter-group-view.component';
import { DskFiltersService } from './services/dsk-filters.service';
import { FilterPromptLayout } from './components/filter-prompt-layout/filter-prompt-layout.component';

import { DesignerFilterRowComponent } from './../modules/analyze/designer/filter/row';
import { DesignerStringFilterComponent } from './../modules/analyze/designer/filter/string';
import { DesignerDateFilterComponent } from './../modules/analyze/designer/filter/date';
import { DesignerNumberFilterComponent } from './../modules/analyze/designer/filter/number';
import { FilterChipsComponent } from './../modules/analyze/designer/filter/chips-u';
import { AggregatedFiltersComponent } from './aggregated-filters/aggregated-filters.component';

import {
  RemoteFolderSelectorComponent,
  CreatefolderDialogComponent
} from './components/remote-folder-selector';

import {
  DxDataGridService,
  ErrorDetailService,
  ErrorDetailDialogService,
  MenuService,
  LocalSearchService,
  ToastService,
  UserService,
  JwtService,
  ConfigService,
  SideNavService,
  WindowService,
  HeaderProgressService,
  DynamicModuleService,
  CustomIconService,
  DndPubsubService,
  CommonSemanticService,
  CookiesService
} from './services';
import { ShowPasswordDirective } from './directives/show-password.directive';

const MODULES = [
  CommonModuleAngular4,
  DxDataGridModule,
  DxPivotGridModule,
  DxDataGridModule,
  DxTextBoxModule,
  DxButtonModule,
  DxSliderModule,
  DxTooltipModule,
  DxCheckBoxModule,
  RouterModule,
  FormsModule,
  ReactiveFormsModule,
  DxTemplateModule,
  MaterialModule,
  NgxPopperModule,
  FlexLayoutModule,
  DndModule,
  DragDropModule,
  CommonPipesModule,
  HttpClientModule,
  OwlDateTimeModule,
  OwlNativeDateTimeModule,
  UChartModule,
  ClickOutsideModule,
  MapBoxModule,
  ColorPickerModule
];

const COMPONENTS = [
  PivotGridComponent,
  ReportGridComponent,
  ErrorDetailComponent,
  DataFormatDialogComponent,
  DateFormatDialogComponent,
  SidenavComponent,
  BrandingLogoComponent,
  AccordionMenuComponent,
  AccordionMenuLinkComponent,
  SearchBoxComponent,
  ConfirmDialogComponent,
  JsPlumbConnectorComponent,
  JsPlumbCanvasComponent,
  JsPlumbTableComponent,
  JsPlumbJoinLabelComponent,
  JoinDialogComponent,
  DateFormatDialogComponent,
  AliasRenameDialogComponent,
  AggregateChooserComponent,
  PasswordToggleComponent,
  ChoiceGroupComponent,
  SearchBoxComponent,
  FieldDetailsComponent,
  CronDatePickerComponent,
  CronJobSchedularComponent,
  ChartGridComponent,
  RemoteFolderSelectorComponent,
  CreatefolderDialogComponent,
  SSOAuthComponent,
  EmailListComponent,
  CustomColorPickerComponent,
  DskFilterDialogComponent,
  DskFilterGroupComponent,
  DskFilterGroupViewComponent,
  DesignerFilterRowComponent,
  DesignerStringFilterComponent,
  DesignerDateFilterComponent,
  DesignerNumberFilterComponent,
  FilterChipsComponent,
  AggregatedFiltersComponent,
  FilterPromptLayout
];

const THIRD_PARTY_COMPONENTS = [DxPivotGridComponent, DxDataGridComponent];

const DIRECTIVES = [
  ClickToCopyDirective,
  E2eDirective,
  JsPlumbEndpointDirective,
  ShowPasswordDirective
];

const SERVICES = [
  ConfigService,
  DxDataGridService,
  DynamicModuleService,
  ErrorDetailDialogService,
  ErrorDetailService,
  HeaderProgressService,
  JwtService,
  LocalSearchService,
  MenuService,
  SideNavService,
  SidenavMenuService,
  ToastService,
  UserService,
  WindowService,
  CustomIconService,
  DndPubsubService,
  CommonSemanticService,
  CookiesService,
  DskFiltersService
];

const INTERCEPTORS = [
  { provide: HTTP_INTERCEPTORS, useClass: AddTokenInterceptor, multi: true },
  {
    provide: HTTP_INTERCEPTORS,
    useClass: HandleErrorInterceptor,
    multi: true
  },
  {
    provide: HTTP_INTERCEPTORS,
    useClass: RefreshTokenInterceptor,
    multi: true
  },
  {
    provide: HTTP_INTERCEPTORS,
    useClass: ProgressIndicatorInterceptor,
    multi: true
  }
];

const GUARDS = [IsUserLoggedInGuard, DefaultModuleGuard, SSOAuthGuard];
@NgModule({
  imports: [
    NgxsModule.forFeature([CommonState]),
    AngularSplitModule.forRoot(),
    ...MODULES
  ],
  declarations: [...COMPONENTS, ...DIRECTIVES],
  entryComponents: COMPONENTS,
  exports: [
    ...MODULES,
    ...THIRD_PARTY_COMPONENTS,
    ...COMPONENTS,
    ...DIRECTIVES
  ],
  providers: [...INTERCEPTORS, ...GUARDS, CustomIconService],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class CommonModuleTs {
  constructor(private _customIconService: CustomIconService) {
    this._customIconService.init();
  }
}

/* CommonModuleGlobal exposes services that are shared for lazy loaded components as well */
@NgModule({})
export class CommonModuleGlobal {
  static forRoot(): ModuleWithProviders {
    return {
      ngModule: CommonModuleGlobal,
      providers: [...SERVICES]
    };
  }
}
