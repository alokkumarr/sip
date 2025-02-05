package com.synchronoss.bda.sip.jwt.token;

import java.io.Serializable;
import java.util.List;

/** Created by pman0003 on 10/3/2017. */
public class TicketDSKDetails implements Serializable {

  private static final long serialVersionUID = 7546190895561288031L;

  private String name;
  private List<String> values;

  /**
   * Gets name
   *
   * @return value of name
   */
  public String getName() {
    return name;
  }

  /** Sets value of name */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets values
   *
   * @return value of values
   */
  public List<String> getValues() {
    return values;
  }

  /** Sets value of values */
  public void setValues(List<String> values) {
    this.values = values;
  }
}
