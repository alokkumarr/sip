import {
  Component,
  OnInit,
  OnDestroy,
  Input,
  Output,
  EventEmitter
} from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import * as moment from 'moment';
import { requireIf } from '../../../../../../common/validators/index';
import { Subscription } from 'rxjs';

import {
  CUSTOM_DATE_PRESET_VALUE,
  DATE_PRESETS,
  DATE_FORMAT
} from '../../../../consts';
import {
  AlertFilterModel,
  AlertFilterEvent
} from '../../../../alerts.interface';

@Component({
  selector: 'alerts-date-filter',
  templateUrl: './alerts-date-filter.component.html',
  styleUrls: ['./alerts-date-filter.component.scss']
})
export class AlertsDateFilterComponent implements OnInit, OnDestroy {
  datePresets = DATE_PRESETS;
  alertFilterForm: FormGroup;
  datePresetSubscription: Subscription;
  showDateFields = false;

  @Output() change = new EventEmitter<AlertFilterEvent>();
  @Input('dateFilter') set setDateFilter(dateFilter: AlertFilterModel) {
    const { preset, gte, lte } = dateFilter;
    const isCustomDate = preset === CUSTOM_DATE_PRESET_VALUE;
    this.showDateFields = isCustomDate;
    this.alertFilterForm.setValue(
      {
        datePreset: dateFilter.preset,
        gte: isCustomDate ? moment.utc(gte) : null,
        lte: isCustomDate ? moment.utc(lte) : null
      },
      { emitEvent: false }
    );
  }

  constructor(private fb: FormBuilder) {
    this.createForm();
  }

  ngOnInit() {}

  ngOnDestroy() {
    if (this.datePresetSubscription) {
      this.datePresetSubscription.unsubscribe();
    }
  }

  createForm() {
    this.alertFilterForm = this.fb.group({
      datePreset: [this.datePresets[2].value, Validators.required],
      gte: [
        moment.utc(),
        [requireIf('datePreset', val => val === CUSTOM_DATE_PRESET_VALUE)]
      ],
      lte: [
        moment.utc(),
        [requireIf('datePreset', val => val === CUSTOM_DATE_PRESET_VALUE)]
      ]
    });

    /* Only show date inputs if custom filter is selected */
    this.datePresetSubscription = this.alertFilterForm
      .get('datePreset')
      .valueChanges.subscribe(data => {
        this.alertFilterForm.get('lte').updateValueAndValidity();
        this.alertFilterForm.get('gte').updateValueAndValidity();
        this.showDateFields = data === CUSTOM_DATE_PRESET_VALUE;
      });

    this.alertFilterForm.valueChanges.subscribe(({ datePreset, lte, gte }) => {
      const formIsValid = !this.alertFilterForm.invalid;
      if (formIsValid) {
        const dateFilter = this.prepareDateFilterModel(datePreset, lte, gte);
        this.change.emit({ filter: dateFilter, isValid: formIsValid });
      }
    });
  }

  prepareDateFilterModel(preset, lte, gte) {
    if (preset !== CUSTOM_DATE_PRESET_VALUE) {
      return {
        preset,
        fieldName: 'starttime',
        type: 'date'
      };
    }
    const startTime = moment
      .utc(
        gte.format(DATE_FORMAT.YYYY_MM_DD) + ' 00:00:00',
        DATE_FORMAT.YYYY_MM_DD_HH_mm_ss
      )
      .valueOf();
    const endTime = moment
      .utc(
        lte.format(DATE_FORMAT.YYYY_MM_DD) + ' 23:59:59',
        DATE_FORMAT.YYYY_MM_DD_HH_mm_ss
      )
      .valueOf();
    return {
      fieldName: 'starttime',
      type: 'date',
      preset,
      gte: startTime,
      lte: endTime
    };
  }
}
