import { SqlBuilder, SqlBuilderPivot, SqlBuilderChart } from '../../models';
import {
  DesignerMode,
  AnalysisStarter,
  Analysis,
  AnalysisType,
  Sort,
  Filter,
  FilterModel,
  ArtifactColumnPivot,
  ArtifactColumnChart,
  ArtifactColumn,
  ArtifactColumns,
  IToolbarActionData,
  DesignerToolbarAciton,
  IToolbarActionResult,
  Artifact,
  Format
} from '../../types';

export {
  ArtifactColumnPivot,
  ArtifactColumnChart,
  Analysis,
  DesignerMode,
  AnalysisStarter,
  AnalysisType,
  SqlBuilder,
  SqlBuilderPivot,
  SqlBuilderChart,
  Sort,
  Filter,
  FilterModel,
  Artifact,
  ArtifactColumn,
  ArtifactColumns,
  IToolbarActionData,
  DesignerToolbarAciton,
  IToolbarActionResult,
  Format
};

export type ArtifactColumnFilter = {
  keyword: string;
  types: ('number' | 'date' | 'string')[];
};

export type PivotArea = 'data' | 'row' | 'column';
export type ChartArea = 'x' | 'y' | 'z' | 'g';

export interface IDEsignerSettingGroupAdapter {
  title: string;
  marker: string;
  type: AnalysisType;
  maxAllowed?: number;
  artifactColumns: ArtifactColumns;
  canAcceptArtifactColumn: (
    groupAdapter: IDEsignerSettingGroupAdapter
  ) => (artifactColumn: ArtifactColumn) => boolean;
  // a callback to possibly transform the artifactColumn added to a group
  transform: (artifactColumn: ArtifactColumn) => void;
  // a callback to undo any transformations done to the element
  reverseTransform: (artifactColumn: ArtifactColumn) => void;
  // a callback to change soomething when the indexes change in artifactColumns
  onReorder: (artifactColumns: ArtifactColumns) => void;
}
