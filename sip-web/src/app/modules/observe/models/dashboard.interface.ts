export enum TileType {
  Analysis = 'analysis',
  KPI = 'kpi'
}

export interface Tile {
  analysis?: any;
  id: string;
  type: TileType;
  cols: number;
  rows: number;
  x: number;
  y: number;
  options?: Object;
}

export interface Dashboard {
  entityId: string;
  categoryId: string;
  autoRefreshEnabled: boolean;
  refreshIntervalSeconds?: number;
  name: string;
  description: string;
  createdBy?: string;
  createdByName?: string;
  updatedBy?: string;
  updatedByName?: string;
  createdAt?: string;
  updatedAt?: string;
  tiles: Array<Tile>;
  options?: Object;
  filters: Array<Object>;
}
