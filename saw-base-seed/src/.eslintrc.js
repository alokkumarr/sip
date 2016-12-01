module.exports = {
  extends: [
    'angular'
  ],
  globals: {
    '__DEVELOPMENT__': true
  },
  rules: {
    'angular/no-service-method': 2,
    'no-negated-condition': 0,
    'quote-props': [1, 'as-needed'],
    'padded-blocks': 0
  }
};
