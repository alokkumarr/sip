import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { UIRouter } from '@uirouter/angular';
import { Observable } from 'rxjs/Observable';
import { of } from 'rxjs/observable/of';
import { catchError } from 'rxjs/operators';

import { DATASETS, TREE_DATA, TREE_VIEW_Data, RAW_SAMPLE, parser_preview } from '../sample-data';

import * as fpGet from 'lodash/fp/get';
import * as forEach from 'lodash/forEach';
import * as find from 'lodash/find';
import * as map from 'lodash/map';
import * as isUndefined from 'lodash/isUndefined';

import { JwtService } from '../../../../login/services/jwt.service';
import { MenuService } from '../../../common/services/menu.service';
import APP_CONFIG from '../../../../../../../appConfig';

@Injectable()
export class WorkbenchService {
  private api = fpGet('api.url', APP_CONFIG);
  private apiWB = fpGet('wbAPI.url', APP_CONFIG);

  constructor(private http: HttpClient,
    private jwt: JwtService,
    private router: UIRouter,
    private menu: MenuService) { }

  /** GET datasets from the server */
  getDatasets(projectName: string): Observable<any> {
    let Params = new HttpParams();
    Params = Params.append('prj', projectName);
    return this.http.get(`${this.apiWB}/dl/sets`, { params: Params })
      .pipe(
      catchError(this.handleError('data', DATASETS)));
  }

  /** GET Staging area tree list */
  getTreeData(projectName: string, path: string): Observable<any> {
    let Params = new HttpParams();
    Params = Params.append('prj', projectName);
    if (path !== null) {
      Params = Params.append('cat', path);
    }
    return this.http.get(`${this.apiWB}/dl/raw`, { params: Params })
      .pipe(
      catchError(this.handleError('data', TREE_VIEW_Data)));
  }

  /** GET raw preview from the server */
  getRawPreviewData(projectName: string, path: string): Observable<any> {
    let Params = new HttpParams();
    Params = Params.append('prj', projectName);
    Params = Params.append('cat', path);
    return this.http.get(`${this.apiWB}/dl/rawpreview`, { params: Params })
      .pipe(
      catchError(this.handleError('data', RAW_SAMPLE)));
  }

   /** GET parsed preview from the server */
  getParsedPreviewData(projectName: string, previewConfig): Observable<any> {
    const endpoint = `${this.apiWB}/preview/raw/inspect?prj=${projectName}`;
    return this.http.post(endpoint, previewConfig)
      .pipe(
      catchError(this.handleError('data', parser_preview)));
  }

  /** File mask search */
  filterFiles(mask, temmpFiles) {
    let selFiles = [];
    if (isUndefined(mask)) {
      return;
    }
    let wildcardSearch: any;
    if (this.startsWith(mask, '*')) {
      wildcardSearch = this.endsWith;
    }
    if (this.endsWith(mask, '*')) {
      wildcardSearch = this.startsWith;
    }
    if (!mask.includes('*')) {
      wildcardSearch = this.exactMatch;
    }
    const filemasksearch = mask.replace('*', '');
    for (let fileCounter = 0; fileCounter < temmpFiles.length; fileCounter++) {
      if (wildcardSearch(temmpFiles[fileCounter].name, filemasksearch)) {
        selFiles.push(temmpFiles[fileCounter]);
      }
    }

    return selFiles;
  }

  // string functions for filemask wild card search
  endsWith(str, suffix) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
  }

  startsWith(str, suffix) {
    return str.indexOf(suffix) === 0;
  }

  exactMatch(str, suffix) {
    return str === suffix;
  }

  uploadFile(fileToUpload: File, projectName: string, path: string): Observable<any> {
    const endpoint = `${this.apiWB}/dl/upload/raw?prj=${projectName}&cat=${path}`;
    const formData: FormData = new FormData();
    formData.append('file', fileToUpload);
    return this.http.post(endpoint, formData);
  }

  createFolder(projectName: string, path: string): Observable<any> { 
    const endpoint = `${this.apiWB}/dl/create/raw?prj=${projectName}&cat=${path}`;
    return this.http.post(endpoint, {})
      .pipe(
      catchError(this.handleError('data', {})));
  }


  /**
   * Handle Http operation that failed.
   * Let the app continue.
   * @param operation - name of the operation that failed
   * @param result - optional value to return as the observable result
   */
  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      return of(result as T);
    };
  }
}