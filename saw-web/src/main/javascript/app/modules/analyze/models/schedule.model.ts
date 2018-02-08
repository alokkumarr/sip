import RepeatOnDaysOfWeek from './repeat-on-days-of-week.model';

export default interface Schedule {
  repeatOnDaysOfWeek: RepeatOnDaysOfWeek;
  repeatInterval:     number;
  repeatUnit:         string;
}
