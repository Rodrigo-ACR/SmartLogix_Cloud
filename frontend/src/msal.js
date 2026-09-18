import { PublicClientApplication } from '@azure/msal-browser';

// =====================================================
// Tenant SmartLogix — Admin y Cliente
// =====================================================
const msalConfig = {
  auth: {
    clientId:    '68258a69-110e-4fcc-a4f8-b0eb57891e0a',
    authority:   'https://login.microsoftonline.com/275bee47-23c3-4b55-87a5-37dc048751cb',
    redirectUri: 'https://3.224.21.65',
    postLogoutRedirectUri: 'https://3.224.21.65/login',
  },
  cache: { cacheLocation: 'localStorage', storeAuthStateInCookie: false }
};

export const msalAdmin   = new PublicClientApplication(msalConfig);
export const msalCliente = msalAdmin;
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
  const accounts = msalAdmin.getAllAccounts();
  localStorage.clear();
  sessionStorage.clear();

  if (accounts.length > 0) {
    // logoutRedirect cierra la sesión también en el lado de Microsoft
    // (gracias a la URL de cierre de sesión del canal frontal configurada en Azure)
    await msalAdmin.logoutRedirect({
      account: accounts[0],
      postLogoutRedirectUri: 'https://3.224.21.65/login'
    });
  } else {
    window.location.href = '/login';
  }
}