var appRoot = require('app-root-path');
const loginPage = require(appRoot + '/src/test/javascript/pages/loginPage.po.js');
const analyzePage = require(appRoot + '/src/test/javascript/pages/analyzePage.po.js');
const homePage = require(appRoot + '/src/test/javascript/pages/homePage.po.js');
const protractor = require('protractor');
const protractorConf = require(appRoot + '/conf/protractor.conf');
const commonFunctions = require(appRoot + '/src/test/javascript/helpers/commonFunctions.js');
const dataSets = require(appRoot + '/src/test/javascript/data/datasets');
const designModePage = require(appRoot + '/src/test/javascript/pages/designModePage.po.js');

describe('Create report type analysis: createReport.test.js', () => {
  const reportDesigner = analyzePage.designerDialog.report;
  const reportName = `e2e report ${(new Date()).toString()}`;
  const reportDescription = 'e2e report description';
  const tables = [{
    name: 'SALES',
    fields: [
      'Integer',
      'String',
      'Date'
    ]
  }/*, {
    name: 'MCT_CONTENT_SUMMARY',
    fields: [
      'Available Items'
    ]
  }*/];
  /*const join = {
    tableA: tables[0].name,
    fieldA: 'Session Id',
    tableB: tables[1].name,
    fieldB: 'Session Id'
  };*/
  const filterValue = 'String';
  const metricName = dataSets.report;
  const analysisType = 'table:report';

  beforeAll(function () {
    // This test may take some time. Such timeout fixes jasmine DEFAULT_TIMEOUT_INTERVAL interval error
    jasmine.DEFAULT_TIMEOUT_INTERVAL = protractorConf.timeouts.extendedDefaultTimeoutInterval;

    // Waiting for results may take some time
    browser.manage().timeouts().implicitlyWait(protractorConf.timeouts.extendedImplicitlyWait);
  });

  beforeEach(function (done) {
    setTimeout(function () {
      expect(browser.getCurrentUrl()).toContain('/login');
      done();
    }, protractorConf.timeouts.pageResolveTimeout);
  });

  afterEach(function (done) {
    setTimeout(function () {
      analyzePage.main.doAccountAction('logout');
      done();
    }, protractorConf.timeouts.pageResolveTimeout);
  });

  afterAll(function () {
    commonFunctions.logOutByClearingLocalStorage();
  });

  it('Should apply filter to Report', () => { // SAWQA-4146
    loginPage.loginAs('admin');

    // Switch to Card View
    commonFunctions.waitFor.elementToBeClickable(analyzePage.analysisElems.cardView);
    analyzePage.analysisElems.cardView.click();

    // Create Report
    homePage.createAnalysis(metricName, analysisType);

    browser.waitForAngularEnabled(false);
    /*element(by.xpath(`//md-checkbox/div/span[text()='Source OS']/ancestor::*[contains(@e2e, 'MCT_DN_SESSION_SUMMARY')]`)).click();
    element(by.xpath(`//md-checkbox/div/span[text()='Available (MB)']/ancestor::*[contains(@e2e, 'MCT_DN_SESSION_SUMMARY')]`)).click();
    element(by.xpath(`//md-checkbox/div/span[text()='Source Model']/ancestor::*[contains(@e2e, 'MCT_DN_SESSION_SUMMARY')]`)).click();
    browser.waitForAngularEnabled(true);*/

    // Select fields and refresh
    tables.forEach(table => {
      table.fields.forEach(field => {
        browser.executeScript(scrollIntoView, reportDesigner.getReportFieldCheckbox(table.name, field));
        commonFunctions.waitFor.elementToBeClickable(reportDesigner.getReportFieldCheckbox(table.name, field));
        reportDesigner.getReportFieldCheckbox(table.name, field).click();
      });
    });

    /*expect(
      reportDesigner
        .getJoinlabel(join.tableA, join.fieldA, join.tableB, join.fieldB, 'inner')
        .isPresent()
    ).toBe(false);

    const endpointA = reportDesigner.getReportFieldEndPoint(join.tableA, join.fieldA, 'right');
    const endpointB = reportDesigner.getReportFieldEndPoint(join.tableB, join.fieldB, 'left');
    browser.actions().dragAndDrop(endpointA, endpointB).perform();

    expect(
      reportDesigner
        .getJoinlabel(join.tableA, join.fieldA, join.tableB, join.fieldB, 'inner')
        .isPresent()
    ).toBe(true);*/

    commonFunctions.waitFor.elementToBeClickable(reportDesigner.refreshBtn);
    reportDesigner.refreshBtn.click();

    // Should apply filters
    const filters = analyzePage.filtersDialogUpgraded;
    const filterAC = filters.getFilterAutocomplete(0);
    const stringFilterInput = filters.getNumberFilterInput(0);
    const fieldName = tables[0].fields[0];

    commonFunctions.waitFor.elementToBeVisible(reportDesigner.filterBtn);
    commonFunctions.waitFor.elementToBeClickable(reportDesigner.filterBtn);
    reportDesigner.filterBtn.click();

    commonFunctions.waitFor.elementToBeClickable(designModePage.filterWindow.addFilter(tables[0].name));
    designModePage.filterWindow.addFilter(tables[0].name).click();

    filterAC.sendKeys(fieldName, protractor.Key.DOWN, protractor.Key.ENTER);
    stringFilterInput.sendKeys("123");
    commonFunctions.waitFor.elementToBeClickable(filters.applyBtn);
    filters.applyBtn.click();
    browser.sleep(3000);
    // TODO: below code is not working in headless mode something is wrong with chrome. will test again and enable it.
    // commonFunctions.waitFor.elementToBeVisible(element(by.xpath('//div[@class="dx-datagrid" or contains(@class,"non-ideal-state__container ")]')));
    // Verify the applied filters
    const appliedFilter = filters.getAppliedFilter(fieldName);
    commonFunctions.waitFor.elementToBePresent(appliedFilter);
    commonFunctions.waitFor.elementToBeVisible(appliedFilter);
    expect(appliedFilter.isPresent()).toBe(true);

    // Save
    const save = analyzePage.saveDialogUpgraded;
    const designer = analyzePage.designerDialog;
    commonFunctions.waitFor.elementToBeVisible(designer.saveBtn);
    commonFunctions.waitFor.elementToBeClickable(designer.saveBtn);
    designer.saveBtn.click();

    commonFunctions.waitFor.elementToBeVisible(designer.saveDialogUpgraded);
    expect(designer.saveDialog).toBeTruthy();

    save.nameInput.clear().sendKeys(reportName);
    save.descriptionInput.clear().sendKeys(reportDescription);
    commonFunctions.waitFor.elementToBeVisible(save.saveBtn);
    commonFunctions.waitFor.elementToBeClickable(save.saveBtn);
    save.saveBtn.click();

    const createdAnalysis = analyzePage.main.getCardTitle(reportName);

    commonFunctions.waitFor.elementToBePresent(createdAnalysis)
      .then(() => expect(createdAnalysis.isPresent()).toBe(true));

    // Delete
    const main = analyzePage.main;
    const cards = main.getAnalysisCards(reportName);
    main.getAnalysisCards(reportName).count()
      .then(count => {
        main.doAnalysisAction(reportName, 'delete');
        commonFunctions.waitFor.elementToBeClickable(main.confirmDeleteBtn);
        main.confirmDeleteBtn.click();
        commonFunctions.waitFor.cardsCountToUpdate(cards, count);
        //expect(main.getAnalysisCards(reportName).isPresent()).toBe(false);
      });
  });

  var scrollIntoView = function (element) {
    arguments[0].scrollIntoView();
  };
});
