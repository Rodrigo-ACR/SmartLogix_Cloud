import { PublicClientApplication } from '@azure/msal-browser';

// =====================================================
// Microsoft Entra ID — Tenant Interno 01
// =====================================================
const msalConfig = {
  auth: {
    clientId:    '6bb7a14b-f648-4e85-945c-6a8c946e856b',
    authority:   'https://login.microsoftonline.com/57bd7ed4-95cb-4813-b649-72cfc68924db',
    redirectUri: 'http://localhost:5173',
  },
  cache: { cacheLocation: 'localStorage' }
};

export const msalInstance = new PublicClientApplication(msalConfig);

export const loginRequest = {
  scopes: [
    'api://6bb7a14b-f648-4e85-945c-6a8c946e856b/read',
    'api://6bb7a14b-f648-4e85-945c-6a8c946e856b/write'
  ]
};

// Obtiene el Access Token via acquireTokenSilent
export async function getAccessToken() {
  const accounts = msalInstance.getAllAccounts();
  if (accounts.length === 0) return null;
  const account = accounts[0];
  msalInstance.setActiveAccount(account);
  try {
    const result = await msalInstance.acquireTokenSilent({ ...loginRequest, account });
    return result.accessToken;
  } catch {
    return null;
  }
}

export function isAuthenticated() {
  return msalInstance.getAllAccounts().length > 0;
}

export function getAccount() {
  return msalInstance.getAllAccounts()[0] || null;
}

export async function loginMsal() {
  await msalInstance.loginRedirect(loginRequest);
}

export async function logoutMsal() {
  await msalInstance.logoutRedirect();
}
