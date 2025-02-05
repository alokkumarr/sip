import * as map from 'lodash/map';
import * as flatMap from 'lodash/flatMap';
import * as assign from 'lodash/assign';
import * as isEmpty from 'lodash/isEmpty';
import * as fpPipe from 'lodash/fp/pipe';
import * as fpOmit from 'lodash/fp/omit';
import * as fpMapValues from 'lodash/fp/mapValues';
import * as mapValues from 'lodash/mapValues';
import * as orderBy from 'lodash/orderBy';
import * as keys from 'lodash/keys';
import * as find from 'lodash/find';
import * as concat from 'lodash/concat';
import * as some from 'lodash/some';
import * as set from 'lodash/set';
import * as isUndefined from 'lodash/isUndefined';
import * as mapKeys from 'lodash/mapKeys';
import * as fpMap from 'lodash/fp/map';
import * as fpSplit from 'lodash/fp/split';
import * as fpFilter from 'lodash/fp/filter';
import * as fpPick from 'lodash/fp/pick';
import * as moment from 'moment';
import { ArtifactColumnDSL, QueryDSL, ChartOptions } from 'src/app/models';
import { NUMBER_TYPES } from './../consts';

export function substituteEmptyValues(data) {
  return fpPipe(
    fpMap(
      fpMapValues(value => {
        return value === '' ? 'Undefined' : value;
      })
    )
  )(data);
}

export function flattenPivotData(data, sipQuery) {
  data = alterDateInData(data, sipQuery);
  if (sipQuery.artifacts) {
    // const columnRowFields = sipQuery.artifacts[0].fields.filter(field =>
    //   ['row', 'column', 'data'].includes(field.area)
    // );
    // As per AC on 5216, if key is empty show undefined
    data = substituteEmptyValues(data);
    return data;
  }
  const nodeFieldMap = getNodeFieldMapPivot(sipQuery);
  return parseNodePivot(data, {}, nodeFieldMap, 0);
}

/** Map the tree level to the columnName of the field
 * Example:
 * row_field_1: 0 -> SOURCE_OS
 * row_field_2: 1 -> SOURCE_MANUFACTURER
 * column_field_1: 2 -> TARGET_OS
 */
function getNodeFieldMapPivot(sqlBuilder) {
  const rowFieldMap = map(sqlBuilder.rowFields, 'columnName');
  const columnFieldMap = map(sqlBuilder.columnFields, 'columnName');
  return concat(rowFieldMap, columnFieldMap);
}

function parseNodePivot(node, dataObj, nodeFieldMap, level) {
  if (!isUndefined(node.key)) {
    // As per AC on 5216, if key is empty show undefined
    node.key = isEmpty(node.key) ? 'undefined' : node.key;
    const columnName = getColumnName(nodeFieldMap, level);
    dataObj[columnName] = node.key_as_string || node.key;
  }

  const nodeName = getChildNodeName(node);
  if (nodeName && node[nodeName]) {
    const data = flatMap(node[nodeName].buckets, bucket =>
      parseNodePivot(bucket, dataObj, nodeFieldMap, level + 1)
    );
    return data;
  }
  const datum = parseLeafPivot(node, dataObj);

  return datum;
}

function parseLeafPivot(node, dataObj) {
  const dataFields = fpPipe(
    fpOmit(['doc_count', 'key', 'key_as_string']),
    fpMapValues('value')
  )(node);

  return {
    ...dataFields,
    ...dataObj
  };
}

function getColumnName(fieldMap, level) {
  // take out the .keyword form the columnName
  // if there is one
  const columnName = fieldMap[level - 1];
  const split = columnName.split('.');
  if (split[1]) {
    return split[0];
  }
  return columnName;
}

function getChildNodeName(node) {
  const nodeKeys = keys(node);
  const childNodeName = find(nodeKeys, key => {
    const isRow = key.indexOf('row_level') > -1;
    const isColumn = key.indexOf('column_level') > -1;
    return isRow || isColumn;
  });

  return childNodeName;
}

/** the mapping between the tree level, and the columName of the field
 * Example:
 * string_field_1: 0 -> SOURCE_OS (marker on the checked attribute)
 * string_field_2: 1 -> SOURCE_MANUFACTURER
 */
function getNodeFieldMapChart(nodeFields) {
  return map(nodeFields, 'columnName');
}

export function getStringFieldsFromDSLArtifact(
  fields: ArtifactColumnDSL[]
): string[] {
  return fields
    .filter(field => field.type === 'string' && !field.aggregate)
    .map(field => field.columnName.replace('.keyword', ''));
}

/** parse the tree structure data and return a flattened array:
 * [{
 *   x: ..,
 *   y: ..,
 *   g: ..,
 *   z: ..
 * }, ..]
 */
export function flattenChartData(data, sqlBuilder) {
  if (sqlBuilder.artifacts) {
    const stringFields = getStringFieldsFromDSLArtifact(
      sqlBuilder.artifacts[0].fields
    );
    if (stringFields.length === 0) {
      return data;
    } else {
      /* If any string data is blank, replace it with 'Undefined'. This avoids
      highcharts giving default 'Series 1' label to blank data
      */
      const result = data.map(row => {
        const res = { ...row };
        stringFields.forEach(field => {
          res[field] = res[field] || 'Undefined';
        });
        return res;
      });
      return result;
    }
  }

  const nodeFieldMap = getNodeFieldMapChart(sqlBuilder.nodeFields);
  const sorts = sqlBuilder.sorts;

  return fpPipe(
    nestedData => parseNodeChart(data, {}, nodeFieldMap, 1),
    flattenedData => {
      /* Order chart data manually. Backend doesn't sort chart data. */
      if (!isEmpty(sorts)) {
        return orderBy(
          flattenedData,
          map(sorts, 'columnName'),
          map(sorts, 'order')
        );
      }
      return flattenedData;
    }
  )(data);
}

export function wrapFieldValues(data) {
  return fpPipe(
    fpMap(
      fpMapValues(value => {
        return value === null ? null : `"${value}"`;
      })
    )
  )(data);
}

export function alterDateInData(data, sipQuery, analysisType = 'pivot') {
  if (isEmpty(data)) {
    return data;
  }
  const dateFields = [];
  flatMap(sipQuery.artifacts, artifact =>
    fpPipe(
      fpMap(fpPick(['columnName', 'type', 'aggregate', 'alias'])),
      fpFilter(({ type, columnName, aggregate, alias }) => {
        if (
          type === 'date' &&
          !['count', 'distinctCount', 'distinctcount'].includes(aggregate)
        ) {
          dateFields.push(
            analysisType === 'report' ? alias || columnName : columnName
          );
        }
      })
    )(artifact.fields)
  );

  return data.map(row => {
    return mapValues(row, (value, key) => {
      value = value === null ? 'null' : value;
      if (dateFields.includes(key)) {
        value = value.includes('Z')
          ? moment(value)
              .utc()
              .format('YYYY-MM-DD HH:mm:ss')
          : value;
      }
      return value;
    });
  });
}

export function flattenReportData(data, analysis) {
  if (analysis.designerEdit) {
    return data;
  }

  data = alterDateInData(data, analysis.sipQuery, analysis.type);

  return data.map(row => {
    return mapKeys(row, (value, key) => {
      /* If the column has aggregation, preserve the aggregate name when removing keyword */
      const hasAggregateFunction = key.includes('(') && key.includes(')');

      if (!hasAggregateFunction) {
        return removeKeyword(key);
      }

      const [, columnName] = fpPipe(fpSplit('('))(key);
      return removeKeyword(columnName.split(')')[0]);
    });
  });
}

function removeKeyword(key: string) {
  if (!key) {
    return key;
  }
  return key.replace('.keyword', '');
}

function parseNodeChart(node, dataObj, nodeFieldMap, level) {
  if (!isUndefined(node.key)) {
    dataObj[nodeFieldMap[level - 2]] = node.key;
    if (!node.key) {
      dataObj[nodeFieldMap[level - 2]] = 'Undefined';
    }
  }
  // dataObj[nodeFieldMap[level - 2]] = !isUndefined(node.key) ? node.key
  const childNode = node[`node_field_${level}`];
  if (childNode) {
    const data = flatMap(childNode.buckets, bucket =>
      parseNodeChart(bucket, dataObj, nodeFieldMap, level + 1)
    );
    return data;
  }
  const datum = parseLeafChart(node, dataObj);
  return datum;
}

function parseLeafChart(node, dataObj) {
  const dataFields = fpPipe(
    fpOmit(['doc_count', 'key', 'key_as_string']),
    fpMapValues('value')
  )(node);

  return assign(dataFields, dataObj);
}

/**
 * Includes a new property to chart options for the chart engine.
 * reversed instucts the highchart engine to plot the chart in descending order
 * which is needed when desc is applied for a field in x-axis.
 *
 * @param {*} chartOptions
 * @param {*} sipQuery
 * @returns {chartOptions}
 */

export function setReverseProperty(
  chartOptions: ChartOptions,
  sipQuery: QueryDSL
) {
  const xAxisField = find(
    sipQuery.artifacts[0].fields,
    field => field.area === 'x'
  );
  if (!NUMBER_TYPES.includes(xAxisField.type)) {
    return chartOptions;
  }
  const reversed = shouldReverseChart(sipQuery);
  set(chartOptions, 'xAxis.reversed', reversed);
  return chartOptions;
}

export function shouldReverseChart(sipQuery: QueryDSL) {
  const firstArtifactFields = sipQuery.artifacts[0].fields;
  const xAxisField = find(firstArtifactFields, ({ area }) => area === 'x');
  const xAxisColumnName = xAxisField.columnName;
  const sorts = sipQuery.sorts;
  if (!isEmpty(sorts)) {
    return some(
      sorts,
      ({ order, columnName }) =>
        order === 'desc' && columnName === xAxisColumnName
    );
  }
  return false;
}
