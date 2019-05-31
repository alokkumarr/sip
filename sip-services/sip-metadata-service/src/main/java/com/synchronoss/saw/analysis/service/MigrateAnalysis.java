package com.synchronoss.saw.analysis.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.synchronoss.saw.analysis.metadata.AnalysisMetadata;
import com.synchronoss.saw.analysis.modal.Analysis;
import com.synchronoss.saw.analysis.service.migrationservice.AnalysisSipDslConverter;
import com.synchronoss.saw.analysis.service.migrationservice.ChartConverter;
import com.synchronoss.saw.analysis.service.migrationservice.GeoMapConverter;
import com.synchronoss.saw.analysis.service.migrationservice.MigrationStatus;
import com.synchronoss.saw.analysis.service.migrationservice.MigrationStatusObject;
import com.synchronoss.saw.analysis.service.migrationservice.PivotConverter;
import com.synchronoss.saw.exceptions.MissingFieldException;
import com.synchronoss.saw.exceptions.SipReadEntityException;
import com.synchronoss.saw.util.FieldNames;
import com.synchronoss.saw.util.SipMetadataUtils;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.ojai.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class MigrateAnalysis {
  private static final Logger logger = LoggerFactory.getLogger(MigrateAnalysis.class);

  public String migrationDirectory = "/opt/migration/";
  public String migrationStatusFile = "migrationStatus.json";

  private AnalysisMetadata analysisMetadataStore = null;
  private String listAnalysisUrl;
  private String tableName;
  private String basePath;
  private String migrationStatusTable;
  private String metadataTable;
  private static ObjectMapper objectMapper = new ObjectMapper();
  private Map<String, String> semanticMap;

  public MigrateAnalysis() {}

  /**
   * Parameterized constructor.
   *
   * @param tableName MAPR DB table
   * @param basePath MAPR DB location
   * @param listAnalysisUri List analysis API endpoint
   * @param statusFilePath Output location for migration status
   */
  public MigrateAnalysis(
      String tableName,
      String basePath,
      String listAnalysisUri,
      String statusFilePath,
      String migrationStatusTable) {

    this.basePath = basePath;
    this.tableName = tableName;
    this.listAnalysisUrl = listAnalysisUri;
    this.migrationStatusTable = migrationStatusTable;
    //    this.statusFilePath = statusFilePath;
  }

  /**
   * Converts analysis definition in binary table to new SIP DSL format.
   *
   * @param tableName - Analysis metastore table name
   * @param basePath - Table path
   * @param listAnalysisUri - API to get list of existing analysis
   */
  public void convertBinaryToJson(
      String tableName,
      String basePath,
      String listAnalysisUri,
      String migrationStatusTable,
      String metadataTable)
      throws Exception {
    logger.trace("Migration process will begin here");
    this.migrationStatusTable = migrationStatusTable;
    this.basePath = basePath;
    this.metadataTable = metadataTable;
    analysisMetadataStore = new AnalysisMetadata(tableName, basePath);
    semanticMap = getMetaData();
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.set("Content-type", MediaType.APPLICATION_JSON_UTF8_VALUE);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    HttpEntity<?> requestEntity = new HttpEntity<Object>(semanticNodeQuery(), requestHeaders);
    logger.debug("Analysis server URL {}", listAnalysisUri + "/analysis");
    String url = listAnalysisUri + "/analysis";
    RestTemplate restTemplate = new RestTemplate();
    ResponseEntity analysisBinaryData =
        restTemplate.exchange(url, HttpMethod.POST, requestEntity, JsonNode.class);
    if (analysisBinaryData.getBody() != null) {
      logger.debug("Analysis data = " + analysisBinaryData.getBody());
      JsonNode jsonNode = (JsonNode) analysisBinaryData.getBody();
      JsonElement jelement = new com.google.gson.JsonParser().parse(jsonNode.toString());
      JsonObject analysisBinaryObject = jelement.getAsJsonObject();
      JsonArray analysisList =
          analysisBinaryObject.get("contents").getAsJsonObject().getAsJsonArray("analyze");

      MigrationStatus migrationStatus = convertAllAnalysis(analysisList);
      logger.info("Total number of Files for migration : ", migrationStatus.getTotalAnalysis());
      logger.info("Number of Files Successfully Migrated : ", migrationStatus.getSuccessCount());
      logger.info("Number of Files Successfully Migrated : ", migrationStatus.getFailureCount());
    }
  }

  /**
   * Migrates a list of analysis to new SIP DSL Structure and writes to mapr db json table.
   *
   * @param analysisList List of old analysis definitions
   * @return
   */
  private MigrationStatus convertAllAnalysis(JsonArray analysisList) {
    MigrationStatus migrationStatus = new MigrationStatus();
    List<MigrationStatusObject> analysisStatus = new ArrayList<>();

    migrationStatus.setTotalAnalysis(analysisList.size());

    AtomicInteger successfulMigration = new AtomicInteger();
    AtomicInteger failedMigration = new AtomicInteger();

    (analysisList)
        .forEach(
            analysisElement -> {
              MigrationStatusObject migrationStatusObject = new MigrationStatusObject();
              JsonObject analysisObject = analysisElement.getAsJsonObject();
              String analysisId = analysisObject.get(FieldNames.ID).getAsString();

              migrationStatusObject.setAnalysisId(analysisId);
              migrationStatusObject.setType(analysisObject.get("type").getAsString());
              Analysis analysis = null;

              try {
                analysis = convertOldAnalysisObjtoSipDsl(analysisElement.getAsJsonObject());
                logger.info("Inserting analysis " + analysis.getId() + " into json store");
                JsonElement parsedAnalysis =
                    SipMetadataUtils.toJsonElement(objectMapper.writeValueAsString(analysis));
                analysisMetadataStore.create(analysis.getId(), parsedAnalysis);

                migrationStatusObject.setAnalysisId(analysis.getId());
                logger.info(
                    "Analysis Id in inner block : " + migrationStatusObject.getAnalysisId());
                migrationStatusObject.setType(analysis.getType());
                migrationStatusObject.setAnalysisMigrated(true);
                migrationStatusObject.setMessage("Success");
                migrationStatusObject.setExecutionsMigrated(false);
                // Migration of Executions is done via proxy-service
                successfulMigration.incrementAndGet();
              } catch (JsonProcessingException | MissingFieldException exception) {
                logger.error("Unable to convert analysis to json ", exception);

                migrationStatusObject.setAnalysisMigrated(false);
                migrationStatusObject.setMessage(exception.getMessage());
                migrationStatusObject.setExecutionsMigrated(false);
                failedMigration.incrementAndGet();
              } catch (Exception exception) {
                if (analysis != null) {
                  logger.error("Unable to process analysis " + analysis.getId());
                } else {
                  logger.error("Unable to process analysis");
                }

                migrationStatusObject.setAnalysisMigrated(false);
                migrationStatusObject.setMessage(exception.getMessage());
                migrationStatusObject.setExecutionsMigrated(false);
                failedMigration.incrementAndGet();
              }

              if (saveMigrationStatus(migrationStatusObject, migrationStatusTable, basePath)) {
                logger.info("Successfully written the migration status to MaprDB..!!");
                logger.debug("Written Id = " + migrationStatusObject.getAnalysisId());
              }
              analysisStatus.add(migrationStatusObject);
            });

    migrationStatus.setSuccessCount(successfulMigration.get());
    migrationStatus.setFailureCount(failedMigration.get());

    migrationStatus.setMigrationStatus(analysisStatus);
    return migrationStatus;
  }

  /**
   * Converts old analysis object to new SIP DSL definition.
   *
   * @param analysisObject Single analysis object in old definition
   * @return
   */
  private Analysis convertOldAnalysisObjtoSipDsl(JsonObject analysisObject) throws Exception {
    Analysis analysis = null;

    String analysisType = analysisObject.get("type").getAsString();

    AnalysisSipDslConverter converter = null;

    switch (analysisType) {
      case "chart":
        converter = new ChartConverter();
        break;
      case "pivot":
        converter = new PivotConverter();
        break;
      case "esReport":
        throw new UnsupportedOperationException("ES Report migration not supported yet");
      case "report":
        throw new UnsupportedOperationException("DL Report migration not supported yet");
      case "map":
        converter = new GeoMapConverter();
        break;
      default:
        logger.error("Unknown analysis type");
        break;
    }

    if (converter != null) {
      analysis = converter.convert(analysisObject);
      // Updating the semantic id
      updatedSemanticId(analysis, semanticMap);
    } else {
      logger.error("Unknown analysis type");
    }

    return analysis;
  }

  /**
   * Saves migration status to a file.
   *
   * @param migrationStatus Migration status JSON object
   * @return
   */
  private boolean saveMigrationStatus(
      MigrationStatusObject migrationStatus, String migrationStatusTable, String basePath) {
    boolean status = true;

    String id = migrationStatus.getAnalysisId();
    logger.debug("Started Writing into MaprDB, id : " + id);
    logger.debug("Object body to be written : " + new Gson().toJson(migrationStatus));
    logger.debug(
        "Details coming to saveMigration : \n migrationStatusTable = "
            + migrationStatusTable
            + "basePath = "
            + basePath);
    try {
      AnalysisMetadata analysisMetadataStore1 =
          new AnalysisMetadata(migrationStatusTable, basePath);
      logger.debug("Connection established with MaprDB..!!");
      logger.info("Started Writing the status into MaprDB, id : " + id);
      analysisMetadataStore1.create(id, new Gson().toJson(migrationStatus));
    } catch (Exception e) {
      logger.error(e.getMessage());
      logger.error(
          "Error occurred while writing the status to location: " + migrationStatus,
          e.getMessage());

      status = false;
    }

    return status;
  }

  private String semanticNodeQuery() {
    return "{\n"
        + "   \"contents\":{\n"
        + "      \"keys\":[\n"
        + "         {\n"
        + "            \"module\":\"ANALYZE\"\n"
        + "         }\n"
        + "      ],\n"
        + "      \"action\":\"export\"\n"
        + "   }\n"
        + "}";
  }

  /**
   * Return the semantic id for the migration.
   *
   * @param analysis Analysis
   * @param semanticMap semanticMap
   * @return
   */
  private void updatedSemanticId(Analysis analysis, Map<String, String> semanticMap) {
    String analysisSemanticId =
        analysis != null && analysis.getSemanticId() != null ? analysis.getSemanticId() : null;
    if (analysisSemanticId != null && semanticMap != null && !semanticMap.isEmpty()) {
      for (Map.Entry<String, String> entry : semanticMap.entrySet()) {
        if (analysisSemanticId.equalsIgnoreCase(entry.getValue())) {
          break;
        } else {
          String semanticArtifactName = entry.getKey();
          String artifactName = analysis.getSipQuery().getArtifacts().get(0).getArtifactsName();
          if (semanticArtifactName.equalsIgnoreCase(artifactName)) {
            analysis.setSemanticId(entry.getKey());
            break;
          }
        }
      }
    }
  }

  /**
   * Main function.
   *
   * @param args - command line args
   * @throws IOException - In case of file errors
   */
  public static void main1(String[] args) throws Exception {
    String analysisFile = args[0];
    System.out.println("Convert analysis from file = " + analysisFile);

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    File jsonFile = new File(analysisFile);
    JsonObject jsonObject = gson.fromJson(new FileReader(jsonFile), JsonObject.class);

    JsonObject analyzeObject =
        jsonObject.getAsJsonObject("contents").getAsJsonArray("analyze").get(0).getAsJsonObject();

    MigrateAnalysis ma = new MigrateAnalysis();

    Analysis analysis = ma.convertOldAnalysisObjtoSipDsl(analyzeObject);

    System.out.println(gson.toJson(analysis));
  }

  /**
   * Main function.
   *
   * @param args Command-line args
   * @throws IOException In-case of file error
   */
  public static void main(String[] args) throws IOException {
    String analysisFile = args[0];
    System.out.println("Convert analysis from file = " + analysisFile);

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    File jsonFile = new File(analysisFile);

    JsonObject analysisBinaryObject = gson.fromJson(new FileReader(jsonFile), JsonObject.class);

    JsonArray analysisList =
        analysisBinaryObject.get("contents").getAsJsonObject().getAsJsonArray("analyze");

    MigrateAnalysis ma = new MigrateAnalysis();

    MigrationStatus migrationStatus = ma.convertAllAnalysis(analysisList);
    System.out.println(gson.toJson(migrationStatus));
  }

  /**
   * Return all the semantic details with key/value.
   *
   * @return
   */
  private Map<String, String> getMetaData() {
    logger.info("Inside getMetadata method");
    Map<String, String> semanticMap = new HashMap<>();
    List<Document> docs = null;
    List<JsonObject> objDocs = new ArrayList<>();
    try {
      analysisMetadataStore = new AnalysisMetadata(metadataTable, basePath);
      docs = analysisMetadataStore.searchAll();
      if (docs == null) {
        return null;
      }
      for (Document d : docs) {
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objDocs.add(objectMapper.readValue(d.asJsonString(), JsonObject.class));
      }
      logger.info("docs:: " + docs);
    } catch (Exception e) {
      logger.error("Exception occurred while fetching analysis by category for userId", e);
      throw new SipReadEntityException(
          "Exception occurred while fetching analysis by category for userId", e);
    }
    for (JsonObject object : objDocs) {
      String semanticId = object.get("_id").getAsString();
      String artifactName =
          object
              .get("artifacts")
              .getAsJsonArray()
              .get(0)
              .getAsJsonObject()
              .get("artifactName")
              .getAsString();
      semanticMap.put(artifactName, semanticId);
    }
    logger.info("sematicMap:: " + semanticMap);
    return semanticMap;
  }
}
