export function routesConfig($stateProvider, $urlRouterProvider) {
  'ngInject';

  $urlRouterProvider.otherwise('/');

  const states = [
    {
      name: 'index',
      url: '/',
      onEnter: ($state, $window) => {
        'ngInject';
        // this hack redirecting is only for the moment
        // this should be done on the server
        $window.location = `${$window.location.origin}${$window.location.pathname}login.html`;
      }
    }, {
      name: 'observe',
      url: '/observe',
      component: 'observePage'
    }, {
      name: 'analyze',
      url: '/analyze',
      component: 'analyzePage'
    }, {
      name: 'analyze.view',
      url: '/:id',
      component: 'analyzeView'
    }, {
      name: 'alerts',
      url: '/alerts'
    }
  ];

  states.forEach(state => {
    $stateProvider.state(state);
  });
}
