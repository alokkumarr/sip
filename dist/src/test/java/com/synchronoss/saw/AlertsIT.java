package com.synchronoss.saw;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.documentationConfiguration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test managing alerts, collecting metrics for alerts during data ingestion and and evaluating
 * alerts after data has been ingested.
 */
public class AlertsIT extends BaseIT {
  private final Logger log = LoggerFactory.getLogger(getClass().getName());

  private static final String OPERATORS = "operators";
  private static final String ALERT_PATH = "/saw/services/alerts";

  @Test
  public void testTriggerAlert() throws JsonProcessingException {
    String testId = testId();
    ObjectNode root = mapper.createObjectNode();
    root.put("metric", 100);
    given(new RequestSpecBuilder()
            .addFilter(documentationConfiguration(restDocumentation))
            .build())
        .queryParam("APP_KEY", "stream_1")
        .queryParam("APP_VERSION", "1")
        .queryParam("APP_MODULE", "1")
        .queryParam("EVENT_TYPE", "1")
        .queryParam("EVENT_ID", "1")
        .queryParam("EVENT_DATE", "1")
        .queryParam("RECEIVED_TS", "1")
        .contentType(ContentType.JSON)
        .body(root)
        .when()
        .post("sip/rtis/events")
        .then()
        .assertThat()
        .statusCode(200)
        .body("result", equalTo("success"));
  }

  /** This test-case is check the scenario to create a alert. */
  @Test
  public void createAlert() throws JsonProcessingException, IOException {

    HashMap<?, ?> alertObject =
        given(authSpec)
            .body(prepareAlertsDataSet())
            .when()
            .post(ALERT_PATH)
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .response()
            .getBody()
            .jsonPath()
            .getJsonObject("alert");

    log.debug("alertObject : " + alertObject);

    JsonNode jsonNode = mapper.convertValue(alertObject, JsonNode.class);
    Long alertRulesSysId = Long.valueOf(jsonNode.get("alertRulesSysId").toString());
    log.debug("alertRulesSysId : " + alertRulesSysId);
    assertFalse(alertRulesSysId <= 0);

    // delete alert after testing
    this.tearDownAlert(alertRulesSysId);
  }

  /** This test-case is check the scenario to delete a alert. */
  @Test
  public void deleteAlert() throws IOException {
    HashMap<?, ?> alertObject =
        given(authSpec)
            .body(prepareAlertsDataSet())
            .when()
            .post(ALERT_PATH)
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .response()
            .getBody()
            .jsonPath()
            .getJsonObject("alert");

    log.debug("alertObject : " + alertObject);

    JsonNode jsonNode = mapper.convertValue(alertObject, JsonNode.class);
    Long alertRulesSysId = Long.valueOf(jsonNode.get("alertRulesSysId").toString());
    log.debug("alertRulesSysId : " + alertRulesSysId);

    assertFalse(alertRulesSysId <= 0);

    String urlForDelete = ALERT_PATH + "/" + alertRulesSysId;
    log.debug("deleteAlerts urlForDelete : " + urlForDelete);
    given(authSpec).when().delete(urlForDelete).then().assertThat().statusCode(200);
  }

  /** This test-case is check the scenario that listing of alerts. */
  @Test
  public void readAlerts() throws IOException {
    HashMap<?, ?> alertObject =
        given(authSpec)
            .body(prepareAlertsDataSet())
            .when()
            .post(ALERT_PATH)
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .response()
            .getBody()
            .jsonPath()
            .getJsonObject("alert");

    log.debug("alertObject : " + alertObject);

    JsonNode jsonNode = mapper.convertValue(alertObject, JsonNode.class);
    Long alertRulesSysId = Long.valueOf(jsonNode.get("alertRulesSysId").toString());
    log.debug("alertRulesSysId : " + alertRulesSysId);

    given(authSpec)
        .body(prepareAlertsDataSet())
        .when()
        .get(ALERT_PATH)
        .then()
        .assertThat()
        .statusCode(200);

    List<?> listOfAlerts =
        given(authSpec)
            .when()
            .get(ALERT_PATH)
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .response()
            .jsonPath()
            .getList("$");

    log.debug("readAlert :" + listOfAlerts);
    assertTrue(listOfAlerts.size() > 0);

    // delete alert after testing
    this.tearDownAlert(alertRulesSysId);
  }

  /** This test-case is check the scenario to update a alert. */
  @Test
  public void updateAlert() throws IOException {
    HashMap<?, ?> alertObject =
        given(authSpec)
            .body(prepareAlertsDataSet())
            .when()
            .post(ALERT_PATH)
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .response()
            .getBody()
            .jsonPath()
            .getJsonObject("alert");

    log.debug("alertObject : " + alertObject);

    JsonNode jsonNode = mapper.convertValue(alertObject, JsonNode.class);
    Long alertRulesSysId = Long.valueOf(jsonNode.get("alertRulesSysId").toString());
    log.debug("alertRulesSysId : " + alertRulesSysId);
    assertFalse(alertRulesSysId <= 0);

    String urlForThatoUpdate = ALERT_PATH + "/" + alertRulesSysId;
    log.debug("updateAlerts urlForThetoUpdate : " + urlForThatoUpdate);
    HashMap<?, ?> alertObject1 =
        given(authSpec)
            .body(prepareUpdateAlertDataSet())
            .when()
            .put(urlForThatoUpdate)
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .response()
            .body()
            .jsonPath()
            .getJsonObject("alert");

    String activeInd =
        mapper.convertValue(alertObject1, JsonNode.class).get("activeInd").toString();
    log.debug("activeInd :" + activeInd);
    assertEquals("false", activeInd);

    // delete alert after testing
    this.tearDownAlert(alertRulesSysId);
  }

  /** This method is used to tear down the alert after each test case executes. */
  public void tearDownAlert(Long alertRulesSysId) throws JsonProcessingException {
    assertFalse(alertRulesSysId <= 0);
    String urlForDelete = ALERT_PATH + "/" + alertRulesSysId;
    log.debug("deleteAlert urlForDelete : " + urlForDelete);
    given(authSpec).when().delete(urlForDelete).then().assertThat().statusCode(200);
  }

  /** This test-case is check the scenario to operator a alert. */
  @Test
  public void testOperators() throws IOException {

    HashMap<?, ?> alertObject =
        given(authSpec)
            .body(prepareAlertsDataSet())
            .when()
            .post(ALERT_PATH)
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .response()
            .getBody()
            .jsonPath()
            .getJsonObject("alert");

    log.debug("alertObject : " + alertObject);

    JsonNode jsonNode = mapper.convertValue(alertObject, JsonNode.class);
    Long alertRulesSysId = Long.valueOf(jsonNode.get("alertRulesSysId").toString());

    log.debug("alertRulesSysId : " + alertRulesSysId);
    assertFalse(alertRulesSysId <= 0);

    String urlForOperators = ALERT_PATH + "/" + OPERATORS;
    log.debug("operators url to get list : " + urlForOperators);

    List<?> listOfOperators =
        given(authSpec)
            .when()
            .get(urlForOperators)
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .response()
            .jsonPath()
            .get("operators");

    log.debug("listOfOperators : " + listOfOperators);
    assertTrue(listOfOperators.size() > 0);

    // delete alert after testing
    this.tearDownAlert(alertRulesSysId);
  }

  /** This test-case is check the scenario to list by categoryId a alert. */
  @Test
  public void testListByCategoryId() throws IOException {
    HashMap<?, ?> alertObject =
        given(authSpec)
            .body(prepareAlertsDataSet())
            .when()
            .post(ALERT_PATH)
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .response()
            .getBody()
            .jsonPath()
            .getJsonObject("alert");

    log.debug("alertObject : " + alertObject);

    JsonNode jsonNode = mapper.convertValue(alertObject, JsonNode.class);
    Long alertRulesSysId = Long.valueOf(jsonNode.get("alertRulesSysId").toString());

    log.debug("alertRulesSysId : " + alertRulesSysId);
    assertFalse(alertRulesSysId <= 0);

    List<?> listOfAlerts =
        given(authSpec)
            .queryParam("id", "12")
            .when()
            .get(ALERT_PATH)
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .response()
            .jsonPath()
            .get("$");

    log.debug("listOfAlerts : " + listOfAlerts.size());
    assertTrue(listOfAlerts.size() > 0);

    // delete alert after testing
    this.tearDownAlert(alertRulesSysId);
  }

  /**
   * This method is used to in test-case where creation of alerts is required.
   *
   * @return object {@link ObjectNode}
   */
  private ObjectNode prepareAlertsDataSet() {
    ObjectNode root = mapper.createObjectNode();
    root.put("alertRulesSysId", "123456");
    root.put("activeInd", "false");
    root.put("aggregation", "AVG");
    root.put("alertSeverity", "CRITICAL");
    root.put("categoryId", "12");
    root.put("product", "MCT");
    root.put("datapodId", "abc");
    root.put("datapodName", "ABc");
    root.put("monitoringEntity", "abc123");
    root.put("operator", "LT");
    root.put("alertDescription", "Tests");
    root.put("alertName", "myName");
    root.put("thresholdValue", "2");
    return root;
  }

  /**
   * This method is used to in test-case where updating of alerts is required.
   *
   * @return object {@link ObjectNode}
   */
  private ObjectNode prepareUpdateAlertDataSet() {
    ObjectNode root = mapper.createObjectNode();
    root.put("alertRulesSysId", "123456");
    root.put("activeInd", "false");
    root.put("aggregation", "SUM");
    root.put("alertSeverity", "CRITICAL");
    root.put("categoryId", "12");
    root.put("product", "MCT");
    root.put("datapodId", "abc");
    root.put("datapodName", "ABc");
    root.put("monitoringEntity", "abc123");
    root.put("operator", "GT");
    root.put("alertDescription", "Tests");
    root.put("alertName", "myName");
    root.put("thresholdValue", "2");
    return root;
  }

  /** This test-case is check the alert count. */
  @Test
  public void testAlertCount() throws IOException {
    String urlForAlertCount = ALERT_PATH + "/" + "count";
    log.debug("URL for Alert count : " + urlForAlertCount);
    ObjectNode root = mapper.createObjectNode();
    root.put("preset", "YTD");
    root.put("groupBy", "StartTime");
    given(authSpec)
        .contentType(ContentType.JSON)
        .body(root)
        .when()
        .post(urlForAlertCount)
        .then()
        .assertThat()
        .statusCode(200);
  }
}
