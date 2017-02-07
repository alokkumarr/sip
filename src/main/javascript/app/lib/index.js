import angular from 'angular';

import 'angular-translate';
import 'angular-translate/dist/angular-translate-loader-partial/angular-translate-loader-partial';
import 'angular-translate/dist/angular-translate-interpolation-messageformat/angular-translate-interpolation-messageformat';

import {CommonModule} from './common';
import {ComponentsModule} from './components';
import {DirectivesModule} from './directives';

export const LibModule = 'LibModule';

angular
  .module(LibModule, [
    'pascalprecht.translate',
    CommonModule,
    ComponentsModule,
    DirectivesModule
  ]);
