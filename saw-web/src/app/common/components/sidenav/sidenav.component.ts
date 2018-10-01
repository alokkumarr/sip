import { Component, Input, ViewChild, OnInit } from '@angular/core';

import { SidenavMenuService } from './sidenav-menu.service';

const style = require('./sidenav.component.scss');

@Component({
  selector: 'app-sidenav',
  templateUrl: './sidenav.component.html',
  styles: [
    `:host {
      z-index: 999;
      height: 100%;
    }`,
    style
  ]
})

export class SidenavComponent implements OnInit {

  @Input() menu: any;
  @Input() id: any;

  constructor(
    public _sidenav: SidenavMenuService
  ) { }
  @ViewChild('sidenav') public sidenav;

  public unregister: any;
  public _moduleName: string;

  ngOnInit() {
    this._moduleName = '';
    this._sidenav.subscribe(({menu, module}) => this.update(menu, module));
  }

  getMenuHeader() {
    return {
      analyze: 'Analysis',
      observe: 'Dashboards',
      admin: 'Manage',
      workbench: 'WORKBENCH'
    }[this._moduleName.toLowerCase()] || '';
  }

  update(data, moduleName = '') {
    this._moduleName = moduleName;
    this.menu = data;
  }

  toggleNav() {
    this.sidenav.toggle();
  }
}

