import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { msalInstance, loginRequest } from './msal.js';

import './assets/styles/main.css';
import './assets/styles/navbar.css';
import './assets/styles/login.css';

async function initApp() {
  try {
    await msalInstance.initialize();
    const redirectResult = await msalInstance.handleRedirectPromise();

    // Si viene del redirect de Azure AD → guardar token
    if (redirectResult?.accessToken) {
      localStorage.setItem('azure_token', redirectResult.accessToken);
      const account = redirectResult.account;
      if (account) {
        localStorage.setItem('nombre', account.name || account.username);
        localStorage.setItem('rol', 'ADMIN');
        localStorage.setItem('id', account.localAccountId);
      }
    }

    // Si ya hay cuentas activas → refrescar token silenciosamente
    const accounts = msalInstance.getAllAccounts();
    if (accounts.length > 0 && !redirectResult) {
      msalInstance.setActiveAccount(accounts[0]);
      try {
        const result = await msalInstance.acquireTokenSilent({
          ...loginRequest,
          account: accounts[0]
        });
        localStorage.setItem('azure_token', result.accessToken);
        localStorage.setItem('nombre', accounts[0].name || accounts[0].username);
        localStorage.setItem('rol', 'ADMIN');
        localStorage.setItem('id', accounts[0].localAccountId);
      } catch { /* silent fail */ }
    }
  } catch (e) {
    console.warn('MSAL init:', e);
  }

  createApp(App).use(router).mount('#app');
}

initApp();
