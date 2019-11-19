'use strict';
const commonFunctions = require('../utils/commonFunctions');
const ChannelModel = require('./components/ChannelModel');

class ChannelActions extends ChannelModel {
  constructor() {
    super();
    // all the page elements in the channel list component
  }

  createNewChannel(channelInfo) {
    this.clickOnChannelType(channelInfo.sourceType);
    this.clickOnChannelNextButton();
    this.fillChannelName(channelInfo.channelName);
    this.selectAccessType(channelInfo.access);
    this.enterHostName(channelInfo.sftpHost);
    this.fillUserName(channelInfo.sftpUser);
    this.fillPortNumber(channelInfo.sftpPort);
    this.fillPassword(channelInfo.sftpPwd);
    this.fillDescription(channelInfo.desc);
    this.clickOnCreateButton();
  }

  createNewApiChannel(channelInfo) {
    this.clickOnChannelType(channelInfo.sourceType);
    this.clickOnChannelNextButton();
    this.fillChannelName(channelInfo.channelName);
    this.fillHostName(channelInfo.hostName);
    if (channelInfo.port) channelActions.fillPortNumber(channelInfo.port);
    this.selectMethodType(channelInfo.method);
    this.fillEndPoint(channelInfo.endPoint);
    if (channelInfo.method === 'POST') this.fillRequestBody(JSON.stringify(channelInfo.body));
    if (channelInfo.headers) this.addHeaders(channelInfo.headers);
    if (channelInfo.queryParams) this.addQueryParams(channelInfo.queryParams);
    this.fillDescription(channelInfo.desc);
    this.clickOnTestConnectivity();
    this.verifyTestConnectivityLogs(channelInfo.testConnectivityMessage);
    this.closeTestConnectivity();
    this.clickOnCreateButton();
  }
}

module.exports = ChannelActions;
