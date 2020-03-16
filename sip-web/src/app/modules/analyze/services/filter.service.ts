import { Injectable } from '@angular/core';
import { Location } from '@angular/common';
import { Router } from '@angular/router';
import * as cloneDeep from 'lodash/cloneDeep';
import * as filter from 'lodash/filter';
import * as flatMap from 'lodash/flatMap';
import * as get from 'lodash/get';
import * as fpMap from 'lodash/fp/map';
import * as fpFilter from 'lodash/fp/filter';
import * as fpPipe from 'lodash/fp/pipe';
import * as fpOmit from 'lodash/fp/omit';
import * as forEach from 'lodash/forEach';
import * as isArray from 'lodash/isArray';

import { AnalyzeDialogService } from './analyze-dialog.service';
import { AnalyzeService } from './analyze.service';
import { AnalysisDSL } from '../types';

@Injectable()
export class FilterService {
  constructor(
    public _dialog: AnalyzeDialogService,
    private router: Router,
    private locationService: Location,
    private _analyzeService: AnalyzeService
  ) {}

  getRuntimeFiltersFrom(filters = []) {
    return filter(f => f.isRuntimeFilter, filters);
  }

  supportsAggregatedFilters(analysis: AnalysisDSL): boolean {
    /* DL reports are not supported for aggregated filters yet */
    if (analysis.type === 'report') {
      return false;
    }
    return [/*'report', */ 'esReport'].includes(analysis.type)
      ? flatMap(
          analysis.sipQuery.artifacts,
          artifact => artifact.fields
        ).some(field => Boolean(field.aggregate))
      : true;
  }

  hasRuntimeAggregatedFilters(analysis: AnalysisDSL): boolean {
    return analysis.sipQuery.filters.some(
      f => f.isAggregationFilter && f.isRuntimeFilter
    );
  }

  openRuntimeModal(analysis: AnalysisDSL, filters = [], navigateTo: string, designerPage) {
    return new Promise(resolve => {
      this._dialog
        .openFilterPromptDialog(
          filters,
          analysis,
          this.supportsAggregatedFilters(analysis) &&
            this.hasRuntimeAggregatedFilters(analysis),
          designerPage
        )
        .afterClosed()
        .subscribe(result => {
          if (!result) {
            return resolve();
          }
          const runtimeFiltersWithValues = result.filters;
          const allFiltersWithEmptyRuntimeFilters = analysis.sipQuery.filters;
          const allFiltersWithValues = this.mergeValuedRuntimeFiltersIntoFilters(
            runtimeFiltersWithValues,
            allFiltersWithEmptyRuntimeFilters
          );
          if (analysis.designerEdit && analysis.type === 'report') {
            analysis.sipQuery.filters = result.filters;
          } else {
            analysis.sipQuery.filters = allFiltersWithValues;
          }

          resolve(analysis);
          this.navigateTo(navigateTo, analysis.category);
        });
    });
  }


  mergeFilters(filters, flattenedFilters, filterObj) {
    forEach(filters, filter => {
      if (filter.filters || isArray(filter)) {
        this.mergeFilters(filter, flattenedFilters, filterObj);
      }
      if (filter.columnName &&
        (filter.uuid === filterObj.uuid
          && filter.columnName === filterObj.columnName)) {
        filter.model = cloneDeep(filterObj.model);
        if (filter.isAggregationFilter) {
          filter.aggregate = cloneDeep(filterObj.aggregate);
        }
      }
    });
    return flattenedFilters;
  }

  mergeValuedRuntimeFiltersIntoFilters(
    runtimeFilters,
    allFiltersWithEmptyRuntimeFilters
  ) {
    forEach(runtimeFilters, filter => {
      this.mergeFilters(allFiltersWithEmptyRuntimeFilters, [], filter);
    });
    return allFiltersWithEmptyRuntimeFilters;
  }

  private navigateTo(navigateTo, analysisCategory) {
    switch (navigateTo) {
      case 'home':
        this.router.navigate(['analyze', analysisCategory]);
        break;
      case 'back':
        this.locationService.back();
        break;
    }
  }

  getCleanedRuntimeFilterValues(analysis) {
    const filters = get(analysis, 'sipQuery.filters');
    const flattenedFilters = this._analyzeService.flattenAndFetchFilters(filters, []);
    const reportType = analysis.type === 'report' && analysis.designerEdit ? 'query' : 'designer';
    if (analysis.type === 'report' && reportType === 'query') {
      return filters;
    }
    // due to a bug, the backend sends runtimeFilters, with the values that were last used
    // so we have to delete model from runtime filters
    return fpPipe(
      fpFilter(f => f.isRuntimeFilter),
      fpMap(fpOmit('model'))
    )(flattenedFilters);
  }

  public getRuntimeFilterValuesIfAvailable(
    analysis,
    navigateBack: string = null,
    designerPage
  ) {
    const clone = cloneDeep(analysis);
    const cleanedRuntimeFilters = this.getCleanedRuntimeFilterValues(clone);

    if (!cleanedRuntimeFilters.length) {
      return Promise.resolve(clone);
    }
    return this.openRuntimeModal(clone, cleanedRuntimeFilters, navigateBack, designerPage);
  }
}
