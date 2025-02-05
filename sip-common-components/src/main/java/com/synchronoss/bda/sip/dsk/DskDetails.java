package com.synchronoss.bda.sip.dsk;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang.builder.ToStringBuilder;

@ApiModel
public class DskDetails {

  @JsonProperty("message")
  @ApiModelProperty(notes = "gives success or error message", name = "message")
  private String message;

  @JsonProperty("dskGroupPayload")
  @ApiModelProperty(notes = "dsk group payload", name = "dskGroupPayload")
  private DskGroupPayload dskGroupPayload;

  @JsonProperty("securityGroupSysId")
  @ApiModelProperty(notes = "securityGroupSysId", name = "securityGroupSysId")
  private Long securityGroupSysId;

  @JsonProperty("customerCode")
  @ApiModelProperty(notes = "customerCode", name = "customerCode")
  private String customerCode;

  @JsonProperty("customerId")
  @ApiModelProperty(notes = "customerId", name = "customerId")
  private Long customerId;

  @JsonProperty("isJvCustomer")
  @ApiModelProperty(notes = "User is super admin or not ", name = "isJvCustomer")
  private Integer isJvCustomer;

  @JsonProperty("filterByCustomerCode")
  @ApiModelProperty(notes = "filterByCustomerCode parameter", name = "filterByCustomerCode")
  private Integer filterByCustomerCode;

  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  @JsonProperty("message")
  public void setMessage(String message) {
    this.message = message;
  }

  @JsonProperty("dskGroupPayload")
  public DskGroupPayload getDskGroupPayload() {
    return dskGroupPayload;
  }

  @JsonProperty("dskGroupPayload")
  public void setDskGroupPayload(DskGroupPayload dskGroupPayload) {
    this.dskGroupPayload = dskGroupPayload;
  }

  @JsonProperty("customerId")
  public Long getCustomerId() {
    return customerId;
  }

  @JsonProperty("customerId")
  public void setCustomerId(Long customerId) {
    this.customerId = customerId;
  }

  @JsonProperty("customerCode")
  public String getCustomerCode() {
    return customerCode;
  }

  @JsonProperty("customerCode")
  public void setCustomerCode(String customerCode) {
    this.customerCode = customerCode;
  }

  @JsonProperty("isJvCustomer")
  public Integer getIsJvCustomer() {
    return isJvCustomer;
  }

  @JsonProperty("isJvCustomer")
  public void setIsJvCustomer(Integer isJvCustomer) {
    this.isJvCustomer = isJvCustomer;
  }

  @JsonProperty("filterByCustomerCode")
  public Integer getFilterByCustomerCode() {
    return filterByCustomerCode;
  }

  @JsonProperty("filterByCustomerCode")
  public void setFilterByCustomerCode(Integer filterByCustomerCode) {
    this.filterByCustomerCode = filterByCustomerCode;
  }

  @JsonProperty("securityGroupSysId")
  public Long getSecurityGroupSysId() {
    return securityGroupSysId;
  }

  @JsonProperty("securityGroupSysId")
  public void setSecurityGroupSysId(Long securityGroupSysId) {
    this.securityGroupSysId = securityGroupSysId;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("message", message)
        .append("dskGroupPayload", dskGroupPayload)
        .append("customerCode", customerCode)
        .append("isJvCustomer", isJvCustomer)
        .append("filterByCustomerCode", filterByCustomerCode)
        .append("securityGroupSysId", securityGroupSysId)
        .toString();
  }
}
