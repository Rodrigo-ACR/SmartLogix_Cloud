// Comando reutilizable para login
Cypress.Commands.add('login', (correo, password) => {
  cy.visit('/login')
  cy.get('input[type="email"]').clear().type(correo)
  cy.get('input[type="password"]').clear().type(password)
  cy.get('button[type="submit"]').click()
})
