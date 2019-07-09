var appRoot = require('app-root-path');

const TEST_BASE_DIR = appRoot + '/e2e/v2/tests/';

const SMOKE = [
  TEST_BASE_DIR + 'login-logout/login.test.js',
  TEST_BASE_DIR + 'login-logout/logout.test.js'
];

const SANITY = [...SMOKE, TEST_BASE_DIR + 'ForgotPassword/preResetPwd.test.js'];
// ALL the test with minimal data set so that all the functionality is touched
const CRITICAL = [
  ...SANITY,
  TEST_BASE_DIR + 'analyze/analyze.test.js',
  TEST_BASE_DIR + 'bis/activateAndDeActivateRoute.test.js',
  TEST_BASE_DIR + 'bis/activateDeActivateChannel.test.js',
  TEST_BASE_DIR + 'bis/createAndDeleteChannel.test.js',
  TEST_BASE_DIR + 'bis/createAndDeleteRoute.test.js',
  TEST_BASE_DIR + 'bis/scheduleRoute.test.js',
  TEST_BASE_DIR + 'bis/updateAndDeleteChannel.test.js',
  TEST_BASE_DIR + 'change-password/changepassword.test.js',
  TEST_BASE_DIR + 'charts/createAndDelete.test.js',
  TEST_BASE_DIR + 'charts/editAndDelete.test.js',
  TEST_BASE_DIR + 'charts/forkEditAndDelete.test.js',
  TEST_BASE_DIR + 'charts/preview.test.js',
  TEST_BASE_DIR + 'create-delete-dashboards/actualVsTargetKPI.test.js',
  TEST_BASE_DIR + 'create-delete-dashboards/charts.test.js',
  TEST_BASE_DIR + 'create-delete-dashboards/chartsGlobalFilter.test.js',
  TEST_BASE_DIR + 'create-delete-dashboards/esReport.test.js',
  TEST_BASE_DIR + 'create-delete-dashboards/esReportGlobalFilter.test.js',
  TEST_BASE_DIR + 'create-delete-dashboards/pivot.test.js',
  TEST_BASE_DIR + 'create-delete-dashboards/pivotWithGlobalFilter.test',
  TEST_BASE_DIR + 'create-delete-dashboards/snapshotKPI.test.js',
  TEST_BASE_DIR + 'ForgotPassword/preResetPwd.test.js',
  TEST_BASE_DIR + 'pivots/pivotFilters.test.js',
  TEST_BASE_DIR + 'privilege/privilege.test.js',
  TEST_BASE_DIR + 'prompt-filter/chartPromptFilters.test.js',
  TEST_BASE_DIR + 'prompt-filter/esReportPromptFilters.test.js',
  TEST_BASE_DIR + 'prompt-filter/pivotPrompt.test.js',
  TEST_BASE_DIR + 'prompt-filter/reportPromptFilters.test.js',
  TEST_BASE_DIR + 'reports/createAndDeleteReport.test.js'
];
// All tests which were executed in critical with larger data set
const REGRESSION = [
  ...CRITICAL,
  TEST_BASE_DIR + 'change-password/changepassword.test.js'
];
// Used for local development and testing some implementations
const DEVELOPMENT = [
  //TEST_BASE_DIR + 'dummy/dummyDevelopmentTests1.js',
  //TEST_BASE_DIR + 'dummy/dummyDevelopmentTests2.js'
  TEST_BASE_DIR + 'charts/topNBottomNForCharts.test.js'
];

module.exports = {
  SMOKE,
  SANITY,
  CRITICAL,
  REGRESSION,
  DEVELOPMENT
};
