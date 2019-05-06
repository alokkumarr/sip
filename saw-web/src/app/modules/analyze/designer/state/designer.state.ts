import { State, Action, StateContext, Selector } from '@ngxs/store';
import * as cloneDeep from 'lodash/cloneDeep';
import * as get from 'lodash/get';
import * as unset from 'lodash/unset';
import * as findIndex from 'lodash/findIndex';
import * as forEach from 'lodash/forEach';
import * as fpPipe from 'lodash/fp/pipe';
import * as fpFlatMap from 'lodash/fp/flatMap';
import * as fpReduce from 'lodash/fp/reduce';
import * as fpFilter from 'lodash/fp/filter';
import moment from 'moment';
// import { setAutoFreeze } from 'immer';
// import produce from 'immer';
import { moveItemInArray } from '@angular/cdk/drag-drop';
import { DesignerStateModel, DSLChartOptionsModel } from '../types';
import {
  DesignerInitGroupAdapters,
  DesignerAddColumnToGroupAdapter,
  DesignerMoveColumnInGroupAdapter,
  DesignerRemoveColumnFromGroupAdapter,
  DesignerClearGroupAdapters,
  DesignerInitEditAnalysis,
  DesignerInitForkAnalysis,
  DesignerInitNewAnalysis,
  DesignerUpdateAnalysisMetadata,
  DesignerUpdateAnalysisChartType,
  DesignerUpdateSorts,
  DesignerUpdateFilters,
  DesignerUpdatebooleanCriteria,
  DesignerUpdateAnalysisChartTitle,
  DesignerUpdateAnalysisChartInversion,
  DesignerUpdateAnalysisChartLegend,
  DesignerUpdateAnalysisChartLabelOptions,
  DesignerUpdateAnalysisChartXAxis,
  DesignerUpdateAnalysisChartYAxis,
  DesignerAddArtifactColumn,
  DesignerRemoveArtifactColumn,
  DesignerUpdateArtifactColumn,
  DesignerApplyChangesToArtifactColumns,
  DesignerRemoveAllArtifactColumns,
  DesignerLoadMetric,
  DesignerResetState,
  DesignerUpdatePivotGroupIntreval
} from '../actions/designer.actions';
import { DesignerService } from '../designer.service';
import {
  DATE_TYPES,
  DEFAULT_DATE_FORMAT,
  CUSTOM_DATE_PRESET_VALUE,
  CHART_DATE_FORMATS_OBJ
} from '../../consts';

// setAutoFreeze(false);

const defaultDesignerState: DesignerStateModel = {
  groupAdapters: [],
  analysis: null,
  metric: null
};

const defaultDSLChartOptions: DSLChartOptionsModel = {
  chartTitle: null,
  chartType: null,
  isInverted: false,
  legend: {
    align: '',
    layout: ''
  },
  labelOptions: {
    enabled: false,
    value: ''
  },
  xAxis: {
    title: null
  },
  yAxis: {
    title: null
  }
};

@State<DesignerStateModel>({
  name: 'designerState',
  defaults: <DesignerStateModel>cloneDeep(defaultDesignerState)
})
export class DesignerState {
  constructor(private _designerService: DesignerService) {}

  @Selector()
  static groupAdapters(state: DesignerStateModel) {
    return state.groupAdapters;
  }

  @Action(DesignerLoadMetric)
  async loadMetrics(
    { patchState }: StateContext<DesignerStateModel>,
    { metric }: DesignerLoadMetric
  ) {
    patchState({
      metric: {
        metricName: metric.metricName,
        artifacts: metric.artifacts
      }
    });
  }

  @Action(DesignerAddArtifactColumn)
  addArtifactColumn(
    { getState, patchState, dispatch }: StateContext<DesignerStateModel>,
    { artifactColumn }: DesignerAddArtifactColumn
  ) {
    const analysis = getState().analysis;
    const sipQuery = analysis.sipQuery;
    let artifacts = sipQuery.artifacts;
    const isDateType = DATE_TYPES.includes(artifactColumn.type);

    /* If analysis is chart and this is a date field, assign a default
      groupInterval. For pivots, use dateInterval if available */
    const groupInterval = {
      groupInterval:
        analysis.type === 'chart' && artifactColumn.type === 'date'
          ? CHART_DATE_FORMATS_OBJ[
              artifactColumn.dateFormat || <string>artifactColumn.format
            ].groupInterval
          : artifactColumn.dateInterval
    };

    const artifactsName =
      artifactColumn.table || (<any>artifactColumn).tableName;

    /* Find the artifact inside sipQuery of analysis stored in state */
    const artifactIndex = artifacts.findIndex(
      artifact => artifact.artifactsName === artifactsName
    );
    const artifactColumnToBeAdded = {
      aggregate: artifactColumn.aggregate,
      alias: artifactColumn.alias,
      area: artifactColumn.area,
      columnName: artifactColumn.columnName,
      displayType:
        artifactColumn.displayType || (<any>artifactColumn).comboType,
      dataField: artifactColumn.name || artifactColumn.columnName,
      displayName: artifactColumn.displayName,
      ...groupInterval,
      name: artifactColumn.name,
      type: artifactColumn.type,
      table: artifactColumn.table || (<any>artifactColumn).tableName,
      ...(isDateType
        ? {
            dateFormat:
              <string>artifactColumn.format || DEFAULT_DATE_FORMAT.value
          }
        : { format: artifactColumn.format })
    };

    if (artifactIndex < 0) {
      artifacts = [
        ...artifacts,
        { artifactsName, fields: [artifactColumnToBeAdded] }
      ];
    } else {
      artifacts[artifactIndex].fields = [
        ...artifacts[artifactIndex].fields,
        artifactColumnToBeAdded
      ];
    }

    patchState({
      analysis: { ...analysis, sipQuery: { ...sipQuery, artifacts } }
    });
    return dispatch(new DesignerApplyChangesToArtifactColumns());
  }

  @Action(DesignerRemoveArtifactColumn)
  removeArtifactColumn(
    { getState, patchState, dispatch }: StateContext<DesignerStateModel>,
    { artifactColumn }: DesignerRemoveArtifactColumn
  ) {
    const analysis = getState().analysis;
    const sipQuery = analysis.sipQuery;
    const artifacts = sipQuery.artifacts;

    /* Find the artifact inside sipQuery of analysis stored in state */
    const artifactsName =
      artifactColumn.table || (<any>artifactColumn).tableName;
    const artifactIndex = artifacts.findIndex(
      artifact => artifact.artifactsName === artifactsName
    );

    if (artifactIndex < 0) {
      return patchState({});
    }

    const artifactColumnIndex = artifacts[artifactIndex].fields.findIndex(
      field => field.columnName === artifactColumn.columnName
    );

    artifacts[artifactIndex].fields.splice(artifactColumnIndex, 1);

    patchState({
      analysis: { ...analysis, sipQuery: { ...sipQuery, artifacts } }
    });
    return dispatch(new DesignerApplyChangesToArtifactColumns());
  }

  @Action(DesignerUpdateArtifactColumn)
  updateArtifactColumn(
    { getState, patchState }: StateContext<DesignerStateModel>,
    { artifactColumn }: DesignerUpdateArtifactColumn
  ) {
    const { analysis, groupAdapters } = getState();
    const sipQuery = analysis.sipQuery;
    const artifacts = sipQuery.artifacts;

    /* Find the artifact inside sipQuery of analysis stored in state */
    const artifactsName =
      artifactColumn.table || (<any>artifactColumn).tableName;
    const artifactIndex = artifacts.findIndex(
      artifact => artifact.artifactsName === artifactsName
    );

    if (artifactIndex < 0) {
      return patchState({});
    }

    const artifactColumnIndex = artifacts[artifactIndex].fields.findIndex(
      field => field.columnName === artifactColumn.columnName
    );

    artifacts[artifactIndex].fields[artifactColumnIndex] = {
      ...artifacts[artifactIndex].fields[artifactColumnIndex],
      ...artifactColumn
    };

    const targetAdapterIndex = findIndex(
      groupAdapters,
      adapter =>
        adapter.marker ===
        artifacts[artifactIndex].fields[artifactColumnIndex].area
    );
    const targetAdapter = groupAdapters[targetAdapterIndex];
    const adapterColumnIndex = findIndex(
      targetAdapter.artifactColumns,
      col => col.columnName === artifactColumn.columnName
    );
    const adapterColumn = targetAdapter.artifactColumns[adapterColumnIndex];

    forEach(artifactColumn, (value, prop) => {
      adapterColumn[prop] = value;
    });
    return patchState({
      analysis: {
        ...analysis,
        sipQuery: { ...sipQuery, artifacts }
      },
      groupAdapters: [...groupAdapters]
    });
  }

  @Action(DesignerApplyChangesToArtifactColumns)
  reorderArtifactColumns({
    getState,
    patchState
  }: StateContext<DesignerStateModel>) {
    const { analysis, groupAdapters } = getState();
    const sipQuery = analysis.sipQuery;
    const artifacts = sipQuery.artifacts;

    // reorder artifactColumns
    const areaIndexMap = fpPipe(
      fpFlatMap(adapter => adapter.artifactColumns),
      fpReduce((accumulator, artifactColumn) => {
        accumulator[artifactColumn.columnName] = artifactColumn.areaIndex;
        return accumulator;
      }, {})
    )(groupAdapters);

    forEach(artifacts, artifact => {
      forEach(artifact.fields, field => {
        field.areaIndex = areaIndexMap[field.columnName];
      });
    });

    // unset fetch limit if there are more thatn 1 y fields

    const dataFields = fpPipe(
      fpFlatMap(artifact => artifact.fields),
      fpFilter(field => field.area === 'y')
    )(artifacts);

    if (dataFields.length === 2) {
      forEach(dataFields, field => {
        unset(field, 'limitType');
        unset(field, 'limitValue');
      });
    }
  }

  @Action(DesignerRemoveAllArtifactColumns)
  removeAllArtifactColumns({
    patchState,
    getState
  }: StateContext<DesignerStateModel>) {
    const analysis = getState().analysis;
    const sipQuery = analysis.sipQuery;
    const artifacts = (sipQuery.artifacts || []).map(artifact => ({
      ...artifact,
      fields: []
    }));

    return patchState({
      analysis: { ...analysis, sipQuery: { ...sipQuery, artifacts } }
    });
  }

  @Action(DesignerUpdateAnalysisMetadata)
  updateCategoryId(
    { patchState, getState }: StateContext<DesignerStateModel>,
    { metadata }: DesignerUpdateAnalysisMetadata
  ) {
    const analysis = getState().analysis;
    return patchState({
      analysis: { ...analysis, ...metadata }
    });
  }

  @Action(DesignerUpdateAnalysisChartType)
  updateChartType(
    { patchState, getState }: StateContext<DesignerStateModel>,
    { chartType }: DesignerUpdateAnalysisChartType
  ) {
    const analysis = getState().analysis;
    const chartOptions = analysis.chartOptions || defaultDSLChartOptions;
    return patchState({
      analysis: {
        ...analysis,
        chartOptions: { ...chartOptions, chartType }
      }
    });
  }

  @Action(DesignerUpdateAnalysisChartInversion)
  updateChartInversion(
    { patchState, getState }: StateContext<DesignerStateModel>,
    { isInverted }: DesignerUpdateAnalysisChartInversion
  ) {
    const analysis = getState().analysis;
    const chartOptions = analysis.chartOptions || defaultDSLChartOptions;
    return patchState({
      analysis: {
        ...analysis,
        chartOptions: { ...chartOptions, isInverted }
      }
    });
  }

  @Action(DesignerUpdateAnalysisChartTitle)
  updateChartTitle(
    { patchState, getState }: StateContext<DesignerStateModel>,
    { chartTitle }: DesignerUpdateAnalysisChartTitle
  ) {
    const analysis = getState().analysis;
    const chartOptions = analysis.chartOptions || defaultDSLChartOptions;
    return patchState({
      analysis: {
        ...analysis,
        chartOptions: { ...chartOptions, chartTitle }
      }
    });
  }

  @Action(DesignerUpdateAnalysisChartLegend)
  updateChartLegend(
    { patchState, getState }: StateContext<DesignerStateModel>,
    { legend }: DesignerUpdateAnalysisChartLegend
  ) {
    const analysis = getState().analysis;
    const chartOptions = analysis.chartOptions || defaultDSLChartOptions;
    return patchState({
      analysis: {
        ...analysis,
        chartOptions: { ...chartOptions, legend }
      }
    });
  }

  @Action(DesignerUpdateAnalysisChartLabelOptions)
  updateChartLabelOptions(
    { patchState, getState }: StateContext<DesignerStateModel>,
    { labelOptions }: DesignerUpdateAnalysisChartLabelOptions
  ) {
    const analysis = getState().analysis;
    const chartOptions = analysis.chartOptions || defaultDSLChartOptions;
    return patchState({
      analysis: {
        ...analysis,
        chartOptions: { ...chartOptions, labelOptions }
      }
    });
  }

  @Action(DesignerUpdateAnalysisChartXAxis)
  updateChartXAxis(
    { patchState, getState }: StateContext<DesignerStateModel>,
    { xAxis }: DesignerUpdateAnalysisChartXAxis
  ) {
    const analysis = getState().analysis;
    const chartOptions = analysis.chartOptions || defaultDSLChartOptions;
    return patchState({
      analysis: {
        ...analysis,
        chartOptions: { ...chartOptions, xAxis }
      }
    });
  }

  @Action(DesignerUpdateAnalysisChartYAxis)
  updateChartYAxis(
    { patchState, getState }: StateContext<DesignerStateModel>,
    { yAxis }: DesignerUpdateAnalysisChartYAxis
  ) {
    const analysis = getState().analysis;
    const chartOptions = analysis.chartOptions || defaultDSLChartOptions;
    return patchState({
      analysis: {
        ...analysis,
        chartOptions: { ...chartOptions, yAxis }
      }
    });
  }

  @Action(DesignerUpdateSorts)
  updateSorts(
    { patchState, getState }: StateContext<DesignerStateModel>,
    { sorts }: DesignerUpdateSorts
  ) {
    const analysis = getState().analysis;
    const sipQuery = analysis.sipQuery;
    return patchState({
      analysis: { ...analysis, sipQuery: { ...sipQuery, sorts } }
    });
  }

  @Action(DesignerInitEditAnalysis)
  @Action(DesignerInitForkAnalysis)
  @Action(DesignerInitNewAnalysis)
  initAnalysis(
    { patchState }: StateContext<DesignerStateModel>,
    {
      analysis
    }:
      | DesignerInitNewAnalysis
      | DesignerInitEditAnalysis
      | DesignerInitForkAnalysis
  ) {
    return patchState({ analysis });
  }

  @Action(DesignerInitGroupAdapters)
  initGroupAdapter({ patchState, getState }: StateContext<DesignerStateModel>) {
    const analysis = getState().analysis;
    const { type } = analysis;
    const fields = get(analysis, 'artifacts[0].columns', []);
    let groupAdapters;
    switch (type) {
      case 'pivot':
        groupAdapters = this._designerService.getPivotGroupAdapters(fields);
        break;
      case 'chart':
        const { chartOptions } = analysis;
        groupAdapters = this._designerService.getChartGroupAdapters(
          fields,
          chartOptions.chartType
        );
        break;
      case 'map':
        const { mapOptions } = analysis;
        groupAdapters = this._designerService.getMapGroupAdapters(
          fields,
          mapOptions.mapType
        );
        break;
      default:
        groupAdapters = [];
        break;
    }
    return patchState({ groupAdapters });
  }

  @Action(DesignerAddColumnToGroupAdapter)
  addColumnToGroupAdapter(
    { patchState, getState, dispatch }: StateContext<DesignerStateModel>,
    {
      artifactColumn,
      columnIndex,
      adapterIndex
    }: DesignerAddColumnToGroupAdapter
  ) {
    const groupAdapters = getState().groupAdapters;
    const adapter = groupAdapters[adapterIndex];

    adapter.artifactColumns.splice(columnIndex, 0, artifactColumn);
    // disabled immer because having immutability for groupAdapters causes conflicts in the designer
    // so it will stay disabled until a refactoring of the whole designer to ngxs
    // const groupAdapters = produce(getState().groupAdapters, draft => {
    //   draft[adapterIndex].artifactColumns.splice(
    //     columnIndex,
    //     0,
    //     artifactColumn
    //   );
    // });

    adapter.transform(artifactColumn);
    adapter.onReorder(adapter.artifactColumns);
    patchState({ groupAdapters: [...groupAdapters] });
    return dispatch(new DesignerAddArtifactColumn(artifactColumn));
  }

  @Action(DesignerClearGroupAdapters)
  clearGroupAdapters(
    { patchState, getState, dispatch }: StateContext<DesignerStateModel>,
    {  }: DesignerClearGroupAdapters
  ) {
    const groupAdapters = getState().groupAdapters;

    forEach(groupAdapters, adapter => {
      forEach(adapter.artifactColumns, column => {
        adapter.reverseTransform(column);
      });

      adapter.artifactColumns = [];
    });
    patchState({ groupAdapters: [...groupAdapters] });
    return dispatch(new DesignerRemoveAllArtifactColumns());
  }

  @Action(DesignerRemoveColumnFromGroupAdapter)
  removeColumnFromGroupAdapter(
    { patchState, getState, dispatch }: StateContext<DesignerStateModel>,
    { columnIndex, adapterIndex }: DesignerRemoveColumnFromGroupAdapter
  ) {
    const groupAdapters = getState().groupAdapters;
    const adapter = groupAdapters[adapterIndex];
    const column = adapter.artifactColumns[columnIndex];
    adapter.reverseTransform(column);
    groupAdapters[adapterIndex].artifactColumns.splice(columnIndex, 1);
    // const updatedGroupAdapters = produce(groupAdapters, draft => {
    //   draft[adapterIndex].artifactColumns.splice(columnIndex, 1);
    // });
    const updatedAdapter = groupAdapters[adapterIndex];
    adapter.onReorder(updatedAdapter.artifactColumns);
    patchState({ groupAdapters: [...groupAdapters] });
    return dispatch(new DesignerRemoveArtifactColumn(column));
  }

  @Action(DesignerMoveColumnInGroupAdapter)
  moveColumnInGroupAdapter(
    { patchState, getState, dispatch }: StateContext<DesignerStateModel>,
    {
      previousColumnIndex,
      currentColumnIndex,
      adapterIndex
    }: DesignerMoveColumnInGroupAdapter
  ) {
    const groupAdapters = getState().groupAdapters;
    const adapter = groupAdapters[adapterIndex];
    const columns = adapter.artifactColumns;
    moveItemInArray(columns, previousColumnIndex, currentColumnIndex);
    // const groupAdapters = produce(getState().groupAdapters, draft => {
    //   const adapter = draft[adapterIndex];
    //   const columns = adapter.artifactColumns;
    //   moveItemInArray(columns, previousColumnIndex, currentColumnIndex);
    // });
    adapter.onReorder(adapter.artifactColumns);
    patchState({ groupAdapters: [...groupAdapters] });
    return dispatch(new DesignerApplyChangesToArtifactColumns());
  }

  @Action(DesignerUpdateFilters)
  updateFilters(
    { patchState, getState }: StateContext<DesignerStateModel>,
    { filters }: DesignerUpdateFilters
  ) {
    const analysis = getState().analysis;
    const sipQuery = analysis.sipQuery;
    filters.forEach(filter => {
      filter.artifactsName = filter.tableName;
      if (
        filter.type === 'date' &&
        !filter.isRuntimeFilter &&
        !filter.isGlobalFilter &&
        filter.model.preset === CUSTOM_DATE_PRESET_VALUE
      ) {
        filter.model = {
          operator: 'BTW',
          otherValue: filter.model.lte
            ? moment(filter.model.lte).valueOf()
            : null,
          value: filter.model.gte ? moment(filter.model.gte).valueOf() : null,
          format: 'epoch_millis'
        };
      }
    });
    return patchState({
      analysis: { ...analysis, sipQuery: { ...sipQuery, filters } }
    });
  }

  @Action(DesignerUpdatebooleanCriteria)
  updatebooleanCriteria(
    { patchState, getState }: StateContext<DesignerStateModel>,
    { booleanCriteria }: DesignerUpdatebooleanCriteria
  ) {
    const analysis = getState().analysis;
    const sipQuery = analysis.sipQuery;
    return patchState({
      analysis: { ...analysis, sipQuery: { ...sipQuery, booleanCriteria } }
    });
  }

  @Action(DesignerResetState)
  resetState({ patchState }: StateContext<DesignerStateModel>) {
    patchState(cloneDeep(defaultDesignerState));
  }

  @Action(DesignerUpdatePivotGroupIntreval)
  updatePivotGroupIntreval(
    { patchState, getState }: StateContext<DesignerStateModel>,
    { artifactColumn }: DesignerUpdatePivotGroupIntreval
  ) {
    const analysis = getState().analysis;
    const sipQuery = analysis.sipQuery;
    sipQuery.artifacts.forEach(table => {
      if (table.artifactsName === artifactColumn.table) {
        table.fields.forEach(row => {
          if (row.columnName === artifactColumn.columnName) {
            row.groupInterval = artifactColumn.dateInterval;
            delete row.format;
          }
        });
      }
    });
    return patchState({
      analysis: { ...analysis, sipQuery: { ...sipQuery } }
    });
  }
}
