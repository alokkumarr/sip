import omit from 'lodash/omit';
import fpMap from 'lodash/fp/map';
import fpGet from 'lodash/fp/get';

export function AnalyzeService($http, $timeout, $q) {
  'ngInject';

  return {
    getCategories,
    getCategory,
    getMethods,
    getArtifacts,
    getAnalyses,
    deleteAnalysis,
    getLastPublishedAnalysis,
    getPublishedAnalysesByAnalysisId,
    getPublishedAnalysisById,
    executeAnalysis,
    getAnalysisById,
    getDataByQuery,
    generateQuery,
    getNewPivotAnalysis,
    saveReport,
    getSemanticLayerData,
    chartBe2Fe,
    chartFe2Be
  };

  function getAnalyses(category, query) {
    return $http.get('/api/analyze/analyses', {params: {category, query}})
      .then(fpGet('data'))
      .then(fpMap(analysis => {
        if (analysis.type === 'chart') {
          return chartBe2Fe(analysis);
        }
        return analysis;
      }));
  }

  function getPublishedAnalysesByAnalysisId(id) {
    return $http.get(`/api/analyze/publishedAnalyses/${id}`).then(fpGet('data'));
  }

  function getLastPublishedAnalysis(id) {
    return $http.get(`/api/analyze/lastPublishedAnalysis/${id}`).then(fpGet('data'));
  }

  function getPublishedAnalysisById(id) {
    return $http.get(`/api/analyze/publishedAnalysis/${id}`).then(fpGet('data'));
  }

  function executeAnalysis(analysisId) {
    return $q(resolve => {
      $timeout(() => {
        resolve({
          publishedAnalysisId: 3,
          analysisId
        });
      }, 0);
    });
  }

  function getAnalysisById(id) {
    return $http.get(`/api/analyze/byId/${id}`).then(fpGet('data'));
  }

  function deleteAnalysis(id) {
    return $http.delete(`/api/analyze/byId/${id}`).then(fpGet('data'));
  }

  function getCategories() {
    return $http.get('/api/analyze/categories').then(fpGet('data'));
  }

  function getCategory(id) {
    return $http.get(`/api/analyze/category/${id}`).then(fpGet('data'));
  }

  function getMethods() {
    return $http.get('/api/analyze/methods').then(fpGet('data'));
  }

  function getArtifacts() {
    return $http.get('/api/analyze/artifacts').then(fpGet('data'));
  }

  function getDataByQuery() {
    return $http.get('/api/analyze/dataByQuery').then(fpGet('data'));
  }

  function getNewPivotAnalysis() {
    return $http.get('/api/analyze/newPivotAnalysis').then(fpGet('data'));
  }

  function generateQuery(payload) {
    return $http.post('/api/analyze/generateQuery', payload).then(fpGet('data'));
  }

  function saveReport(payload) {
    return $http.post('/api/analyze/saveReport', payload).then(fpGet('data'));
  }

  function getSemanticLayerData() {
    return $http.get('/api/analyze/semanticLayerData').then(fpGet('data'));
  }

  /**
   * Converts chart type analysis from backend
   * to a format usable on front-end
   */
  function chartBe2Fe(source) {
    const result = omit(source, ['_id', 'chart_type', 'plot_variant']);
    result.id = source._id || source.id;
    result.chartType = source.chart_type || source.chartType;
    result.plotVariant = source.plot_variant || source.plotVariant;

    return result;
  }

  function chartFe2Be() {
    // TODO
  }
}
