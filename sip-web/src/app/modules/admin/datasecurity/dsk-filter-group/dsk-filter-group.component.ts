import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import {
  DSKFilterGroup,
  DSKFilterField,
  DSKFilterOperator,
  DSKFilterBooleanCriteria
} from '../dsk-filter.model';

import { PopperContent } from 'ngx-popper';
import { MatChipInputEvent } from '@angular/material';

@Component({
  selector: 'dsk-filter-group',
  templateUrl: './dsk-filter-group.component.html',
  styleUrls: ['./dsk-filter-group.component.scss']
})
export class DskFilterGroupComponent implements OnInit {
  @Input() filterGroup: DSKFilterGroup;
  @Input() selfIndex: number; // stores the position inside parent (for removal)
  @Output() onRemoveGroup = new EventEmitter();
  @Output() onChange = new EventEmitter();

  readonly separatorKeysCodes: number[] = [ENTER, COMMA];
  constructor() {}

  ngOnInit() {
    this.filterGroup = this.filterGroup || {
      booleanCriteria: DSKFilterBooleanCriteria.AND,
      booleanQuery: []
    };
  }

  toggleCriteria() {
    if (this.filterGroup.booleanCriteria === DSKFilterBooleanCriteria.AND) {
      this.filterGroup.booleanCriteria = DSKFilterBooleanCriteria.OR;
    } else {
      this.filterGroup.booleanCriteria = DSKFilterBooleanCriteria.AND;
    }
    this.onChange.emit(this.filterGroup);
  }

  addField(popper: PopperContent) {
    popper.hide();
    this.filterGroup.booleanQuery.push({
      columnName: '',
      model: {
        operator: DSKFilterOperator.ISIN,
        value: []
      }
    });
    this.onChange.emit(this.filterGroup);
  }

  /**
   * Removes field from this.filterGroup's booleanQuery.
   *
   * @param {number} fieldIndex
   * @memberof DskFilterGroupComponent
   */
  removeField(fieldIndex: number) {
    this.filterGroup.booleanQuery.splice(fieldIndex, 1);
    this.onChange.emit(this.filterGroup);
  }

  addGroup(popper: PopperContent) {
    popper.hide();
    this.filterGroup.booleanQuery.push({
      booleanCriteria: DSKFilterBooleanCriteria.AND,
      booleanQuery: []
    });
    this.onChange.emit(this.filterGroup);
  }

  /**
   * Removes group from this.filterGroup's booleanQuery.
   * This is a handler for an output event fired from the child group
   * component, since the child component owns the remove button.
   *
   * @param {*} childId
   * @memberof DskFilterGroupComponent
   */
  removeGroup(childId) {
    this.filterGroup.booleanQuery.splice(childId, 1);
    this.onChange.emit(this.filterGroup);
  }

  updateAttributeName(childId: number, value: string) {
    (<DSKFilterField>this.filterGroup.booleanQuery[childId]).columnName = value;
    this.onChange.emit(this.filterGroup);
  }

  addValue(childId: number, event: MatChipInputEvent) {
    const input = event.input;
    const value = event.value;
    if ((value || '').trim()) {
      (<DSKFilterField>this.filterGroup.booleanQuery[childId]).model.value.push(
        value
      );
      this.onChange.emit(this.filterGroup);
    }

    if (input) {
      input.value = '';
    }
  }

  removeValue(childId: number, valueId: number) {
    (<DSKFilterField>this.filterGroup.booleanQuery[childId]).model.value.splice(
      valueId,
      1
    );
    this.onChange.emit(this.filterGroup);
  }
}
