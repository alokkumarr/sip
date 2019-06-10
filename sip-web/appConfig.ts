import { environment } from "./src/environments/environment";

let apiUrl;
const endpoints = { security: "security", services: "services" };

/* eslint-disable */
if (environment.production) {
  apiUrl = window.location.origin;
} else {
  // Note: To run against a local Docker environment, use localhost
  // apiUrl = 'http://sip-bmum0001.sncrbda.dev.cloud.synchronoss.net';
  apiUrl = "http://52.91.244.221";
  // apiUrl = 'https://saw01-rd-sip-vaste.sncrcorp.net';

  apiUrl = "http://saw-rd601.ana.dev.vaste.sncrcorp.net";
  // apiUrl = 'http://saw-rd602.ana.dev.vaste.sncrcorp.net';
  // apiUrl = 'https://saw-pac-sip-vaste.sncrcorp.net';
}
/* eslint-enable */
apiUrl += "/sip";

export default {
  login: {
    url: `${apiUrl}/${endpoints.security}`,
    jwtKey: "jwtToken"
  },
  api: {
    url: `${apiUrl}/${endpoints.services}`,
    pluginUrl: `${apiUrl}/web`
  }
};
