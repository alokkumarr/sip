package com.synchronoss.saw.batch.plugin.controllers;

import com.synchronoss.saw.batch.exception.SftpProcessorException;
import com.synchronoss.saw.batch.exceptions.SipNestedRuntimeException;
import com.synchronoss.saw.batch.extensions.SipPluginContract;
import com.synchronoss.saw.batch.model.BisConnectionTestPayload;
import com.synchronoss.saw.batch.model.BisDataMetaInfo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.io.IOException;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/ingestion/batch/sftp")
public class SawBisSftpPluginController {

  @Autowired
  @Qualifier("sftpService")
  private SipPluginContract sftpServiceImpl;
  
  /**
   * This end-point to test connectivity for existing route.
   */
  @ApiOperation(value = "To test connectivity for existing route",
      nickname = "sftpActionBis", notes = "", response = HttpStatus.class)
  @ApiResponses(value = { @ApiResponse(code = 200, 
      message = "Request has been succeeded without any error"),
      @ApiResponse(code = 404, message = "The resource you were trying to reach is not found"),
      @ApiResponse(code = 500, message = "Server is down. Contact System adminstrator") })
  @RequestMapping(value = "/routes/{routeId}/status", method = RequestMethod.GET, 
      produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public HttpStatus connectRoute(@ApiParam(value = "Route id to test connectivity",
          required = true) @PathVariable(name = "routeId",required = true) Long routeId) {
    return sftpServiceImpl.connectRoute(routeId);
  }
  /**
   * This end-point to test connectivity for route.
   */
  
  @ApiOperation(value = "To test connectivity for route without an entity present on the system",
      nickname = "sftpActionBis", notes = "", response = HttpStatus.class)
  @ApiResponses(value = { @ApiResponse(code = 200, 
      message = "Request has been succeeded without any error"),
      @ApiResponse(code = 404, message = "The resource you were trying to reach is not found"),
      @ApiResponse(code = 500, message = "Server is down. Contact System adminstrator") })
  @RequestMapping(value = "/routes/test", method = RequestMethod.POST, 
      produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public HttpStatus connectImmediateRoute(@ApiParam(value = "Payload to test connectivity",
          required = true) @Valid @RequestBody BisConnectionTestPayload payload) 
          throws SipNestedRuntimeException, IOException {
    return sftpServiceImpl.immediateConnectRoute((payload));
  }

  /**
   * This end-point to test connectivity for channel.
   */
  @ApiOperation(value = "To test connectivity for existing channel",
      nickname = "sftpActionBis", notes = "", response = HttpStatus.class)
  @ApiResponses(value = { @ApiResponse(code = 200, 
      message = "Request has been succeeded without any error"),
      @ApiResponse(code = 404, message = "The resource you were trying to reach is not found"),
      @ApiResponse(code = 500, message = "Server is down. Contact System adminstrator") })
  @RequestMapping(value = "/channels/{channelId}/status", method = RequestMethod.GET, 
      produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public HttpStatus connectChannel(@ApiParam(value = "Channel id to test connectivity",
          required = true) @PathVariable(name = "channelId",required = true) Long channelId) {
    return sftpServiceImpl.connectChannel(channelId);
  }
  
  /**
   * This end-point to test connectivity for channel.
   */
  
  @ApiOperation(value = "To test connectivity for channel without an entity present on the system",
      nickname = "sftpActionBis", notes = "", response = HttpStatus.class)
  @ApiResponses(value = { @ApiResponse(code = 200, 
      message = "Request has been succeeded without any error"),
      @ApiResponse(code = 404, message = "The resource you were trying to reach is not found"),
      @ApiResponse(code = 500, message = "Server is down. Contact System adminstrator") })
  @RequestMapping(value = "/channels/test", method = RequestMethod.POST, 
      produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public HttpStatus connectImmediateChannel(@ApiParam(value = "Payload to test connectivity",
          required = true) @Valid @RequestBody BisConnectionTestPayload payload) {
    return sftpServiceImpl.immediateConnectChannel(payload);
  }

  /**
   * This end-point to transfer data from remote channel without logging.
   * i.e. immediate transfer while in design phase.
   */
  
  @ApiOperation(value = "To pull data from remote channel either provide "
      + "(channelId & routeId) or other details",
      nickname = "sftpActionBis", notes = "", response = List.class)
  @ApiResponses(value = { @ApiResponse(code = 200, 
      message = "Request has been succeeded without any error"),
      @ApiResponse(code = 404, message = "The resource you were trying to reach is not found"),
      @ApiResponse(code = 500, message = "Server is down. Contact System adminstrator") })
  @RequestMapping(value = "/channel/transfers", method = RequestMethod.POST, 
      produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public List<BisDataMetaInfo> immediateTransfer(@ApiParam(value = "Payload structure which "
      + "to be used to "
      + "initiate the transfer",
          required = true) @Valid @RequestBody(required = true) 
      BisConnectionTestPayload requestBody) {
    List<BisDataMetaInfo> response = null;
    try {
      if (Long.valueOf(requestBody.getChannelId()) > 0L 
          && Long.valueOf(requestBody.getRouteId()) > 0L) {
        response = sftpServiceImpl.transferData(
        Long.valueOf(requestBody.getChannelId()), Long.valueOf(requestBody.getRouteId()));  
      } else {
        response = sftpServiceImpl.immediateTransfer(requestBody);
      }
    } catch (Exception e) {
      throw new SftpProcessorException("Exception occured while transferring the file", e);
    }
    return response;
  }
}
