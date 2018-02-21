declare const require: any;

import { Component, Inject, Input, EventEmitter, Output } from '@angular/core';
import * as isUndefined from 'lodash/isUndefined';
import cronstrue from 'cronstrue';

const template = require('./cron-job-schedular.component.html');
require('./cron-job-schedular.component.scss');

@Component({
  selector: 'cron-job-schedular',
  template
})

export class CronJobSchedularComponent {
  constructor() 
  public state: any;
  public model: any;
  @Input() public model: any;
  @Input() public crondetails: any;
  @Output() onCronChanged: EventEmitter<any> = new EventEmitter();

  ngOnInit() {
  	this.dailyTypeDay = {
  	  hour: '',
  	  minute: '',
  	  second: '',
  	  hourType: 'AM'
  	};
  	this.dailyTypeWeek = {
  	  hour: '',
  	  minute: '',
  	  second: '',
  	  hourType: 'AM'
  	};
  	this.weeklybasisDate = {
  	  hour: '',
  	  minute: '',
  	  second: '',
  	  hourType: 'AM'
  	}
  	this.specificDayMonth = {
  	  hour: '',
  	  minute: '',
  	  second: '',
  	  hourType: 'AM' 
  	}
  	this.specificWeekDayMonth = {
  	  hour: '',
  	  minute: '',
  	  second: '',
  	  hourType: 'AM'
  	}
  	this.specificMonthDayYear = {
  	  hour: '',
  	  minute: '',
  	  second: '',
  	  hourType: 'AM'
  	}
  	this.specificMonthWeekYear = {
  	  hour: '',
  	  minute: '',
  	  second: '',
  	  hourType: 'AM'
  	}
  	this.model = {}
  	this.daily = {};
  	this.weekly = {};
  	this.monthly = {};
  	this.yearly = {};
    this.days = this.range(1, 31);
    this.months = this.range(1,12);
    this.weeks = [{
      value:'#1',
      label:'First'
    },{
      value:'#2',
      label:'Second'
    },{
      value:'#3',
      label:'Third'
    },{
      value:'#4',
      label:'Fourth'
    },{
      value:'#5',
      label:'Fifth'
    },{
      value:'L',
      label:'LAST'
    }];
    this.dayStrings = ['MON','TUE','WED','THU','FRI','SAT','SUN'];
    this.monthStrings = [{
      value:'1',
      label:'January'
    },{
      value:'2',
      label:'Febuary'
    },{
      value:'3',
      label:'March'
    },{
      value:'4',
      label:'April'
    },{
      value:'5',
      label:'May'
    },{
      value:'6',
      label:'June'
    },{
      value:'7',
      label:'July'
    },{
      value:'8',
      label:'August'
    },{
      value:'9',
      label:'September'
    },{
      value:'10',
      label:'October'
    },{
      value:'11',
      label:'November'
    },{
      value:'12',
      label:'December'
    }];
    if (this.crondetails.activeTab === '' || isUndefined(this.crondetails.activeTab)) {
      this.scheduleType = 'daily';
    } else {
      this.loadData();
    }
  }

  private range(start: number, end: number): number[] {
    const length = end - start + 1;
    return Array.apply(undefined, Array(length)).map((_, i) => i + start);
  }

  resetData() {
    this.ngOnInit();
  }

  openSchedule(event, scheduleType) {
    this.resetData();
  	this.scheduleType = scheduleType;
  }

  onDateChange() {
    this.regenerateCron();
  }

  hourToCron(hour, hourType) {
    return hourType === 'AM' ? (hour === 12 ? 0 : hour) : (hour === 12 ? 12 : hour + 12);
  }

  getDaySuffix(num) {
    const array = ('' + num).split('').reverse();
    if (array[1] != '1') { // Number is in the teens
      switch (array[0]) {
      case '1': return `${num}st`;
      case '2': return `${num}nd`;
      case '3': return `${num}rd`;
      }
    }
    return `${num}th`;
  }

  regenerateCron() {
    switch (this.scheduleType) {
    case 'daily':
      switch (this.daily.dailyType) {
      case 'everyDay':
        this.CronExpression = `0 ${this.dailyTypeDay.minute} ${this.hourToCron(this.dailyTypeDay.hour, this.dailyTypeDay.hourType)} 1/${this.daily.days} * ? *`;
        break;
      case 'everyWeek':
        this.CronExpression = `0 ${this.dailyTypeWeek.minute} ${this.hourToCron(this.dailyTypeWeek.hour, this.dailyTypeWeek.hourType)} ? * MON-FRI *`;
        break;
      default:
        throw 'Invalid cron daily subtab selection';
      }
      break;
    case 'weeklybasis':
      const days = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN']
          .reduce((acc, day) => this.weekly[day] ? acc.concat([day]) : acc, [])
          .join(',');
      this.CronExpression = `0 ${this.weeklybasisDate.minute} ${this.hourToCron(this.weeklybasisDate.hour, this.weeklybasisDate.hourType)} ? * ${days} *`;
      break;
    case 'monthly':
      switch (this.monthly.monthlyType) {
      case 'specificDay':
        this.CronExpression = `0 ${this.specificDayMonth.minute} ${this.hourToCron(this.specificDayMonth.hour, this.specificDayMonth.hourType)} ${this.monthly.specificDay} 1/${this.monthly.specificMonth} ? *`;
        break;
      case 'specificWeekDay':
        this.CronExpression = `0 ${this.specificWeekDayMonth.minute} ${this.hourToCron(this.specificWeekDayMonth.hour, this.specificWeekDayMonth.hourType)} ? 1/${this.monthly.specificWeekDayMonthWeek} ${this.monthly.specificWeekDayDay}${this.monthly.specificWeekDayMonth} *`;
        break;
      default:
        throw 'Invalid cron monthly subtab selection';
      }
      break;
    case 'yearly':
      switch (this.yearly.yearlyType) {
      case 'specificMonthDay':
        this.CronExpression = `0 ${this.specificMonthDayYear.minute} ${this.hourToCron(this.specificMonthDayYear.hour, this.specificMonthDayYear.hourType)} ${this.yearly.specificMonthDayDay} ${this.yearly.specificMonthDayMonth} ? *`;
        break;
      case 'specificMonthWeek':
        this.CronExpression = `0 ${this.specificMonthWeekYear.minute} ${this.hourToCron(this.specificMonthWeekYear.hour, this.specificMonthWeekYear.hourType)} ? ${this.yearly.specificMonthWeekMonth} ${this.yearly.specificMonthWeekDay}${this.yearly.specificMonthWeekMonthWeek} *`;
        break;
      default:
        throw 'Invalid cron yearly subtab selection';
      }
      break;
    default:
      console.log('wrong selection');
    }
    if (this.isValid(this.CronExpression)) {
      console.log(this.CronExpression);
      this.onCronChange();
    }
  }

  onCronChange() {
    this.onCronChanged.emit(this.CronExpression);
  }

  isValid(expression) {
    const QUARTZ_REGEX = /^\s*($|#|\w+\s*=|(\?|\*|(?:[0-5]?\d)(?:(?:-|\/|\,)(?:[0-5]?\d))?(?:,(?:[0-5]?\d)(?:(?:-|\/|\,)(?:[0-5]?\d))?)*)\s+(\?|\*|(?:[0-5]?\d)(?:(?:-|\/|\,)(?:[0-5]?\d))?(?:,(?:[0-5]?\d)(?:(?:-|\/|\,)(?:[0-5]?\d))?)*)\s+(\?|\*|(?:[01]?\d|2[0-3])(?:(?:-|\/|\,)(?:[01]?\d|2[0-3]))?(?:,(?:[01]?\d|2[0-3])(?:(?:-|\/|\,)(?:[01]?\d|2[0-3]))?)*)\s+(\?|\*|(?:0?[1-9]|[12]\d|3[01])(?:(?:-|\/|\,)(?:0?[1-9]|[12]\d|3[01]))?(?:,(?:0?[1-9]|[12]\d|3[01])(?:(?:-|\/|\,)(?:0?[1-9]|[12]\d|3[01]))?)*)\s+(\?|\*|(?:[1-9]|1[012])(?:(?:-|\/|\,)(?:[1-9]|1[012]))?(?:L|W)?(?:,(?:[1-9]|1[012])(?:(?:-|\/|\,)(?:[1-9]|1[012]))?(?:L|W)?)*|\?|\*|(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)(?:(?:-)(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))?(?:,(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)(?:(?:-)(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))?)*)\s+(\?|\*|(?:[1-7]|MON|TUE|WED|THU|FRI|SAT|SUN)(?:(?:-|\/|\,|#)(?:[1-5]))?(?:L)?(?:,(?:[1-7]|MON|TUE|WED|THU|FRI|SAT|SUN)(?:(?:-|\/|\,|#)(?:[1-5]))?(?:L)?)*|\?|\*|(?:MON|TUE|WED|THU|FRI|SAT|SUN)(?:(?:-)(?:MON|TUE|WED|THU|FRI|SAT|SUN))?(?:,(?:MON|TUE|WED|THU|FRI|SAT|SUN)(?:(?:-)(?:MON|TUE|WED|THU|FRI|SAT|SUN))?)*)(|\s)+(\?|\*|(?:|\d{4})(?:(?:-|\/|\,)(?:|\d{4}))?(?:,(?:|\d{4})(?:(?:-|\/|\,)(?:|\d{4}))?)*))$/;
    const formattedExpression = expression.toUpperCase();
    return !!formattedExpression.match(QUARTZ_REGEX);   
  }

  loadData() {
    console.log("inside load data");
    console.log(this.crondetails);
    console.log(cronstrue.toString(this.crondetails.cronexp));
  }
}

