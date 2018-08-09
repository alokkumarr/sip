import * as map from 'lodash/map';
import * as flatMap from 'lodash/flatMap';
import * as assign from 'lodash/assign';
import * as isEmpty from 'lodash/isEmpty';
import * as fpPipe from 'lodash/fp/pipe';
import * as fpOmit from 'lodash/fp/omit';
import * as fpMapValues from 'lodash/fp/mapValues';
import * as orderBy from 'lodash/orderBy';
import * as keys from 'lodash/keys';
import * as find from 'lodash/find';
import * as concat from 'lodash/concat';
import * as isUndefined from 'lodash/isUndefined';
import * as forEach from 'lodash/forEach';


export function flattenPivotData(data, sqlBuilder) {
  const nodeFieldMap = getNodeFieldMapPivot(sqlBuilder);

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
  if (node.key) {
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

/** parse the tree structure data and return a flattened array:
 * [{
 *   x: ..,
 *   y: ..,
 *   g: ..,
 *   z: ..
 * }, ..]
 */
export function flattenChartData(data, sqlBuilder) {
  const nodeFieldMap = getNodeFieldMapChart(sqlBuilder.nodeFields);
  const sorts = sqlBuilder.sorts;
  data = (sqlBuilder.dataFields.length < 2 && !isUndefined(sqlBuilder.dataFields[0].limitType)) ? parseLimit(data, sqlBuilder, false) : data;
  return fpPipe(
    nestedData => parseNodeChart(data, {}, nodeFieldMap, 1, sqlBuilder),
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

function parseLimit(data, sqlBuilder, flattenData) {
  let pasreLimitData = false;
  let groupBy = false;
  let dateExisitsInDimension = false;
  let dateExistsInGroupBy = false;
  forEach(sqlBuilder.nodeFields, field => {
    if (field.checked === 'g' && field.type !== 'date') {
      groupBy = true;
      pasreLimitData = false;
    } else if (field.checked === 'g' && field.type === 'date') {
      dateExistsInGroupBy = true;
    }
  });

  forEach(sqlBuilder.nodeFields, field => {
    if (field.checked === 'x' && field.type === 'date') {
      dateExisitsInDimension = true;
      if (!groupBy) {
        pasreLimitData = true;
      }
    }
  });

  //data = dateExistsInGroupBy ? parseUnflattenedData(data) : ((pasreLimitData && !dateExistsInGroupBy) ? sqlBuilder.dataFields[0].limitType === 'top' ? data.slice(0, sqlBuilder.dataFields[0].limitValue) : data.slice(Math.max(data.length - sqlBuilder.dataFields[0].limitValue, 1)) : data)

  if (dateExistsInGroupBy && !flattenData) {
    console.log("parse un flattneed data");
    data = parseUnflattenedData(data);
  }

  if (pasreLimitData && !dateExistsInGroupBy && flattenData) {
    console.log("front end parse");
    data = sqlBuilder.dataFields[0].limitType === 'top' ? data.slice(0, sqlBuilder.dataFields[0].limitValue) : data.slice(Math.max(data.length - sqlBuilder.dataFields[0].limitValue, 1));
  }
  return data;
}

function parseUnflattenedData(data) {
  console.log(data);
  return data;
}

function parseNodeChart(node, dataObj, nodeFieldMap, level, sqlBuilder) {
  if (!isUndefined(node.key)) {
    dataObj[nodeFieldMap[level - 2]] = node.key;
    if (!node.key) {
      dataObj[nodeFieldMap[level - 2]] = 'Undefined';
    }
  }
  //dataObj[nodeFieldMap[level - 2]] = !isUndefined(node.key) ? node.key
  const childNode = node[`node_field_${level}`];
  if (childNode) {
    let data = flatMap(childNode.buckets, bucket =>
      parseNodeChart(bucket, dataObj, nodeFieldMap, level + 1, sqlBuilder)
    );
    let data = (sqlBuilder.dataFields.length < 2 && !isUndefined(sqlBuilder.dataFields[0].limitType)) ? parseLimit(data, sqlBuilder, true) : data;
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
