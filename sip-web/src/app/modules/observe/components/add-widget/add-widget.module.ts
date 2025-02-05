import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MaterialModule } from '../../../../material.module';
import { CommonPipesModule } from '../../../../common/pipes/common-pipes.module';

import { AddWidgetComponent } from './add-widget.component';
import { WidgetTypeComponent } from './widget-type/widget-type.component';
import { WidgetCategoryComponent } from './widget-category/widget-category.component';
import { WidgetMetricComponent } from './widget-metric/widget-metric.component';
import { WidgetAnalysisComponent } from './widget-analysis/widget-analysis.component';
import { WidgetKPIComponent } from './widget-kpi/widget-kpi.component';
import { WidgetHeaderComponent } from './widget-header/widget-header.component';

const components = [
  AddWidgetComponent,
  WidgetTypeComponent,
  WidgetCategoryComponent,
  WidgetMetricComponent,
  WidgetAnalysisComponent,
  WidgetKPIComponent,
  WidgetHeaderComponent
];

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MaterialModule,
    CommonPipesModule
  ],
  exports: [AddWidgetComponent, WidgetKPIComponent, WidgetHeaderComponent],
  declarations: components,
  providers: []
})
export class AddWidgetModule {}
