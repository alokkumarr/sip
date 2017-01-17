import filter from 'lodash/fp/filter';
import flatMap from 'lodash/fp/flatMap';
import pipe from 'lodash/fp/pipe';
import get from 'lodash/fp/get';
import first from 'lodash/first';
import forEach from 'lodash/forEach';

import template from './analyze-report.component.html';
import style from './analyze-report.component.scss';

export const AnalyzeReportComponent = {
  template,
  styles: [style],
  bindings: {
    analysis: '<'
  },
  controller: class AnalyzeReportController {
    constructor($componentHandler, $mdDialog, $scope, $timeout, $log, AnalyzeService) {
      'ngInject';

      this._$componentHandler = $componentHandler;
      this._$mdDialog = $mdDialog;
      this._$scope = $scope;
      this._$timeout = $timeout;
      this._$log = $log;
      this._AnalyzeService = AnalyzeService;

      this.DESIGNER_MODE = 'designer';
      this.QUERY_MODE = 'query';

      this.states = {
        sqlMode: this.DESIGNER_MODE,
        detailsExpanded: false
      };

      this.data = {
        category: null,
        title: 'Untitled Report',
        description: '',
        query: ''
      };

      this.gridData = [];
      this.columns = [];

      this._AnalyzeService.getDataByQuery()
        .then(data => {
          this.gridData = data;
          this.reloadPreviewGrid();
        });
    }

    $onInit() {
      if (this.analysis.name) {
        this.data.title = this.analysis.name;
      }

      if (this.analysis.description) {
        this.data.description = this.analysis.description;
      }

      this.unregister = this._$componentHandler.events.on('$onInstanceAdded', e => {
        if (e.key === 'ard-canvas') {
          this.initCanvas(e.instance);
        }
      });
    }

    $onDestroy() {
      if (this.unregister) {
        this.unregister();
      }
    }

    cancel() {
      this._$mdDialog.cancel();
    }

    toggleDetailsPanel() {
      this.states.detailsExpanded = !this.states.detailsExpanded;
    }

    initCanvas(canvas) {
      this.canvas = canvas;

      this._AnalyzeService.getArtifacts()
        .then(data => {
          this.fillCanvas(data);
          this.reloadPreviewGrid();
        });

      this.canvas._$eventHandler.on('changed', () => {
        this.reloadPreviewGrid();
      });
    }

    fillCanvas(data) {
      const model = this.canvas.model;

      model.clear();

      /* eslint-disable camelcase */
      forEach(data, itemA => {
        const table = model.addTable(itemA.artifact_name);

        table.setMeta(itemA);
        table.setPosition(itemA.artifact_position[0], itemA.artifact_position[1]);

        forEach(itemA.artifact_attributes, itemB => {
          const field = table.addField(itemB.column_name);

          field.setMeta(itemB);
          field.displayName = itemB.display_name;
          field.alias = itemB.alias_name;
          field.type = itemB.type;
          field.checked = itemB.checked;
          field.isHidden = Boolean(itemB.hide);
          field.isJoinEligible = Boolean(itemB.join_eligible);
          field.isFilterEligible = Boolean(itemB.filter_eligible);
        });
      });

      forEach(data, itemA => {
        forEach(itemA.sql_builder.joins, itemB => {
          const tableA = itemB.criteria[0].table_name;
          const tableB = itemB.criteria[1].table_name;

          if (tableA !== tableB) {
            model.addJoin(itemB.type, {
              table: tableA,
              field: itemB.criteria[0].column_name,
              side: itemB.criteria[0].side
            }, {
              table: tableB,
              field: itemB.criteria[1].column_name,
              side: itemB.criteria[1].side
            });
          }
        });

        forEach(itemA.sql_builder.order_by_columns, itemB => {
          model.addSort({
            table: itemA.artifact_name,
            field: itemB.column_name,
            order: itemB.order
          });
        });

        forEach(itemA.sql_builder.group_by_columns, itemB => {
          model.addGroup({
            table: itemA.artifact_name,
            field: itemB
          });
        });

        forEach(itemA.sql_builder.filters, itemB => {
          model.addFilter({
            table: itemA.artifact_name,
            field: itemB.column_name,
            booleanCriteria: itemB.boolean_criteria,
            operator: itemB.operator,
            searchConditions: itemB.search_conditions
          });
        });
      });
      /* eslint-enable camelcase */
    }

    generatePayload() {
      const model = this.canvas.model;
      const tableArtifacts = [];

      /* eslint-disable camelcase */
      forEach(model.tables, table => {
        const tableArtifact = {
          artifact_name: table.name,
          artifact_position: [table.x, table.y],
          artifact_attributes: [],
          sql_builder: {
            group_by_columns: [],
            order_by_columns: [],
            joins: [],
            filters: []
          },
          data: []
        };

        tableArtifacts.push(tableArtifact);

        forEach(table.fields, field => {
          const fieldArtifact = {
            column_name: field.meta.column_name,
            display_name: field.meta.display_name,
            alias_name: field.alias,
            type: field.meta.type,
            hide: field.isHidden,
            join_eligible: field.meta.join_eligible,
            filter_eligible: field.meta.filter_eligible,
            checked: field.checked
          };

          tableArtifact.artifact_attributes.push(fieldArtifact);
        });

        const joins = filter(model.joins, join => {
          return join.leftSide.table === table;
        });

        forEach(joins, join => {
          const joinArtifact = {
            type: join.type,
            criteria: []
          };

          joinArtifact.criteria.push({
            table_name: join.leftSide.table.name,
            column_name: join.leftSide.field.name,
            side: join.leftSide.side
          });

          joinArtifact.criteria.push({
            table_name: join.rightSide.table.name,
            column_name: join.rightSide.field.name,
            side: join.rightSide.side
          });

          tableArtifact.sql_builder.joins.push(joinArtifact);
        });

        const sorts = filter(model.sorts, sort => {
          return sort.table === table;
        });

        forEach(sorts, sort => {
          const sortArtifact = {
            column_name: sort.field.name,
            order: sort.order
          };

          tableArtifact.sql_builder.order_by_columns.push(sortArtifact);
        });

        const groups = filter(model.groups, group => {
          return group.table === table;
        });

        forEach(groups, group => {
          tableArtifact.sql_builder.group_by_columns.push(group.field.name);
        });

        const filters = filter(model.filters, filter => {
          return filter.table === table;
        });

        forEach(filters, filter => {
          const filterArtifact = {
            column_name: filter.field.name,
            boolean_criteria: filter.booleanCriteria,
            operator: filter.operator,
            search_conditions: filter.searchConditions
          };

          tableArtifact.sql_builder.filters.push(filterArtifact);
        });
      });
      /* eslint-enable camelcase */

      return {
        _artifacts: tableArtifacts
      };
    }

    reloadPreviewGrid() {
      this._$timeout(() => {
        this.columns = this.getSelectedColumns(this.canvas.model.tables);

        const grid = first(this._$componentHandler.get('ard-grid-container'));

        if (grid) {
          grid.reload(this.columns, this.gridData);
        }
      });
    }

    getSelectedColumns(tables) {
      return pipe(
        flatMap(get('fields')),
        filter(get('checked'))
      )(tables);
    }

    setSqlMode(mode) {
      this.states.sqlMode = mode;

      if (mode === this.QUERY_MODE) {
        this._AnalyzeService.generateQuery({})
          .then(result => {
            this.data.query = result.query;
          });
      }
    }

    openPreviewModal(ev) {
      const scope = this._$scope.$new();

      scope.model = {
        gridData: this.gridData,
        columns: this.columns
      };

      this._$mdDialog
        .show({
          template: '<analyze-report-preview model="model"></analyze-report-preview>',
          targetEvent: ev,
          fullscreen: true,
          autoWrap: false,
          skipHide: true,
          scope
        });
    }

    openSortModal(ev) {
      const scope = this._$scope.$new();

      scope.model = {
        fields: this.canvas.model.getSelectedFields(),
        sorts: this.canvas.model.sorts
      };

      this._$mdDialog
        .show({
          template: '<analyze-report-sort model="model"></analyze-report-sort>',
          targetEvent: ev,
          fullscreen: true,
          skipHide: true,
          scope
        });
    }

    openDescriptionModal(ev) {
      const scope = this._$scope.$new();

      scope.model = {
        description: this.data.description
      };

      scope.onSave = data => {
        this.data.description = data.description;
      };

      this._$mdDialog.show({
        template: '<analyze-report-description model="model" on-save="onSave($data)"></analyze-report-description>',
        fullscreen: false,
        skipHide: true,
        targetEvent: ev,
        clickOutsideToClose: true,
        scope
      });
    }

    openExportModal() {
    }

    openSaveModal(ev) {
      if (!this.canvas) {
        return;
      }

      const scope = this._$scope.$new();

      scope.model = {
        artifacts: this.generatePayload(),
        category: this.data.category,
        title: this.data.title,
        description: this.data.description
      };

      scope.onSave = data => {
        this.data.category = data.category;
        this.data.title = data.title;
        this.data.description = data.description;

        this._$log.log(data);
      };

      this._$mdDialog
        .show({
          template: '<analyze-report-save model="model" on-save="onSave($data)"></analyze-report-save>',
          targetEvent: ev,
          fullscreen: true,
          skipHide: true,
          scope
        });
    }

    openPublishModal() {
    }
  }
};
