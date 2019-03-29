import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import * as isString from 'lodash/isString';
import { FormControl, FormBuilder, FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import { ArtifactColumnChart, Region } from '../../../types';
import { getMapDataIndexByGeoType } from '../../../../../../common/components/charts/map-data-index';
import { DesignerChangeEvent } from '../../../types';

@Component({
  selector: 'designer-region-selector',
  templateUrl: 'designer-region-selector.component.html'
})
export class DesignerRegionSelectorComponent implements OnInit {
  @Input() public artifactColumn: ArtifactColumnChart;
  @Output() public change: EventEmitter<
    DesignerChangeEvent
  > = new EventEmitter();

  public stateForm: FormGroup = this.fb.group({
    regionCtrl: ''
  });
  public regionCtrl = new FormControl();
  public filteredRegions: Observable<Region>;

  constructor(private fb: FormBuilder) {}

  ngOnInit() {
    const defaultRegion = this.artifactColumn.region || { name: '' };
    this.filteredRegions = this.stateForm.get('regionCtrl').valueChanges.pipe(
      startWith(defaultRegion),
      map(value => {
        if (isString(value)) {
          return this._filterRegions(value);
        }
        return this._filterRegions(value.name);
      })
    );
  }

  onRegionSelected(region) {
    this.artifactColumn.region = region;
    this.change.emit({ subject: 'region' });
  }

  displayWithRegion(option) {
    return option.name;
  }

  private _filter = (array, name) => {
    const filterValue = name.toLowerCase();

    return array.filter(
      item => item.name.toLowerCase().indexOf(filterValue) === 0
    );
  };

  private _filterRegions(value) {
    const mapDataIndex = getMapDataIndexByGeoType(this.artifactColumn.geoType);
    if (value) {
      return mapDataIndex
        .map(group => ({
          name: group.name,
          children: this._filter(group.children, value)
        }))
        .filter(group => group.children.length > 0);
    }

    return mapDataIndex;
  }
}
