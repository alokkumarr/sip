import fpGroupBy from 'lodash/fp/groupBy';
import fpPipe from 'lodash/fp/pipe';
import fpMapValues from 'lodash/fp/mapValues';
import forEach from 'lodash/forEach';
import filter from 'lodash/filter';
import map from 'lodash/map';

import template from './analyze-pivot-settings.component.html';
import style from './analyze-pivot-settings.component.scss';
import {DATE_TYPES} from '../../consts';

export const ANALYZE_PIVOT_SETTINGS_SIDENAV_ID = 'ANALYZE_PIVOT_SETTINGS_SIDENAV_ID';

const MAX_POSSIBLE_FILEDS_OF_SAME_AREA = 5;

const AGGREGATE_TYPES = [{
  label: 'Sum',
  value: 'sum',
  icon: 'icon-Sum'
}, {
  label: 'Average',
  value: 'avg',
  icon: 'icon-AVG'
}, {
  label: 'Mininum',
  value: 'min',
  icon: 'icon-MIN'
}, {
  label: 'Maximum',
  value: 'max',
  icon: 'icon-MAX'
}, {
  label: 'Count',
  value: 'count',
  icon: 'icon-Count'
}];

const DEFAULT_AGGREGATE_TYPE = AGGREGATE_TYPES[0];
const AGGREGATE_TYPES_OBJ = fpPipe(
  fpGroupBy('value'),
  fpMapValues(v => v[0])
)(AGGREGATE_TYPES);

const AREA_TYPES = [{
  label: 'Row',
  value: 'row',
  icon: 'icon-row'
}, {
  label: 'Column',
  value: 'column',
  icon: 'icon-column'
}, {
  label: 'Data',
  value: 'data',
  icon: 'icon-data'
}];

const NUMBER_ICON = 'icon-number-type';
const DATE_ICON = 'icon-calendar';
const STRING_ICON = 'icon-string-type';
const NUMBER_TOOLTIP = 'TOOLTIP_NUMBER_TYPE';
const STRING_TOOLTIP = 'TOOLTIP_STRING_TYPE';
const DATE_TOOLTIP = 'TOOLTIP_DATE_TYPE';
const ARTIFACT_ICON_TYPES_OBJ = {
  string: {
    tooltip: STRING_TOOLTIP,
    icon: STRING_ICON
  },
  long: {
    tooltip: NUMBER_TOOLTIP,
    icon: NUMBER_ICON
  },
  int: {
    tooltip: NUMBER_TOOLTIP,
    icon: NUMBER_ICON
  },
  integer: {
    tooltip: NUMBER_TOOLTIP,
    icon: NUMBER_ICON
  },
  double: {
    tooltip: NUMBER_TOOLTIP,
    icon: NUMBER_ICON
  },
  float: {
    tooltip: NUMBER_TOOLTIP,
    icon: NUMBER_ICON
  },
  timestamp: {
    tooltip: DATE_TOOLTIP,
    icon: DATE_ICON
  },
  date: {
    tooltip: DATE_TOOLTIP,
    icon: DATE_ICON
  }
};

const GROUP_INTERVALS = [{
  label: 'YEAR',
  value: 'year'
}, {
  label: 'QUARTER',
  value: 'quarter'
}, {
  label: 'MONTH',
  value: 'month'
}, {
  label: 'DAY',
  value: 'day'
}, {
  label: 'DAY_OF_WEEK',
  value: 'dayOfWeek'
}];

const DEFAULT_GROUP_INTERVAL = GROUP_INTERVALS[2];

const DEFAULT_AREA_TYPE = AREA_TYPES[0];
const AREA_TYPES_OBJ = fpPipe(
  fpGroupBy('value'),
  fpMapValues(v => v[0])
)(AREA_TYPES);

export const AnalyzePivotSettingsComponent = {
  template,
  styles: [style],
  bindings: {
    onApplySettings: '&',
    artifactColumns: '<'
  },
  controller: class AnalyzePivotSettingsController {
    constructor(AnalyzeService, FilterService, $mdSidenav, $translate) {
      'ngInject';
      // TODO filter possible areas based on column type
      this.AGGREGATE_TYPES = AGGREGATE_TYPES;
      this.AGGREGATE_TYPES_OBJ = AGGREGATE_TYPES_OBJ;
      this.DEFAULT_AGGREGATE_TYPE = DEFAULT_AGGREGATE_TYPE;

      this.AREA_TYPES = AREA_TYPES;
      this.AREA_TYPES_OBJ = AREA_TYPES_OBJ;
      this.DEFAULT_AREA_TYPE = DEFAULT_AREA_TYPE;

      this.GROUP_INTERVALS = GROUP_INTERVALS;

      this.ARTIFACT_ICON_TYPES_OBJ = ARTIFACT_ICON_TYPES_OBJ;

      this.ANALYZE_PIVOT_SETTINGS_SIDENAV_ID = ANALYZE_PIVOT_SETTINGS_SIDENAV_ID;

      this.DATE_TYPES = DATE_TYPES;

      this._FilterService = FilterService;
      this._AnalyzeService = AnalyzeService;
      this._$mdSidenav = $mdSidenav;
      this._$translate = $translate;
    }

    $onInit() {
      this._$translate(map(GROUP_INTERVALS, 'label')).then(translations => {
        forEach(GROUP_INTERVALS, groupInterval => {
          groupInterval.label = translations[groupInterval.label];
        });
      });
    }

    openMenu($mdMenu, ev) {
      $mdMenu.open(ev);
    }

    applySettings(artifactColumns) {
      // this._$mdSidenav(ANALYZE_PIVOT_SETTINGS_SIDENAV_ID).close();
      this.onApplySettings({columns: artifactColumns});
    }

    onChecked(artifactColumn) {
      if (!artifactColumn.area) {
        artifactColumn.area = DEFAULT_AREA_TYPE.value;
        if (DATE_TYPES.includes(artifactColumn.type)) {
          artifactColumn.groupInterval = DEFAULT_GROUP_INTERVAL.value;
        }
      }
      if (!this.canBeChecked(artifactColumn)) {
        artifactColumn.checked = false;
        artifactColumn.area = null;
        artifactColumn.groupInterval = null;
      }
    }

    canBeChecked(artifactColumn) {
      // only 5 fields of the same type can be selected at a time
      const columnsWithSameArea = filter(this.artifactColumns,
        ({area}) => artifactColumn.checked && (artifactColumn.area === area));
      return columnsWithSameArea.length <= MAX_POSSIBLE_FILEDS_OF_SAME_AREA;
    }

    onSelectAreaType(area, artifactColumn) {
      artifactColumn.area = area;

      if (artifactColumn.area === 'data' && !artifactColumn.aggregate) {
        artifactColumn.aggregate = DEFAULT_AGGREGATE_TYPE.value;
      }
    }

    onSelectAggregateType(aggregateType, artifactColumn) {
      artifactColumn.aggregate = aggregateType.value;
    }

    onSelectGroupInterval(groupInterval, artifactColumn) {
      artifactColumn.groupInterval = groupInterval.value;
    }

    inputChanged(field) {
      this.onChange({field});
    }
  }
};
