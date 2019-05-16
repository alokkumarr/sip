package com.synchronoss.querybuilder;

import java.io.IOException;
import java.util.List;
import com.synchronoss.querybuilder.model.globalfilter.GlobalFilterExecutionObject;
import org.apache.http.client.HttpClient;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SAWElasticSearchQueryBuilder {

  public static final Logger logger = LoggerFactory.getLogger(SAWElasticSearchQueryBuilder.class);

  /**
   * Initialize elastic search query result size
   */
  Integer size =10000;
  HttpClient client;
  public SAWElasticSearchQueryBuilder(Integer size, HttpClient client)
  {
    this.size=size;
    this.client = client;
  }

  /**
   *
   */
  public SAWElasticSearchQueryBuilder(HttpClient client)
  {
    this.client = client;
  }

  
  /**
   * This method will generate the Elastic Search Query based<br/>
   * on the {@link EntityType}
   * 
   * @param type
   * @param jsonString
   * @return query
   * @throws AssertionError
   * @throws ProcessingException 
   */
  public String getQuery(EntityType type, String jsonString, Integer timeOut) throws IllegalArgumentException, ProcessingException {
    String query = null;
    try {
      //assert (type.find(type) == null);
      //assert (jsonString == null || jsonString.equals(""));
      if (type.equals(EntityType.ESREPORT)) {
        query = new SAWReportTypeElasticSearchQueryBuilder(jsonString, timeOut,client).buildDataQuery(size);
      } else {
        query =
                type.equals(EntityType.CHART) ? new SAWChartTypeElasticSearchQueryBuilder(jsonString, timeOut, client)
                        .buildQuery() : new SAWPivotTypeElasticSearchQueryBuilder(jsonString, timeOut, client).buildQuery();
        }
      }catch(IllegalStateException | IOException | NullPointerException exception){
        throw new IllegalArgumentException(exception.getMessage());

    }
    return query;
  }

  /**
   * This method will generate the Elastic Search Query based<br/>
   * on the {@link EntityType}
   * 
   * @param type
   * @param jsonString
   * @return query
   * @throws AssertionError
   */
  public SearchSourceBuilder getSearchSourceBuilder(EntityType type, String jsonString, Integer timeOut)
      throws IllegalArgumentException {
    SearchSourceBuilder query = null;
    try {
      if (type.equals(EntityType.ESREPORT)) {
        query = new SAWReportTypeElasticSearchQueryBuilder(jsonString, timeOut, client).getSearchSourceBuilder(size);
      } else {
      query =
          type.equals(EntityType.CHART) ? new SAWChartTypeElasticSearchQueryBuilder(jsonString, timeOut,client)
              .getSearchSourceBuilder() : new SAWPivotTypeElasticSearchQueryBuilder(jsonString, timeOut, client)
              .getSearchSourceBuilder();
    }} catch (IllegalStateException | IOException | ProcessingException exception) {
      throw new IllegalArgumentException("Type not supported :" + exception.getMessage());
    }
    return query;
  }

  /**
   * This method will generate the Elastic Search Query based<br/>
   * on the {@link EntityType}
   * 
   * @param type
   * @param jsonString
   * @return query
   * @throws AssertionError
   */
  public SearchSourceBuilder getSearchSourceBuilder(EntityType type, String jsonString, String dataSecurityKey, Integer timeOut)
      throws IllegalArgumentException {
    SearchSourceBuilder query = null;
    try {
      if (type.equals(EntityType.ESREPORT)) {
        query = new SAWReportTypeElasticSearchQueryBuilder(jsonString,dataSecurityKey, timeOut, client).getSearchSourceBuilder(size);
      } else {
      query =
          type.equals(EntityType.CHART) ? new SAWChartTypeElasticSearchQueryBuilder(jsonString,dataSecurityKey, timeOut, client)
              .getSearchSourceBuilder() : new SAWPivotTypeElasticSearchQueryBuilder(jsonString, dataSecurityKey, timeOut, client)
              .getSearchSourceBuilder();
    }} catch (IllegalStateException | IOException | ProcessingException exception) {
      throw new IllegalArgumentException("Type not supported :" + exception.getMessage());
    }
    return query;
  }

    /**
     *
     * @param jsonString
     * @return
     * @throws IllegalArgumentException
     */
  public List<GlobalFilterExecutionObject> getsearchSourceBuilder(String jsonString) throws IllegalArgumentException
  {
      try {

           return new GlobalFilterDataQueryBuilder(jsonString).buildQuery();
      } catch (IOException | ProcessingException e) {
          throw new IllegalArgumentException("Exception occurred while parsing global filter request :" + e.getMessage());
      }
  }

}
