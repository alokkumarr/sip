'use strict';

const logger = require('../../../conf/logger')(__filename);
const commonFunctions = require('../../utils/commonFunctions');
const expect = require('chai').expect;

class TestConnectivity {
  constructor() {
    this._testConnectivitySection = element(
      by.css(`[e2e="test-connectivity"]`)
    );
    this._connectivtyOuptut = element(by.css(`[id="screen"]`));
    this._closeTestConnectivityLog = element(
      by.css(`[e2e="close-test-connectivity"]`)
    );
  }

  verifyTestConnectivityLogs(msg = null) {
    commonFunctions.waitFor.elementToBeVisible(this._testConnectivitySection);
    commonFunctions.waitFor.elementToBeVisible(this._connectivtyOuptut);

    this._connectivtyOuptut.getText().then(content => {
      console.log('Content from logs---' + content);
      if (msg) {
        expect(content).to.contain(msg);
      }
    });
  }
  closeTestConnectivity() {
    commonFunctions.clickOnElement(this._closeTestConnectivityLog);
    commonFunctions.waitFor.elementToBeNotVisible(
      this._testConnectivitySection
    );
  }
}

module.exports = TestConnectivity;
