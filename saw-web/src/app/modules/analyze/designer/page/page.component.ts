import { Location } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AnalyzeService } from '../../services/analyze.service';
import { DesignerSaveEvent, DesignerMode } from '../types';
import { ConfirmDialogComponent } from '../../../../common/components/confirm-dialog';
import { ConfirmDialogData } from '../../../../common/types';
import { MatDialog, MatDialogConfig } from '@angular/material';

const CONFIRM_DIALOG_DATA: ConfirmDialogData = {
  title: 'There are unsaved changes',
  content: 'Do you want to discard unsaved changes and go back?',
  positiveActionLabel: 'Discard',
  negativeActionLabel: 'Cancel'
};

interface NewDesignerQueryParams {
  mode: DesignerMode;
  categoryId: string;
  metricName: string;
  semanticId: string;
  type: string;
  chartType?: string;
}

interface ExistingDesignerQueryParams {
  mode: DesignerMode;
  analysisId: string;
}

type DesignerQueryParams = NewDesignerQueryParams | ExistingDesignerQueryParams;

@Component({
  selector: 'designer-page',
  templateUrl: './page.component.html',
  styleUrls: ['./page.component.scss']
})
export class DesignerPageComponent implements OnInit {
  analysis: any;
  analysisStarter: any;
  designerMode: string;

  constructor(
    private locationService: Location,
    private analyzeService: AnalyzeService,
    private dialogService: MatDialog,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.convertParamsToData(this.route.snapshot
      .queryParams as DesignerQueryParams);
  }

  warnUser() {
    return this.dialogService.open(ConfirmDialogComponent, {
      width: 'auto',
      height: 'auto',
      data: CONFIRM_DIALOG_DATA
    } as MatDialogConfig);
  }

  onBack(isInDraftMode) {
    if (isInDraftMode) {
      this.warnUser()
        .afterClosed()
        .subscribe(shouldDiscard => {
          if (shouldDiscard) {
            this.locationService.back();
          }
        });
    } else {
      this.locationService.back();
    }
  }

  onSave({ analysis, requestExecution }: DesignerSaveEvent) {
    if (requestExecution) {
      this.locationService.back();
    }
  }

  convertParamsToData(params: DesignerQueryParams) {
    this.designerMode = params.mode;

    /* If new analysis, setup a basic analysis starter */
    if (params.mode === 'new') {
      const newAnalysisParams = <NewDesignerQueryParams>params;
      this.analysisStarter = {
        categoryId: newAnalysisParams.categoryId,
        metricName: newAnalysisParams.metricName,
        semanticId: newAnalysisParams.semanticId,
        type: newAnalysisParams.type,
        chartType: newAnalysisParams.chartType,
        name: 'Untitled Analysis',
        description: '',
        scheduled: null
      };

      /* Else, load existing analysis */
    } else {
      const existingAnalysisParams = <ExistingDesignerQueryParams>params;
      this.analyzeService
        .readAnalysis(existingAnalysisParams.analysisId)
        .then(analysis => {
          this.analysis = analysis;
        });
    }
  }
}
