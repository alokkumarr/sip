package com.synchronoss.sip.alert.service;

import static com.synchronoss.sip.alert.util.AlertUtils.saveNotificationStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.synchronoss.saw.model.Model.Operator;
import com.synchronoss.sip.alert.modal.AlertNotificationLog;
import com.synchronoss.sip.alert.modal.AlertRuleDetails;
import com.synchronoss.sip.alert.modal.AlertSubscriberToken;
import com.synchronoss.sip.alert.modal.NotificationSubscriber;
import com.synchronoss.sip.alert.modal.Subscriber;
import com.synchronoss.sip.alert.util.AlertUtils;
import com.synchronoss.sip.utils.RestUtil;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class EmailNotifier implements Notifier {

  private static final Logger logger = LoggerFactory.getLogger(EmailNotifier.class);

  @Autowired
  RestUtil restUtil;

  @Autowired
  AlertService alertService;

  @Autowired
  SubscriberService subscriberService;

  @Value("${sip.service.alert.mail.body}")
  private String mailBody;

  @Value("${sip.service.alert.dashborad.url}")
  private String alertDashboardPath;

  @Value("${sip.service.storage-proxy.service.host}")
  private String transportUrl;

  @Value("${sip.service.alert.mail.subject}")
  private String mailSubject;

  @Value("${sip.service.metastore.base}")
  @NotNull
  private String basePath;

  @Value("${sip.service.metastore.notificationTable}")
  @NotNull
  private String notificationLogTable;

  @Value("${sip.service.alert.unsubscribe.url}")
  private String alertUnsubscribePath;

  @Value("${subscriber.secret.key}")
  private String secretKey;

  private ObjectMapper objectMapper = new ObjectMapper();

  private RestTemplate restTemplate = null;

  private String alertTriggerSysId;

  private AlertRuleDetails alertRule;

  public void setAlertTriggerSysId(String alertTriggerSysId) {
    this.alertTriggerSysId = alertTriggerSysId;
  }

  public void setAlertRule(AlertRuleDetails alertRule) {
    this.alertRule = alertRule;
  }

  public EmailNotifier() {
  }

  public EmailNotifier(AlertRuleDetails alertRule, String alertTriggerSysId) {
    this.alertRule = alertRule;
    this.alertTriggerSysId = alertTriggerSysId;
  }

  @Override
  public void notify(String content) {
    logger.info("checking null pointer inside notify : {}",
        alertRule.getAlertRuleName(), alertTriggerSysId);
    sendMailNotification(alertRule, alertTriggerSysId);
  }

  /**
   * Sends email notification.
   *
   * @param alertRulesDetails AlertRulesDetails
   * @param alertTriggerSysId alertTriggerSysId
   */
  public void sendMailNotification(AlertRuleDetails alertRulesDetails, String alertTriggerSysId) {
    logger.info("sending email notification");
    logger.info("secret key : {}", secretKey);
    AlertNotificationLog notificationLog = new AlertNotificationLog();
    logger.info("checking null pointers : {}",
        alertRulesDetails.getAlertRuleName(), alertTriggerSysId);
    notificationLog.setAlertRuleName(alertRulesDetails.getAlertRuleName());
    notificationLog.setThresholdValue(alertRulesDetails.getThresholdValue());
    notificationLog.setAttributeName(alertRulesDetails.getAttributeName());
    notificationLog.setAlertSeverity(alertRulesDetails.getAlertSeverity());

    Set<String> recipients = new HashSet<>();
    List<NotificationSubscriber> notificationSubscriberList = subscriberService
        .getSubscribersById(alertRulesDetails.getSubscribers());
    notificationSubscriberList.forEach(subs -> {
      recipients.add(subs.getChannelValue());
    });

    List<String> recipientsList =
        getActiveSubscribers(
            recipients,
            alertRulesDetails.getAlertRulesSysId());
    logger.trace("Final Recipients : {}, alertSysId {}", recipientsList,
        alertRulesDetails.getAlertRulesSysId());
    try {
      if (recipientsList != null && recipientsList.size() > 0) {
        // String recipients = String.join(",", recipientsList);
        recipientsList.stream()
            .forEach(
                recipient -> {
                  AlertSubscriberToken subscriber =
                      new AlertSubscriberToken(
                          alertRulesDetails.getAlertRulesSysId(),
                          alertRulesDetails.getAlertRuleName(),
                          alertRulesDetails.getAlertRuleDescription(),
                          alertTriggerSysId,
                          recipient);
                  sendMail(
                      alertRulesDetails,
                      recipient,
                      AlertUtils.getSubscriberToken(subscriber, secretKey));
                });
        notificationLog.setNotifiedStatus(Boolean.TRUE);
      } else {
        notificationLog.setMessage(
            "Receipients are missing for the alertRuleId:"
                + alertRulesDetails.getAlertRulesSysId());
      }
      notificationLog.setCreatedTime(new Date());
      saveNotificationStatus(notificationLog, basePath, notificationLogTable);
    } catch (RuntimeException exeception) {
      notificationLog.setNotifiedStatus(false);
      notificationLog.setMessage(exeception.toString());
      saveNotificationStatus(notificationLog, basePath, notificationLogTable);
      logger.error("Exception occured while sending Email Notification" + exeception);
    }
  }

  private List<String> getActiveSubscribers(Set<String> recipients, String alertRulesSysId) {
    List<String> recipientsList = new ArrayList<String>();
    recipientsList.addAll(recipients);
    List<Subscriber> subscribers = alertService.fetchInactiveSubscriberByAlertId(alertRulesSysId);
    subscribers.forEach(
        subscriber -> {
          if (subscriber.getActive() == Boolean.FALSE
              && recipientsList.contains(subscriber.getEmail())) {
            recipientsList.remove(subscriber.getEmail());
          }
        });
    return recipientsList;
  }

  /**
   * sends mail.
   *
   * @param alertRulesDetails AlertRulesDetails
   * @return status of mail notification
   */
  public boolean sendMail(
      AlertRuleDetails alertRulesDetails, String recipients, String subscriberToken) {
    ObjectNode mailRequestPayload = objectMapper.createObjectNode();
    mailRequestPayload.put("recipients", recipients);
    mailRequestPayload.put("subject", mailSubject);
    String preparedMailBody =
        prepareMailBody(alertRulesDetails, mailBody, alertDashboardPath, subscriberToken);
    mailRequestPayload.put("content", preparedMailBody);
    String mailRequestBody = null;
    try {
      mailRequestBody = objectMapper.writeValueAsString(mailRequestPayload);
    } catch (JsonProcessingException e) {
      logger.error("Error occured while parsing the request body of mail API {}", e);
      return false;
    }
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.set("Content-type", MediaType.APPLICATION_JSON_UTF8_VALUE);
    HttpEntity<?> requestEntity = new HttpEntity<Object>(mailRequestBody, requestHeaders);
    String url = transportUrl + "/exports/email/send";
    restTemplate = restUtil.restTemplate();
    ResponseEntity<JsonNode> aliasResponse =
        restTemplate.exchange(url, HttpMethod.POST, requestEntity, JsonNode.class);
    JsonNode response = (JsonNode) aliasResponse.getBody();
    logger.info("Alert id : {}, email sent : {}", alertRulesDetails.getAlertRulesSysId(),
        response.has("emailSent") ? response.get("emailSent").asBoolean() : false);
    return response.has("emailSent") ? response.get("emailSent").asBoolean() : false;
  }

  /**
   * prepares mail body.
   *
   * @param alertRulesDetails AlertRulesDetails
   * @param body mailbody
   * @param alertLink link for alert dashboard
   * @param subscriberToken subscriberToken
   * @return prepared mail body
   */
  public String prepareMailBody(
      AlertRuleDetails alertRulesDetails, String body, String alertLink, String subscriberToken) {
    logger.debug("prepare mail body starts here :" + body);
    if (alertRulesDetails.getNotification() != null
        && alertRulesDetails.getNotification().getEmail().getTemplate() != null) {
      // override the Body template if its configured for specific alerts.
      body = alertRulesDetails.getNotification().getEmail().getTemplate();
    }
    if (body.contains(MailBodyResolver.ALERT_RULE_NAME)) {
      String alertRuleName = alertRulesDetails.getAlertRuleName();
      if (alertRuleName == null) {
        alertRuleName = "";
      }
      body = body.replaceAll("\\" + MailBodyResolver.ALERT_RULE_NAME, alertRuleName);
    }
    if (body.contains(MailBodyResolver.CATEGORY)) {
      String category = alertRulesDetails.getCategoryId();
      if (category == null) {
        category = "";
      }
      body = body.replaceAll("\\" + MailBodyResolver.CATEGORY, category);
    }
    if (body.contains(MailBodyResolver.LINK_FOR_ALERT)) {
      String link = alertLink;
      if (link == null) {
        link = "";
      }
      body = body.replaceAll("\\" + MailBodyResolver.LINK_FOR_ALERT, link);
    }
    if (body.contains(MailBodyResolver.ALERT_SEVERITY)) {
      String severity = null;
      if (alertRulesDetails.getAlertSeverity() != null) {
        severity = alertRulesDetails.getAlertSeverity().value();
      } else {
        severity = "";
      }
      body = body.replaceAll("\\" + MailBodyResolver.ALERT_SEVERITY, severity);
    }
    if (body.contains(MailBodyResolver.ALERT_RULE_DESCRIPTION)) {
      String alertDescrptn = alertRulesDetails.getAlertRuleDescription();
      if (alertDescrptn == null) {
        alertDescrptn = "";
      }
      body = body.replaceAll("\\" + MailBodyResolver.ALERT_RULE_DESCRIPTION, alertDescrptn);
    }
    if (body.contains(MailBodyResolver.ATTRIBUTE_NAME)) {
      String attrName = alertRulesDetails.getAttributeName();
      if (attrName == null) {
        attrName = "";
      } else {
        if (attrName.endsWith(".keyword")) {
          attrName = attrName.replace(".keyword", "");
        }
      }
      body = body.replaceAll("\\" + MailBodyResolver.ATTRIBUTE_NAME, attrName);
    }
    if (body.contains(MailBodyResolver.ATTRIBUTE_VALUE)) {
      String attributeValue = alertRulesDetails.getAttributeValue();
      if (attributeValue == null) {
        attributeValue = "";
      }
      body = body.replaceAll("\\" + MailBodyResolver.ATTRIBUTE_VALUE, attributeValue);
    }
    if (body.contains(MailBodyResolver.THRESHOLD_VALUE)) {
      String alertCondition = getReadableConditionWithValues(alertRulesDetails);
      if (alertCondition == null) {
        alertCondition = "";
      }
      body = body.replaceAll("\\" + MailBodyResolver.THRESHOLD_VALUE, alertCondition);
    }
    if (body.contains(MailBodyResolver.LOOKBACK_PERIOD)) {
      String lookBackperiod = alertRulesDetails.getLookbackPeriod();
      if (lookBackperiod == null) {
        lookBackperiod = "";
      }
      body = body.replaceAll("\\" + MailBodyResolver.LOOKBACK_PERIOD, lookBackperiod);
    }
    if (body.contains(MailBodyResolver.UNSUBSCRIBE_LINK)) {
      body =
          body.replaceAll(
              "\\" + MailBodyResolver.UNSUBSCRIBE_LINK,
              "\"" + String.format(alertUnsubscribePath, subscriberToken) + "\"");
    }
    logger.debug("prepare mail body ends here :" + this.getClass().getName() + ": " + body);
    return body;
  }

  private String getReadableConditionWithValues(AlertRuleDetails alertRulesDetails) {
    Operator operator = alertRulesDetails.getOperator();
    Double threshold = alertRulesDetails.getThresholdValue();
    Double otherThreshold = alertRulesDetails.getOtherThresholdValue();
    String readbleOperator = alertService.getReadableOperator(operator);
    if (operator == Operator.BTW) {
      return readbleOperator + " " + otherThreshold + " and " + threshold;
    } else {
      return readbleOperator + " " + threshold;
    }
  }

  interface MailBodyResolver {

    String ALERT_RULE_NAME = "$alertRuleName";
    String CATEGORY = "$category";
    String LINK_FOR_ALERT = "$link";
    String ALERT_SEVERITY = "$alertSeverity";
    String ALERT_RULE_DESCRIPTION = "$alertRuleDescription";
    String THRESHOLD_VALUE = "$thresholdValue";
    String ATTRIBUTE_NAME = "$attributeName";
    String ATTRIBUTE_VALUE = "$attributeValue";
    String LOOKBACK_PERIOD = "$lookbackPeriod";
    String UNSUBSCRIBE_LINK = "$unsubscribeLink";
  }
}
