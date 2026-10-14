import { PublicClientApplication } from '@azure/msal-browser';

const msalConfig = {
  auth: {
    clientId:    '2b5921ae-eb94-4deb-aea5-38abef3d7cec',
    authority:   'https://login.microsoftonline.com/99383baf-0e02-4567-b639-98d2b555a85d',
    redirectUri: 'https://100.51.163.7',
    postLogoutRedirectUri: 'https://100.51.163.7/login',
  },
  cache: { cacheLocation: 'localStorage', storeAuthStateInCookie: false }
};

export const msalAdmin   = new PublicClientApplication(msalConfig);
export const msalCliente = msalAdmin;
export const msalInstance = msalAdmin;

export const loginRequestAdmin = {
  scopes: [
    'api://2b5921ae-eb94-4deb-aea5-38abef3d7cec/read',
    'api://2b5921ae-eb94-4deb-aea5-38abef3d7cec/write'
  ]
};

export const loginRequestCliente = loginRequestAdmin;
export const loginRequest = loginRequestAdmin;

export async function getAccessToken() {
  const accounts = msalAdmin.getAllAccounts();
  if (accounts.length === 0) return null;
  const account = accounts[0];
  msalAdmin.setActiveAccount(account);
  try {
    const result = await msalAdmin.acquireTokenSilent({ ...loginRequestAdmin, account });
    return result.accessToken;
  } catch { return null; }
}

export function isAuthenticated() {
  return msalAdmin.getAllAccounts().length > 0;
}

export function getAccount() {
  return msalAdmin.getAllAccounts()[0] || null;
}

export async function loginMsal() {
  await msalAdmin.loginRedirect(loginRequestAdmin);
}

export async function logoutMsal() {
  localStorage.clear();
  sessionStorage.clear();
  msalAdmin.clearCache();
  const accounts = msalAdmin.getAllAccounts();
  if (accounts.length > 0) {
    const tenantId = '99383baf-0e02-4567-b639-98d2b555a85d';
    const postLogout = encodeURIComponent('https://100.51.163.7/login');
    window.location.href =
      `https://login.microsoftonline.com/${tenantId}/oauth2/v2.0/logout?post_logout_redirect_uri=${postLogout}`;
  } else {
    window.location.href = '/login';
  }
}