module.exports = {

  sidenavElements: {
    menuBtn: element(by.css('.main-content sidenav-btn .e2e-sidenav-btn')),
    myAnalyses: element(by.xpath('//*[@id="left-side-nav"]/div/md-sidenav/md-content/accordion-menu/ul/li[1]/accordion-menu-link/div/div/button/div/span[1]')),
    firstCategory: element(by.xpath('//*[@id="left-side-nav"]/div/md-sidenav/md-content/accordion-menu/ul/li[1]/accordion-menu-link/div/div/ul/li[1]/accordion-menu-link/div/div/a/div/span'))

  }
};
