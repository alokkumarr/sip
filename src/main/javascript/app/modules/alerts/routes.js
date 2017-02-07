export function routesConfig($stateProvider) {
  'ngInject';

  const states = [
    {
      name: 'alerts',
      url: '/alerts'
    }
  ];

  states.forEach(state => {
    $stateProvider.state(state);
  });
}
