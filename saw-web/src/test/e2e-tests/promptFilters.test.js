/*
 Created by Anudeep
 */

const login = require('../javascript/pages/loginPage.po.js');
const analyzePage = require('../javascript/pages/analyzePage.po.js');
const commonFunctions = require('../javascript/helpers/commonFunctions.js');
const homePage = require('../javascript/pages/homePage.po');
const savedAlaysisPage = require('../javascript/pages/savedAlaysisPage.po');
const protractorConf = require('../../../../saw-web/conf/protractor.conf');
const using = require('jasmine-data-provider');
const categories = require('../javascript/data/categories');
const subCategories = require('../javascript/data/subCategories');
const dataSets = require('../javascript/data/datasets');
const designModePage = require('../javascript/pages/designModePage.po.js');
let AnalysisHelper = require('../javascript/api/AnalysisHelper');
let ApiUtils = require('../javascript/api/APiUtils');
const globalVariables = require('../javascript/helpers/globalVariables');
const utils = require('../javascript/helpers/utils');

describe('Prompt filter tests: promptFilters.test.js', () => {
  const defaultCategory = categories.privileges.name;
  const categoryName = categories.analyses.name;
  const subCategoryName = subCategories.createAnalysis.name;
  const chartDesigner = analyzePage.designerDialog.chart;
  //updated fields
  const metrics = 'Integer';
  const dimension = 'String';
  const yAxisName2 = 'Long';
  const groupName = 'Date';
  const metricName = dataSets.pivotChart;
  const sizeByName = 'Float';
  let analysisId;
  let host;
  let token; 
  const fieldName = metrics;

  beforeAll(function () {
    host = new ApiUtils().getHost(browser.baseUrl);
    token = new AnalysisHelper().getToken(host);
    jasmine.DEFAULT_TIMEOUT_INTERVAL = protractorConf.timeouts.extendedDefaultTimeoutInterval;
    
  });

  beforeEach(function (done) {
    setTimeout(function () {
      console.log('I am here.....1')
      expect(browser.getCurrentUrl()).toContain('/login');
      done();
    }, protractorConf.timeouts.pageResolveTimeout);
  });

  afterEach(function (done) {
    setTimeout(function () {
      console.log('I am here.....2')
      new AnalysisHelper().delete(host, token, protractorConf.config.customerCode, analysisId);
      analyzePage.main.doAccountAction('logout');
      done();
    }, protractorConf.timeouts.pageResolveTimeout);
  });

  it('should able to apply prompt filter for charts from analysis detail/view page', () => {
    let currentTime = new Date().getTime();
    let user = 'userOne';
    let chartType = 'chart:column';
    let type = chartType.split(":")[1];

    let name = chartType+' ' + globalVariables.e2eId+'-'+currentTime;
    let description ='Description:'+chartType+' for e2e ' + globalVariables.e2eId+'-'+currentTime;

    console.log('I am here.....3')
    applyFilters(user, name, description, type);
    //From analysis detail/view page
    //Execute the analysis from and verify it asks for prompt filter
    commonFunctions.waitFor.elementToBeVisible(savedAlaysisPage.actionsMenuBtn);
    commonFunctions.waitFor.elementToBeClickable(savedAlaysisPage.actionsMenuBtn);
    savedAlaysisPage.actionsMenuBtn.click();
    commonFunctions.waitFor.elementToBeVisible(savedAlaysisPage.executeMenuOption);
    commonFunctions.waitFor.elementToBeClickable(savedAlaysisPage.executeMenuOption);
    savedAlaysisPage.executeMenuOption.click();
    //Verify filter dailog
    commonFunctions.waitFor.elementToBeVisible(analyzePage.prompt.filterDialog);
    expect(analyzePage.prompt.filterDialog.isDisplayed()).toBeTruthy();
    expect(analyzePage.prompt.selectedField.getAttribute("value")).toEqual(fieldName);
    //apply fileters and execute
    
});

  const validateSelectedFilters = (filters) => {

    analyzePage.appliedFiltersDetails.selectedFilters.map(function(elm) {
      return elm.getText();
    }).then(function(displayedFilters) {
      expect(utils.arrayContainsArray(displayedFilters, filters)).toBeTruthy();
    });
  };

  const applyFilters = (user, name, description, type) => {
    console.log('I am here.....4')
   //Create new analysis.
   new AnalysisHelper().createChart(host, token,name,description, type);
   login.loginAs(user);
   homePage.navigateToSubCategoryUpdated(categoryName, subCategoryName, defaultCategory);
   //Change to Card View.
   commonFunctions.waitFor.elementToBeVisible(analyzePage.analysisElems.cardView);
   commonFunctions.waitFor.elementToBeClickable(analyzePage.analysisElems.cardView);
   analyzePage.analysisElems.cardView.click();
   //Open the created analysis.
   const createdAnalysis = analyzePage.main.getCardTitle(name);       
   commonFunctions.waitFor.elementToBeVisible(createdAnalysis);
   commonFunctions.waitFor.elementToBeClickable(createdAnalysis);
   createdAnalysis.click();
   //get analysis id from current url
   browser.getCurrentUrl().then(url => {
     analysisId = commonFunctions.getAnalysisIdFromUrl(url);
   });
   commonFunctions.waitFor.elementToBeClickable(savedAlaysisPage.editBtn);
   savedAlaysisPage.editBtn.click();
   //apply filters
   const filters = analyzePage.filtersDialogUpgraded;
   const filterAC = filters.getFilterAutocomplete(0);
   commonFunctions.waitFor.elementToBeClickable(chartDesigner.filterBtn);
   chartDesigner.filterBtn.click();
   commonFunctions.waitFor.elementToBeClickable(designModePage.filterWindow.addFilter('sample'));
   designModePage.filterWindow.addFilter('sample').click();
   filterAC.sendKeys(fieldName, protractor.Key.DOWN, protractor.Key.ENTER);
   commonFunctions.waitFor.elementToBeVisible(filters.prompt);
   commonFunctions.waitFor.elementToBeClickable(filters.prompt);
   filters.prompt.click();
   commonFunctions.waitFor.elementToBeClickable(filters.applyBtn);
   filters.applyBtn.click();
   browser.sleep(1000);
   //TODO: Need to check that filters applied or not.
   commonFunctions.waitFor.elementToBeVisible(analyzePage.appliedFiltersDetails.filterText);
   commonFunctions.waitFor.elementToBeVisible(analyzePage.appliedFiltersDetails.filterClear);
   commonFunctions.waitFor.elementToBeVisible(analyzePage.appliedFiltersDetails.selectedFiltersText);
   validateSelectedFilters([fieldName]);
    //Save
   const save = analyzePage.saveDialog;
   const designer = analyzePage.designerDialog;
   commonFunctions.waitFor.elementToBeClickable(designer.saveBtn);
   designer.saveBtn.click();
   commonFunctions.waitFor.elementToBeVisible(designer.saveDialog);
   commonFunctions.waitFor.elementToBeClickable(save.saveBtn);
   save.saveBtn.click();

  };

});