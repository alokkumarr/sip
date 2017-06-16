import template from './roles-list-view.component.html';
import isUndefined from 'lodash/isUndefined';

export const RolesListViewComponent = {
  template,
  bindings: {
    roles: '<',
    updater: '<',
    customer: '<',
    onAction: '&',
    searchTerm: '<'
  },
  controller: class RolesListViewController {
    constructor(dxDataGridService) {
      'ngInject';
      this._dxDataGridService = dxDataGridService;
      this._gridListInstance = null;
    }

    $onInit() {
      this.gridConfig = this.getGridConfig();
      this.updaterSubscribtion = this.updater.subscribe(update => this.onUpdate(update));
    }

    $onChanges(changedObj) {
      if (!isUndefined(changedObj.roles.currentValue)) {
        this.reloadDataGrid(changedObj.roles.currentValue);
      }
    }

    $onDestroy() {
      this.updaterSubscribtion.unsubscribe();
    }

    onUpdate({roles}) {
      /* eslint-disable */
      roles && this.reloadDataGrid(roles);
      /* eslint-enable */
    }

    reloadDataGrid(roles) {
      this._gridListInstance.option('dataSource', roles);
      this._gridListInstance.refresh();
    }

    onGridInitialized(e) {
      this._gridListInstance = e.component;
    }

    openDeleteModal(role) {
      this.onAction({
        type: 'delete',
        model: role
      });
    }

    openEditModal(role) {
      this.onAction({
        type: 'edit',
        model: role
      });
    }

    getGridConfig() {
      const dataSource = this.roles || [];
      const columns = [{
        caption: 'ROLE NAME',
        dataField: 'roleName',
        allowSorting: true,
        alignment: 'left',
        width: '20%',
        cellTemplate: 'roleNameCellTemplate'
      }, {
        caption: 'ROLE TYPE',
        dataField: 'roleType',
        allowSorting: true,
        alignment: 'left',
        width: '20%',
        cellTemplate: 'roleTypeCellTemplate'
      }, {
        caption: 'ROLE DESCRIPTION',
        dataField: 'roleDesc',
        allowSorting: true,
        alignment: 'left',
        width: '20%',
        cellTemplate: 'roleCodeCellTemplate'
      }, {
        caption: 'DSK',
        dataField: 'dsk',
        allowSorting: true,
        alignment: 'left',
        width: '15%',
        cellTemplate: 'dskCellTemplate'
      }, {
        caption: 'STATUS',
        dataField: 'activeStatusInd',
        allowSorting: true,
        alignment: 'left',
        width: '10%',
        cellTemplate: 'statusCellTemplate'
      }, {
        caption: '',
        width: '5%',
        cellTemplate: 'actionCellTemplate'
      }];

      return this._dxDataGridService.mergeWithDefaultConfig({
        onInitialized: this.onGridInitialized.bind(this),
        columns,
        dataSource,
        paging: {
          pageSize: 10
        },
        pager: {
          showPageSizeSelector: true,
          showInfo: true
        }
      });
    }
  }
};
