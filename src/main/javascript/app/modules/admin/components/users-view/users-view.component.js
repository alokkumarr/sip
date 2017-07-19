import template from './users-view.component.html';
import style from './users-view.component.scss';
import AbstractComponentController from 'app/common/components/abstractComponent';
import {Subject} from 'rxjs/Subject';
import {AdminMenuData, UsersTableHeader} from '../../consts';

export const UsersViewComponent = {
  template,
  styles: [style],
  controller: class UsersViewPageController extends AbstractComponentController {
    constructor($componentHandler, $injector, $compile, $state, $mdDialog, $mdToast, JwtService, UsersManagementService, $window, $rootScope) {
      'ngInject';
      super($injector);
      this._$compile = $compile;
      this.$componentHandler = $componentHandler;
      this.UsersManagementService = UsersManagementService;
      this._$window = $window;
      this._$state = $state;
      this._$mdDialog = $mdDialog;
      this._$mdToast = $mdToast;
      this._JwtService = JwtService;
      this._$rootScope = $rootScope;
      this._usersCache = [];
      this.updater = new Subject();
      this.admin = {};
      this.states = {
        searchTerm: '',
        searchTermValue: ''
      };
      this.resp = this._JwtService.getTokenObj();
      this._$rootScope.showProgress = true;
      this.UsersManagementService.getActiveUsersList(this.resp.ticket.custID).then(admin => {
        this.admin = admin;
        if (this.admin.valid) {
          this._usersCache = this.userList = this.admin.users;
          this._$rootScope.showProgress = false;
        } else {
          this._$rootScope.showProgress = false;
          this._$mdToast.show({
            template: '<md-toast><span>' + this.admin.validityMessage + '</md-toast>',
            position: 'bottom left',
            toastClass: 'toast-primary'
          });
        }
      }).catch(() => {
        this._$rootScope.showProgress = false;
      });
      this.custCode = this.resp.ticket.custCode;
    }
    $onInit() {
      const leftSideNav = this.$componentHandler.get('left-side-nav')[0];
      leftSideNav.update(AdminMenuData, 'ADMIN');
    }
    openNewUserModal() {
      this._$rootScope.showProgress = true;
      this.UsersManagementService.getRoles(this.resp.ticket.custID).then(response => {
        this.response = response;
        if (this.response.valid) {
          this._$rootScope.showProgress = false;
          this.showDialog({
            controller: scope => {
              scope.roles = this.response.roles;
              scope.onSaveAction = users => {
                this._usersCache = this.userList = users;
                this.applySearchFilter();
                this.updater.next({users: this.userList});
              };
            },
            template: '<user-new roles="roles" on-save="onSaveAction(users)"></user-new>',
            fullscreen: false
          });
        } else {
          this._$rootScope.showProgress = false;
          this._$mdToast.show({
            template: '<md-toast><span>' + this.response.validityMessage + '</md-toast>',
            position: 'bottom left',
            toastClass: 'toast-primary'
          });
        }
      }).catch(() => {
        this._$rootScope.showProgress = false;
      });
    }

    openDeleteModal(user) {
      this.user = user;
      const tokenCustId = this.resp.ticket.custID;
      const tokenMasterLoginId = this.resp.ticket.masterLoginId;
      const userObj = {
        userId: user.userId,
        customerId: tokenCustId,
        masterLoginId: tokenMasterLoginId
      };
      const confirm = this._$mdDialog.confirm()
        .title('Are you sure you want to delete this user?')
        .textContent('User ID : ' + this.user.masterLoginId)
        .ok('Delete')
        .cancel('Cancel');

      this._$mdDialog.show(confirm).then(() => {
        this._$rootScope.showProgress = true;
        return this.UsersManagementService.deleteUser(userObj);
      }).then(data => {
        if (data.valid) {
          this._$rootScope.showProgress = false;
          this._usersCache = this.userList = data.users;
          this.applySearchFilter();
          this.updater.next({users: this.userList});
          this._$mdToast.show({
            template: '<md-toast><span> User is successfully inactivated </md-toast>',
            position: 'bottom left',
            toastClass: 'toast-primary'
          });
        } else {
          this._$rootScope.showProgress = false;
          this._$mdToast.show({
            template: '<md-toast><span>' + data.validityMessage + '</md-toast>',
            position: 'bottom left',
            toastClass: 'toast-primary'
          });
        }
      });
    }

    openEditModal(user) {
      const editUser = {};
      angular.merge(editUser, user);
      this._$rootScope.showProgress = true;
      this.UsersManagementService.getRoles(this.resp.ticket.custID).then(response => {
        this.response = response;
        if (this.response.valid) {
          this._$rootScope.showProgress = false;
          this.showDialog({
            controller: scope => {
              scope.roles = this.response.roles;
              scope.user = editUser;
              scope.onUpdateAction = users => {
                this._usersCache = this.userList = users;
                this.applySearchFilter();
                this.updater.next({users: this.userList});
              };
            },
            template: '<user-edit roles="roles" user="user" on-update="onUpdateAction(users)" ></user-edit>',
            fullscreen: false
          });
        } else {
          this._$rootScope.showProgress = false;
          this._$mdToast.show({
            template: '<md-toast><span>' + this.response.validityMessage + '</md-toast>',
            position: 'bottom left',
            toastClass: 'toast-primary'
          });
        }
      }).catch(() => {
        this._$rootScope.showProgress = false;
      });
    }

    onCardAction(actionType, payload) {
      switch (actionType) {
        case 'delete':
          this.openDeleteModal(payload);
          break;
        case 'edit':
          this.openEditModal(payload);
          break;
        default:
      }
    }
    checkColumns(name) {
      this.headerList = [];
      this.headerList = UsersTableHeader;
      for (let i = 0; i < this.headerList.length; i++) {
        if (this.headerList[i].name === name) {
          return true;
        }
      }
      return false;
    }
    applySearchFilter() {
      this.searchText = [];
      this.searchText = this.states.searchTerm.split(/:(.*)/).slice(0, -1);
      switch (this.searchText.length) {
        case 0: {
          this.states.searchTermValue = this.states.searchTerm;
          this.userList = this.UsersManagementService.searchUsers(this._usersCache, this.states.searchTerm, 'All');
          break;
        }
        case 2: {
          if (this.checkColumns(this.searchText[0].trim().toUpperCase())) {
            this.states.searchTermValue = this.searchText[1].trim();
            this.userList = this.UsersManagementService.searchUsers(this._usersCache, this.searchText[1].trim(), this.searchText[0].trim().toUpperCase());
          } else {
            this.states.searchTermValue = '';
            this.userList = this.UsersManagementService.searchUsers(this._usersCache, this.states.searchTermValue, 'All');
            this._$mdToast.show({
              template: '<md-toast><span>"' + this.searchText[0].trim() + '" - Column does not exist.</md-toast>',
              position: 'bottom left',
              toastClass: 'toast-primary'
            });
          }
          break;
        }
        default: {
          break;
        }
      }
      this.updater.next({users: this.userList});
    }
  }
};
