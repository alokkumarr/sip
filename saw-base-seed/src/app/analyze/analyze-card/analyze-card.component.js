import template from './analyze-card.component.html';
import style from './analyze-card.component.scss';

export const AnalyzeCardComponent = {
  template,
  styles: [style],
  bindings: {
    type: '<',
    title: '<',
    labels: '<',
    schedule: '<',
    chart: '<',
    report: '<'
  },
  controller: class AnalyzeCardController {
    getLabelsString() {
      return (this.labels || []).join(', ');
    }
  }
};
