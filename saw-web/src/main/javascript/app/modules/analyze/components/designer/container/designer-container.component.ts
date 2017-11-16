import {
  Component,
  Input,
  Output,
  EventEmitter
} from '@angular/core';
import assign from 'lodash/assign';

import DesignerService from '../designer.service';
import Analysis from '../../../models/analysis.model';
import {
  DesignerMode,
  AnalysisType,
  AnalysisStarter
} from '../../../types';
const template = require('./designer-container.component.html');
require('./designer-container.component.scss');

@Component({
  selector: 'designer-container',
  template
})
export default class DesignerContainerComponent {
  @Input() public analysisStarter?: AnalysisStarter;
  @Input() public analysis?: Analysis;
  @Input() public designerMode: DesignerMode;
  @Output() public onBack: EventEmitter<any> = new EventEmitter();
  public isInDraftMode: boolean = false;

  constructor(private _designerService: DesignerService) {}

  ngOnInit() {

    switch (this.designerMode) {
    case 'new':
      this.initNewAnalysis();
      break;
    case 'edit':
      break;

    default:
      break;
    }
  }

  initNewAnalysis() {
    const {type, semanticId} = this.analysisStarter;
    this._designerService.createAnalysis(semanticId, type).then(newAnalysis => {
      this.analysis = assign(this.analysisStarter, newAnalysis);
      console.log('newAnalysis: ', this.analysis);
    });
  }

  onSave() {
    console.log('save');
  }
}
