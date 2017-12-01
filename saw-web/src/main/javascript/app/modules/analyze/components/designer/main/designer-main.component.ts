import {
  Component,
  Input,
  Output,
  EventEmitter
} from '@angular/core';
import * as map from 'lodash/map';
import * as filter from 'lodash/filter';
import { DesignerService } from '../designer.service';
import {
  IDEsignerSettingGroupAdapter,
  ArtifactColumn,
  ArtifactColumns,
  ArtifactColumnFilter,
  Analysis,
  ArtifactColumnPivot
} from '../types';
import { TYPE_ICONS_OBJ } from '../../../consts';

const template = require('./designer-main.component.html');
require('./designer-main.component.scss');

@Component({
  selector: 'designer-main',
  template
})
export class DesignerMainComponent {
  @Output() public onSettingsChange: EventEmitter<ArtifactColumnPivot[]> = new EventEmitter();
  @Input() public analysis: Analysis;

  constructor(private _designerService: DesignerService) {}

}
