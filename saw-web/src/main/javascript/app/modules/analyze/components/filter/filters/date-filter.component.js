import * as map from 'lodash/map';
import * as forEach from 'lodash/forEach';
import * as unset from 'lodash/unset';
import * as template from './date-filter.component.html';

export const DateFilterComponent = {
  template,
  bindings: {
    model: '<',
    onChange: '&'
  },
  controller: class DateFilterController {
    constructor($filter, $translate) {
      this._$filter = $filter;
      this.presets = [{
        value: 'TW',
        keyword: 'THIS_WEEK'
      }, {
        value: 'MTD',
        keyword: 'MONTH_TO_DATE'
      }, {
        value: 'YTD',
        keyword: 'YEAR_TO_DATE'
      }, {
        value: 'LW',
        keyword: 'LAST_WEEK'
      }, {
        value: 'LTW',
        keyword: 'LAST_2_WEEKS'
      }, {
        value: 'LM',
        keyword: 'LAST_MONTH'
      }, {
        value: 'LQ',
        keyword: 'LAST_QUARTER'
      }, {
        value: 'LTM',
        keyword: 'LAST_3_MONTHS'
      }, {
        value: 'LSM',
        keyword: 'LAST_6_MONTHS'
      }, {
        value: 'CUSTOM',
        keyword: 'CUSTOM'
      }];
      $translate(map(this.presets, 'keyword')).then(translations => {
        forEach(this.presets, preset => {
          preset.label = translations[preset.keyword];
        });
      });
    }
    $onInit() {
      /* this.model has string values. Change them to actual date objects
      to be able to work with md-datepicker first.
      */
      this.model = this.model || {};
      this.tempModel = {
        preset: this.model.preset,
        gte: this.model.gte ? new Date(this.model.gte) : '',
        lte: this.model.lte ? new Date(this.model.lte) : ''
      };
    }

    onPresetSelected() {
      this.tempModel.gte = null;
      this.tempModel.lte = null;
      this.onChange({model: {preset: this.tempModel.preset}});
    }

    onModelChange() {
      this.updatedDate = this.model || {
        gte: '',
        lte: ''
      };
      this.dateFormat = 'yyyy-MM-dd HH:ss:mm';
      unset(this.updatedDate, 'preset');
      this.updatedDate.gte = this._$filter('date')(this.tempModel.gte, this.dateFormat);
      this.updatedDate.lte = this._$filter('date')(this.tempModel.lte, this.dateFormat);
      this.onChange({model: this.updatedDate});
    }
  }
};
