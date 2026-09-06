/**
 * E2E-01 — Login como Administrador
 * Verifica que un usuario con rol ADMIN puede iniciar sesión
 * y es redirigido al dashboard de administración.
 */
describe('E2E-01: Login Administrador', () => {

  beforeEach(() => {
    cy.visit('/login')
  })

  it('muestra el formulario de login', () => {
    cy.get('input[type="email"]').should('be.visible')
    cy.get('input[type="password"]').should('be.visible')
    cy.get('button[type="submit"]').should('contain', 'Ingresar')
  })

  it('login exitoso como admin redirige a /admin', () => {
    cy.get('input[type="email"]').type('admin@smartlogix.cl')
    cy.get('input[type="password"]').type('1234')
    cy.get('button[type="submit"]').click()

    // Verifica redirección al dashboard admin
    cy.url().should('include', '/admin')

    // Verifica que el dashboard está visible
    cy.contains('Dashboard').should('be.visible')
  })

  it('login con credenciales incorrectas muestra error', () => {
    cy.get('input[type="email"]').type('admin@smartlogix.cl')
    cy.get('input[type="password"]').type('wrongpassword')
    cy.get('button[type="submit"]').click()

    // Permanece en login y muestra mensaje de error
    cy.url().should('include', '/login')
    cy.get('.error-msg').should('be.visible')
  })

  it('admin puede navegar al módulo de pedidos', () => {
    cy.get('input[type="email"]').type('admin@smartlogix.cl')
    cy.get('input[type="password"]').type('1234')
    cy.get('button[type="submit"]').click()

    cy.url().should('include', '/admin')

    // Navegar a pedidos
    cy.visit('/admin/pedidos')
    cy.url().should('include', '/admin/pedidos')
    cy.contains('Pedidos').should('be.visible')
  })

  it('admin puede navegar al módulo de envíos', () => {
    cy.get('input[type="email"]').type('admin@smartlogix.cl')
    cy.get('input[type="password"]').type('1234')
    cy.get('button[type="submit"]').click()

    cy.visit('/admin/envios')
    cy.url().should('include', '/admin/envios')
    cy.contains('Envíos').should('be.visible')
  })
})
