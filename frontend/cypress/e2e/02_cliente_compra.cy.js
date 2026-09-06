/**
 * E2E-02 — Login como Cliente y flujo de compra
 * Verifica que un cliente puede iniciar sesión, ver el catálogo
 * de productos y realizar un pedido.
 */
describe('E2E-02: Login Cliente y flujo de compra', () => {

  beforeEach(() => {
    cy.visit('/login')
  })

  it('login exitoso como cliente redirige a /inicio', () => {
    cy.get('input[type="email"]').type('rodrigoconcharomero+1@gmail.com')
    cy.get('input[type="password"]').type('Rodrigo2481')
    cy.get('button[type="submit"]').click()

    cy.url().should('include', '/inicio')
    cy.contains('Bienvenido').should('be.visible')
  })

  it('cliente puede ver el catálogo de productos', () => {
    cy.get('input[type="email"]').type('rodrigoconcharomero+1@gmail.com')
    cy.get('input[type="password"]').type('Rodrigo2481')
    cy.get('button[type="submit"]').click()

    cy.url().should('include', '/inicio')

    // Espera que carguen los productos
    cy.get('.productos-grid', { timeout: 10000 }).should('be.visible')
    cy.get('.producto-card').should('have.length.greaterThan', 0)
  })

  it('cliente puede agregar un producto al carrito', () => {
    cy.get('input[type="email"]').type('rodrigoconcharomero+1@gmail.com')
    cy.get('input[type="password"]').type('Rodrigo2481')
    cy.get('button[type="submit"]').click()

    cy.url().should('include', '/inicio')

    // Espera que carguen los productos y agrega el primero
    cy.get('.producto-card', { timeout: 10000 }).first().within(() => {
      cy.get('.btn-primary').click()
    })

    // Verifica que el carrito flotante aparece
    cy.get('.carrito-fab').should('be.visible')
    cy.get('.carrito-count').should('contain', '1')
  })

  it('cliente puede ver sus pedidos', () => {
    cy.get('input[type="email"]').type('rodrigoconcharomero+1@gmail.com')
    cy.get('input[type="password"]').type('Rodrigo2481')
    cy.get('button[type="submit"]').click()

    cy.url().should('include', '/inicio')

    // Navegar a mis pedidos
    cy.visit('/mis-pedidos')
    cy.url().should('include', '/mis-pedidos')
    cy.contains('Mis Pedidos').should('be.visible')
  })

  it('usuario no autenticado es redirigido al login', () => {
    // Limpiar localStorage para simular sesión cerrada
    cy.clearLocalStorage()
    cy.visit('/inicio')

    // Debe redirigir a login
    cy.url().should('include', '/login')
  })
})
