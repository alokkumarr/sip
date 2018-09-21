
import { NgModule } from '@angular/core';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { RouterModule }  from '@angular/router';
import { CommonModuleTs } from '../../common';
import { AdminListViewComponent } from './list-view';
import { AdminMainViewComponent } from './main-view';
import { AdminPageComponent } from './page';
import { AdminService } from './main-view/admin.service';
import { RoleService } from './role/role.service';
import { PrivilegeService } from './privilege/privilege.service';
import { ExportService } from './export/export.service';
import { ImportService } from './import/import.service';
import { routes } from './routes';
import {
  AdminExportViewComponent,
  AdminExportListComponent
} from './export';
import { CategoryService } from './category/category.service';
import {
  UserEditDialogComponent,
  UserService
} from './user';
import {
  CategoryEditDialogComponent,
  CategoryDeleteDialogComponent
} from './category';
import {
  RoleEditDialogComponent
} from './role';
import {
  AdminImportViewComponent,
  AdminImportListComponent,
  AdminImportFileListComponent
} from './import';
import {
  PrivilegeEditDialogComponent,
  PrivilegeEditorComponent,
  PrivilegeRowComponent
} from './privilege';
import {JwtService} from '../../common/services';
import {
  AddTokenInterceptor,
  HandleErrorInterceptor,
  RefreshTokenInterceptor
} from '../../common/interceptor';
import { SidenavMenuService } from '../../common/components/sidenav';
import { dxDataGridService, ToastService, LocalSearchService } from '../../common/services';

import { isAdminGuard, GoToDefaultAdminPageGuard} from './guards';

const COMPONENTS = [
  AdminPageComponent,
  AdminMainViewComponent,
  AdminListViewComponent,
  UserEditDialogComponent,
  RoleEditDialogComponent,
  PrivilegeEditDialogComponent,
  PrivilegeEditorComponent,
  PrivilegeRowComponent,
  AdminExportViewComponent,
  AdminExportListComponent,
  CategoryEditDialogComponent,
  CategoryDeleteDialogComponent,
  AdminImportViewComponent,
  AdminImportListComponent,
  AdminImportFileListComponent
];

const INTERCEPTORS = [
  { provide: HTTP_INTERCEPTORS, useClass: AddTokenInterceptor, multi: true },
  {
    provide: HTTP_INTERCEPTORS,
    useClass: HandleErrorInterceptor,
    multi: true
  },
  {
    provide: HTTP_INTERCEPTORS,
    useClass: RefreshTokenInterceptor,
    multi: true
  }
];

const GUARDS = [isAdminGuard, GoToDefaultAdminPageGuard];

const SERVICES = [
  SidenavMenuService,
  AdminService,
  UserService,
  JwtService,
  dxDataGridService,
  LocalSearchService,
  ToastService,
  RoleService,
  PrivilegeService,
  ExportService,
  ImportService,
  CategoryService
];
@NgModule({
  imports: [
    CommonModuleTs,
    RouterModule.forChild(routes)
  ],
  declarations: COMPONENTS,
  entryComponents: COMPONENTS,
  providers: [
    ...INTERCEPTORS,
    ...SERVICES,
    ...GUARDS
  ],
  exports: [
    AdminPageComponent
  ]
})
export class AdminModule {}
