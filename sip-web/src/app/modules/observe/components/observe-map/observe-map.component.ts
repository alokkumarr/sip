import {
  Component,
  Input,
  Output,
  OnInit,
  OnDestroy,
  AfterViewInit,
  EventEmitter
} from '@angular/core';
import { BehaviorSubject, Subscription } from 'rxjs';
import { AnalyzeService } from '../../../analyze/services/analyze.service';
import * as isUndefined from 'lodash/isUndefined';

import { EXECUTION_MODES } from '../../../analyze/services/analyze.service';

@Component({
  selector: 'observe-map',
  templateUrl: './observe-map.component.html',
  styles: [
    `
      :host {
        height: 100%;
        width: 100%;
        display: block;
      }
    `
  ]
  // styleUrls: ['./observe-map.component.scss']
})
export class ObserveMapComponent implements OnInit, OnDestroy, AfterViewInit {
  @Input() analysis: any;
  @Input() item: any;
  @Input() enableChartDownload: boolean;
  @Input() updater: BehaviorSubject<Array<any>>;
  @Input() ViewMode: boolean;
  @Output() onRefresh = new EventEmitter<any>();

  public chartUpdater = new BehaviorSubject([]);
  public requesterSubscription: Subscription;
  public data: Array<any>;

  constructor(public analyzeService: AnalyzeService) {}

  ngOnInit() {
    this.subscribeToRequester();
  }

  ngAfterViewInit() {
    this.initChart();
  }

  ngOnDestroy() {
    this.requesterSubscription.unsubscribe();
  }

  /* Accept changes from parent component and pass those on to chart.
     Having separate requester and chartUpdater allows transforming
     changes coming from parent before passing them on. */
  subscribeToRequester() {
    this.requesterSubscription = this.updater.subscribe(changes => {
      this.chartUpdater.next(changes);
    });
  }

  initChart() {
    if (isUndefined(this.analysis._executeTile) || this.analysis._executeTile) {
      this.onRefreshData().then(() => {
        this.item && this.onRefresh.emit(this.item);
      });
    }
  }

  onRefreshData() {
    return this.analyzeService
      .getDataBySettings(this.analysis, EXECUTION_MODES.LIVE)
      .then(
        ({ data }) => {
          this.data = data;
          return data;
        },
        err => {
          throw err;
        }
      );
  }
}
