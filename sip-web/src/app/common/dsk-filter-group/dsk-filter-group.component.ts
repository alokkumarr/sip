import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { Router } from '@angular/router';
import {
  DSKFilterGroup,
  DSKFilterField,
  DSKFilterOperator,
  DSKFilterBooleanCriteria
} from '../dsk-filter.model';

import { NUMBER_TYPES, DATE_TYPES } from './../consts';

import { PopperContent } from 'ngx-popper';
import { MatChipInputEvent } from '@angular/material';
import { JwtService } from 'src/app/common/services';
import { DskFiltersService, DskEligibleField } from './../services/dsk-filters.service';

import * as toString from 'lodash/toString';
import * as cloneDeep from 'lodash/cloneDeep';
import * as isEmpty from 'lodash/isEmpty';
import * as groupBy from 'lodash/groupBy';
import * as isUndefined from 'lodash/isUndefined';
import * as map from 'lodash/map';
import * as reduce from 'lodash/reduce';
import * as get from 'lodash/get';
import * as fpPipe from 'lodash/fp/pipe';
import * as fpFlatMap from 'lodash/fp/flatMap';
import * as fpFilter from 'lodash/fp/filter';
import { v4 as uuid } from 'uuid';

import { Artifact, Filter } from './../../modules/analyze/designer/types';
import { ArtifactDSL } from '../../models';


export const defaultFilters: DSKFilterGroup = {
  booleanCriteria: DSKFilterBooleanCriteria.AND,
  booleanQuery: []
};

const TYPE_MAP = reduce(
  [
    ...map(NUMBER_TYPES, type => ({ type, generalType: 'number' })),
    ...map(DATE_TYPES, type => ({ type, generalType: 'date' })),
    { type: 'string', generalType: 'string' }
  ],
  (typeMap, { type, generalType }) => {
    typeMap[type] = generalType;
    return typeMap;
  },
  {}
);

@Component({
  selector: 'dsk-filter-group',
  templateUrl: './dsk-filter-group.component.html',
  styleUrls: ['./dsk-filter-group.component.scss']
})
export class DskFilterGroupComponent implements OnInit {
  filterGroup: DSKFilterGroup = cloneDeep(defaultFilters);
  readonly separatorKeysCodes: number[] = [ENTER, COMMA];
  dskEligibleFields: Array<DskEligibleField> = [];
  @Output() public filterModelChange: EventEmitter<null> = new EventEmitter();
  filteredColumns: [];
  groupedFilters;
  filters;
  public TYPE_MAP = TYPE_MAP;
  @Input() data;
  @Input('filterGroup') set _filterGroup(filters: DSKFilterGroup) {
    this.filterGroup = filters || cloneDeep(defaultFilters);
    this.onChange.emit(this.filterGroup);
  }
  @Input() selfIndex: number; // stores the position inside parent (for removal)
  @Output() onRemoveGroup = new EventEmitter();
  @Output() onChange = new EventEmitter();
  @Input() showGlobalOption: boolean;

  constructor(
    jwtService: JwtService,
    dskFilterService: DskFiltersService,
    router: Router
  ) {
    if (!(router.url).includes('/analyze/')) {
      dskFilterService
      .getEligibleDSKFieldsFor(jwtService.customerId, jwtService.productId)
      .subscribe(fields => {
        this.dskEligibleFields = fields;
      });
    }
  }

  ngOnInit() {
    this.filters = cloneDeep(this.data.filters);
    this.groupedFilters = groupBy(this.data.filters, 'tableName');
    this.onChange.emit(this.filterGroup);
  }

  filterAutocompleteFields(value) {
    const filterValue = toString(value).toLowerCase();
    return this.dskEligibleFields.filter(option =>
      (option.displayName || option.columnName)
        .toLowerCase()
        .includes(filterValue)
    );
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
    if (this.data.mode === 'DSK') {
      this.filterGroup.booleanQuery.push({
        columnName: '',
        model: {
          operator: DSKFilterOperator.ISIN,
          values: []
        }
      });
      this.onChange.emit(this.filterGroup);
    } else if (this.data.mode === 'ANALYZE') {
      this.filterGroup.booleanQuery.push({
        columnName: '',
        artifactsName: this.data.artifacts[0].artifactName,
        type: '',
        isGlobalFilter: false,
        isRuntimeFilter: false,
        isOptional: false,
        isAggregationFilter: false,
        uuid: uuid(),
        model: {
          operator: '',
          values: ''
        }
      });
      this.onChange.emit(this.filterGroup);
    }

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

  preventSpace(event: KeyboardEvent) {
    if (event.code === 'Space') {
      return false;
    }
    return true;
  }

  updateAttributeName(childId: number, value: string) {
    (<DSKFilterField>this.filterGroup.booleanQuery[childId]).columnName = value;
    this.onChange.emit(this.filterGroup);
  }

  addValue(childId: number, event: MatChipInputEvent) {
    const input = event.input;
    const value = event.value;
    if ((value || '').trim()) {
      (<DSKFilterField>(
        this.filterGroup.booleanQuery[childId]
      )).model.values.push(value);
      this.onChange.emit(this.filterGroup);
    }

    if (input) {
      input.value = '';
    }
  }

  removeValue(childId: number, valueId: number) {
    (<DSKFilterField>(
      this.filterGroup.booleanQuery[childId]
    )).model.values.splice(valueId, 1);
    this.onChange.emit(this.filterGroup);
  }

  artifactSelect(artifact, childId) {
    this.addFilter(artifact, true);
    (<DSKFilterField>this.filterGroup.booleanQuery[childId]).artifactsName = artifact;
    this.onChange.emit(this.filterGroup);
  }

  fetchColumns(childId) {
    if (isEmpty(get(this.filterGroup.booleanQuery[childId], 'artifactsName'))) {
      return [];
    }
    let dropDownData = this.data.artifacts.find((data: any) =>
      data.artifactName === get(this.filterGroup.booleanQuery[childId], 'artifactsName')
    );
    return dropDownData.columns;
  }

  columnsSelect(column, childId) {
    (<DSKFilterField>this.filterGroup.booleanQuery[childId]).columnName = column;
    const { artifactsName } = (<DSKFilterField>this.filterGroup.booleanQuery[childId]);
    (<DSKFilterField>this.filterGroup.booleanQuery[childId]).type = fpPipe(
      fpFlatMap(artifact => artifact.columns || artifact.fields),
      fpFilter(({ columnName, table }) => {
        return table.toLowerCase === artifactsName.toLowerCase && columnName === column;
      })
    )(this.data.artifacts)[0].type;
    this.onChange.emit(this.filterGroup);
  }

  artifactTrackByFn(_, artifact: Artifact | ArtifactDSL) {
    return (
      (<Artifact>artifact).artifactName || (<ArtifactDSL>artifact).artifactsName
    );
  }

  fetchType(childId) {
    if (isUndefined(this.filterGroup.booleanQuery[childId])) {
      return '';
    }
    return get(this.filterGroup.booleanQuery[childId], 'type');
  }

  onFilterModelChange(filter, childId) {
    (<DSKFilterField>this.filterGroup.booleanQuery[childId]).model = cloneDeep(filter);
    this.onChange.emit(this.filterGroup);
  }

  addFilter(tableName, initialAdd = false, isAggregationFilter = false) {
    const newFilter: Filter = {
      type: null,
      tableName,
      isOptional: false,
      columnName: null,
      isRuntimeFilter: false,
      isAggregationFilter,
      isGlobalFilter: false,
      model: null
    };
    if (!this.groupedFilters[tableName]) {
      this.groupedFilters[tableName] = [];
    }
    this.groupedFilters[tableName] = [
      ...this.groupedFilters[tableName],
      newFilter
    ];
  }

  onGlobalCheckboxToggle(value, childId) {
    if (!this.data.supportsGlobalFilters) {
      return;
    }
    if (value) {
      delete (<DSKFilterField>this.filterGroup.booleanQuery[childId]).model;
    }
    (<DSKFilterField>this.filterGroup.booleanQuery[childId]).isGlobalFilter = value;
    this.onChange.emit(this.filterGroup);
  }

  onRuntimeCheckboxToggle(value, childId) {
    (<DSKFilterField>this.filterGroup.booleanQuery[childId]).isRuntimeFilter = value;
    if (value) {
      delete (<DSKFilterField>this.filterGroup.booleanQuery[childId]).model;
    }
    this.onChange.emit(this.filterGroup);
  }

  onOptionalCheckboxToggle(value, childId) {
    (<DSKFilterField>this.filterGroup.booleanQuery[childId]).isOptional = value;
    this.onChange.emit(this.filterGroup);
  }
}
