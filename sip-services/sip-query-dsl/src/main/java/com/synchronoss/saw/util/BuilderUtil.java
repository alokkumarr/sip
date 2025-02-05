package com.synchronoss.saw.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synchronoss.saw.model.Field;
import com.synchronoss.saw.model.Filter;
import com.synchronoss.saw.model.SipQuery.BooleanCriteria;
import com.synchronoss.saw.model.Sort;
import com.synchronoss.saw.model.globalfilter.GlobalFilters;
import com.synchronoss.saw.model.kpi.KPIBuilder;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.threeten.extra.YearQuarter;

public class BuilderUtil {

  public static final String VALUE = ".value";
  public static final String SUFFIX = ".keyword";
  public static final String BUCKET_SORT = "bucketSort";

  public static final int SIZE =
      ((System.getProperty("aggr.es.size") != null
              && !System.getProperty("aggr.es.size").equals(""))
          ? Integer.parseInt(System.getProperty("aggr.es.size"))
          : 1000);

  /**
   * Get Repository Node Tree.
   *
   * @param jsonString Json String
   * @param node Node
   * @return JsonNode
   * @throws JsonProcessingException when JSON failed to parse.
   * @throws IOException When unable to find json object.
   */
  public static JsonNode getRepositoryNodeTree(String jsonString, String node)
      throws JsonProcessingException, IOException {

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode objectNode = objectMapper.readTree(jsonString);
    JsonNode repository = objectNode.get(node);
    return repository;
  }

  /**
   * List to JSON String.
   *
   * @param objects List of Objects.
   * @return String
   * @throws JsonProcessingException when JSON parsing error
   */
  public static String listToJSONString(List<Object> objects) throws JsonProcessingException {
    String jsonString = null;
    ObjectMapper objectMapper = new ObjectMapper();
    jsonString = objectMapper.writeValueAsString(objects);
    return jsonString;
  }

  /**
   * Global filter.
   * @param jsonString
   * @param node
   * @return
   * @throws IOException
   */
  public static GlobalFilters getNodeTreeGlobalFilters(String jsonString, String node)
      throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    JsonNode objectNode = objectMapper.readTree(jsonString);
    GlobalFilters globalFilters = objectMapper.treeToValue(objectNode, GlobalFilters.class);
    return globalFilters;
  }

  /**
   * KPI.
   * @param jsonString
   * @return
   * @throws IOException
   */
  public static KPIBuilder getNodeTreeKPIBuilder(String jsonString) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    JsonNode objectNode = objectMapper.readTree(jsonString);
    KPIBuilder kpiBuilder = objectMapper.treeToValue(objectNode, KPIBuilder.class);
    return kpiBuilder;
  }

  /**
   * construct DSK Compatible String.
   *
   * @param dskJSONString
   * @return String
   * @throws JsonProcessingException When Json parsing issue.
   */
  public static String constructDSKCompatibleString(String dskJSONString)
      throws JsonProcessingException {
    return "{\"dataSecurityKey\":" + dskJSONString + "}";
  }

  /**
   * Create dynamic Decipher.
   *
   * @param dynamic
   * @return Dynamic Converter
   */
  public static DynamicConvertor dynamicDecipher(String dynamic) {
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String dateFormatLte = "23:59:59";
    String dateFormatGte = "00:00:00";
    String space = " ";
    DynamicConvertor dynamicConvertor = new DynamicConvertor();
    LocalDateTime now = LocalDateTime.now();
    switch (dynamic.toUpperCase()) {
      case "YESTERDAY":
        LocalDateTime yesterday = now.minusDays(1);
        dynamicConvertor.setLte(yesterday.format(dateTimeFormatter) + space + dateFormatLte);
        dynamicConvertor.setGte(yesterday.format(dateTimeFormatter) + space + dateFormatGte);
        break;
      case "TODAY":
        {
          LocalDateTime today = now;
          dynamicConvertor.setLte(today.format(dateTimeFormatter) + space + dateFormatLte);
          dynamicConvertor.setGte(today.format(dateTimeFormatter) + space + dateFormatGte);
          break;
        }
      case "YTD":
        {
          LocalDateTime firstDay = now.with(TemporalAdjusters.firstDayOfYear());
          dynamicConvertor.setLte(now.format(dateTimeFormatter) + space + dateFormatLte);
          dynamicConvertor.setGte(firstDay.format(dateTimeFormatter) + space + dateFormatGte);
          break;
        }
      case "MTD":
        {
          LocalDateTime firstDayOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
          dynamicConvertor.setLte(now.format(dateTimeFormatter) + space + dateFormatLte);
          dynamicConvertor.setGte(
              firstDayOfMonth.format(dateTimeFormatter) + space + dateFormatGte);
          break;
        }
      case "LTM":
        {
          LocalDateTime last3Month = now.minusMonths(3);
          dynamicConvertor.setLte(
              now.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()).format(dateTimeFormatter)
                  + space
                  + dateFormatLte);
          dynamicConvertor.setGte(
              last3Month.with(TemporalAdjusters.firstDayOfMonth()).format(dateTimeFormatter)
                  + space
                  + dateFormatGte);
          break;
        }
      case "LSM":
        {
          LocalDateTime last6Months = now.minusMonths(6);
          dynamicConvertor.setLte(
              now.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()).format(dateTimeFormatter)
                  + space
                  + dateFormatLte);
          dynamicConvertor.setGte(
              last6Months.with(TemporalAdjusters.firstDayOfMonth()).format(dateTimeFormatter)
                  + space
                  + dateFormatGte);
          break;
        }
      case "LY":
        {
          LocalDateTime currentDayOflastYearDate = now.minusMonths(12);
          dynamicConvertor.setLte(
              currentDayOflastYearDate
                      .with(TemporalAdjusters.lastDayOfYear())
                      .format(dateTimeFormatter)
                  + space
                  + dateFormatLte);
          dynamicConvertor.setGte(
              currentDayOflastYearDate
                      .with(TemporalAdjusters.firstDayOfYear())
                      .format(dateTimeFormatter)
                  + space
                  + dateFormatGte);
          break;
        }
      case "LM":
        {
          LocalDateTime lastMonth = now.minusMonths(1);
          dynamicConvertor.setLte(
              lastMonth.with(TemporalAdjusters.lastDayOfMonth()).format(dateTimeFormatter)
                  + space
                  + dateFormatLte);
          dynamicConvertor.setGte(
              lastMonth.with(TemporalAdjusters.firstDayOfMonth()).format(dateTimeFormatter)
                  + space
                  + dateFormatGte);
          break;
        }
      case "LQ":
        {
          YearQuarter quarter = YearQuarter.now();
          dynamicConvertor.setLte(
              quarter.minusQuarters(1).atEndOfQuarter().toString() + space + dateFormatLte);
          dynamicConvertor.setGte(
              quarter.minusQuarters(1).atDay(1).toString() + space + dateFormatGte);
          break;
        }
      case "LW":
        {
          DayOfWeek firstDayOfWeek = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
          LocalDateTime lastWeek = now.minusWeeks(1);
          LocalDateTime startOfWeek =
              lastWeek.with(TemporalAdjusters.previousOrSame(firstDayOfWeek.plus(1)));
          LocalDateTime endOfWeek = lastWeek.with(TemporalAdjusters.nextOrSame(firstDayOfWeek));
          dynamicConvertor.setLte(endOfWeek.format(dateTimeFormatter) + space + dateFormatLte);
          dynamicConvertor.setGte(startOfWeek.format(dateTimeFormatter) + space + dateFormatGte);
          break;
        }
      case "LSW":
        {
          DayOfWeek firstDayOfWeek = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
          LocalDateTime lastWeek = now.minusWeeks(6);
          dynamicConvertor.setLte(
              now.with(DayOfWeek.MONDAY).minusDays(1).format(dateTimeFormatter)
                  + space
                  + dateFormatLte);
          dynamicConvertor.setGte(
              lastWeek.with(DayOfWeek.MONDAY).format(dateTimeFormatter) + space + dateFormatGte);
          break;
        }
      case "TW":
        {
          DayOfWeek firstDayOfWeek = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
          LocalDateTime lastWeek = now;
          LocalDateTime startOfWeek =
              lastWeek.with(TemporalAdjusters.previousOrSame(firstDayOfWeek.plus(1)));
          dynamicConvertor.setLte(now.format(dateTimeFormatter) + space + dateFormatLte);
          dynamicConvertor.setGte(startOfWeek.format(dateTimeFormatter) + space + dateFormatGte);
          break;
        }
      case "LTW":
        {
          LocalDateTime last2Week = now.minusWeeks(2);
          dynamicConvertor.setLte(
              now.with(DayOfWeek.MONDAY).minusDays(1).format(dateTimeFormatter)
                  + space
                  + dateFormatLte);
          dynamicConvertor.setGte(
              last2Week.with(DayOfWeek.MONDAY).format(dateTimeFormatter) + space + dateFormatGte);
          break;
        }
      default:
        throw new IllegalArgumentException(dynamic + " not present");
    }

    return dynamicConvertor;
  }

  /**
   * Dynamic Decipher For Prior.
   *
   * @param dynamic
   * @return Dynamic Converter
   */
  public static DynamicConvertor dynamicDecipherForPrior(String dynamic) {
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String dateFormatLte = "23:59:59";
    String dateFormatGte = "00:00:00";
    String space = " ";
    DynamicConvertor dynamicConvertor = new DynamicConvertor();
    LocalDateTime now = LocalDateTime.now();
    DayOfWeek firstDayOfWeek = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
    switch (dynamic.toUpperCase()) {
      case "YESTERDAY":
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime dayBeforeYesterday = yesterday.minusDays(1);
        dynamicConvertor.setLte(
            dayBeforeYesterday.format(dateTimeFormatter) + space + dateFormatLte);
        dynamicConvertor.setGte(
            dayBeforeYesterday.format(dateTimeFormatter) + space + dateFormatGte);
        break;
      case "TODAY":
        {
          LocalDateTime yestday = now.minusDays(1);
          dynamicConvertor.setLte(yestday.format(dateTimeFormatter) + space + dateFormatLte);
          dynamicConvertor.setGte(yestday.format(dateTimeFormatter) + space + dateFormatGte);
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
                  + space
                  + dateFormatLte);
          dynamicConvertor.setGte(
              priorDayOfYear.minusDays(1).format(dateTimeFormatter) + space + dateFormatGte);
          break;
        }
      case "MTD":
        {
          LocalDateTime firstDayOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
          int calculatedDayDifference = now.getDayOfMonth() - firstDayOfMonth.getDayOfMonth();
          LocalDateTime lastMonth = firstDayOfMonth.minusDays(1);
          LocalDateTime priorDayOfMonth = lastMonth.minusDays(calculatedDayDifference);
          dynamicConvertor.setLte(lastMonth.format(dateTimeFormatter) + space + dateFormatLte);
          dynamicConvertor.setGte(
              priorDayOfMonth.format(dateTimeFormatter) + space + dateFormatGte);
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
                  + space
                  + dateFormatLte);
          dynamicConvertor.setGte(
              last3Month.with(TemporalAdjusters.firstDayOfMonth()).format(dateTimeFormatter)
                  + space
                  + dateFormatGte);
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
                  + space
                  + dateFormatLte);
          dynamicConvertor.setGte(
              last6Months.with(TemporalAdjusters.firstDayOfMonth()).format(dateTimeFormatter)
                  + space
                  + dateFormatGte);
          break;
        }
      case "LM":
        {
          LocalDateTime priorLastMonth = now.minusMonths(1);
          LocalDateTime lastMonth = priorLastMonth.minusMonths(1);
          dynamicConvertor.setLte(
              lastMonth.with(TemporalAdjusters.lastDayOfMonth()).format(dateTimeFormatter)
                  + space
                  + dateFormatLte);
          dynamicConvertor.setGte(
              lastMonth.with(TemporalAdjusters.firstDayOfMonth()).format(dateTimeFormatter)
                  + space
                  + dateFormatGte);
          break;
        }
      case "LY":
        {
          LocalDateTime priorLastYear = now.minusYears(1);
          LocalDateTime lastYear = priorLastYear.minusYears(1);
          dynamicConvertor.setLte(
              lastYear.with(TemporalAdjusters.lastDayOfYear()).format(dateTimeFormatter)
                  + space
                  + dateFormatLte);
          dynamicConvertor.setGte(
              lastYear.with(TemporalAdjusters.firstDayOfYear()).format(dateTimeFormatter)
                  + space
                  + dateFormatGte);
          break;
        }
      case "LQ":
        {
          YearQuarter lastQuarter = YearQuarter.now().minusQuarters(1);
          YearQuarter priorQuarter = lastQuarter.now().minusQuarters(1);
          dynamicConvertor.setLte(
              priorQuarter.minusQuarters(1).atEndOfQuarter().toString() + space + dateFormatLte);
          dynamicConvertor.setGte(
              priorQuarter.minusQuarters(1).atDay(1).toString() + space + dateFormatGte);
          break;
        }
      case "LW":
        {
          LocalDateTime priorLastWeek = now.minusWeeks(2);
          LocalDateTime startOfWeek =
              priorLastWeek.with(TemporalAdjusters.previousOrSame(firstDayOfWeek.plus(1)));
          LocalDateTime endOfWeek =
              priorLastWeek.with(TemporalAdjusters.nextOrSame(firstDayOfWeek));
          dynamicConvertor.setLte(endOfWeek.format(dateTimeFormatter) + space + dateFormatLte);
          dynamicConvertor.setGte(startOfWeek.format(dateTimeFormatter) + space + dateFormatGte);
          break;
        }
      case "LSW":
        {
          LocalDateTime last6Week = now.minusWeeks(6);
          LocalDateTime priorlast6Week = last6Week.minusWeeks(6);
          dynamicConvertor.setLte(
              last6Week.with(DayOfWeek.MONDAY).minusDays(1).format(dateTimeFormatter)
                  + space
                  + dateFormatLte);
          dynamicConvertor.setGte(
              priorlast6Week.with(DayOfWeek.MONDAY).format(dateTimeFormatter)
                  + space
                  + dateFormatGte);
          break;
        }
      case "TW":
        {
          LocalDateTime startOfWeek =
              now.with(TemporalAdjusters.previousOrSame(firstDayOfWeek.plus(1)));
          Long calculatedDayDifference = ChronoUnit.DAYS.between(startOfWeek, now);
          LocalDateTime week = startOfWeek.minusHours(24);
          LocalDateTime lastWeek = week.minusDays(calculatedDayDifference);
          dynamicConvertor.setLte(week.format(dateTimeFormatter) + space + dateFormatLte);
          dynamicConvertor.setGte(lastWeek.format(dateTimeFormatter) + space + dateFormatGte);
          break;
        }
      case "LTW":
        {
          LocalDateTime last2Week = now.minusWeeks(2);
          LocalDateTime priorLast2Week = last2Week.minusWeeks(2);
          dynamicConvertor.setLte(
              last2Week.with(DayOfWeek.MONDAY).minusDays(1).format(dateTimeFormatter)
                  + space
                  + dateFormatLte);
          dynamicConvertor.setGte(
              priorLast2Week.with(DayOfWeek.MONDAY).format(dateTimeFormatter)
                  + space
                  + dateFormatGte);
          break;
        }
      default:
        throw new IllegalArgumentException(dynamic + " not present");
    }

    return dynamicConvertor;
  }

  /**
   * calulates lte date and gte date based on presetCal and forms dynamicConverter.
   *
   * @param presetCal
   * @return Dynamic Converter
   */
  public static DynamicConvertor getDynamicConvertForPresetCal(String presetCal) {
    presetCal = presetCal.trim();
    final String hypen = "-";
    if (!presetCal.contains(hypen)) {
      throw new IllegalArgumentException("presetCal " + presetCal + " is in invalid format");
    }
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    DynamicConvertor dynamicConvertor = new DynamicConvertor();
    LocalDateTime now = LocalDateTime.now();
    int indexOfHyphen = presetCal.indexOf(hypen);
    int length = presetCal.length();
    String presetType = presetCal.substring(indexOfHyphen + 1, length);
    Long presetNumber;
    try {
      presetNumber = Long.valueOf(presetCal.substring(0, indexOfHyphen));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("presetCal " + presetCal + " is in invalid format");
    }
    if (presetNumber < 0) {
      throw new RuntimeException("Lookback period cannot be negative");
    }
    switch (presetType.toUpperCase()) {
      case "DAY":
        LocalDateTime frm = now.minusDays(presetNumber);
        dynamicConvertor.setGte(frm.format(dateTimeFormatter));
        dynamicConvertor.setLte(now.format(dateTimeFormatter));
        break;
      case "HOUR":
        LocalDateTime fromDateTime = now.minusHours(presetNumber);
        dynamicConvertor.setGte(fromDateTime.format(dateTimeFormatter));
        dynamicConvertor.setLte(now.format(dateTimeFormatter));
        break;
      case "MINUTE":
        LocalDateTime fromDate = now.minusMinutes(presetNumber);
        dynamicConvertor.setGte(fromDate.format(dateTimeFormatter));
        dynamicConvertor.setLte(now.format(dateTimeFormatter));
        break;
      case "MONTH":
        LocalDateTime fromDateForMonth = now.minusMonths(presetNumber);
        dynamicConvertor.setGte(fromDateForMonth.format(dateTimeFormatter));
        dynamicConvertor.setLte(now.format(dateTimeFormatter));
        break;
      case "WEEK":
        LocalDateTime fromDateForWeek = now.minusWeeks(presetNumber);
        dynamicConvertor.setGte(fromDateForWeek.format(dateTimeFormatter));
        dynamicConvertor.setLte(now.format(dateTimeFormatter));
        break;
      default:
        throw new IllegalArgumentException("presetCal " + presetCal + " not present");
    }
    return dynamicConvertor;
  }

  /**
   * Re-arrange the query field for ES sorting.
   *
   * @param dataFields
   * @param sorts
   * @return list of fields
   */
  public static List<Field> buildFieldBySort(List<Field> dataFields, List<Sort> sorts) {
    List<Field> fields = new ArrayList<>();
    dataFields.forEach(field -> {
      boolean sortMatch = sorts.stream().anyMatch(p -> p.getColumnName().equalsIgnoreCase(field.getColumnName()));
      if (sortMatch) {
        fields.add(field);
      }
    });
    Collections.reverse(fields);
    dataFields.forEach(field -> {
      if (!fields.contains(field)) {
        fields.add(field);
      }
    });

    Collections.reverse(fields);
    return fields;
  }

  /**
   * This method builds the recursive filter with AND & OR conjunction.If we have multiple filter it
   * means it is old filter which supports conjuction AND or OR but not both,So here the structure
   * is converted into recursive structure with one filter consists of nesting.
   *
   * @param filters filterlist
   * @param booleanCriteria booleanCriteria
   * @return single nested filter
   */
  public static Filter buildNestedFilter(List<Filter> filters, BooleanCriteria booleanCriteria) {
    if (filters.size() == 1) {
      Filter filter = filters.get(0);
      if (filter.getBooleanCriteria() == null && filter.getFilters() == null) {
        Filter recursiveFilter = new Filter();
        recursiveFilter.setBooleanCriteria(booleanCriteria);
        recursiveFilter.setFilters(filters);
        return recursiveFilter;
      }
      return filter;
    } else {
      Filter filter = new Filter();
      booleanCriteria = booleanCriteria != null ? booleanCriteria : BooleanCriteria.AND;
      filter.setBooleanCriteria(booleanCriteria);
      filter.setFilters(filters);
      return filter;
    }
  }
}
