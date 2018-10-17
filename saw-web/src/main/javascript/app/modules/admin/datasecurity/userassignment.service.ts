import { Injectable } from '@angular/core';
import { JwtService } from '../../../../login/services/jwt.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import AppConfig from '../../../../../../../appConfig';

const apiUrl = AppConfig.api.url;

@Injectable()
export class UserAssignmentService {

  constructor(
    private _http : HttpClient,
    private _jwtService: JwtService
  ) {}

  //Add a new security group detail.
  addSecurityGroup(data) {
    let requestBody = {};
    let path;
    console.log(data);
    switch (data.mode) {
    case 'create':
      requestBody = {
        description: data.description,
        securityGroupName: data.securityGroupName
      }
      path = 'auth/addSecurityGroups';
      break;
    case 'edit':
      requestBody = {
        description: data.description,
        securityGroupName: data.securityGroupName,
        oldsecurityGroupName: data.groupSelected
      }
      path = 'auth/UpdateSecurityGroups';
      break;
    }
    return this.postRequest(path, requestBody);
  }

  ////edit an exiting security group detail.
  editSecurityGroup(securityGroup) {
    const requestBody = {
      ...securityGroup,
      createdBy: this._jwtService.getUserName(),
      userId: this._jwtService.getUserId()
    }
    return this.postRequest(`auth/addSecurityGroups`, requestBody);
  }

  addAttributetoGroup(attribute, mode) {
    let path;
    switch (mode) {
    case 'create':
      path = 'auth/addSecurityGroupDskAttributeValues';
      break;
    case 'edit':
      path = 'auth/updateAttributeValues';
      break;
    }
    return this.postRequest(path, attribute);
  }

  getSecurityAttributes(request) {
    return this.postRequest(`auth/fetchDskAllAttributeValues`, request)
  }

  getSecurityGroups() {
    return this.getRequest('auth/getSecurityGroups');
  }

  getRequest(path) {
    return this._http.get(`http://54.87.146.107/saw/security/${path}`).toPromise();
  }

  postRequest(path: string, params: Object) {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type':  'application/json'
      })
    };
    return this._http.post(`http://54.87.146.107/saw/security/${path}`, params, httpOptions).toPromise();
    //return this._http.post(`${apiUrl}/${path}`, params, httpOptions).toPromise();
  }
}
