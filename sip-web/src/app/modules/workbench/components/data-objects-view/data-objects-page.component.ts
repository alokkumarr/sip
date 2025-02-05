import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material';
import { BehaviorSubject, timer } from 'rxjs';
import * as get from 'lodash/get';
import * as orderBy from 'lodash/orderBy';
import * as cloneDeep from 'lodash/cloneDeep';
import * as isEmpty from 'lodash/isEmpty';
import * as set from 'lodash/set';
import * as forEach from 'lodash/forEach';
import * as concat from 'lodash/concat';

import { LocalSearchService } from '../../../../common/services/local-search.service';
import { WorkbenchService } from '../../services/workbench.service';
import { ToastService } from '../../../../common/services/toastMessage.service';
import { SearchBoxComponent } from '../../../../common/components/search-box';

import { DATASET_CATEGORIES_TYPE } from '../../consts';
import { DatasetFilters } from '../../models/workbench.interface';

@Component({
  selector: 'data-objects-page',
  templateUrl: './data-objects-page.component.html',
  styleUrls: ['./data-objects-page.component.scss'],
  providers: [DatePipe]
})
export class DataobjectsComponent implements OnInit, OnDestroy {
  public availableSets: Array<any> = [];
  public availableDP: Array<any> = [];
  public viewState = 'card';
  public states = {
    searchTerm: '',
    searchTermValue: ''
  };
  public updater = new BehaviorSubject([]);
  public dpUpdater = new BehaviorSubject([]);
  public dataView = 'sets';
  public contentHeight: number;
  public timer;
  public timerSubscription;
  public poll = false;
  public interval = 20000;
  public dsTypeFilters: DatasetFilters = {
    type: 'string',
    name: 'dstype',
    label: 'Filter By Dataset Type',
    isMultiSelect: false,
    data: cloneDeep(DATASET_CATEGORIES_TYPE)
  };
  public dsTagFilter: DatasetFilters = {
    type: 'string',
    name: 'tags',
    label: 'Filter By Dataset Tags',
    isMultiSelect: true,
    data: []
  };
  public searchFilters = [];
  public filterList = {};

  @ViewChild(SearchBoxComponent, { static: true })
  searchBox: SearchBoxComponent;
  @ViewChild('dsFilterSideNav', { static: true }) dsFilterSideNav;

  constructor(
    public router: Router,
    public dialog: MatDialog,
    public LocalSearch: LocalSearchService,
    public workBench: WorkbenchService,
    public datePipe: DatePipe,
    public _toastMessage: ToastService
  ) {}

  ngOnInit() {
    this.getDatasets();
    this.getListOfAllowableTags();
  }

  ngOnDestroy() {
    if (this.poll) {
      this.stopPolling();
    }
  }

  startPolling() {
    /**
     * Calls list datasets/datapods api every 10 seconds or whatever set interval
     * @memberof DatasetsComponent
     */
    this.timer = timer(0, this.interval);
    this.timerSubscription = this.timer.subscribe(() => {
      this.getDatasets();
    });
    this.poll = true;
  }

  stopPolling() {
    this.timerSubscription && this.timerSubscription.unsubscribe();
    this.poll = false;
  }

  /**
   * Make different api call based on the filters.
   * Added as part of SIP-10023.
   */
  getDatasets(): void {
    const subscribedObj = isEmpty(this.filterList)
      ? this.workBench.getDatasets()
      : this.workBench.getFilteredDatasets(this.filterList);

    subscribedObj.subscribe(data => {
      this.availableSets = orderBy(data, 'system.modifiedTime', 'desc');
      this.updateData(this.availableSets);
    });
  }

  getDatapods(): void {
    this.workBench.getListOfSemantic().subscribe((data: any[]) => {
      this.availableDP = get(data, 'contents[0].ANALYZE');
      this.updateData(this.availableDP);
    });
  }

  updateData(data): void {
    setTimeout(() => {
      this.dataView === 'sets'
        ? this.updater.next(data)
        : this.dpUpdater.next(data);
    });
    setTimeout(() => {
      this.contentHeight = window.innerHeight - 170;
    });
  }

  /**
   * Toggling from Card and list views
   *
   * @memberof DataobjectsComponent
   */
  onViewChange(): void {
    if (this.states.searchTerm !== '') {
      this.applySearchFilter(this.states.searchTerm);
    } else {
      this.dataView === 'sets'
        ? this.updateData(this.availableSets)
        : this.updateData(this.availableDP);
    }
  }

  applySearchFilter(value): void {
    this.states.searchTerm = value;

    const DS_SEARCH_CONFIG = [
      {
        keyword: 'Data Set Name',
        fieldName: 'system',
        accessor: system => system.name
      },
      {
        keyword: 'Added By',
        fieldName: 'system',
        accessor: system => system.createdBy
      },
      {
        keyword: 'Last Updated',
        fieldName: 'system',
        accessor: system =>
          this.datePipe.transform(system.modifiedTime * 1000, 'short')
      },
      {
        keyword: 'Description',
        fieldName: 'system',
        accessor: system => system.description
      }
    ];

    const DP_SEARCH_CONFIG = [
      {
        keyword: 'Datapod Name',
        fieldName: 'metricName'
      },
      {
        keyword: 'Created by',
        fieldName: 'createdBy'
      },
      {
        keyword: 'Last Updated',
        fieldName: 'createdAt',
        accessor: createdAt =>
          this.datePipe.transform(createdAt * 1000, 'short')
      },
      {
        keyword: 'Description',
        fieldName: 'description'
      }
    ];
    let SEARCH_CONFIG = [];
    let DATA = [];
    if (this.dataView === 'sets') {
      SEARCH_CONFIG = DS_SEARCH_CONFIG;
      DATA = this.availableSets;
    } else {
      SEARCH_CONFIG = DP_SEARCH_CONFIG;
      DATA = this.availableDP;
    }

    const searchCriteria: any = this.LocalSearch.parseSearchTerm(
      this.states.searchTerm
    );
    this.states.searchTermValue = searchCriteria.trimmedTerm;

    this.LocalSearch.doSearch(searchCriteria, DATA, SEARCH_CONFIG).then(
      data => {
        this.updateData(data);
      },
      err => {
        this._toastMessage.error(err.message);
      }
    );
  }

  addDataSet(): void {
    this.router.navigate(['workbench', 'dataset', 'add']);
  }

  /**
   * Toggling view from Datasets and Datapods
   *
   * @memberof DataobjectsComponent
   */
  onDataObjectViewChange() {
    this.stopPolling();
    this.states.searchTerm === '';

    // Have to directly interact with search component to clear and close it while switching views
    this.searchBox.onClose();
    this.dataView === 'pods' ? this.getDatapods() : this.getDatasets();
  }

  onResize(event) {
    this.contentHeight = event.target.innerHeight - 165;
  }

  togglePoll() {
    this.poll ? this.stopPolling() : this.startPolling();
  }

  applyOrResetDSFilters(event) {
    this.filterList = event.data;
    this.dsFilterSideNav.close();
    this.getDatasets();
  }

  // Get the list of allowable tags for all projects. Added as part of SIP-8963
  getListOfAllowableTags() {
    this.workBench.getAllProjectsAllowableTagList().subscribe(result => {
      let allowableTags = [];
      forEach(result, res => {
        if (res.allowableTags) {
          allowableTags = concat(allowableTags, res.allowableTags);
        }
      });
      allowableTags.unshift('No Tags');
      set(this.dsTagFilter, 'data', allowableTags);
      this.searchFilters = [this.dsTypeFilters, this.dsTagFilter];
    });
  }
}
