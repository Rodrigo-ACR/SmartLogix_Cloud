import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { msalAdmin, msalCliente, loginRequestAdmin, loginRequestCliente } from './msal.js';

import './assets/styles/main.css';
import './assets/styles/navbar.css';
import './assets/styles/login.css';

async function procesarRedirect(msalInstance, loginRequest, rol) {
  try {
    await msalInstance.initialize();
    const redirectResult = await msalInstance.handleRedirectPromise();

    if (redirectResult?.accessToken) {
      // Leer rol desde el token JWT (claims)
      const claims = JSON.parse(atob(redirectResult.accessToken.split('.')[1]));
      const rolToken = claims.roles?.[0] || rol;

      localStorage.setItem('azure_token', redirectResult.accessToken);
      localStorage.setItem('tenant', rol === 'ADMIN' ? 'interno01' : 'smartlogix');
      const account = redirectResult.account;
      if (account) {
        localStorage.setItem('nombre', account.name || account.username);
        localStorage.setItem('rol', rolToken);
        localStorage.setItem('id', account.localAccountId);
      }
    }

    // Si ya hay cuentas activas → refrescar token
    const accounts = msalInstance.getAllAccounts();
    if (accounts.length > 0 && !redirectResult) {
      msalInstance.setActiveAccount(accounts[0]);
      try {
        const result = await msalInstance.acquireTokenSilent({
          ...loginRequest,
          account: accounts[0]
        });
        const claims = JSON.parse(atob(result.accessToken.split('.')[1]));
        const rolToken = claims.roles?.[0] || rol;

        localStorage.setItem('azure_token', result.accessToken);
        localStorage.setItem('tenant', rol === 'ADMIN' ? 'interno01' : 'smartlogix');
        localStorage.setItem('nombre', accounts[0].name || accounts[0].username);
        localStorage.setItem('rol', rolToken);
        localStorage.setItem('id', accounts[0].localAccountId);
      } catch { }
    }
  } catch (e) {
    console.warn(`MSAL init (${rol}):`, e);
  }
}

async function initApp() {
  // Inicializar ambas instancias MSAL
  await procesarRedirect(msalAdmin,   loginRequestAdmin,   'ADMIN');
  await procesarRedirect(msalCliente, loginRequestCliente, 'CLIENTE');

  createApp(App).use(router).mount('#app');
}

initApp();
