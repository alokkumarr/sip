var testDataReader = require('../testdata/testDataReader.js');
const using = require('jasmine-data-provider');
const protractorConf = require('../conf/protractor.conf');
var appRoot = require('app-root-path');
var fs = require('fs');


  describe('DEV1 test from dev1.js', () => {

    using(testDataReader.testData['DEV1']['dp'], function(data, id) {

    it(`${id}:${data.description}`, function() {
      console.log('I am in test');
      console.log(JSON.stringify(id));
      console.log(JSON.stringify(data));
      expect(true).toBe(false);

    }).result.testInfo = {testId: id, data: data, feature:'DEV1', dataProvider:'dp'};
  });
});

using(testDataReader.testData['DEV2']['dp'], function(data, id) {
  describe('DEV2 test from dev1.js', () => {


    it(`${id}:${data.description}`, function() {
      console.log('I am in test');
      console.log(JSON.stringify(id));
      console.log(JSON.stringify(data));
      if(data.user == 'admin') {
        expect(true).toBe(true);
      } else {
        expect(true).toBe(false);
      }

    }).result.testInfo = {testId: id, data: data, feature:'DEV2', dataProvider:'dp'};
  });
});
