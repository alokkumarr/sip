package com.synchronoss.saw.semantic.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.synchronoss.saw.exceptions.SipCreateEntityException;
import com.synchronoss.saw.exceptions.SipDeleteEntityException;
import com.synchronoss.saw.exceptions.SipJsonValidationException;
import com.synchronoss.saw.exceptions.SipReadEntityException;
import com.synchronoss.saw.semantic.model.DataSemanticObjects;
import com.synchronoss.saw.semantic.model.MetaDataObjects;
import com.synchronoss.saw.semantic.model.request.BinarySemanticNode;
import com.synchronoss.saw.semantic.model.request.SemanticNode;
import com.synchronoss.saw.util.SipMetadataUtils;
import com.synchronoss.sip.utils.RestUtil;
import com.synchronoss.sip.utils.SipCommonUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import sncr.bda.cli.MetaDataStoreRequestAPI;
import sncr.bda.core.file.HFileOperations;
import sncr.bda.datasets.conf.DataSetProperties;
import sncr.bda.store.generic.schema.Action;
import sncr.bda.store.generic.schema.Category;
import sncr.bda.store.generic.schema.MetaDataStoreStructure;

/**
 * This class is used to migrate the existing HBase Binary data to MapRDBStore. It is independent
 * migration class.
 *
 * @author spau0004
 */
@Service
public class MigrationService {

  private static final Logger logger = LoggerFactory.getLogger(MigrationService.class);
  private String existingBinarySemanticPath = "/services/metadata/semantic_metadata";

  @Autowired private RestUtil restUtil;

  private RestTemplate restTemplate = null;

  @Value("${semantic.binary-migration-requires}")
  @NotNull
  private boolean migrationRequires;

  @Value("${metastore.base}")
  @NotNull
  private String basePath;

  @Value("${semantic.workbench-url}")
  @NotNull
  private String workbenchURl;

  @Value("${semantic.transport-metadata-url}")
  @NotNull
  private String transportUri;

  @Value("${semantic.migration-metadata-home}")
  @NotNull
  private String migrationMetadataHome;

  /**
   * This method provides an entry point to migration service.
   *
   * @throws Exception exception
   */
  public void init() throws Exception {
    logger.info("Migration initiated: " + migrationRequires);
    if (migrationRequires) {
      try {
        convertHBaseBinaryToMaprdbStore(transportUri, basePath, migrationMetadataHome);
      } catch (Exception ex) {
        logger.error("Exception occurred while migrating semantic binary store to MapRDB", ex);
      }
    } else {
      createStore(basePath);
    }
    logger.info("Migration ended: " + migrationRequires);
  }

  /**
   * Run migration.
   *
   * @param args arguments
   * @throws IOException exception
   */
  public static void main(String[] args) throws IOException {
    String dataLocation =
        "{\n"
            + "    \"contents\": [\n"
            + "        {\n"
            + "            \"dataLocation\": \"/var/sip/services/saw-analyze-samples/sample-spark/"
            + "data-product.ndjson\"\n"
            + "        }\n"
            + "    ]\n"
            + "}";

    Resource resource = new ClassPathResource("semantic.json");
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode json = objectMapper.readTree(resource.getFile());

    String mdService = json.toString();
    String sanitizedMdService = SipCommonUtils.sanitizeJson(mdService);
    objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    DataSemanticObjects contentDataLocation =
        objectMapper.readValue(dataLocation, DataSemanticObjects.class);
    MetaDataObjects metaDataObjects =
        objectMapper.readValue(sanitizedMdService, MetaDataObjects.class);

    String analyzeStr =
        objectMapper.writeValueAsString(metaDataObjects.getContents().get(0).getAnalyze());

    String sanitizedAnalyzeStr = SipCommonUtils.sanitizeJson(analyzeStr);
    List<BinarySemanticNode> binaryNodes =
        objectMapper.readValue(sanitizedAnalyzeStr,
            new TypeReference<List<BinarySemanticNode>>() {});
    SemanticNode semanticNode = null;
    for (BinarySemanticNode binarySemanticNode : binaryNodes) {
      String id = binarySemanticNode.getId();
      semanticNode = new SemanticNode();
      semanticNode.set_id(id);
      semanticNode.setId(id);
      semanticNode.setCustomerCode(binarySemanticNode.getCustomerCode());
      semanticNode.setModule(
          com.synchronoss.saw.semantic.model.request.SemanticNode.Module.ANALYZE);
      semanticNode.setProjectCode(
          binarySemanticNode.getProjectCode() != null
              ? binarySemanticNode.getProjectCode()
              : "workbench");
      semanticNode.setUsername(
          binarySemanticNode.getUsername() != null
              ? binarySemanticNode.getUsername()
              : "sipadmin@synchronoss.com");
      semanticNode.setMetricName(binarySemanticNode.getMetricName());
      if (binarySemanticNode.getEsRepository() != null) {
        semanticNode.setEsRepository(binarySemanticNode.getEsRepository());
      } else {
        String repositoryStr = objectMapper.writeValueAsString(binarySemanticNode.getRepository());
        String sanitizedRepositoryStr = SipCommonUtils.sanitizeJson(repositoryStr);
        JsonNode repository =
            objectMapper.readTree(sanitizedRepositoryStr);
        ArrayNode repositories = (ArrayNode) repository.get("objects");
        ObjectNode newRepoNode = null;
        List<Object> repositoryObjects = new ArrayList<>();
        for (JsonNode repositoryNode : repositories) {
          String dataObjectId = repositoryNode.get("EnrichedDataObjectId").asText();
          String dataSetName = repositoryNode.get("EnrichedDataObjectName").asText();
          logger.trace("dataObjectId : " + dataObjectId);
          logger.trace("dataSetName : " + dataSetName);
          String format = "parquet";
          newRepoNode = objectMapper.createObjectNode();
          newRepoNode.put("name", dataSetName);
          newRepoNode.put("format", format);
          repositoryObjects.add(newRepoNode);
        }
        semanticNode.setRepository(repositoryObjects);
      }
      semanticNode.setArtifacts(binarySemanticNode.getArtifacts());
      semanticNode.setSupports(binarySemanticNode.getSupports());
      System.out.println(objectMapper.writeValueAsString(semanticNode));
    }
    JsonNode data =
        objectMapper.readTree(
            objectMapper.writeValueAsString(contentDataLocation.getContents().get(0)));
    System.out.println(data.get("dataLocation"));
    Path file = Paths.get("/var/sip/services/saw-analyze-samples/sample-spark/");

    System.out.println(file.getFileName());
    System.out.println(
        (FilenameUtils.getExtension(file.getFileName().toString()).equals("")) ? "empty" : "data");
  }

  /**
   * This method will get the data from binary store & make it sure. binary & maprDB store are in
   * sync.
   *
   * @throws Exception exception
   */
  public void convertHBaseBinaryToMaprdbStore(
      String transportUri, String basePath, String migrationMetadataHome) throws Exception {
    logger.trace("migration process will begin here");
    HttpHeaders requestHeaders = new HttpHeaders();
    // Constructing the request structure to get the list of semantic from Binary
    // Store
    requestHeaders.set("Content-type", MediaType.APPLICATION_JSON_UTF8_VALUE);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    logger.debug("transportMetadataURIL server URL {}", transportUri + "/md");
    HttpEntity<?> requestEntity =
        new HttpEntity<Object>(semanticNodeQuery("search"), requestHeaders);
    String url = transportUri + "/md";
    restTemplate = restUtil.restTemplate();
    ResponseEntity<MetaDataObjects> binarySemanticStoreData =
        restTemplate.exchange(url, HttpMethod.POST, requestEntity, MetaDataObjects.class);

    logger.trace(
        "binarySemanticStoreData status Code :" + binarySemanticStoreData.getStatusCode().value());
    logger.trace("binarySemanticStoreData hasBody :" + binarySemanticStoreData.hasBody());
    logger.trace(
        "binarySemanticStoreData body :"
            + (binarySemanticStoreData.hasBody()
                ? binarySemanticStoreData.getBody().toString()
                : "NA"));
    logger.trace("binarySemanticStoreData getHeaders :" + binarySemanticStoreData.getHeaders());
    logger.trace(
        "binarySemanticStoreData getBody().getContents() :"
            + binarySemanticStoreData.getBody().getContents().size());
    logger.trace(
        "binarySemanticStoreData.getBody()!=null :" + (binarySemanticStoreData.getBody() != null));
    logger.trace(
        "binarySemanticStoreData.getBody().getContents()!=null :"
            + (binarySemanticStoreData.getBody().getContents() != null));
    logger.trace(
        "binarySemanticStoreData.getBody().getContents().size() > 0) :"
            + (binarySemanticStoreData.getBody().getContents().size() > 0));
    logger.trace(
        "binarySemanticStoreData if size > 0 :"
            + (binarySemanticStoreData.getBody().getContents().size() > 0
                ? objectMapper.writeValueAsString(binarySemanticStoreData)
                : "NA"));
    logger.trace(
        "if block :"
            + ((binarySemanticStoreData.getBody() != null
                && (binarySemanticStoreData.getBody().getContents() != null
                    && binarySemanticStoreData.getBody().getContents().size() > 0))));
    if ((binarySemanticStoreData.getBody() != null
        && (binarySemanticStoreData.getBody().getContents() != null)
        && (binarySemanticStoreData.getBody().getContents().size() > 0))) {
      logger.trace(objectMapper.writeValueAsString(binarySemanticStoreData));

      String analyzeStr = objectMapper.writeValueAsString(
          binarySemanticStoreData.getBody().getContents().get(0).getAnalyze());
      String sanitizedAnalyseStr = SipCommonUtils.sanitizeJson(analyzeStr);
      List<BinarySemanticNode> binaryNodes =
          objectMapper.readValue(sanitizedAnalyseStr,
              new TypeReference<List<BinarySemanticNode>>() {});
      SemanticNode semanticNode = null;
      for (BinarySemanticNode binarySemanticNode : binaryNodes) {
        String id = binarySemanticNode.getId();
        semanticNode = new SemanticNode();
        semanticNode.set_id(id);
        semanticNode.setId(id);
        logger.trace("Checking for the Id in existence : " + id);
        if (!(readSemantic(semanticNode, basePath))) {
          semanticNode.setCustomerCode(binarySemanticNode.getCustomerCode());
          semanticNode.setModule(
              com.synchronoss.saw.semantic.model.request.SemanticNode.Module.ANALYZE);
          semanticNode.setProjectCode(
              binarySemanticNode.getProjectCode() != null
                  ? binarySemanticNode.getProjectCode()
                  : "workbench");
          semanticNode.setUsername(
              binarySemanticNode.getUsername() != null
                  ? binarySemanticNode.getUsername()
                  : "sipadmin@synchronoss.com");
          semanticNode.setMetricName(binarySemanticNode.getMetricName());
          if (binarySemanticNode.getEsRepository() != null) {
            String repositoryStr = objectMapper
                .writeValueAsString(binarySemanticNode.getEsRepository());
            String sanitizedRepositoryStr = SipCommonUtils.sanitizeJson(repositoryStr);
            JsonNode esRepository =
                objectMapper.readTree(sanitizedRepositoryStr);
            String indexName = esRepository.get("indexName").asText();
            // This check has been introduced due to special case where existing pods are
            // has both esRepository & repository SIP-4960
            if (indexName != null && !indexName.trim().equals("")) {
              semanticNode.setEsRepository(binarySemanticNode.getEsRepository());
            } else {
              semanticNode =
                  settingRepositories(binarySemanticNode, url, requestHeaders, semanticNode);
            }
          } else {
            semanticNode =
                settingRepositories(binarySemanticNode, url, requestHeaders, semanticNode);
          }
          semanticNode.setArtifacts(binarySemanticNode.getArtifacts());
          semanticNode.setSupports(binarySemanticNode.getSupports());
          try {
            logger.trace(
                "semanticNode description which is going to migrate :"
                    + objectMapper.writeValueAsString(semanticNode));
            addSemantic(semanticNode, basePath);
          } catch (Exception ex) {
            logger.trace("Throwing an exception while adding the semantic to the new store");
            throw new SipCreateEntityException(
                "Exception generated during migration while creating an semantic entity ", ex);
          }
          try {
            logger.info(
                "HFileOperations.exists(basePath + existingBinarySemanticPath)): "
                    + migrationMetadataHome
                    + existingBinarySemanticPath);
            if (HFileOperations.exists(migrationMetadataHome + existingBinarySemanticPath)) {
              HFileOperations.deleteEnt(migrationMetadataHome + existingBinarySemanticPath);
            }
          } catch (Exception e) {
            logger.trace(
                "Exception occurred while removing "
                    + migrationMetadataHome
                    + existingBinarySemanticPath
                    + " :",
                e);
          }
          // end of Id check if it is there then ignore
        } else {
          logger.info("This " + semanticNode.get_id() + " already exists on the store");
        }
      }
    } else {
      createStore(basePath);
      logger.info("migration has been completed on previous installation");
    }
    logger.trace("migration process will ends here");
  }

  /**
   * This will generate query for the dataObject to get the dlLocation.
   *
   * @return String.
   */
  private String dataObjectQuery(String dataObjectId, String operation) {
    return "{\"contents\":{\"keys\":[{\"id\":\""
        + dataObjectId
        + "\"}],\"action\":\""
        + operation
        + "\",\"context\":\"DataObject\"}}";
  }

  private SemanticNode settingRepositories(
      BinarySemanticNode binarySemanticNode,
      String url,
      HttpHeaders requestHeaders,
      SemanticNode semanticNode)
      throws JsonProcessingException, IOException {
    List<String> listOfDataObjectIds = new ArrayList<>();
    List<String> listOfDataObjectNames = new ArrayList<>();
    ObjectMapper objectMapper = new ObjectMapper();
    String repositoryStr = objectMapper.writeValueAsString(binarySemanticNode.getRepository());
    String sanitizedRepositoryStr = SipCommonUtils.sanitizeJson(repositoryStr);
    JsonNode repository =
        objectMapper.readTree(sanitizedRepositoryStr);
    ArrayNode repositories = (ArrayNode) repository.get("objects");
    ObjectNode newRepoNode = null;
    HttpEntity<?> dataObjectRequestEntity = null;
    ResponseEntity<DataSemanticObjects> binaryDataObjectNode = null;
    JsonNode dataObjectData = null;
    List<Object> repositoryObjects = new ArrayList<>();
    for (JsonNode repositoryNode : repositories) {
      String dataObjectId = repositoryNode.get("EnrichedDataObjectId").asText();
      String dataSetName = repositoryNode.get("EnrichedDataObjectName").asText();
      logger.trace("dataObjectId : " + dataObjectId);
      logger.trace("dataSetName : " + dataSetName);
      newRepoNode = objectMapper.createObjectNode();
      newRepoNode.put(DataSetProperties.Name.toString(), dataSetName);
      requestHeaders.set("Content-type", MediaType.APPLICATION_JSON_UTF8_VALUE);
      dataObjectRequestEntity =
          new HttpEntity<Object>(dataObjectQuery(dataObjectId, "read"), requestHeaders);
      logger.trace(
          "dataObjectRequestEntity : " + objectMapper.writeValueAsString(dataObjectRequestEntity));
      logger.debug("transportMetadataURIL server URL {}", url);
      restTemplate = restUtil.restTemplate();
      binaryDataObjectNode =
          restTemplate.exchange(
              url, HttpMethod.POST, dataObjectRequestEntity, DataSemanticObjects.class);

      String dataObjectStr = objectMapper
          .writeValueAsString(binaryDataObjectNode.getBody().getContents().get(0));
      String sanitizedDataObjectStr = SipCommonUtils.sanitizeJson(dataObjectStr);
      dataObjectData =
          objectMapper.readTree(sanitizedDataObjectStr);
      logger.trace("dataObjectData : " + objectMapper.writeValueAsString(dataObjectData));
      newRepoNode.put(
          DataSetProperties.PhysicalLocation.toString(),
          dataObjectData.get("dataLocation").asText());
      Path file = Paths.get(dataObjectData.get("dataLocation").asText());
      String format =
          (FilenameUtils.getExtension(file.getFileName().toString()).equals("")
                  || FilenameUtils.getExtension(file.getFileName().toString()) != null)
              ? FilenameUtils.getExtension(file.getFileName().toString())
              : "parquet";
      newRepoNode.put(DataSetProperties.Format.toString(), format);
      repositoryObjects.add(newRepoNode);
      listOfDataObjectIds.add(dataObjectId);
      listOfDataObjectNames.add(dataSetName);
    }
    semanticNode.setRepository(repositoryObjects);
    semanticNode.setParentDataSetIds(listOfDataObjectIds);
    semanticNode.setParentDataSetNames(listOfDataObjectNames);
    return semanticNode;
  }

  /**
   * This will generate query for semanticNode.
   *
   * @return String
   */
  private String semanticNodeQuery(String operation) {
    return "{\"contents\":{\"keys\":[{\"type\":\"semantic\",\"module\":\"ANALYZE\"}],\"action\":\""
        + operation
        + "\",\"select\":\"everything\",\"context\":\"Semantic\"}}";
  }

  private void addSemantic(SemanticNode node, String basePath)
      throws SipJsonValidationException, SipCreateEntityException {
    logger.trace("Adding semantic with an Id : {}", node.get_id());
    node.setCreatedBy(node.getUsername());
    ObjectMapper mapper = new ObjectMapper();
    try {
      List<MetaDataStoreStructure> structure =
          SipMetadataUtils.node2JsonObject(
              node, basePath, node.get_id(), Action.create, Category.Semantic);
      logger.trace(
          "addSemantic : Before invoking request to MaprDB JSON store :{}",
          mapper.writeValueAsString(structure));
      MetaDataStoreRequestAPI requestMetaDataStore = new MetaDataStoreRequestAPI(structure);
      requestMetaDataStore.process();
    } catch (Exception ex) {
      logger.error("Problem on the storage while creating an entity", ex);
      throw new SipCreateEntityException("Problem on the storage while creating an entity.", ex);
    }
    logger.trace("Response : " + node.toString());
  }


  private void createStore(String basePath)
      throws SipJsonValidationException, SipCreateEntityException {
    logger.trace("creating store starts here ");
    ObjectMapper mapper = new ObjectMapper();
    try {
      SemanticNode  node = new SemanticNode();
      List<MetaDataStoreStructure> structure =
          SipMetadataUtils.node2JsonObject(
              node, basePath, null, Action.init, Category.Semantic);
      logger.trace(
          "createStore : Before invoking request to MaprDB JSON store :{}",
          mapper.writeValueAsString(structure));
      MetaDataStoreRequestAPI requestMetaDataStore = new MetaDataStoreRequestAPI(structure);
      requestMetaDataStore.process();
    } catch (Exception ex) {
      logger.error("Problem on the storage while creating an entity", ex);
      throw new SipCreateEntityException("Problem on the storage while creating an entity.", ex);
    }
    logger.trace("creating store ends here ");
  }
  
  
  private boolean readSemantic(SemanticNode node, String basePath)
      throws SipJsonValidationException, SipReadEntityException {
    Preconditions.checkArgument(node.get_id() != null, "Id is mandatory attribute.");
    logger.trace("reading semantic from the store with an Id : {}", node.get_id());
    SemanticNode nodeRetrieved = null;
    SemanticNode newSemanticNode = null;
    boolean exists = true;
    ObjectMapper mapper = new ObjectMapper();
    try {
      List<MetaDataStoreStructure> structure =
          SipMetadataUtils.node2JsonObject(
              node, basePath, node.get_id(), Action.read, Category.Semantic);
      logger.trace(
          "readSemantic : Before invoking request to MaprDB JSON store :{}",
          mapper.writeValueAsString(structure));
      MetaDataStoreRequestAPI requestMetaDataStore = new MetaDataStoreRequestAPI(structure);
      requestMetaDataStore.process();
      String jsonStringFromStore = requestMetaDataStore.getResult().toString();
      nodeRetrieved = mapper.readValue(jsonStringFromStore, SemanticNode.class);
      logger.trace("Id: {}", nodeRetrieved.get_id());
      nodeRetrieved.setId(nodeRetrieved.get_id());
      nodeRetrieved.setStatusMessage("Entity has retrieved successfully");
      newSemanticNode = new SemanticNode();
      org.springframework.beans.BeanUtils.copyProperties(nodeRetrieved, newSemanticNode, "_id");
    } catch (Exception ex) {
      exists = false;
    }
    return exists;
  }

  /**
   * deleteSemantic.
   *
   * @param node node
   * @param basePath basePath
   * @throws SipJsonValidationException when SipJsonValidationException
   * @throws SipDeleteEntityException when SipDeleteEntityException
   */
  public void deleteSemantic(SemanticNode node, String basePath)
      throws SipJsonValidationException, SipDeleteEntityException {
    Preconditions.checkArgument(node.get_id() != null, "Id is mandatory attribute.");
    logger.trace("Deleting semantic from the store with an Id : {}", node.get_id());
    SemanticNode responseObject = new SemanticNode();
    try {
      List<MetaDataStoreStructure> structure =
          SipMetadataUtils.node2JsonObject(
              node, basePath, node.get_id(), Action.delete, Category.Semantic);
      logger.trace("Before invoking request to MaprDB JSON store :{}", structure);
      MetaDataStoreRequestAPI requestMetaDataStore = new MetaDataStoreRequestAPI(structure);
      requestMetaDataStore.process();
      responseObject.setId(node.get_id());
    } catch (Exception ex) {
      throw new SipDeleteEntityException("Problem on the storage while updating an entity", ex);
    }
  }
}
