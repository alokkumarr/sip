import { Component, Input, Output, EventEmitter } from '@angular/core';
import {
  ArtifactColumn,
  ArtifactColumns,
  IDEsignerSettingGroupAdapter,
  DesignerChangeEvent
} from '../../types';
import { TYPE_ICONS_OBJ } from '../../../../consts';
import { DesignerService } from '../../designer.service';

const template = require('./designer-settings-group.component.html');
require('./designer-settings-group.component.scss');

@Component({
  selector: 'designer-settings-group',
  template
})
export class DesignerSettingsGroupComponent {
  @Output() public fieldsChange: EventEmitter<null> = new EventEmitter();
  @Output()
  public fieldPropChange: EventEmitter<
    DesignerChangeEvent
  > = new EventEmitter();
  @Output()
  public removeField: EventEmitter<ArtifactColumn> = new EventEmitter();
  @Input() public artifactColumns: ArtifactColumns;
  @Input() public groupAdapter: IDEsignerSettingGroupAdapter;
  @Input() public groupAdapters: Array<IDEsignerSettingGroupAdapter>;

  public dndSortableContainerObj = {};
  public allowDropFn;
  public fieldCount;

  public TYPE_ICONS_OBJ = TYPE_ICONS_OBJ;

  public removeFromCallback = (payload, index, container) => {
    this._designerService.removeArtifactColumnFromGroup(payload, container);
    this.fieldsChange.emit();
  };

  public addToCallback = (payload, index, container) => {
    this._designerService.addArtifactColumnIntoGroup(payload, container, index);
    this.fieldsChange.emit();
  };

  constructor(private _designerService: DesignerService) {}

  ngOnInit() {
    this.allowDropFn = this.groupAdapter.canAcceptArtifactColumn(
      this.groupAdapter,
      this.groupAdapters
    );
    this.fieldCount = this.artifactColumns.length;
    console.log(this.fieldCount);
  }

  onRemoveField(artifactColumn: ArtifactColumn) {
    artifactColumn.aliasName = '';
    this.removeField.emit(artifactColumn);
  }

  trackByFn(_, artifactColumn: ArtifactColumn) {
    return artifactColumn.columnName;
  }
}
