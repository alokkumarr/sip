package com.synchronoss.saw.analysis.service.migrationservice;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.synchronoss.saw.analysis.modal.Analysis;
import com.synchronoss.saw.es.QueryBuilderUtil;
import com.synchronoss.saw.exceptions.MissingFieldException;
import com.synchronoss.saw.model.Axis;
import com.synchronoss.saw.model.ChartOptions;
import com.synchronoss.saw.model.Field;
import com.synchronoss.saw.model.Field.GroupInterval;
import com.synchronoss.saw.model.Field.LimitType;
import com.synchronoss.saw.model.Store;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ChartConverter implements AnalysisSipDslConverter {
  @Override
  public Analysis convert(JsonObject oldAnalysisDefinition) throws MissingFieldException {
    Analysis analysis = new Analysis();

    analysis = setCommonParams(analysis, oldAnalysisDefinition);

    // Extract artifact name from "artifacts"
    // NOTE: For charts and pivots, there will be only one object in artifacts. For reports,
    // there will be 2 objects
    String artifactName = null;

    JsonArray artifactsArray = oldAnalysisDefinition.getAsJsonArray("artifacts");

    // Handling artifact name for charts and pivots
    JsonObject artifact = artifactsArray.get(0).getAsJsonObject();
    artifactName = artifact.get("artifactName").getAsString();

    // Set chartProperties
    analysis.setChartOptions(createChartOptions(oldAnalysisDefinition));

    Store store = buildStoreObject(oldAnalysisDefinition);

    JsonElement sqlQueryBuilderElement = oldAnalysisDefinition.get("sqlBuilder");
    if (sqlQueryBuilderElement != null) {
      JsonObject sqlQueryBuilderObject = sqlQueryBuilderElement.getAsJsonObject();
      analysis.setSipQuery(
          generateSipQuery(artifactName, sqlQueryBuilderObject, artifactsArray, store));
    }
    return analysis;
  }

  @Override
  public List<Field> generateArtifactFields(JsonObject sqlBuilder, JsonArray artifactsArray) {
    List<Field> fields = new LinkedList<>();

    if (sqlBuilder.has("dataFields")) {
      JsonArray dataFields = sqlBuilder.getAsJsonArray("dataFields");

      for (JsonElement dataField : dataFields) {
        fields.add(buildArtifactField(dataField.getAsJsonObject(), artifactsArray));
      }
    }

    if (sqlBuilder.has("nodeFields")) {
      JsonArray nodeFields = sqlBuilder.getAsJsonArray("nodeFields");

      for (JsonElement dataField : nodeFields) {
        fields.add(buildArtifactField(dataField.getAsJsonObject(), artifactsArray));
      }
    }

    return fields;
  }

  @Override
  public Field buildArtifactField(JsonObject fieldObject, JsonArray artifactsArray) {
    Field field = new Field();
    field = setCommonFieldProperties(field, fieldObject, artifactsArray);

    if (fieldObject.has("comboType")) {
      field.setDisplayType(fieldObject.get("comboType").getAsString());
    }

    if (fieldObject.has("checked")) {
      String checkedVal = fieldObject.get("checked").getAsString();

      field.setArea(checkedVal);
    }

    if (field.getType() != null) {
      if (field.getType() == Field.Type.DATE) {

        field.setMinDocCount(0);
        String dateFormat = field.getDateFormat();
        String interval = QueryBuilderUtil.dateFormats.get(
                dateFormat.replaceAll(QueryBuilderUtil.SPACE_REGX, QueryBuilderUtil.EMPTY_STRING));
        switch (interval) {
          case "month":
            field.setGroupInterval(GroupInterval.MONTH);
            break;
          case "year":
            field.setGroupInterval(GroupInterval.YEAR);
            break;
          case "day":
            field.setGroupInterval(GroupInterval.DAY);
            break;
          case "hour":
            field.setGroupInterval(GroupInterval.HOUR);
            break;
          default:
            // throw new IllegalArgumentException(interval + " not present");
        }
      }
    }

    // Set limit fields
    // If limits are set in field object in sqlbuilder, that has to be used,
    // else it has to be checked in artifacts array

    if (fieldObject.has("limitType") && fieldObject.has("limitValue")) {
      LimitType limitType = LimitType.fromValue(fieldObject.get("limitType").getAsString());
      field.setLimitType(limitType);

      int limitValue = fieldObject.get("limitValue").getAsInt();
      field.setLimitValue(limitValue);
    } else {
      // Check if limit is set in artifacts
      String columnName = field.getColumnName();

      Limit limit = extractLimitArtifactsArray(columnName, artifactsArray);

      if (limit != null) {
        field.setLimitType(limit.limitType);
        field.setLimitValue(limit.limitValue);
      }
    }

    return field;
  }

  private Limit extractLimitArtifactsArray(String columnName, JsonArray artifactsArray) {
    Limit limit = null;

    for (JsonElement artifactElement : artifactsArray) {
      limit = getLimitFromArtifactObject(columnName, artifactElement.getAsJsonObject());

      if (limit != null) {
        break;
      }
    }

    return limit;
  }

  private Limit getLimitFromArtifactObject(String columnName, JsonObject artifactObject) {
    Limit limit = null;

    if (artifactObject.has("columns")) {
      JsonArray columns = artifactObject.getAsJsonArray("columns");

      for (JsonElement columnElement : columns) {
        JsonObject column = columnElement.getAsJsonObject();

        String artifactColumnName = column.get("columnName").getAsString();

        if (columnName.equalsIgnoreCase(artifactColumnName)) {
          if (column.has("limitType") && column.has("limitValue")) {
            String limitType = column.get("limitType").getAsString();

            int limitValue = column.get("limitValue").getAsInt();

            limit = new Limit();

            limit.limitType = LimitType.fromValue(limitType);

            limit.limitValue = limitValue;

            break;
          }
        }
      }
    }

    return limit;
  }

  private ChartOptions createChartOptions(JsonObject oldAnalysisDefinition) {
    Boolean isInverted = null;
    JsonObject legendObject = null;
    Map<Object, Object> legend = new LinkedHashMap<Object, Object>();
    String chartType = null;
    String chartTitle = null;
    JsonObject labelOptions = null;
    Map<String, String> label = new LinkedHashMap<String, String>();

    Axis xaxis = null;

    Axis yaxis = null;

    if (oldAnalysisDefinition.has("isInverted")) {
      isInverted = oldAnalysisDefinition.get("isInverted").getAsBoolean();
    }

    if (oldAnalysisDefinition.has("legend")) {
      legendObject = oldAnalysisDefinition.getAsJsonObject("legend");
      legendObject
          .entrySet()
          .forEach(
              (Map.Entry<String, JsonElement> entry) ->
                  legend.put(entry.getKey(), entry.getValue().getAsString()));
    }

    if (oldAnalysisDefinition.has("chartType")) {
      chartType = oldAnalysisDefinition.get("chartType").getAsString();
    }

    if (oldAnalysisDefinition.has("chartTitle")) {
      chartTitle = oldAnalysisDefinition.get("chartTitle").getAsString();
    }

    if (oldAnalysisDefinition.has("labelOptions")) {
      labelOptions = oldAnalysisDefinition.getAsJsonObject("labelOptions");
      labelOptions
          .entrySet()
          .forEach(
              (Map.Entry<String, JsonElement> entry) ->
                  label.put(entry.getKey(), entry.getValue().getAsString()));
    }

    if (oldAnalysisDefinition.has("xAxis")) {

      JsonObject xaxisObject = oldAnalysisDefinition.getAsJsonObject("xAxis");

      if (xaxisObject.has("title") && !xaxisObject.get("title").isJsonNull()) {
        xaxis = new Axis();
        xaxis.setTitle(xaxisObject.get("title").getAsString());
      }
    }

    if (oldAnalysisDefinition.has("yAxis")) {
      JsonObject yaxisObject = oldAnalysisDefinition.getAsJsonObject("yAxis");

      if (yaxisObject.has("title") && !yaxisObject.get("title").isJsonNull()) {
        yaxis = new Axis();
        yaxis.setTitle(yaxisObject.get("title").getAsString());
      }
    }
    ChartOptions chartOptions = new ChartOptions();

    chartOptions.setInverted(isInverted);
    chartOptions.setLegend(legend);
    chartOptions.setChartTitle(chartTitle);
    chartOptions.setChartType(chartType);
    chartOptions.setLabelOptions(label);
    chartOptions.setxAxis(xaxis);
    chartOptions.setyAxis(yaxis);
    return chartOptions;
  }

  class Limit {
    LimitType limitType;
    Integer limitValue;
  }
}
