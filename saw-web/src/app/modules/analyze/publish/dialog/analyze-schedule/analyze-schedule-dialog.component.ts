import { Component, OnInit, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { ENTER, COMMA } from '@angular/cdk/keycodes';
import * as map from 'lodash/map';
import * as forEach from 'lodash/forEach';
import * as find from 'lodash/find';
import * as isEmpty from 'lodash/isEmpty';
import * as first from 'lodash/first';
import * as fpMap from 'lodash/fp/map';
import * as fpPipe from 'lodash/fp/pipe';
import * as moment from 'moment';
import * as get from 'lodash/get';

import { AnalyzeService } from '../../../services/analyze.service';
import { JwtService } from '../../../../../common/services';
import { Analysis } from '../../../types';
import { PRIVILEGES } from '../../../consts';

const SEMICOLON = 186;

@Component({
  selector: 'analyze-schedule-dialog',
  templateUrl: './analyze-schedule-dialog.component.html',
  styleUrls: ['./analyze-schedule-dialog.component.scss']
})
export class AnalyzeScheduleDialogComponent implements OnInit {
  categories: any[] = [];
  dateFormat = 'mm/dd/yyyy';
  hasSchedule = false;
  cronValidateField = false;
  regexOfEmail = /^[_a-z0-9]+(\.[_a-z0-9]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,4})$/;
  separatorKeys = [ENTER, COMMA, SEMICOLON];
  emails: string[] = [];
  repeatIntervals = ['DAYS', 'WEEKS'];
  repeatInterval = this.repeatIntervals[0];
  repeatOrdinals = [1, 2, 3, 4, 5, 6, 7];
  repeatOrdinal = this.repeatOrdinals[0];
  daysOfWeek = [
    'SUNDAY',
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY'
  ];
  repeatOnDaysOfWeek = map(this.daysOfWeek, dayString => ({
    keyword: dayString,
    checked: false
  }));
  crondetails: any = {};
  endCriteria = {
    never: {
      keyword: 'NEVER'
    },
    after: {
      keyword: 'AFTER',
      occurenceCount: 1
    },
    on: {
      keyword: 'ON',
      endDate: null
    }
  };
  endCriterion = this.endCriteria.never.keyword;
  loadCronLayout = false;
  ftp = [];
  locations = [];
  scheduleState: 'new' | 'exist' | 'delete';
  token: any;
  errorFlagMsg = false;
  loadCron = false;
  emailValidateFlag = false;
  isReport: boolean;
  fileType: any;

  constructor(
    public _dialogRef: MatDialogRef<AnalyzeScheduleDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      analysis: Analysis;
    },
    public _analyzeService: AnalyzeService,
    public _jwt: JwtService
  ) {}

  ngOnInit() {
    this.scheduleState = 'new';
    this.fileType = 'csv';
    this.token = this._jwt.getTokenObj();
    this._analyzeService.getCategories(PRIVILEGES.PUBLISH).then(response => {
      this.categories = response;
      this.setDefaultCategory();
      this.fetchCronDetails();
    });

    this.isReport = ['report', 'esReport'].includes(
      get(this.data.analysis, 'type'));

    this.fileType = get(this.data.analysis, 'type') === 'pivot' ? 'xlsx' : 'csv';
  }

  trackByIndex(index) {
    return index;
  }

  onCategorySelected(value) {
    this.data.analysis.categoryId = value;
  }

  setDefaultCategory() {
    const analysis = this.data.analysis;
    if (!analysis.categoryId) {
      const defaultCategory = find(
        this.categories,
        category => category.children.length > 0
      );

      if (defaultCategory) {
        analysis.categoryId = first(defaultCategory.children).id;
      }
    }
  }

  fetchCronDetails() {
    const { type, id, categoryId } = this.data.analysis;
    if (type !== 'chart') {
      this.getFTPLocations();
    }
    const requestCron = {
      jobName: id,
      categoryId: categoryId,
      groupName: this.token.ticket.custCode
    };
    this._analyzeService.getCronDetails(requestCron).then(
      (response: any) => {
        this.loadCron = true;
        if (response.statusCode === 200) {
          this.loadCronLayout = true;
          const jobDetails = response.data.jobDetails;

          if (jobDetails) {
            const {
              cronExpression,
              activeTab,
              activeRadio,
              jobScheduleTime,
              endDate,
              analysisID,
              emailList,
              fileType,
              ftp
            } = jobDetails;
            this.crondetails = {
              cronexp: cronExpression,
              startDate: jobScheduleTime,
              activeTab,
              activeRadio,
              endDate
            };
            if (analysisID) {
              this.scheduleState = 'exist';
            }
            this.emails = emailList;
            this.hasSchedule = true;
            if (type !== 'chart') {
              this.ftp = ftp;
            }
            this.emails = emailList;
            this.hasSchedule = true;
            this.fileType = fileType;
          }
        }
      },
      () => {
        this.loadCronLayout = true;
      }
    );
  }

  getFTPLocations() {
    const request = {
      jobGroup: this.token.ticket.custCode
    };
    this._analyzeService.getlistFTP(request).then((response: any) => {
      this.locations = response.ftp;
    });
  }

  onCronChanged(cronexpression) {
    this.crondetails = cronexpression;
  }

  alphanumericUnique() {
    return Math.random()
      .toString(36)
      .substring(7);
  }

  removeSchedule() {
    const analysis = this.data.analysis;
    this.scheduleState = 'delete';
    analysis.schedule = {
      categoryId: analysis.categoryId,
      groupName: this.token.ticket.custCode,
      jobName: analysis.id,
      scheduleState: this.scheduleState
    };
    this.triggerSchedule();
  }

  publish() {
    const analysis = this.data.analysis;
    if (this.validateForm()) {
      let cronJobName = analysis.id;
      const crondetails = this.crondetails;
      if (crondetails.activeTab === 'immediate') {
        this.scheduleState = 'new';
        cronJobName = cronJobName + '-' + this.alphanumericUnique();
        crondetails.cronexp = '';
        crondetails.startDate = moment.utc().format();
      }

      analysis.schedule = {
        scheduleState: this.scheduleState,
        activeRadio: crondetails.activeRadio,
        activeTab: crondetails.activeTab,
        analysisID: analysis.id,
        analysisName: analysis.name,
        cronExpression: crondetails.cronexp,
        description: '',
        emailList: this.emails,
        ftp: this.ftp,
        fileType: this.fileType,
        jobName: cronJobName,
        endDate: crondetails.endDate,
        metricName: analysis.metricName,
        type: analysis.type,
        userFullName: analysis.userFullName,
        jobScheduleTime: crondetails.startDate,
        categoryID: analysis.categoryId,
        jobGroup: this.token.ticket.custCode
      };
      this.triggerSchedule();
    }
  }

  triggerSchedule() {
    this._dialogRef.close(this.data.analysis);
  }

  validateForm() {
    this.errorFlagMsg = false;
    this.emailValidateFlag = false;
    this.cronValidateField = false;
    let validationCheck = true;

    const validateFields = {
      emails: this.validateEmails(this.emails),
      schedule: this.validateSchedule(),
      publish: this.validatePublishSelection()
    };
    fpPipe(
      fpMap(check => {
        if (check === false) {
          validationCheck = false;
        }
      })
    )(validateFields);
    return validationCheck;
  }

  validatePublishSelection() {
    if (
      isEmpty(this.emails) &&
      isEmpty(this.ftp) &&
      this.data.analysis.type !== 'chart'
    ) {
      this.errorFlagMsg = true;
      return false;
    }
    return true;
  }

  validateSchedule() {
    if (
      isEmpty(this.crondetails.cronexp) &&
      this.crondetails.activeTab !== 'immediate'
    ) {
      this.cronValidateField = true;
      return false;
    }
  }

  validateEmails(emails) {
    const emailsList = emails;
    let emailsAreValid = true;
    if (isEmpty(emailsList) && this.data.analysis.type === 'chart') {
      emailsAreValid = false;
      this.emailValidateFlag = true;
    }
    forEach(emailsList, email => {
      const isEmailvalid = this.regexOfEmail.test(email.toLowerCase());
      if (!isEmailvalid) {
        emailsAreValid = false;
        // cancel forEach
        this.emailValidateFlag = true;
      }
    });
    return emailsAreValid;
  }

  validateThisEmail(oneEmail) {
    return this.regexOfEmail.test(oneEmail.toLowerCase());
  }

  addEmail(event) {
    const { input, value } = event;

    // Add our fruit
    const trimmed = (value || '').trim();
    if (trimmed) {
      this.emails.push(trimmed);
    }

    // Reset the input value
    if (input) {
      input.value = '';
    }
  }

  removeEmail(index) {
    if (index >= 0) {
      this.emails.splice(index, 1);
    }
  }

  onLocationSelected(value) {
    this.ftp = value;
  }

  close() {
    this._dialogRef.close();
  }
}
