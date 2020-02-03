import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { MaterialModule } from 'src/app/material.module';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';

import { DesignerFilterDialogComponent } from './designer-filter-dialog.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('DesignerFilterRowComponent', () => {
  let component: DesignerFilterDialogComponent;
  let fixture: ComponentFixture<DesignerFilterDialogComponent>;

  const mockDialogRef = {
    close: jasmine.createSpy('close')
  };

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [MaterialModule, NoopAnimationsModule],
      declarations: [DesignerFilterDialogComponent],
      providers: [
        {
          provide: MatDialogRef,
          useValue: mockDialogRef
        },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {}
        }
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DesignerFilterDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should return aggregated filters', () => {
    component.groupedFilters = {
      table: [{ isAggregationFilter: true }, { isAggregationFilter: false }]
    };
    expect(component.aggregatedFiltersFor('table').length).toEqual(1);
  });

  it('should remove aggregated filters correctly', () => {
    component.groupedFilters = {
      table: [
        { isAggregationFilter: false, id: 1 },
        { isAggregationFilter: true, id: 1 },
        { isAggregationFilter: true, id: 2 }
      ]
    };

    /* Delete the first aggregation filter */
    component.removeFilter(0, 'table', true);

    expect(component.groupedFilters.table[0].isAggregationFilter).toEqual(
      false
    );
    expect(component.groupedFilters.table[1].id).toEqual(2);
  });
});
