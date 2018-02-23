import * as map from 'lodash/map';
import * as isEmpty from 'lodash/isEmpty';
import * as forEach from 'lodash/forEach';
import * as template from './string-filter.component.html';

import {AnalyseTypes} from '../../../consts';

export const StringFilterComponent = {
  template,
  bindings: {
    model: '<',
    options: '<',
    onChange: '&'
  },
  controller: class StringFilterController {
    constructor($mdConstant, $filter, $translate) {
      'ngInject';
      this.isEmpty = isEmpty;
      const semicolon = 186;
      this.displayChips = false;
      this.separatorKeys = [$mdConstant.KEY_CODE.ENTER, $mdConstant.KEY_CODE.COMMA, semicolon];
      this.presets = [{
        value: 'EQ',
        keyword: 'EQUALS'
      }, {
        value: 'NEQ',
        keyword: 'NOT_EQUAL'
      }, {
        value: 'ISIN',
        keyword: 'IS_IN'
      }, {
        value: 'ISNOTIN',
        keyword: 'IS_NOT_IN'
      }, {
        value: 'CONTAINS',
        keyword: 'CONTAINS'
      }, {
        value: 'SW',
        keyword: 'STARTS_WITH'
      }, {
        value: 'EW',
        keyword: 'ENDS_WITH'
      }];
      $translate(map(this.presets, 'keyword')).then(translations => {
        forEach(this.presets, operator => {
          operator.label = translations[operator.keyword];
        });
      });
    }

    $onInit() {
      this.options = this.options || {};
      this.model = this.model || {};
      this.keywords = {...{modelValues: []}, ...this.model};

      if (this.options.type === AnalyseTypes.ESReport) {
        this.disablePresets = true;
        this.keywords.operator = 'ISIN';
      }

      this.tempModel = {};
      this.tempModel.value = this.keywords.modelValues[0];
    }

    onPresetSelected() {
      this.tempModel.value = null;
      this.keywords.modelValues = [];
    }

    onInputChange() {
      this.keywords.modelValues = [];
      this.keywords.modelValues.push(this.tempModel.value);
      this.onChange({model: this.keywords});
    }

    onChipsChange() {
      this.onChange({
        model: this.options.type === AnalyseTypes.ESReport ?
          {modelValues: this.keywords.modelValues} :
          this.keywords
      });
    }
  }
};
