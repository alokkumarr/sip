import angular from 'angular';
import 'angular-ui-router';

import 'angular-material';
import 'angular-material/angular-material.css';

import 'angular-ui-grid';
import 'angular-ui-grid/ui-grid.css';

import 'mottle';

import 'devextreme/ui/data_grid';
import 'devextreme/integration/angular';
import 'devextreme/dist/css/dx.common.css';
import 'devextreme/dist/css/dx.light.css';

import 'fonts/icomoon.css';

import AppConfig from '../../../../appConfig';

import {routesConfig} from './routes';
import {themeConfig} from './theme';
import {runConfig} from './run';

import {LibModule} from './lib';
import {ObserveModule} from './modules/observe';
import {AnalyzeModule} from './modules/analyze';
import {AlertsModule} from './modules/alerts';

import {HeaderComponent, RootComponent, FooterComponent} from './layout';

// import from login module
import {AuthServiceFactory} from '../login/services/auth.service';
import {UserServiceFactory} from '../login/services/user.service';
import {JwtServiceFactory} from '../login/services/jwt.service';

export const AppModule = 'app';

angular
  .module(AppModule, [
    'ui.router',
    'ngMaterial',
    'dx',
    'ui.grid',
    LibModule,
    ObserveModule,
    AnalyzeModule,
    AlertsModule
  ])
  .config(routesConfig)
  .config(themeConfig)
  .run(runConfig)
  .value('AppConfig', AppConfig)
  .factory('AuthService', AuthServiceFactory)
  .factory('UserService', UserServiceFactory)
  .factory('JwtService', JwtServiceFactory)
  .component('root', RootComponent)
  .component('headerComponent', HeaderComponent)
  .component('footerComponent', FooterComponent);
