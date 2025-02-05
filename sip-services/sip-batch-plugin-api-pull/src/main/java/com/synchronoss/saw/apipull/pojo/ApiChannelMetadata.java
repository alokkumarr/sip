package com.synchronoss.saw.apipull.pojo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "channelName",
  "description",
  "channelType",
  "hostAddress",
  "port",
  "apiEndPoint",
  "httpMethod",
  "queryParameters",
  "headerParameters",
  "urlParameters",
  "bodyParameters"
})
public class ApiChannelMetadata {

  /** (Required) */
  @JsonProperty("channelName")
  private String channelName;

  @JsonProperty("description")
  private String description;
  /** (Required) */
  @JsonProperty("channelType")
  private String channelType;
  /** (Required) */
  @JsonProperty("hostAddress")
  @JsonAlias("hostName")
  @SerializedName(value = "hostAddress", alternate = "hostName")
  private String hostAddress;

  @JsonProperty("port")
  @JsonAlias("portNo")
  @SerializedName(value = "port", alternate = "portNo")
  private Integer port;
  /** (Required) */
  @JsonProperty("apiEndPoint")
  private String apiEndPoint;
  /** (Required) */
  @JsonProperty("httpMethod")
  private HttpMethod httpMethod = HttpMethod.fromValue("GET");
  /** (Required) */
  @JsonProperty("queryParameters")
  private List<QueryParameter> queryParameters = null;

  @JsonProperty("headerParameters")
  private List<HeaderParameter> headerParameters = null;
  /** (Required) */
  @JsonProperty("urlParameters")
  private List<Object> urlParameters = null;

  @JsonProperty("bodyParameters")
  private BodyParameters bodyParameters;

  /** (Required) */
  @JsonProperty("channelName")
  public String getChannelName() {
    return channelName;
  }

  /** (Required) */
  @JsonProperty("channelName")
  public void setChannelName(String channelName) {
    this.channelName = channelName;
  }

  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(String description) {
    this.description = description;
  }

  /** (Required) */
  @JsonProperty("channelType")
  public String getChannelType() {
    return channelType;
  }

  /** (Required) */
  @JsonProperty("channelType")
  public void setChannelType(String channelType) {
    this.channelType = channelType;
  }

  /** (Required) */
  @JsonProperty("hostAddress")
  public String getHostAddress() {
    return hostAddress;
  }

  /** (Required) */
  @JsonProperty("hostAddress")
  public void setHostAddress(String hostAddress) {
    this.hostAddress = hostAddress;
  }

  @JsonProperty("port")
  public Integer getPort() {
    return port;
  }

  @JsonProperty("port")
  public void setPort(Integer port) {
    this.port = port;
  }

  /** (Required) */
  @JsonProperty("apiEndPoint")
  public String getApiEndPoint() {
    return apiEndPoint;
  }

  /** (Required) */
  @JsonProperty("apiEndPoint")
  public void setApiEndPoint(String apiEndPoint) {
    this.apiEndPoint = apiEndPoint;
  }

  /** (Required) */
  @JsonProperty("httpMethod")
  public HttpMethod getHttpMethod() {
    return httpMethod;
  }

  /** (Required) */
  @JsonProperty("httpMethod")
  public void setHttpMethod(HttpMethod httpMethod) {
    this.httpMethod = httpMethod;
  }

  /** (Required) */
  @JsonProperty("queryParameters")
  public List<QueryParameter> getQueryParameters() {
    return queryParameters;
  }

  /** (Required) */
  @JsonProperty("queryParameters")
  public void setQueryParameters(List<QueryParameter> queryParameters) {
    this.queryParameters = queryParameters;
  }

  @JsonProperty("headerParameters")
  public List<HeaderParameter> getHeaderParameters() {
    return headerParameters;
  }

  @JsonProperty("headerParameters")
  public void setHeaderParameters(List<HeaderParameter> headerParameters) {
    this.headerParameters = headerParameters;
  }

  /** (Required) */
  @JsonProperty("urlParameters")
  public List<Object> getUrlParameters() {
    return urlParameters;
  }

  /** (Required) */
  @JsonProperty("urlParameters")
  public void setUrlParameters(List<Object> urlParameters) {
    this.urlParameters = urlParameters;
  }

  @JsonProperty("bodyParameters")
  public BodyParameters getBodyParameters() {
    return bodyParameters;
  }

  @JsonProperty("bodyParameters")
  public void setBodyParameters(BodyParameters bodyParameters) {
    this.bodyParameters = bodyParameters;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("channelName", channelName)
        .append("description", description)
        .append("channelType", channelType)
        .append("hostAddress", hostAddress)
        .append("port", port)
        .append("apiEndPoint", apiEndPoint)
        .append("httpMethod", httpMethod)
        .append("queryParameters", queryParameters)
        .append("headerParameters", headerParameters)
        .append("urlParameters", urlParameters)
        .append("bodyParameters", bodyParameters)
        .toString();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(port)
        .append(headerParameters)
        .append(apiEndPoint)
        .append(channelName)
        .append(bodyParameters)
        .append(channelType)
        .append(httpMethod)
        .append(urlParameters)
        .append(description)
        .append(hostAddress)
        .append(queryParameters)
        .toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof ApiChannelMetadata) == false) {
      return false;
    }
    ApiChannelMetadata rhs = ((ApiChannelMetadata) other);
    return new EqualsBuilder()
        .append(port, rhs.port)
        .append(headerParameters, rhs.headerParameters)
        .append(apiEndPoint, rhs.apiEndPoint)
        .append(channelName, rhs.channelName)
        .append(bodyParameters, rhs.bodyParameters)
        .append(channelType, rhs.channelType)
        .append(httpMethod, rhs.httpMethod)
        .append(urlParameters, rhs.urlParameters)
        .append(description, rhs.description)
        .append(hostAddress, rhs.hostAddress)
        .append(queryParameters, rhs.queryParameters)
        .isEquals();
  }
}
