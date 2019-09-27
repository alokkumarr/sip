import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModuleTs } from '../../common';
// import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgxsModule } from '@ngxs/store';
// import { DxTemplateModule } from 'devextreme-angular/core/template';
// import { DxDataGridModule } from 'devextreme-angular/ui/data-grid';
import { DxSelectBoxModule } from 'devextreme-angular/ui/select-box';

// import { MaterialModule } from '../../material.module';
// import { FlexLayoutModule } from '@angular/flex-layout';
import { RouterModule } from '@angular/router';
import { routes } from './routes';
import { AlertsPageComponent } from './components/alerts-page/alerts-page.component';
import { ObserveService } from '../observe/services/observe.service';
import {
  AlertsConfigurationComponent,
  AddAlertComponent,
  ConfirmActionDialogComponent
} from './components/configure/index';
import {
  AlertsViewComponent,
  AlertChartComponent,
  AlertsGridComponent,
  AlertDetailComponent,
  AlertsDateFilterComponent,
  AlertsStringFilterComponent,
  AlertsFiltersComponent
} from './components/alerts-view/index';
import { AlertsState } from './state/alerts.state';
import { IsAdminGuard } from '../admin/guards';

const components = [
  AlertsPageComponent,
  AlertsViewComponent,
  AlertChartComponent,
  AlertsConfigurationComponent,
  AddAlertComponent,
  ConfirmActionDialogComponent,
  AlertsGridComponent,
  AlertDetailComponent,
  AlertsDateFilterComponent,
  AlertsStringFilterComponent,
  AlertsFiltersComponent
];

@NgModule({
  declarations: components,
  imports: [
    CommonModuleTs,
    RouterModule.forChild(routes),
    // FormsModule,
    // ReactiveFormsModule,
    // MaterialModule,
    // FlexLayoutModule,
    // DxTemplateModule,
    // DxDataGridModule,
    DxSelectBoxModule,
    NgxsModule.forFeature([AlertsState])
  ],
  entryComponents: components,
  providers: [IsAdminGuard, ObserveService],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AlertsModule {}
