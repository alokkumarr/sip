import template from './analyze-page.component.html';
import style from './analyze-page.component.scss';

export const AnalyzePageComponent = {
  template,
  styles: [style],
  controller: class AnalyzePageController {
    /** @ngInject */
    constructor($componentHandler, $http) {
      this.$componentHandler = $componentHandler;
      this.$http = $http;
    }

    $onInit() {
      const leftSideNav = this.$componentHandler.get('left-side-nav')[0];

      this.$http.get('/api/menu/analyze')
        .then(response => {
          leftSideNav.update(response.data);
        });
    }
  }
};
