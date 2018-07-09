import { NgModule } from '@angular/core';
import { CommonModuleTs } from '../../../common';
import { AnalyzeActionsModule } from '../actions';
import { ExecutedViewComponent } from './executed-view.component';
import { ExecutedListComponent } from './list';
import { ExecutedReportViewComponent } from './report';
import { ExecutedPivotViewComponent } from './pivot';
import { ExecutedChartViewComponent } from './chart';
import { ChartService } from '../services/chart.service';
import { ExecuteService } from '../services/execute.service';
import { ToastService } from '../../../common/services/toastMessage.service';
import { UChartModule } from '../../../common/components/charts';
import { AnalyzeExportService } from '../services/analyze-export.service';
import { AnalyzeFilterModule } from '../components/designer/filter';

const COMPONENTS = [
  ExecutedViewComponent,
  ExecutedListComponent,
  ExecutedReportViewComponent,
  ExecutedPivotViewComponent,
  ExecutedChartViewComponent
];

@NgModule({
  imports: [
    CommonModuleTs,
    AnalyzeFilterModule,
    AnalyzeActionsModule,
    UChartModule
  ],
  declarations: [
    ...COMPONENTS
  ],
  entryComponents: COMPONENTS,
  providers: [
    ExecuteService,
    ToastService,
    AnalyzeExportService,
    ChartService
  ],
  exports: [ExecutedViewComponent]
})
export class ExecutedViewModule {}
export {
  ExecutedViewComponent
};
