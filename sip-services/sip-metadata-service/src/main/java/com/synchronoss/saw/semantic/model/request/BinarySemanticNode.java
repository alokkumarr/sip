package com.synchronoss.saw.semantic.model.request;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BinarySemanticNode {

  @JsonProperty("statusMessage")
  private String statusMessage;

  @JsonProperty("type")
  private String type;

  @JsonProperty("_id")
  private String _id;

  @JsonProperty("id")
  private String id;

  @JsonProperty("createdBy")
  private String createdBy;

  @JsonProperty("updatedBy")
  private String updatedBy;

  @JsonProperty("createdAt")
  private long createdAt;

  @JsonProperty("updatedAt")
  private long updatedAt;

  @JsonProperty("parentDataSetNames")
  private List<String> parentDataSetNames;

  @JsonProperty("parentDataSetIds")
  private List<String> parentDataSetIds;

  /**
   * The Customercode Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("customerCode")
  private String customerCode;
  /**
   * The Projectcode Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("projectCode")
  private String projectCode;
  /** (Required). */
  @JsonProperty("dataSetId")
  private List<Object> dataSetId = null;

  @JsonProperty("groupByColumns")
  private List<Object> groupByColumns = null;

  @JsonProperty("sqlBuilder")
  private Object sqlBuilder = null;

  @JsonProperty("saved")
  private Boolean saved = false;

  /**
   * The Username Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("username")
  private String username;
  /**
   * The Datasecuritykey Schema.
   *
   * <p>
   */
  @JsonProperty("dataSecurityKey")
  private String dataSecurityKey;
  /**
   * The Module Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("module")
  private BinarySemanticNode.Module module = BinarySemanticNode.Module.fromValue("ANALYZE");
  /**
   * The Metricname Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("metricName")
  private String metricName;
  /** (Required). */
  @JsonProperty("supports")
  private List<Object> supports = null;
  /** (Required). */
  @JsonProperty("artifacts")
  private List<Object> artifacts = null;
  /**
   * The Esrepository Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("esRepository")
  private Object esRepository;
  /**
   * The Repository Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("repository")
  private Object repository;

  @JsonIgnore private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("groupByColumns")
  public List<Object> getGroupByColumns() {
    return groupByColumns;
  }

  @JsonProperty("groupByColumns")
  public void setGroupByColumns(List<Object> groupByColumns) {
    this.groupByColumns = groupByColumns;
  }

  @JsonProperty("sqlBuilder")
  public Object getSqlBuilder() {
    return sqlBuilder;
  }

  @JsonProperty("sqlBuilder")
  public void setSqlBuilder(Object sqlBuilder) {
    this.sqlBuilder = sqlBuilder;
  }

  @JsonProperty("saved")
  public Boolean getSaved() {
    return saved;
  }

  @JsonProperty("saved")
  public void setSaved(Boolean saved) {
    this.saved = saved;
  }

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  /**
   * The Customercode Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("customerCode")
  public String getCustomerCode() {
    return customerCode;
  }

  /**
   * The Customercode Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("customerCode")
  public void setCustomerCode(String customerCode) {
    this.customerCode = customerCode;
  }

  @JsonProperty("_id")
  public String get_id() {
    return _id;
  }

  @JsonProperty("_id")
  public void set_id(String _id) {
    this._id = _id;
  }

  @JsonProperty("type")
  public String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  /**
   * The Projectcode Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("projectCode")
  public String getProjectCode() {
    return projectCode;
  }

  /**
   * The Projectcode Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("projectCode")
  public void setProjectCode(String projectCode) {
    this.projectCode = projectCode;
  }

  /** (Required). */
  @JsonProperty("dataSetId")
  public List<Object> getDataSetId() {
    return dataSetId;
  }

  /** (Required). */
  @JsonProperty("dataSetId")
  public void setDataSetId(List<Object> dataSetId) {
    this.dataSetId = dataSetId;
  }

  /**
   * The Username Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("username")
  public String getUsername() {
    return username;
  }

  /**
   * The Username Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("username")
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * The Datasecuritykey Schema.
   *
   * <p>
   */
  @JsonProperty("dataSecurityKey")
  public String getDataSecurityKey() {
    return dataSecurityKey;
  }

  /**
   * The Datasecuritykey Schema.
   *
   * <p>
   */
  @JsonProperty("dataSecurityKey")
  public void setDataSecurityKey(String dataSecurityKey) {
    this.dataSecurityKey = dataSecurityKey;
  }

  /**
   * The Module Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("module")
  public BinarySemanticNode.Module getModule() {
    return module;
  }

  /**
   * The Module Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("module")
  public void setModule(BinarySemanticNode.Module module) {
    this.module = module;
  }

  /**
   * The Metricname Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("metricName")
  public String getMetricName() {
    return metricName;
  }

  /**
   * The Metricname Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("metricName")
  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  /** (Required). */
  @JsonProperty("supports")
  public List<Object> getSupports() {
    return supports;
  }

  /** (Required). */
  @JsonProperty("supports")
  public void setSupports(List<Object> supports) {
    this.supports = supports;
  }

  /** (Required). */
  @JsonProperty("artifacts")
  public List<Object> getArtifacts() {
    return artifacts;
  }

  /** (Required). */
  @JsonProperty("artifacts")
  public void setArtifacts(List<Object> artifacts) {
    this.artifacts = artifacts;
  }

  /**
   * The Esrepository Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("esRepository")
  public Object getEsRepository() {
    return esRepository;
  }

  /**
   * The Esrepository Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("esRepository")
  public void setEsRepository(Object esRepository) {
    this.esRepository = esRepository;
  }

  /**
   * The Repository Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("repository")
  public Object getRepository() {
    return repository;
  }

  /**
   * The Repository Schema.
   *
   * <p>(Required)
   */
  @JsonProperty("repository")
  public void setRepository(Object repository) {
    this.repository = repository;
  }

  @JsonProperty("parentDataSetNames")
  public List<String> getParentDataSetNames() {
    return parentDataSetNames;
  }

  @JsonProperty("parentDataSetNames")
  public void setParentDataSetNames(List<String> parentDataSetNames) {
    this.parentDataSetNames = parentDataSetNames;
  }

  @JsonProperty("parentDataSetIds")
  public List<String> getParentDataSetIds() {
    return parentDataSetIds;
  }

  @JsonProperty("parentDataSetIds")
  public void setParentDataSetIds(List<String> parentDataSetIds) {
    this.parentDataSetIds = parentDataSetIds;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  public void setAdditionalProperties(Map<String, Object> additionalProperties) {
    this.additionalProperties = additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  @JsonProperty("statusMessage")
  public String getStatusMessage() {
    return statusMessage;
  }

  @JsonProperty("statusMessage")
  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  @JsonProperty("createdBy")
  public String getCreatedBy() {
    return createdBy;
  }

  @JsonProperty("createdBy")
  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  @JsonProperty("updatedBy")
  public String getUpdatedBy() {
    return updatedBy;
  }

  @JsonProperty("updatedBy")
  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  @JsonProperty("createdAt")
  public long getCreatedAt() {
    return createdAt;
  }

  @JsonProperty("createdAt")
  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  @JsonProperty("updatedAt")
  public long getUpdatedAt() {
    return updatedAt;
  }

  @JsonProperty("updatedAt")
  public void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }

  public enum Module {
    ANALYZE("ANALYZE"),
    OBSERVE("OBSERVE"),
    ALERT("ALERT"),
    WORKBENCH("WORKBENCH");
    private static final Map<String, BinarySemanticNode.Module> CONSTANTS = new HashMap<>();

    static {
      for (BinarySemanticNode.Module c : values()) {
        CONSTANTS.put(c.value, c);
      }
    }

    private final String value;

    private Module(String value) {
      this.value = value;
    }

    /**
     * fetch Value.
     *
     * @param value
     * @return BinarySemanticNode.Module
     */
    @JsonCreator
    public static BinarySemanticNode.Module fromValue(String value) {
      BinarySemanticNode.Module constant = CONSTANTS.get(value);
      if (constant == null) {
        throw new IllegalArgumentException(value);
      } else {
        return constant;
      }
    }

    @Override
    public String toString() {
      return this.value;
    }

    @JsonValue
    public String value() {
      return this.value;
    }
  }
}
