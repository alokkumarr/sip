package com.synchronoss.saw.workbench.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.synchronoss.bda.sip.jwt.TokenParser;
import com.synchronoss.bda.sip.jwt.token.Ticket;
import com.synchronoss.saw.workbench.service.WorkbenchExecutionService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.util.Base64;
import java.util.Map;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sncr.bda.base.MetadataBase;
import sncr.bda.datasets.conf.DataSetProperties;
import sncr.xdf.component.Component;

@RestController
@RequestMapping("/internal/workbench/projects/")
public class WorkbenchExecutionController {
  private static final Logger logger = LoggerFactory.getLogger(WorkbenchExecutionController.class);

  @Value("${workbench.project-root}")
  @NotNull
  private String defaultProjectRoot;

  @Value("${workbench.project-path}")
  @NotNull
  private String defaultProjectPath;

  @Autowired
  private WorkbenchExecutionService workbenchExecutionService;

  /**
   * Create dataset function.
   * @param project Project ID
   * @param body Body Parameters
   * @param authToken Authentication Token header
   * @return Returns an object node
   * @throws JsonProcessingException When unable to decode the string
   * @throws Exception General Exception
   */
  @RequestMapping(
      value = "{project}/datasets", method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public ObjectNode create(
      @PathVariable(name = "project", required = true) String project,
      @RequestBody ObjectNode body,
      @RequestHeader("Authorization") String authToken)
      throws JsonProcessingException, Exception {
    logger.info("Create dataset: body = {}", body);
   logger .debug("Create dataset: project = {}", project);
    //.debug("Auth token = {}", authToken);
    if (authToken.startsWith("Bearer")) {
       authToken = authToken.substring("Bearer ".length());
    }
    /* Extract input parameters */
    final String name = body.path("name").asText();
    final String description = body.path("description").asText();
    String input = body.path("input").asText();

    Ticket ticket = TokenParser.retrieveTicket(authToken);

    logger.info(ticket.getUserFullName());

    String component = body.path("component").asText();
    JsonNode configNode = body.path("configuration");
    if (!configNode.isObject()) {
      throw new RuntimeException(
        "Expected config to be an object: " + configNode);
    }
    ObjectNode config = (ObjectNode) configNode;
    /* Build XDF component-specific configuration */
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode xdfConfig = mapper.createObjectNode();
    ObjectNode xdfComponentConfig = xdfConfig.putObject(component);
    if (component.equals("parser")) {
      String rawDirectory = defaultProjectRoot + defaultProjectPath;
      String file = config.path("file").asText();
      config.put("file", rawDirectory + "/" + file);
      xdfConfig.set(component, config);
    } else if (component.equals("sql")) {
      xdfComponentConfig.put("scriptLocation", "inline");
      String script = config.path("script").asText();
      String encoded = Base64.getEncoder()
          .encodeToString(script.getBytes("utf-8"));
      xdfComponentConfig.put("script", encoded);
    } else {
      throw new RuntimeException("Unknown component: " + component);
    }
    /* Build inputs */
    if (!component.equals("parser")) {
      ArrayNode xdfInputs = xdfConfig.putArray("inputs");
      ObjectNode xdfInput = xdfInputs.addObject();
      xdfInput.put("dataSet", input);
    }
    /* Build outputs */
    ArrayNode xdfOutputs = xdfConfig.putArray("outputs");
    ObjectNode xdfOutput = xdfOutputs.addObject();
    xdfOutput.put("dataSet", name);
    xdfOutput.put("name", Component.DATASET.output.name());
    xdfOutput.put("desc", description);

    JsonNode userDataNode = config.get("userdata");
    ObjectNode userData = null;
    if(userDataNode == null){
        userData = mapper.createObjectNode();
    }else{
        if(!userDataNode.isObject()){
            throw new RuntimeException(
                "Expected userData to be an object: " + userDataNode);
        }
        userData = (ObjectNode) userDataNode;
    }
    userData.put(DataSetProperties.createdBy.toString(), ticket.getUserFullName());
    xdfOutput.set(DataSetProperties.UserData.toString(), userData);

    /* Invoke XDF component */
    return workbenchExecutionService.execute(
      project, name, component, xdfConfig.toString());
  }

  

	/**
	 * This method is to preview the data.
	 * 
	 * @param project   is of type String.
	 * @param previewId is of type String.
	 * @return ObjectNode is of type Object.
	 * @throws JsonProcessingException when this exceptional condition happens.
	 * @throws Exception               when this exceptional condition happens.
	 */
	@RequestMapping(value = "{project}/previews/{datasetName}", method = RequestMethod.GET, 
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ObjectNode getPreview(@PathVariable(name = "project", required = true) String project,
			@PathVariable(name = "datasetName", required = true) String datasetName)
			throws JsonProcessingException, Exception {

		logger.debug("Get dataset preview: project = {}", project);
		logger.debug("Get dataset preview: dataset = {}", datasetName);

		/* Get previously created preview */
		ObjectNode content = workbenchExecutionService.getPreview(project, datasetName);

		/* Otherwise return the preview contents */
		return content;

	}

  


  @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Preview does not exist")
  private static class NotFoundException extends RuntimeException {
    private static final long serialVersionUID = 412355610432444770L;
  }
}
