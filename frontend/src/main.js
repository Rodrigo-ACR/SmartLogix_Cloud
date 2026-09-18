import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { msalAdmin, loginRequestAdmin } from './msal.js';

import './assets/styles/main.css';
import './assets/styles/navbar.css';
import './assets/styles/login.css';

const BASE_URL = '';

async function sincronizarUsuarioAzure(token, nombre, correo, rol) {
  try {
    await fetch(`${BASE_URL}/api/productos/usuarios/sync-azure`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      body: JSON.stringify({ nombre, correo, rol })
    });
  } catch (e) {
    console.warn('No se pudo sincronizar usuario con Azure AD:', e);
  }
}

async function procesarRedirect(msalInstance, loginRequest, rolPorDefecto) {
  try {
    await msalInstance.initialize();
    const redirectResult = await msalInstance.handleRedirectPromise();

    if (redirectResult?.accessToken) {
      const claims = JSON.parse(atob(redirectResult.accessToken.split('.')[1]));
      const rolToken = claims.roles?.[0] || rolPorDefecto;
      const account = redirectResult.account;
      const correo = account?.username || '';
      const nombre = account?.name || correo;

      localStorage.setItem('azure_token', redirectResult.accessToken);
      localStorage.setItem('tenant', 'smartlogix');
      if (account) {
        localStorage.setItem('nombre', nombre);
        localStorage.setItem('rol', rolToken);
        localStorage.setItem('id', account.localAccountId);
        localStorage.setItem('correo', correo);
      }

      await sincronizarUsuarioAzure(redirectResult.accessToken, nombre, correo, rolToken);
    }

    const accounts = msalInstance.getAllAccounts();
    if (accounts.length > 0 && !redirectResult) {
      msalInstance.setActiveAccount(accounts[0]);
      try {
        const result = await msalInstance.acquireTokenSilent({
          ...loginRequest,
          account: accounts[0]
        });
        const claims = JSON.parse(atob(result.accessToken.split('.')[1]));
        const rolToken = claims.roles?.[0] || rolPorDefecto;
        const correo = accounts[0].username || '';
        const nombre = accounts[0].name || correo;

        localStorage.setItem('azure_token', result.accessToken);
        localStorage.setItem('tenant', 'smartlogix');
        localStorage.setItem('nombre', nombre);
        localStorage.setItem('rol', rolToken);
        localStorage.setItem('id', accounts[0].localAccountId);
        localStorage.setItem('correo', correo);

        await sincronizarUsuarioAzure(result.accessToken, nombre, correo, rolToken);
      } catch { }
    }
  } catch (e) {
    console.warn('MSAL init:', e);
  }
}

async function initApp() {
  await procesarRedirect(msalAdmin, loginRequestAdmin, 'CLIENTE');
  createApp(App).use(router).mount('#app');
}

initApp();