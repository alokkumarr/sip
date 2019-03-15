package com.synchronoss;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import com.synchronoss.querybuilder.model.globalfilter.GlobalFilters;
import com.synchronoss.querybuilder.model.kpi.KPIBuilder;
import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import org.threeten.extra.YearQuarter;

/** @author spau0004 */
public class BuilderUtil {

  public static final String SUFFIX = ".keyword";
  public static final int SIZE =
      ((System.getProperty("aggr.es.size") != null
              && !System.getProperty("aggr.es.size").equals(""))
          ? Integer.parseInt(System.getProperty("aggr.es.size"))
          : 1000);

  /**
   * This method is used to load the json string to object tree.
   *
   * @param jsonString
   * @return
   * @throws JsonProcessingException
   * @throws IOException
   * @throws ProcessingException
   */
  public static com.synchronoss.querybuilder.model.pivot.SqlBuilder getNodeTree(
      String jsonString, String node)
      throws JsonProcessingException, IOException, ProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    JsonNode objectNode = objectMapper.readTree(jsonString);
    JsonNode sqlNode = objectNode.get(node);
    // schema validation block starts here
    String json = "{ \"sqlBuilder\" :" + sqlNode.toString() + "}";
    JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
    JsonValidator validator = factory.getValidator();
    final JsonNode data = JsonLoader.fromString(json);
    String pivot = System.getProperty("schema.pivot");
    final JsonNode schema = JsonLoader.fromFile(new File(pivot));
    ProcessingReport report = validator.validate(schema, data);
    if (report.isSuccess() == false) {
      throw new ProcessingException(report.toString());
    }
    // schema validation block ends here
    com.synchronoss.querybuilder.model.pivot.SqlBuilder sqlBuilderNode =
        objectMapper.treeToValue(
            sqlNode, com.synchronoss.querybuilder.model.pivot.SqlBuilder.class);
    return sqlBuilderNode;
  }

  public static com.synchronoss.querybuilder.model.chart.SqlBuilder getNodeTreeChart(
      String jsonString, String node)
      throws JsonProcessingException, IOException, ProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    JsonNode objectNode = objectMapper.readTree(jsonString);
    JsonNode sqlNode = objectNode.get(node);
    // schema validation block starts here
    String json = "{ \"sqlBuilder\" :" + sqlNode.toString() + "}";
    JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
    JsonValidator validator = factory.getValidator();
    String chart = System.getProperty("schema.chart");
    if (chart == null) {
      throw new NullPointerException("schema.chart property is not set.");
    }
    final JsonNode data = JsonLoader.fromString(json);
    final JsonNode schema = JsonLoader.fromFile(new File(chart));
    ProcessingReport report = validator.validate(schema, data);
    if (report.isSuccess() == false) {
      throw new ProcessingException(report.toString());
    }
    // schema validation block ends here
    JsonNode objectNode1 = objectMapper.readTree(json);
    com.synchronoss.querybuilder.model.chart.SqlBuilderChart sqlBuilderNodeChart =
        objectMapper.treeToValue(
            objectNode1, com.synchronoss.querybuilder.model.chart.SqlBuilderChart.class);
    com.synchronoss.querybuilder.model.chart.SqlBuilder sqlBuilderNode =
        sqlBuilderNodeChart.getSqlBuilder();
    return sqlBuilderNode;
  }

  public static GlobalFilters getNodeTreeGlobalFilters(String jsonString, String node)
      throws JsonProcessingException, IOException, ProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    JsonNode objectNode = objectMapper.readTree(jsonString);
    GlobalFilters globalFilters = objectMapper.treeToValue(objectNode, GlobalFilters.class);
    return globalFilters;
  }

  public static KPIBuilder getNodeTreeKPIBuilder(String jsonString)
      throws JsonProcessingException, IOException, ProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    JsonNode objectNode = objectMapper.readTree(jsonString);
    KPIBuilder kpiBuilder = objectMapper.treeToValue(objectNode, KPIBuilder.class);
    return kpiBuilder;
  }

  public static com.synchronoss.querybuilder.model.report.SqlBuilder getNodeTreeReport(
      String jsonString, String node) throws IOException, ProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    JsonNode objectNode = objectMapper.readTree(jsonString);
    JsonNode sqlNode = objectNode.get(node);
    // schema validation block starts here
    String json = "{ \"sqlBuilder\" :" + sqlNode.toString() + "}";
    JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
    JsonValidator validator = factory.getValidator();
    String reportSchema = System.getProperty("schema.report");
    if (reportSchema == null) {
      throw new NullPointerException("schema.report property is not set.");
    }
    final JsonNode data = JsonLoader.fromString(json);
    final JsonNode schema = JsonLoader.fromFile(new File(reportSchema));
    ProcessingReport report = validator.validate(schema, data);
    if (report.isSuccess() == false) {
      throw new ProcessingException(report.toString());
    }
    // schema validation block ends here
    JsonNode objectNode1 = objectMapper.readTree(json);
    com.synchronoss.querybuilder.model.report.SqlBuilderReport sqlBuilderNodeReport =
        objectMapper.treeToValue(
            objectNode1, com.synchronoss.querybuilder.model.report.SqlBuilderReport.class);
    com.synchronoss.querybuilder.model.report.SqlBuilder sqlBuilderNode =
        sqlBuilderNodeReport.getSqlBuilder();
    return sqlBuilderNode;
  }

  public static JsonNode getRepositoryNodeTree(String jsonString, String node)
      throws JsonProcessingException, IOException {

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    JsonNode objectNode = objectMapper.readTree(jsonString);
    JsonNode repository = objectNode.get(node);
    return repository;
  }

  public static String listToJSONString(List<Object> objects) throws JsonProcessingException {
    String jsonString = null;
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    jsonString = objectMapper.writeValueAsString(objects);
    return jsonString;
  }

  public static String constructDSKCompatibleString(String dskJSONString)
      throws JsonProcessingException {
    return "{\"dataSecurityKey\":" + dskJSONString + "}";
  }

  public static DynamicConvertor dynamicDecipher(String dynamic) {
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String DATE_FORMAT_LTE = "23:59:59";
    String DATE_FORMAT_GTE = "00:00:00";
    String SPACE = " ";
    DynamicConvertor dynamicConvertor = new DynamicConvertor();
    LocalDateTime now = LocalDateTime.now();
    switch (dynamic) {
      case "Yesterday":
        LocalDateTime yesterday = now.minusDays(1);
        dynamicConvertor.setLte(yesterday.format(dateTimeFormatter) + SPACE + DATE_FORMAT_LTE);
        dynamicConvertor.setGte(yesterday.format(dateTimeFormatter) + SPACE + DATE_FORMAT_GTE);
        break;
      case "Today":
        {
          LocalDateTime today = now;
          dynamicConvertor.setLte(today.format(dateTimeFormatter) + SPACE + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(today.format(dateTimeFormatter) + SPACE + DATE_FORMAT_GTE);
          break;
        }
      case "YTD":
        {
          LocalDateTime firstDay = now.with(TemporalAdjusters.firstDayOfYear());
          dynamicConvertor.setLte(now.format(dateTimeFormatter) + SPACE + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(firstDay.format(dateTimeFormatter) + SPACE + DATE_FORMAT_GTE);
          break;
        }
      case "MTD":
        {
          LocalDateTime firstDayOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
          dynamicConvertor.setLte(now.format(dateTimeFormatter) + SPACE + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(
              firstDayOfMonth.format(dateTimeFormatter) + SPACE + DATE_FORMAT_GTE);
          break;
        }
      case "LTM":
        {
          LocalDateTime last3Month = now.minusMonths(3);
          dynamicConvertor.setLte(
              now.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(
              last3Month.with(TemporalAdjusters.firstDayOfMonth()).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_GTE);
          break;
        }
      case "LSM":
        {
          LocalDateTime last6Months = now.minusMonths(6);
          dynamicConvertor.setLte(
              now.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(
              last6Months.with(TemporalAdjusters.firstDayOfMonth()).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_GTE);
          break;
        }
      case "LY":
        {
          LocalDateTime currentDayOflastYearDate = now.minusMonths(12);
          dynamicConvertor.setLte(
              currentDayOflastYearDate
                      .with(TemporalAdjusters.lastDayOfYear())
                      .format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(
              currentDayOflastYearDate
                      .with(TemporalAdjusters.firstDayOfYear())
                      .format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_GTE);
          break;
        }
      case "LM":
        {
          LocalDateTime lastMonth = now.minusMonths(1);
          dynamicConvertor.setLte(
              lastMonth.with(TemporalAdjusters.lastDayOfMonth()).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(
              lastMonth.with(TemporalAdjusters.firstDayOfMonth()).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_GTE);
          break;
        }
      case "LQ":
        {
          YearQuarter quarter = YearQuarter.now();
          dynamicConvertor.setLte(
              quarter.minusQuarters(1).atEndOfQuarter().toString() + SPACE + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(
              quarter.minusQuarters(1).atDay(1).toString() + SPACE + DATE_FORMAT_GTE);
          break;
        }
      case "LW":
        {
          DayOfWeek firstDayOfWeek = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
          LocalDateTime lastWeek = now.minusWeeks(1);
          LocalDateTime startOfWeek =
              lastWeek.with(TemporalAdjusters.previousOrSame(firstDayOfWeek.plus(1)));
          LocalDateTime endOfWeek = lastWeek.with(TemporalAdjusters.nextOrSame(firstDayOfWeek));
          dynamicConvertor.setLte(endOfWeek.format(dateTimeFormatter) + SPACE + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(startOfWeek.format(dateTimeFormatter) + SPACE + DATE_FORMAT_GTE);
          break;
        }
      case "LSW":
        {
          DayOfWeek firstDayOfWeek = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
          LocalDateTime lastWeek = now.minusWeeks(6);
          dynamicConvertor.setLte(
              now.with(DayOfWeek.MONDAY).minusDays(1).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(
              lastWeek.with(DayOfWeek.MONDAY).format(dateTimeFormatter) + SPACE + DATE_FORMAT_GTE);
          break;
        }
      case "TW":
        {
          DayOfWeek firstDayOfWeek = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
          LocalDateTime lastWeek = now;
          LocalDateTime startOfWeek =
              lastWeek.with(TemporalAdjusters.previousOrSame(firstDayOfWeek.plus(1)));
          dynamicConvertor.setLte(now.format(dateTimeFormatter) + SPACE + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(startOfWeek.format(dateTimeFormatter) + SPACE + DATE_FORMAT_GTE);
          break;
        }
      case "LTW":
        {
          LocalDateTime last2Week = now.minusWeeks(2);
          dynamicConvertor.setLte(
              now.with(DayOfWeek.MONDAY).minusDays(1).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(
              last2Week.with(DayOfWeek.MONDAY).format(dateTimeFormatter) + SPACE + DATE_FORMAT_GTE);
          break;
        }
      default:
        throw new IllegalArgumentException(dynamic + " not present");
    }

    return dynamicConvertor;
  }

  public static DynamicConvertor dynamicDecipherForPrior(String dynamic) {
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String DATE_FORMAT_LTE = "23:59:59";
    String DATE_FORMAT_GTE = "00:00:00";
    String SPACE = " ";
    DynamicConvertor dynamicConvertor = new DynamicConvertor();
    LocalDateTime now = LocalDateTime.now();
    DayOfWeek firstDayOfWeek = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
    switch (dynamic) {
      case "Yesterday":
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime dayBeforeYesterday = yesterday.minusDays(1);
        dynamicConvertor.setLte(
            dayBeforeYesterday.format(dateTimeFormatter) + SPACE + DATE_FORMAT_LTE);
        dynamicConvertor.setGte(
            dayBeforeYesterday.format(dateTimeFormatter) + SPACE + DATE_FORMAT_GTE);
        break;
      case "Today":
        {
          LocalDateTime yestday = now.minusDays(1);
          dynamicConvertor.setLte(yestday.format(dateTimeFormatter) + SPACE + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(yestday.format(dateTimeFormatter) + SPACE + DATE_FORMAT_GTE);
          break;
        }
      case "YTD":
        {
          LocalDateTime firstDayOfYear = now.with(TemporalAdjusters.firstDayOfYear());
          int calculatedDayDifference = now.getDayOfYear() - firstDayOfYear.getDayOfYear();
          LocalDateTime priorDayOfYear = firstDayOfYear.minusDays(calculatedDayDifference);
          dynamicConvertor.setLte(
              firstDayOfYear
                      .minusDays(1)
                      .with(TemporalAdjusters.lastDayOfYear())
                      .format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(
              priorDayOfYear.minusDays(1).format(dateTimeFormatter) + SPACE + DATE_FORMAT_GTE);
          break;
        }
      case "MTD":
        {
          LocalDateTime firstDayOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
          int calculatedDayDifference = now.getDayOfMonth() - firstDayOfMonth.getDayOfMonth();
          LocalDateTime lastMonth = firstDayOfMonth.minusDays(1);
          LocalDateTime priorDayOfMonth = lastMonth.minusDays(calculatedDayDifference);
          dynamicConvertor.setLte(lastMonth.format(dateTimeFormatter) + SPACE + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(
              priorDayOfMonth.format(dateTimeFormatter) + SPACE + DATE_FORMAT_GTE);
          break;
        }
      case "LTM":
        {
          LocalDateTime priorLast3Month = now.minusMonths(3);
          LocalDateTime last3Month = priorLast3Month.minusMonths(3);
          dynamicConvertor.setLte(
              priorLast3Month
                      .minusMonths(1)
                      .with(TemporalAdjusters.lastDayOfMonth())
                      .format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(
              last3Month.with(TemporalAdjusters.firstDayOfMonth()).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_GTE);
          break;
        }
      case "LSM":
        {
          LocalDateTime priorLast6Month = now.minusMonths(6);
          LocalDateTime last6Months = priorLast6Month.minusMonths(6);
          dynamicConvertor.setLte(
              priorLast6Month
                      .minusMonths(1)
                      .with(TemporalAdjusters.lastDayOfMonth())
                      .format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(
              last6Months.with(TemporalAdjusters.firstDayOfMonth()).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_GTE);
          break;
        }
      case "LM":
        {
          LocalDateTime priorLastMonth = now.minusMonths(1);
          LocalDateTime lastMonth = priorLastMonth.minusMonths(1);
          dynamicConvertor.setLte(
              lastMonth.with(TemporalAdjusters.lastDayOfMonth()).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(
              lastMonth.with(TemporalAdjusters.firstDayOfMonth()).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_GTE);
          break;
        }
      case "LY":
        {
          LocalDateTime priorLastYear = now.minusYears(1);
          LocalDateTime lastYear = priorLastYear.minusYears(1);
          dynamicConvertor.setLte(
              lastYear.with(TemporalAdjusters.lastDayOfYear()).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(
              lastYear.with(TemporalAdjusters.firstDayOfYear()).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_GTE);
          break;
        }
      case "LQ":
        {
          YearQuarter lastQuarter = YearQuarter.now().minusQuarters(1);
          YearQuarter priorQuarter = lastQuarter.now().minusQuarters(1);
          dynamicConvertor.setLte(
              priorQuarter.minusQuarters(1).atEndOfQuarter().toString() + SPACE + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(
              priorQuarter.minusQuarters(1).atDay(1).toString() + SPACE + DATE_FORMAT_GTE);
          break;
        }
      case "LW":
        {
          LocalDateTime priorLastWeek = now.minusWeeks(2);
          LocalDateTime startOfWeek =
              priorLastWeek.with(TemporalAdjusters.previousOrSame(firstDayOfWeek.plus(1)));
          LocalDateTime endOfWeek =
              priorLastWeek.with(TemporalAdjusters.nextOrSame(firstDayOfWeek));
          dynamicConvertor.setLte(endOfWeek.format(dateTimeFormatter) + SPACE + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(startOfWeek.format(dateTimeFormatter) + SPACE + DATE_FORMAT_GTE);
          break;
        }
      case "LSW":
        {
          LocalDateTime last6Week = now.minusWeeks(6);
          LocalDateTime priorlast6Week = last6Week.minusWeeks(6);
          dynamicConvertor.setLte(
              last6Week.with(DayOfWeek.MONDAY).minusDays(1).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(
              priorlast6Week.with(DayOfWeek.MONDAY).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_GTE);
          break;
        }
      case "TW":
        {
          LocalDateTime startOfWeek =
              now.with(TemporalAdjusters.previousOrSame(firstDayOfWeek.plus(1)));
          Long calculatedDayDifference = ChronoUnit.DAYS.between(startOfWeek, now);
          LocalDateTime week = startOfWeek.minusHours(24);
          LocalDateTime lastWeek = week.minusDays(calculatedDayDifference);
          dynamicConvertor.setLte(week.format(dateTimeFormatter) + SPACE + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(lastWeek.format(dateTimeFormatter) + SPACE + DATE_FORMAT_GTE);
          break;
        }
      case "LTW":
        {
          LocalDateTime last2Week = now.minusWeeks(2);
          LocalDateTime priorLast2Week = last2Week.minusWeeks(2);
          dynamicConvertor.setLte(
              last2Week.with(DayOfWeek.MONDAY).minusDays(1).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_LTE);
          dynamicConvertor.setGte(
              priorLast2Week.with(DayOfWeek.MONDAY).format(dateTimeFormatter)
                  + SPACE
                  + DATE_FORMAT_GTE);
          break;
        }
      default:
        throw new IllegalArgumentException(dynamic + " not present");
    }

    return dynamicConvertor;
  }
}
