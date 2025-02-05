const using = require('jasmine-data-provider');
const testDataReader = require('../../testdata/testDataReader.js');
const protractorConf = require('../../conf/protractor.conf');
const logger = require('../../conf/logger')(__filename);
const categories = require('../../helpers/data-generation/categories');
const subCategories = require('../../helpers/data-generation/subCategories');
const commonFunctions = require('../../pages/utils/commonFunctions');
const Constants = require('../../helpers/Constants');
const assert = require('chai').assert;

let AnalysisHelper = require('../../helpers/api/AnalysisHelper');
let APICommonHelpers = require('../../helpers/api/APICommonHelpers');

const LoginPage = require('../../pages/LoginPage');
const AnalyzePage = require('../../pages/AnalyzePage');
const ChartDesignerPage = require('../../pages/ChartDesignerPage');
const ExecutePage = require('../../pages/ExecutePage');
const Header = require('../../pages/components/Header');
const users = require('../../helpers/data-generation/users');
describe('Executing create and delete chart tests from charts/editAndDelete.test.js', () => {
  const categoryName = categories.analyses.name;
  const subCategoryName = subCategories.createSubCategories.createAnalysis.name;

  //updated fields
  const metrics = 'Integer';
  const dimension = 'Date';
  const yAxisName2 = 'Long';
  const groupName = 'String';
  const sizeByName = 'Float';

  let editedAnalysisId;
  let host;
  let token;
  let analyses = [];
  beforeAll(() => {
    logger.info('Starting charts/editAndDelete.test.js.....');
    host = APICommonHelpers.getApiUrl(browser.baseUrl);
    token = APICommonHelpers.generateToken(
      host,
      users.admin.loginId,
      users.anyUser.password
    );
    jasmine.DEFAULT_TIMEOUT_INTERVAL = protractorConf.timeouts.timeoutInterval;
  });

  beforeEach(done => {
    setTimeout(() => {
      done();
    }, protractorConf.timeouts.pageResolveTimeout);
  });

  afterEach(function(done) {
    setTimeout(function() {
      if (editedAnalysisId) {
        analyses.push(editedAnalysisId);
      }
      analyses.forEach(id => {
        logger.warn('deleting analysis with id: ' + id);
        new AnalysisHelper().deleteAnalysis(
          host,
          token,
          protractorConf.config.customerCode,
          id
        );
      });

      commonFunctions.clearLocalStorage();
      done();
    }, protractorConf.timeouts.pageResolveTimeout);
  });

  using(
    testDataReader.testData['EDITELETECHART']['positiveTests']
      ? testDataReader.testData['EDITELETECHART']['positiveTests']
      : {},
    (data, id) => {
      it(`${id}:${data.description}`, () => {
        logger.info(`Executing test case with id: ${id}`);
        const now = new Date().getTime();
        const chartName = `e2e ${now}`;
        const chartDescription = `e2e chart description ${now}`;
        const type = data.chartType.split(':')[1];
        if (!token) {
          logger.error('token cannot be null');
          expect(token).toBeTruthy();
          assert.isNotNull(token, 'token cannot be null');
        }

        //Create new analysis.
        let analysis = new AnalysisHelper().createNewAnalysis(
          host,
          token,
          chartName,
          chartDescription,
          Constants.CHART,
          type
        );
        expect(analysis).toBeTruthy();
        assert.isNotNull(analysis, 'analysis should not be null');
        analyses.push(analysis.analysisId);
        const loginPage = new LoginPage();
        loginPage.loginAs(data.user, /analyze/);

        const header = new Header();
        header.openCategoryMenu();
        header.selectCategory(categoryName);
        header.selectSubCategory(subCategoryName);

        const analyzePage = new AnalyzePage();
        analyzePage.goToView('card');
        analyzePage.clickOnAnalysisLink(chartName);

        const executePage = new ExecutePage();
        executePage.clickOnEditLink();

        const chartDesignerPage = new ChartDesignerPage();
        chartDesignerPage.searchInputPresent();
        chartDesignerPage.clearAttributeSelection();

        if (data.chartType === 'chart:pie') {
          chartDesignerPage.clickOnAttribute(dimension, 'Color By');
          chartDesignerPage.clickOnAttribute(metrics, 'Angle');
        } else {
          chartDesignerPage.clickOnAttribute(dimension, 'Dimension');
          chartDesignerPage.clickOnAttribute(metrics, 'Metrics');
        }
        if (data.chartType === 'chart:bubble') {
          chartDesignerPage.clickOnAttribute(sizeByName, 'Size');
          chartDesignerPage.clickOnAttribute(groupName, 'Color By');
        }
        // If Combo then add one more metric field
        if (data.chartType === 'chart:combo') {
          chartDesignerPage.clickOnAttribute(yAxisName2, 'Metrics');
        } else if (data.chartType !== 'chart:bubble') {
          chartDesignerPage.clickOnAttribute(groupName, 'Group By');
        }
        const updatedName = chartName + ' updated';
        const updatedDescription = chartDescription + 'updated';
        //Save
        chartDesignerPage.clickOnSave();
        chartDesignerPage.enterAnalysisName(updatedName);
        chartDesignerPage.enterAnalysisDescription(updatedDescription);
        chartDesignerPage.clickOnSaveAndCloseDialogButton(/analyze/);

        // Verify analysis displayed in list and card view
        commonFunctions.goToHome();
        header.openCategoryMenu();
        header.selectCategory(categoryName);
        header.selectSubCategory(subCategoryName);
        analyzePage.goToView('list');
        analyzePage.verifyElementPresent(
          analyzePage._analysisTitleLink(updatedName),
          true,
          'report should be present in list/card view'
        );
        analyzePage.goToView('card');
        // Go to detail page and very details
        analyzePage.clickOnAnalysisLink(updatedName);

        executePage.verifyTitle(updatedName);
        executePage.getAnalysisId().then(id => {
          editedAnalysisId = id;
        });
        executePage.clickOnActionLink();
        executePage.clickOnDetails();
        executePage.verifyDescription(updatedDescription);
        executePage.closeActionMenu();
        // Delete the report
        executePage.clickOnActionLink();
        executePage.clickOnDelete();
        executePage.confirmDelete();
        analyzePage.verifyToastMessagePresent('Analysis deleted.');
        analyzePage.verifyAnalysisDeleted();
      }).result.testInfo = {
        testId: id,
        data: data,
        feature: 'EDITELETECHART',
        dataProvider: 'positiveTests'
      };
    }
  );
});
