import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { UserAssignmentService } from './../userassignment.service';

const template = require('./add-security-dialog.component.html');
require('./add-security-dialog.component.scss');

@Component({
  selector: 'add-secuirty-dialog',
  template
})
export class AddSecurityDialogComponent {
  public securityGroup = {};
  public errorState: boolean;
  public errorMessage: string;

  constructor(
    private _dialogRef: MatDialogRef<AddSecurityDialogComponent>,
    private _userAssignmentService: UserAssignmentService,
    @Inject(MAT_DIALOG_DATA) public data: {
      mode: 'edit' | 'create'
    }
  ) {}

  submit() {
    this._userAssignmentService.addSecurityGroup(this.data).then(response => {
      if (response.valid) {
        this._dialogRef.close(response.valid);
      } else {
        this.errorState = !response.valid;
        this.errorMessage = response.validityMessage;
      }
    });
  }
}
