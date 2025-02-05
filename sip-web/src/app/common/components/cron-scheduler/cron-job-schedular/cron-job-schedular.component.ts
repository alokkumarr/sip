import { Component, Input, EventEmitter, Output, OnInit } from '@angular/core';
import * as clone from 'lodash/clone';
import * as isUndefined from 'lodash/isUndefined';
import cronstrue from 'cronstrue';
import * as forEach from 'lodash/forEach';
import * as isEmpty from 'lodash/isEmpty';
import * as isFunction from 'lodash/isFunction';
import * as range from 'lodash/range';

import * as moment from 'moment-timezone';
import { timezones } from '../../../utils/timezones';

window['moment'] = moment;
import {
  generateHourlyCron,
  generateDailyCron,
  generateWeeklyCron,
  generateMonthlyCron,
  generateYearlyCron,
  isValid,
  convertToLocal
} from '../../../../common/utils/cronFormatter';

const MOMENT_FORMAT = 'YYYY-MM-DD HH:mm:ss';
import { SCHEDULE_TYPES } from '../../../../common/consts';

@Component({
  selector: 'cron-job-schedular',
  templateUrl: './cron-job-schedular.component.html',
  styleUrls: ['./cron-job-schedular.component.scss']
})
export class CronJobSchedularComponent implements OnInit {
  @Input()
  public model: any;
  @Input()
  public crondetails: any;
  @Output()
  onCronChanged: EventEmitter<any> = new EventEmitter();
  public startAt = new Date();
  public timezones = timezones;
  public timezone = moment.tz.guess();

  NumberMapping: any = { '=1': '#st', '=2': '#nd', '=3': '#rd', other: '#th' };
  DayMapping: any = {
    '=TUE': 'TUESDAY',
    '=WED': 'WEDNESDAY',
    '=THU': 'THURSDAY',
    '=SAT': 'SATURDAY',
    other: '#DAY'
  };

  dailyTypeDay;
  schedules;
  dailyTypeWeek;
  weeklybasisDate;
  specificDayMonth;
  specificWeekDayMonth;
  specificMonthDayYear;
  specificMonthWeekYear;
  immediateTime;
  immediate;
  hourly;
  daily;
  weekly;
  monthly;
  yearly;
  selectedMoments;
  hours;
  minutes;
  days;
  months;
  weeks;
  dayStrings;
  monthStrings;
  scheduleType;
  CronExpression;
  activeRadio;
  startDate;
  selectedTab;
  endDate;
  today;

  ngOnInit() {
    this.today = new Date();
    this.today.setMinutes(this.today.getMinutes() - 2);
    this.dailyTypeDay = {
      hour: '',
      minute: '',
      second: '',
      hourType: 'AM'
    };
    this.schedules = SCHEDULE_TYPES;
    this.dailyTypeWeek = clone(this.dailyTypeDay);
    this.weeklybasisDate = clone(this.dailyTypeDay);
    this.specificDayMonth = clone(this.dailyTypeDay);
    this.specificWeekDayMonth = clone(this.dailyTypeDay);
    this.specificMonthDayYear = clone(this.dailyTypeDay);
    this.specificMonthWeekYear = clone(this.dailyTypeDay);
    this.immediateTime = clone(this.dailyTypeDay);
    this.model = {};
    this.immediate = {};
    this.hourly = {};
    this.daily = {};
    this.weekly = {};
    this.monthly = {};
    this.yearly = {};
    this.selectedMoments = [];
    this.selectedMoments.push(
      new Date(
        moment()
          .local()
          .seconds(0)
          .format()
      )
    );
    this.hours = range(0, 13);
    this.minutes = range(0, 60);
    this.days = range(1, 32);
    this.months = range(1, 13);
    this.weeks = [
      {
        value: '#1',
        label: 'first'
      },
      {
        value: '#2',
        label: 'second'
      },
      {
        value: '#3',
        label: 'third'
      },
      {
        value: '#4',
        label: 'fourth'
      },
      {
        value: '#5',
        label: 'fifth'
      },
      {
        value: 'L',
        label: 'last'
      }
    ];
    this.dayStrings = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
    this.monthStrings = [
      {
        value: 1,
        label: 'January'
      },
      {
        value: 2,
        label: 'Febuary'
      },
      {
        value: 3,
        label: 'March'
      },
      {
        value: 4,
        label: 'April'
      },
      {
        value: 5,
        label: 'May'
      },
      {
        value: 6,
        label: 'June'
      },
      {
        value: 7,
        label: 'July'
      },
      {
        value: 8,
        label: 'August'
      },
      {
        value: 9,
        label: 'September'
      },
      {
        value: 10,
        label: 'October'
      },
      {
        value: 11,
        label: 'November'
      },
      {
        value: 12,
        label: 'December'
      }
    ];
    this.scheduleType = 'immediate';
    this.immediate.immediatetype = '';
    if (!isEmpty(this.crondetails)) {
      this.loadData();
    }
  }

  resetData() {
    this.dailyTypeDay = {
      hour: '',
      minute: '',
      second: '',
      hourType: 'AM'
    };
    this.dailyTypeWeek = clone(this.dailyTypeDay);
    this.weeklybasisDate = clone(this.dailyTypeDay);
    this.specificDayMonth = clone(this.dailyTypeDay);
    this.specificWeekDayMonth = clone(this.dailyTypeDay);
    this.specificMonthDayYear = clone(this.dailyTypeDay);
    this.specificMonthWeekYear = clone(this.dailyTypeDay);
    this.immediateTime = clone(this.dailyTypeDay);
    this.model = {};
    this.hourly = {};
    this.immediate = {};
    this.daily = {};
    this.weekly = {};
    this.monthly = {};
    this.yearly = {};
    this.CronExpression = '';
    this.crondetails.cronexp = '';
    this.crondetails.activeTab = '';
  }

  openSchedule(scheduleType) {
    // this.resetData();
    if (scheduleType.tab.textLabel.toLowerCase() === 'weekly') {
      this.scheduleType = 'weeklybasis';
    } else {
      this.scheduleType = scheduleType.tab.textLabel.toLowerCase();
    }
  }

  onDateChange(event) {
    this.regenerateCron(event);
  }

  afterPickerClosed(element) {
    if (element && isFunction(element.blur)) {
      element.blur();
    }
  }

  generateImmediateSchedule(value) {
    this.scheduleType = 'immediate';
    this.immediate.immediatetype = 'currenttime';
    this.regenerateCron('');
  }

  regenerateCron(dateSelects) {
    switch (this.scheduleType) {
      case 'immediate':
        if (this.immediate.immediatetype === 'currenttime') {
          this.activeRadio = this.immediate.immediatetype;
          this.cronChange();
        }
        break;
      case 'hourly':
        // Generating Cron expression for selections made in hourly tab
        this.CronExpression = generateHourlyCron(
          this.hourly.hours,
          this.hourly.minutes
        );
        if (isValid(this.CronExpression)) {
          this.activeRadio = '';
          this.cronChange();
        }
        break;
      case 'daily':
        // Generating Cron expression for selections made in daily tab
        this.CronExpression = generateDailyCron(this.daily, dateSelects);
        if (isValid(this.CronExpression)) {
          this.activeRadio = this.daily.dailyType;
          this.cronChange();
        }
        break;
      case 'weeklybasis':
        // Generating Cron expression for selections made in weekly tab
        const days = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN']
          .reduce(
            (acc, day) => (this.weekly[day] ? acc.concat([day]) : acc),
            []
          )
          .join(',');
        this.CronExpression = generateWeeklyCron(days, dateSelects);
        if (isValid(this.CronExpression)) {
          this.activeRadio = '';
          this.cronChange();
        }
        break;
      case 'monthly':
        // Generating Cron expression for selections made in monthly tab
        this.CronExpression = generateMonthlyCron(this.monthly, dateSelects);
        if (isValid(this.CronExpression)) {
          this.activeRadio = this.monthly.monthlyType;
          this.cronChange();
        }
        break;
      case 'yearly':
        // Generating Cron expression for selections made in yearly tab
        this.CronExpression = generateYearlyCron(this.yearly, dateSelects);
        if (isValid(this.CronExpression)) {
          this.activeRadio = this.yearly.yearlyType;
          this.cronChange();
        }
        break;
    }
  }

  /**
   * Parses a date object in specific timezone. For example, if selected time zone is
   * America/Los_Angeles, then 5.30 local will become 5.30 American.
   *
   * @param {*} dateObject
   * @returns
   * @memberof CronJobSchedularComponent
   */
  toSelectedTimezone(timezone: string, dateObject: Date): string | Date {
    if (!timezone || !dateObject) {
      return dateObject;
    }
    return moment
      .tz(
        moment(dateObject).format(MOMENT_FORMAT),
        MOMENT_FORMAT,
        timezone || 'UTC'
      )
      .format();
  }

  /**
   * Coverts a UTC date string to a timezone specific object.
   * So 5.30 in supplied timezone will become 5.30 in local
   *
   * @param {string} timezone
   * @param {string} datetime
   * @memberof CronJobSchedularComponent
   */
  fromSelectedTimezone(timezone: string, datetime: string): Date {
    if (!timezone) {
      return new Date(
        moment(datetime)
          .local()
          .seconds(0)
          .format()
      );
    }

    const datetimestring = moment.tz(datetime, timezone).format(MOMENT_FORMAT);
    return new Date(moment(datetimestring, MOMENT_FORMAT).format());
  }

  cronChange() {
    if (this.scheduleType !== 'immediate') {
      this.startDate = this.selectedMoments[0] || moment.utc().seconds(0);
      this.endDate = this.selectedMoments[1] || '';
    }

    this.crondetails = {
      cronexp: this.CronExpression,
      activeTab: this.scheduleType,
      activeRadio: this.activeRadio,
      startDate: this.toSelectedTimezone(this.timezone, this.startDate),
      endDate: this.toSelectedTimezone(this.timezone, this.endDate),
      timezone: this.timezone
    };
    this.onCronChanged.emit(this.crondetails);
  }

  loadData() {
    this.CronExpression = this.crondetails.cronexp;
    this.onCronChanged.emit(this.crondetails);
    this.scheduleType = this.crondetails.activeTab;
    this.activeRadio = this.crondetails.activeRadio;
    this.timezone = moment.tz.guess();
    this.selectedMoments = [];
    this.selectedMoments.push(
      this.fromSelectedTimezone(this.timezone, this.crondetails.startDate)
    );
    if (
      !isUndefined(this.crondetails.endDate) &&
      this.crondetails.endDate !== null
    ) {
      this.selectedMoments.push(
        this.fromSelectedTimezone(this.timezone, this.crondetails.endDate)
      );
    }
    if (isEmpty(this.crondetails.cronexp)) {
      return;
    }
    let parseCronValue;
    let modelDate;
    if (this.scheduleType === 'hourly') {
      parseCronValue = this.crondetails.cronexp.split(' ');
    } else {
      const localCronExpression = convertToLocal(
        this.crondetails.cronexp,
        this.crondetails.timezone
      );
      parseCronValue = cronstrue.toString(localCronExpression).split(' ');
      const fetchTime = parseCronValue[1].split(':');
      const meridium = parseCronValue[2].split(',');
      modelDate = {
        hour: parseInt(fetchTime[0], 10),
        minute: parseInt(fetchTime[1], 10),
        hourType: meridium[0]
      };
    }

    switch (this.scheduleType) {
      case 'hourly':
        this.selectedTab = 1;
        if (this.crondetails.cronexp.match(/\d+ 0\/\d+ \* 1\/1 \* \? \*/)) {
          this.hourly.hours = 0;
          const extractHour = parseCronValue[1].split('/');
          this.hourly.minutes = isNaN(parseInt(extractHour[1], 10))
            ? 0
            : parseInt(extractHour[1], 10);
        } else {
          // Loading/displying values for Cron expression for Hourly tab selection in UI Templete.
          const extractHour = parseCronValue[2].split('/');
          this.hourly.hours = isNaN(parseInt(extractHour[1], 10))
            ? 1
            : parseInt(extractHour[1], 10);
          this.hourly.minutes = isNaN(parseInt(parseCronValue[1], 10))
            ? 0
            : parseInt(parseCronValue[1], 10);
        }
        break;
      case 'daily':
        this.selectedTab = 2;
        // Loading/displying values for Cron expression for daily tab selection in UI Templete.
        this.daily.dailyType = this.crondetails.activeRadio;
        if (this.daily.dailyType === 'everyDay') {
          // First Radio Button: Under daily tab loading data when first radio button is selected.
          this.dailyTypeDay = clone(modelDate); // Loading time values for daily tab under first radio button
          if (isUndefined(parseCronValue[4])) {
            parseCronValue[4] = '1';
          }
          this.daily.days = parseInt(parseCronValue[4], 10);
        } else {
          // Second Raio Button: Under daily tab loading data when second radio button is selected.
          this.dailyTypeWeek = clone(modelDate); // Loading time values for daily tab under Second radio button
        }
        break;
      case 'weeklybasis':
        this.selectedTab = 3;
        // Loading/displying values for Cron expression for daily tab selection in UI Templete.
        const getWeekDays = this.crondetails.cronexp.split(' ');
        forEach(getWeekDays[5].split(','), day => {
          this.weekly[day] = true;
        });

        this.weeklybasisDate = clone(modelDate); // Loading time values for weekly tab
        break;
      case 'monthly':
        this.selectedTab = 4;
        // Loading/displying values for Cron expression for monthly tab selection in UI Templete.
        this.monthly.monthlyType = this.crondetails.activeRadio;
        if (this.monthly.monthlyType === 'monthlyDay') {
          // First Radio Button: Under monthly tab loading data when first radio button is selected.
          this.monthly.specificDay = parseInt(parseCronValue[5], 10);
          if (isUndefined(parseCronValue[10])) {
            parseCronValue[10] = '1';
          }
          this.monthly.specificMonth = parseInt(parseCronValue[10], 10);
          this.specificDayMonth = clone(modelDate); // Loading time values for monthly tab under first radio button
        } else {
          // Second Raio Button: Under monthly tab loading data when second radio button is selected.
          forEach(this.weeks, week => {
            if (week.label === parseCronValue[5]) {
              this.monthly.specificWeekDayMonth = week.value;
            }
          });
          this.monthly.specificWeekDayDay = parseCronValue[6]
            .substr(0, 3)
            .toUpperCase();
          this.monthly.specificWeekDayMonthWeek = parseInt(
            parseCronValue[11],
            10
          );
          if (isNaN(parseInt(parseCronValue[11], 10))) {
            this.monthly.specificWeekDayMonthWeek = 1;
          }
          this.specificWeekDayMonth = clone(modelDate); // Loading time values for monthly tab under second radio button
        }
        break;
      case 'yearly':
        this.selectedTab = 5;
        // Loading/displying values for Cron expression for yearly tab selection in UI Templete.
        this.yearly.yearlyType = this.crondetails.activeRadio;
        if (this.yearly.yearlyType === 'yearlyMonth') {
          // First Radio Button: Under yearly tab loading data when first radio button is selected.
          this.specificMonthDayYear = clone(modelDate); // Loading time values for yearly tab under first radio button
          this.yearly.specificMonthDayMonth =
            new Date(Date.parse(parseCronValue[11] + ' 1, 2018')).getMonth() +
            1;
          this.yearly.specificMonthDayDay = parseInt(parseCronValue[5], 10);
        } else {
          // Second Raio Button: Under yearly tab loading data when second radio button is selected.
          this.specificMonthWeekYear = clone(modelDate); // Loading time values for yearly tab under second radio button
          forEach(this.weeks, week => {
            if (week.label === parseCronValue[5]) {
              this.yearly.specificMonthWeekMonthWeek = week.value;
            }
          });
          this.yearly.specificMonthWeekDay = parseCronValue[6]
            .substr(0, 3)
            .toUpperCase();
          this.yearly.specificMonthWeekMonth =
            new Date(Date.parse(parseCronValue[12] + ' 1, 2018')).getMonth() +
            1;
        }
        break;
    }
  }
}
