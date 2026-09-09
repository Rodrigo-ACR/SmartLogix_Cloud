import { PublicClientApplication } from '@azure/msal-browser';

// =====================================================
// Tenant SmartLogix — Admin y Cliente
// =====================================================
const msalConfig = {
  auth: {
    clientId:    '68258a69-110e-4fcc-a4f8-b0eb57891e0a',
    authority:   'https://login.microsoftonline.com/275bee47-23c3-4b55-87a5-37dc048751cb',
    redirectUri: 'http://localhost:5173',
  },
  cache: { cacheLocation: 'localStorage', storeAuthStateInCookie: false }
};

export const msalAdmin   = new PublicClientApplication(msalConfig);
export const msalCliente = msalAdmin; // mismo tenant
export const msalInstance = msalAdmin;

export const loginRequestAdmin = {
  scopes: [
    'api://68258a69-110e-4fcc-a4f8-b0eb57891e0a/read',
    'api://68258a69-110e-4fcc-a4f8-b0eb57891e0a/write'
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
  const accounts = msalAdmin.getAllAccounts();
  if (accounts.length > 0) {
    try {
      // logoutPopup cierra sesión Microsoft sin necesitar HTTPS
      await msalAdmin.logoutPopup({
        account: accounts[0],
        mainWindowRedirectUri: '/login'
      });
    } catch {
      // Si falla el popup, limpiar cuentas manualmente
      msalAdmin.clearCache();
    }
  }
  window.location.href = '/login';
}