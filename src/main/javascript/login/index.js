import angular from 'angular';
import 'angular-ui-router';
import 'angular-material';
import 'angular-material/angular-material.css';
import '../../../../fonts/style.css';

import AppConfig from '../../../../appConfig';

import {routesConfig} from './routes';
import {themeConfig} from './theme';
import {runConfig} from './run';

import {AuthServiceFactory} from './services/auth.service';
import {UserServiceFactory} from './services/user.service';
import {JwtServiceFactory} from './services/jwt.service';

import {LoginComponent} from './components/login/login.component';
import {PasswordChangeComponent} from './components/passwordChange/passwordChange.component';
import {PasswordPreResetComponent} from './components/passwordReset/passwordPreReset.component';
import {PasswordResetComponent} from './components/passwordReset/passwordReset.component';

export const LoginModule = 'login';

angular
  .module(LoginModule, [
    'ui.router',
    'ngMaterial'
  ])
  .config(routesConfig)
  .config(themeConfig)
  .run(runConfig)
  .value('AppConfig', AppConfig)
  .factory('AuthService', AuthServiceFactory)
  .factory('UserService', UserServiceFactory)
  .factory('JwtService', JwtServiceFactory)
  .component('passwordChangeComponent', PasswordChangeComponent)
  .component('passwordPreResetComponent', PasswordPreResetComponent)
  .component('passwordResetComponent', PasswordResetComponent)
  .component('loginComponent', LoginComponent);
