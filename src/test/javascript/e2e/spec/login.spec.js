const login = require('../pages/common/login.po.js');
const header = require('../pages/components/header.co.js');

describe('Login Tests', () => {

  it('should land on login page', () => {
    expect(browser.getCurrentUrl()).toContain('/login');
  });

  it('should enter valid credentials and attempt to login', () => {
    login.userLogin('admin@frn.com', 'Sawsyncnewuser1!');
  });

  it('should be successfully logged in', () => {
    expect(header.headerElements.symmetraLogo.isPresent()).toBeTruthy();
  });

});
