import { JoinCriterion } from './join-criterion.model';

export interface Join {
  criteria: JoinCriterion[];
  type:     string;
}
