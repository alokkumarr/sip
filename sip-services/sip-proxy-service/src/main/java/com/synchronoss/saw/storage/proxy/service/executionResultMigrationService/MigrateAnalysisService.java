package com.synchronoss.saw.storage.proxy.service.executionResultMigrationService;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.synchronoss.saw.analysis.metadata.AnalysisMetadata;
import com.synchronoss.saw.analysis.service.migrationservice.MigrationStatusObject;
import com.synchronoss.saw.model.SipQuery;
import com.synchronoss.saw.storage.proxy.model.ExecutionResult;
import com.synchronoss.saw.storage.proxy.model.ExecutionType;
import com.synchronoss.saw.storage.proxy.service.StorageProxyService;
import com.synchronoss.saw.storage.proxy.service.productSpecificModuleService.ProductModuleMetaStore;
import com.synchronoss.saw.util.SipMetadataUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.ojai.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author Alok.KumarR
 * @since 3.3.0
 */
@Service
public class MigrateAnalysisService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MigrateAnalysisService.class);
  private final Set<String> executionIds = new HashSet<>();

  @Value("${metastore.binary-store-path}")
  @NotNull
  private String binaryTablePath;

  @Value("${metastore.base}")
  @NotNull
  private String basePath;

  @Value("${metastore.migration}")
  @NotNull
  private String migrationStatusTable;

  @Autowired private StorageProxyService proxyService;

  @Autowired private HBaseUtil hBaseUtil;

  @Autowired private MigrateExecutions migrateExecutions;

  private Map<String, Boolean> migratedAnalysis;

  private AnalysisMetadata analysisMetadataStore = null;

  MigrationStatusObject migrationStatusObject = new MigrationStatusObject();

  @PostConstruct
  private void init() throws Exception {
    convertBinaryStoreToDslJsonStore();
  }

  /**
   * Save the DSL execution result.
   *
   * @param executionId
   * @param executionType
   * @param executedBy
   * @param sipQuery
   * @param dslQueryId
   * @param finishedTime
   * @param executionStatus
   * @param dslExecutionResult
   */
  public void saveDSLJsonExecution(
      String executionId,
      String executionType,
      String executedBy,
      SipQuery sipQuery,
      String dslQueryId,
      Long finishedTime,
      String executionStatus,
      Object dslExecutionResult) {
    try {
      List<Object> objectList = new ArrayList<>();
      objectList.add(dslExecutionResult);

      ExecutionResult executionResult = new ExecutionResult();
      executionResult.setExecutionId(executionId);
      executionResult.setDslQueryId(dslQueryId);
      executionResult.setSipQuery(sipQuery);
      executionResult.setStartTime(finishedTime);
      executionResult.setFinishedTime(finishedTime);
      executionResult.setData(objectList);
      executionResult.setExecutionType(ExecutionType.valueOf(executionType));
      executionResult.setStatus(executionStatus);
      executionResult.setExecutedBy(executedBy);
      proxyService.saveDslExecutionResult(executionResult);
      LOGGER.info("Execution Result Stored successfully in jason Store.");
    } catch (Exception ex) {
      LOGGER.error(
          String.format("Error occurred during saving Execution Result : %s"+ ex.getMessage()));
    }
  }

  /**
   * This method to convert old execution result to new DSL execution results.
   *
   * @param type
   * @param dataNode
   * @param queryNode
   * @return @{@link Object}
   */
  public Object convertOldExecutionToDSLExecution(
      String type, JsonNode dataNode, JsonNode queryNode) {
    List<Object> objectList = new ArrayList<>();
    Object dataConverter;
    try {
      switch (type) {
        case "chart":
          dataConverter = new ChartResultMigration();
          objectList = ((ChartResultMigration) dataConverter).parseData(dataNode, queryNode);
          break;
        case "pivot":
          dataConverter = new PivotResultMigration();
          objectList = ((PivotResultMigration) dataConverter).parseData(dataNode, queryNode);
          break;
        case "esReport":
          throw new UnsupportedOperationException("ES Report migration not supported yet");
        case "report":
          throw new UnsupportedOperationException("DL Report migration not supported yet");
        case "map":
          throw new UnsupportedOperationException("DL Report migration not supported yet");
        default:
          LOGGER.error("Unknown report type");
          break;
      }
    } catch (UnsupportedOperationException e) {
      LOGGER.error(e.getMessage());
    }
    return objectList;
  }

  /** Conversion from binary store to jason store */
  public void convertBinaryStoreToDslJsonStore() {
    LOGGER.debug("Fetch all start execution Id.!");
    fetchExecutionIdFromBinaryStore();

    LOGGER.debug("Total count of execution Ids : {}", executionIds.size());
    if (!executionIds.isEmpty()) {
      for (String executionId : executionIds) {
        if (executionId != null) {
          readExecutionResultFromBinaryStore(executionId);
        }
      }
    }
  }

  /**
   * Returns the list of Migrated Analysis results.
   *
   * @return
   */
  public List<MigrationStatusObject> getMigratedAnalysis() {
    LOGGER.debug("Inside getMigratedAnalysis() !!");
    List<Document> docs = null;
    ProductModuleMetaStore productModuleMetaStore = null;
    List<MigrationStatusObject> analysisList = new ArrayList<>();
    try {
      productModuleMetaStore = new ProductModuleMetaStore(migrationStatusTable, basePath);
      docs = productModuleMetaStore.searchAll();
      if (docs.isEmpty()) {
        LOGGER.info("No Analysis present for migration!!");
        return null;
      }
      ObjectMapper mapper = new ObjectMapper();
      mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      for (Document d : docs) {
        analysisList.add(mapper.readValue(d.asJsonString(), MigrationStatusObject.class));
      }
    } catch (Exception e) {
      LOGGER.error("Exception occurred while reading the MaprDB table : {}", migrationStatusTable);
    }

    LOGGER.debug("Return Analysis list : {}", analysisList);
    return analysisList;
  }

  /**
   * Returns Map of Analysis id and definition migration status flag.
   *
   * @param migrationStatusList
   * @return
   */
  public Map<String, Boolean> extractAnalysisId(List<MigrationStatusObject> migrationStatusList) {
    migratedAnalysis = new HashMap<>();
    for (MigrationStatusObject mso : migrationStatusList) {
      migratedAnalysis.put(mso.getAnalysisId(), mso.isAnalysisMigrated());
    }
    return migratedAnalysis;
  }

  /**
   * Saves migration status to a file.
   *
   * @param msObj
   * @param migrationStatusTable
   * @param basePath
   * @return
   */
  private boolean saveMigrationStatus(
      MigrationStatusObject msObj, String migrationStatusTable, String basePath) {
    boolean status = true;

    LOGGER.info("Started Writing into MaprDB, id : {} ", msObj.getAnalysisId());
    try {
      analysisMetadataStore = new AnalysisMetadata(migrationStatusTable, basePath);
      LOGGER.debug("Connection established with MaprDB..!!");
      LOGGER.info("Started Writing the status into MaprDB, id : {} ", msObj.getAnalysisId());
      ObjectMapper mapper = new ObjectMapper();
      JsonElement parsedMigrationStatus =
          SipMetadataUtils.toJsonElement(mapper.writeValueAsString(msObj));
      analysisMetadataStore.update(msObj.getAnalysisId(), parsedMigrationStatus);
    } catch (Exception e) {
      LOGGER.error("Error occurred while writing the status to location: {}", msObj.toString());
      LOGGER.error("stack trace : {}"+e.getMessage());

      status = false;
    }

    return status;
  }

  /** Retrieve all execution Id from binary store */
  public void fetchExecutionIdFromBinaryStore() {
    LOGGER.info("Fetch analysis start here");
    try {
      Connection connection = hBaseUtil.getConnection();
      LOGGER.info(" Binary table Path = {}"+basePath + binaryTablePath);
      Table table = hBaseUtil.getTable(connection, basePath + binaryTablePath);
      ResultScanner results = hBaseUtil.getResultScanner(table);
      if (results != null) {
        for (Result result : results) {
          String executionId = new String(result.getValue("_search".getBytes(), "id".getBytes()));
          List<MigrationStatusObject> mgObj = getMigratedAnalysis();
          Map<String, Boolean> analysisIdList = extractAnalysisId(mgObj);
          for (Map.Entry<String, Boolean> entry : analysisIdList.entrySet()) {
            String analysisId = entry.getKey();
            Boolean flag = entry.getValue();
            boolean validMigrationCheck =
                !flag
                    && executionId != null
                    && analysisId != null
                    && executionId.contains(analysisId);
            if (validMigrationCheck) {
              executionIds.add(executionId);
            }
          }
        }
      }
    } catch (Exception ex) {
      LOGGER.error(ex.getMessage());
    } finally {
      hBaseUtil.closeConnection();
    }
  }

  /**
   * Read all execution result
   *
   * @param executionId
   */
  public void readExecutionResultFromBinaryStore(String executionId) {
    LOGGER.debug("Fetch execution start here with this table :" + basePath + binaryTablePath);
    try {
      Connection connection = hBaseUtil.getConnection();
      Table table = hBaseUtil.getTable(connection, basePath + binaryTablePath);
      Get get = new Get(Bytes.toBytes(executionId));
      Result result = table.get(get);
      JsonParser parser = new JsonParser();
      JsonObject content =
          parser
              .parse(new String(result.getValue("_source".getBytes(), "content".getBytes())))
              .getAsJsonObject();
      LOGGER.debug("Contents from Binary Store :" + content.toString());

      String type = content.get("type").getAsString();
      if (type != null && type.matches("pivot|chart")) {
        String analysisId = content.get("id").getAsString();
        String executedBy = content.get("executedBy").getAsString();
        String executionType = content.get("executionType").getAsString();
        String executionStatus = content.get("execution_result").getAsString();
        Long executionFinishTs = Long.valueOf(content.get("execution_finish_ts").getAsString());

        JsonObject queryBuilder = new JsonObject();
        queryBuilder.add("queryBuilder", content.get("queryBuilder"));
        SipQuery sipQuery = migrateExecutions.migrate(queryBuilder);

        migrationStatusObject.setAnalysisId(analysisId);
        migrationStatusObject.setAnalysisMigrated(true);
        migrationStatusObject.setType(type);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode dataNode =
            objectMapper.readTree(
                new String(result.getValue("_objects".getBytes(), "data".getBytes())));
        LOGGER.debug("Data Json Node which need to parsed for pivot/chart : {}", dataNode);

        JsonNode queryNode =
            objectMapper
                .readTree(new String(result.getValue("_source".getBytes(), "content".getBytes())))
                .get("queryBuilder");
        LOGGER.debug("Query Node which need to parsed for pivot/chart : {}", queryNode);

        Object dslExecutionResult = convertOldExecutionToDSLExecution(type, dataNode, queryNode);
        LOGGER.debug("dslExecutionResult : {}", dslExecutionResult);
        // store the execution result in json store
        saveDSLJsonExecution(
            executionId,
            executionType,
            executedBy,
            sipQuery,
            analysisId,
            executionFinishTs,
            executionStatus,
            dslExecutionResult);
        migrationStatusObject.setExecutionsMigrated(true);
        migrationStatusObject.setMessage("Success");
        if (saveMigrationStatus(migrationStatusObject, migrationStatusTable, basePath)) {
            LOGGER.info("Migration result saved successfully !! : {}"+ migrationStatusObject.isExecutionsMigrated());
        } else {
            LOGGER.error("Unable to write update AnalysisMigration table!!");
        }
      }
    } catch (Exception ex) {
      LOGGER.error(ex.getMessage());
      migrationStatusObject.setExecutionsMigrated(false);
      migrationStatusObject.setMessage("Failed : " + ex.getMessage());
    } finally {
      hBaseUtil.closeConnection();
    }
  }
}
