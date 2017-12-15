/*
  Created by Alex
 */

const login = require('../javascript/pages/common/login.po.js');
const analyze = require('../javascript/pages/common/analyzePage.po.js');
const analyzePage = require('../javascript/pages/common/analyzePage.po.js');
const homePage = require('../javascript/pages/homePage.po.js');
const executedAnalysis = require('../javascript/pages/common/executedAlaysis.po');
const protractor = require('protractor');
const ec = protractor.ExpectedConditions;
const commonFunctions = require('../javascript/helpers/commonFunctions');
const using = require('jasmine-data-provider');


describe('Privileges tests: privileges.test.js', () => {
  const dataProvider = {
    'All privileges for user': {
      user: 'userOne',
      subCategory: "All DO NOT TOUCH",
      cardOptions: true,
      viewOptions: true,
      create: true,
      edit: true,
      fork: true,
      publish: true,
      execute: true,
      export: true,
      delete: true
    },
    'Create privileges for user': {
      user: 'userOne',
      subCategory: "Create DO NOT TOUCH",
      cardOptions: false,
      viewOptions: false,
      create: true,
      edit: false,
      fork: false,
      publish: false,
      execute: false,
      export: false,
      delete: false
    },
    'Edit privileges for user': {
      user: 'userOne',
      subCategory: "Edit DO NOT TOUCH",
      cardOptions: true,
      viewOptions: false,
      create: false,
      edit: true,
      fork: false,
      publish: false,
      execute: false,
      export: false,
      delete: false
    },
    'Fork privileges for user': {
      user: 'userOne',
      subCategory: "Fork DO NOT TOUCH",
      cardOptions: false,
      viewOptions: false,
      create: false,
      edit: false,
      fork: true,
      publish: false,
      execute: false,
      export: false,
      delete: false
    },
    'Publish privileges for user': {
      user: 'userOne',
      subCategory: "Publish DO NOT TOUCH",
      cardOptions: true,
      viewOptions: false,
      create: false,
      edit: false,
      fork: false,
      publish: true,
      execute: false,
      export: false,
      delete: false
    },
    'Execute privileges for user': {
      user: 'userOne',
      subCategory: "Execute DO NOT TOUCH",
      cardOptions: true,
      viewOptions: true,
      create: false,
      edit: false,
      fork: false,
      publish: false,
      execute: true,
      export: false,
      delete: false
    },
    'Export privileges for user': {
      user: 'userOne',
      subCategory: "Export DO NOT TOUCH",
      cardOptions: false,
      viewOptions: true,
      create: false,
      edit: false,
      fork: false,
      publish: false,
      execute: false,
      export: true,
      delete: false
    },
    'Delete privileges for user': {
      user: 'userOne',
      subCategory: "Delete DO NOT TOUCH",
      cardOptions: true,
      viewOptions: true,
      create: false,
      edit: false,
      fork: false,
      publish: false,
      execute: false,
      export: false,
      delete: true
    },
    'View privileges for user': {
      user: 'userOne',
      subCategory: "View DO NOT TOUCH",
      cardOptions: false,
      viewOptions: false,
      create: false,
      edit: false,
      fork: false,
      publish: false,
      execute: false,
      export: false,
      delete: false
    },
    'All privileges for admin': {
      user: 'admin',
      subCategory: "All DO NOT TOUCH",
      cardOptions: true,
      viewOptions: true,
      create: true,
      edit: true,
      fork: true,
      publish: true,
      execute: true,
      export: true,
      delete: true
    }
    //TODO add case for No Privileges
    //TODO add same validation for admin(create categories with privileges)
  };


  beforeEach(function (done) {
    setTimeout(function () {
      expect(browser.getCurrentUrl()).toContain('/login');
      done();
    }, 1000)
  });

  afterEach(function (done) {
    setTimeout(function () {
      analyzePage.main.doAccountAction('logout');
      done();
    }, 1000)
  });

  afterAll(function () {
    browser.executeScript('window.sessionStorage.clear();');
    browser.executeScript('window.localStorage.clear();');
  });

  using(dataProvider, function (data, description) {
    it('should check ' + description, () => {
        login.loginAs(data.user);
        navigateToDefaultSubCategory();

        // Validate presence of Create Button
        element(analyzePage.analysisElems.addAnalysisBtn.isDisplayed().then(function (isVisible) {
          expect(isVisible).toBe(data.create,
            "Create button expected to be " + data.create + " on Analyze Page, but was " + !data.create);
        }));

        // Go to Card View
        commonFunctions.waitFor.elementToBeClickableAndClick(analyze.analysisElems.cardView);

        // Validate presence of menu on card
        element(analyze.analysisElems.cardMenuButton.isDisplayed().then(function (isVisible) {
          expect(isVisible).toBe(data.cardOptions,
            "Options on card expected to be " + data.cardOptions + " on Analyze Page, but were " + !data.cardOptions);
        }));

        // Validate presence on menu items in card menu
        if (data.cardOptions) {
          analyze.main.getAnalysisActionOptions(analyze.main.firstCard).then(options => {
            let analysisOptions = options;
            expect(options.isPresent()).toBe(true, "Options on card expected to be present on Analyze Page, but weren't");

            //should check privileges on card
            expect(isOptionPresent(analysisOptions, "edit")).toBe(data.edit,
              "Edit button expected to be " + data.edit + " on Analyze Page, but was " + !data.edit);
            expect(analyze.main.getForkBtn(analyze.main.firstCard).isDisplayed()).toBe(data.fork,
              "Fork button expected to be " + data.fork + " on Analyze Page, but was " + !data.fork);
            expect(isOptionPresent(analysisOptions, 'publish')).toBe(data.publish,
              "Publish button expected to be " + data.publish + " on Analyze Page, but was " + !data.publish);
            expect(isOptionPresent(analysisOptions, 'execute')).toBe(data.execute,
              "Execute button expected to be " + data.execute + " on Analyze Page, but was " + !data.execute);
            expect(isOptionPresent(analysisOptions, 'delete')).toBe(data.delete,
              "Delete button expected to be " + data.delete + " on Analyze Page, but was " + !data.delete);
          });

          // Navigate back, close the opened actions menu
          //browser.sleep(1000);
          element(by.css('md-backdrop')).click();
          expect(element(by.css('md-backdrop')).isPresent()).toBe(false);
        }

        // Go to executed analysis page
        analyze.main.firstCardTitle.click();
        const condition = ec.urlContains('/executed');
        browser
          .wait(() => condition, 1000)
          .then(() => expect(browser.getCurrentUrl()).toContain('/executed'));

        // Validate buttons in view mode of analysis
        expect(executedAnalysis.editBtn.isDisplayed()).toBe(data.edit,
          "Edit privilege expected to be " + data.edit + " in view mode, but was " + !data.edit);
        expect(executedAnalysis.forkBtn.isDisplayed()).toBe(data.fork,
          "Fork button expected to be " + data.fork + " in view mode, but was " + !data.fork);
        expect(executedAnalysis.publishBtn.isDisplayed()).toBe(data.publish,
          "Publish button expected to be " + data.publish + " in view mode, but was" + !data.publish);

        // Validate menu in analysis
        element(executedAnalysis.actionsMenuBtn.isDisplayed().then(function (isVisible) {
          expect(isVisible).toBe(data.viewOptions,
            "Options menu button expected to be " + data.viewOptions + " in view mode, but were " + !data.viewOptions);
        }));

        // Validate menu items under menu button
        if (data.viewOptions === true) {

          executedAnalysis.actionsMenuBtn.click();

          element(executedAnalysis.executeMenuOption.isPresent().then(function (isVisible) {
            expect(isVisible).toBe(data.execute,
              "Execute button expected to be " + data.execute + " in view mode, but was" + !data.execute);
          }));

          element(executedAnalysis.exportMenuOption.isPresent().then(function (isVisible) {
            expect(isVisible).toBe(data.export,
              "Export button expected to be " + data.export + " in view mode, but was" + !data.export);
          }));

          element(executedAnalysis.deleteMenuOption.isPresent().then(function (isVisible) {
            expect(isVisible).toBe(data.delete,
              "Delete button expected to be " + data.delete + " in view mode, but was" + !data.delete);
          }));
        }
      }
    );

    function isOptionPresent(options, optionName) {
      const option = analyze.main.getAnalysisOption(options, optionName);
      return option.isPresent();
    }

    // Navigates to specific category where analysis view should happen
    const navigateToDefaultSubCategory = () => {
      const subCategory = homePage.subCategory(data.subCategory);
      commonFunctions.waitFor.elementToBeClickableAndClick(subCategory);
    };
  });
});
