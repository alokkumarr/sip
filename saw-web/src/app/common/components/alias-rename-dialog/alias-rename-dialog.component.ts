import {
  Component,
  Inject
} from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';

const template = require('./alias-rename-dialog.component.html');
const style = require('./alias-rename-dialog.component.scss');

@Component({
  selector: 'alias-rename-dialog',
  template,
  styles: [style]
})
export class AliasRenameDialogComponent {

  public alias: string;

  constructor(
    private _dialogRef: MatDialogRef<AliasRenameDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: {
      alias: string
    }
  ) {
    this.alias = this.data.alias;
  }

  onAliasChange(alias) {
    this.alias = alias;
  }

  close() {
    this._dialogRef.close();
  }

  rename() {
    this._dialogRef.close(this.alias);
  }
}
