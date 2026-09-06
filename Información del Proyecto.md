# 🚚 SmartLogix - Ecosistema de Microservicios
Plataforma logística diseñada para resolver los problemas de sincronización en PYMEs de eCommerce. El sistema fue transicionado a una arquitectura de microservicios para garantizar alta escalabilidad, resiliencia y un despliegue ágil.

---

## 🏗️ Patrones de Diseño y Buenas Prácticas
Para cumplir con los estándares de la industria y resolver los desafíos de comunicación entre componentes, se implementaron los siguientes patrones:

* **Backend For Frontend (BFF):** Actúa como puerta de enlace, centralizando y optimizando las peticiones desde el cliente (Vue.js) hacia los microservicios.
* **Database per Service:** Cada microservicio gestiona su propia base de datos PostgreSQL, garantizando el aislamiento y evitando cuellos de botella.
* **Repository & Factory:** Para un acceso a datos estructurado y una creación de objetos limpia.
* **Circuit Breaker:** Asegura la resiliencia del ecosistema, evitando fallos en cascada si un servicio se encuentra inactivo.

---

## 🔒 Seguridad
Todo el ecosistema opera bajo un modelo *stateless*, utilizando **Tokens JWT** (JSON Web Tokens) para gestionar la autenticación y autorización segura entre el BFF y los microservicios.

## ⚙️ Prerrequisitos de Instalación
Para levantar este proyecto en un entorno local, necesitas tener instalado:
* **Java 17+** (y Maven)
* **PostgreSQL 18** (Corriendo en el puerto 5432)
* **Node.js y NPM** (Para el cliente web)
* **Docker** (Para la posterior contenedorización de los servicios)

---

### 🌐 Asignación de Puertos - Ecosistema SmartLogix

| Componente | Puerto | Descripción |
| :--- | :--- | :--- |
| **BFF (Comunicador)** | `8085` | Puerta de entrada única para el Frontend |
| **MS Inventario** | `8091` | Gestión de stock y productos |
| **MS Pedidos** | `8092` | Procesamiento de órdenes de compra |
| **MS Envíos** | `8093` | Seguimiento y logística de despacho |

---
### 💻 Comandos Útiles (Maven)

| Acción | Comando (Terminal) | Descripción |
| :--- | :--- | :--- |
| **Iniciar Proyecto** | `mvn spring-boot:run` | Levanta el microservicio en el puerto asignado |
| **Limpiar y Compilar** | `mvn clean install` | Reconstruye el proyecto y descarga dependencias |
| **Ejecutar Pruebas** | `mvn test` | Corre las pruebas unitarias del sistema |
---

## 🛠️ Stack Tecnológico
* **Backend:** Java 17+ con Spring Boot 3.x
* **Base de Datos:** PostgreSQL (Database per Service)
* **Frontend:** Vue.js (NPM)
* **Control de Versiones:** Git / GitHub

## 🌿 Estrategia de Ramas (GitFlow)
Para este proyecto se implementó una estrategia de ramificación organizada:
* `main`: Código productivo y estable.
* `develop`: Rama de integración para nuevas funcionalidades.
* `feature/`: Ramas específicas para cada microservicio y componente (BFF, Inventario, Pedidos, Envíos).
