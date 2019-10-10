import { TestBed, async, ComponentFixture } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA, Component, Input } from '@angular/core';
import { DesignerContainerComponent } from './designer-container.component';
import { DesignerService } from '../designer.service';
import { AnalyzeDialogService } from '../../services/analyze-dialog.service';
import { ChartService } from '../../../../common/services/chart.service';
import { AnalyzeService } from '../../services/analyze.service';
import { JwtService } from '../../../../common/services';
import { Store } from '@ngxs/store';
import { MatDialog } from '@angular/material';
import { of } from 'rxjs';

@Component({
  // tslint:disable-next-line
  selector: 'designer-container',
  template: 'DesignerContainer'
})
class DesignerStubComponent {
  @Input() public analysisStarter;
  @Input() public analysis;
  @Input() public designerMode;
}

const dialogStub = {
  open: () => {}
};

describe('Designer Component', () => {
  let component: DesignerContainerComponent;
  let fixture: ComponentFixture<DesignerContainerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: DesignerService, useValue: {} },
        { provide: AnalyzeDialogService, useValue: {} },
        { provide: ChartService, useValue: {} },
        { provide: AnalyzeService, useValue: {} },
        { provide: JwtService, useValue: {} },
        { provide: Store, useValue: { dispatch: () => {} } },
        { provide: MatDialog, useValue: dialogStub }
      ],
      declarations: [DesignerContainerComponent, DesignerStubComponent],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DesignerContainerComponent);
    component = fixture.componentInstance;
    component.artifacts = [
      { artifactName: 'xyz', columns: [{ columnName: 'abc' }] }
    ] as any;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeDefined();
  });

  it('should construct filters for DSL format', () => {
    const filters = [
      {
        type: 'date',
        artifactsName: 'SALES',
        isOptional: false,
        columnName: 'date',
        isRuntimeFilter: false,
        isGlobalFilter: false,
        model: {
          operator: 'BTW',
          value: '2017-01-01',
          otherValue: '2017-01-31'
        }
      }
    ];

    const output = [
      {
        type: 'date',
        artifactsName: 'SALES',
        isOptional: false,
        columnName: 'date',
        isRuntimeFilter: false,
        isGlobalFilter: false,
        model: {
          operator: 'BTW',
          value: '2017-01-01',
          otherValue: '2017-01-31',
          gte: '2017-01-01',
          lte: '2017-01-31',
          preset: 'NA'
        }
      }
    ];
    const DSLFilters = component.generateDSLDateFilters(filters);
    expect(DSLFilters).toEqual(output);
  });

  describe('Derived metrics dialog', () => {
    it('should replace column if it already exists', async(() => {
      const column = { columnName: 'abc', table: 'xyz', type: 'double' };
      const dialogSpy = spyOn(TestBed.get(MatDialog), 'open').and.returnValue({
        afterClosed: () => of(column)
      });
      const changesSpy = spyOn(
        component,
        'handleOtherChangeEvents'
      ).and.returnValue({});

      component.openDerivedMetricDialog(column as any);
      expect(dialogSpy).toHaveBeenCalled();

      expect(changesSpy).toHaveBeenCalledWith({
        subject: 'expressionUpdated',
        column
      });
    }));

    it('should add column if it does not already exists', async(() => {
      const column = { columnName: 'pqr', table: 'xyz', type: 'double' };
      const dialogSpy = spyOn(TestBed.get(MatDialog), 'open').and.returnValue({
        afterClosed: () => of(column)
      });
      const changesSpy = spyOn(
        component,
        'handleOtherChangeEvents'
      ).and.returnValue({});

      component.openDerivedMetricDialog(column as any);
      expect(dialogSpy).toHaveBeenCalled();

      expect(changesSpy).toHaveBeenCalledWith({
        subject: 'derivedMetricAdded',
        column
      });
    }));
  });
});
