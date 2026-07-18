# Plataforma de Gestión de Cursos Online - Desarrollo Cloud Native EFT

Repositorio de nuestra Evaluación Final Transversal. Este proyecto consiste en un sistema distribuido y escalable para administración académica, diseñado bajo una arquitectura orientada a eventos, utilizando un patrón BFF (Backend For Frontend) y procesamiento asíncrono.

---

## Arquitectura del Sistema

El ecosistema está fragmentado en microservicios totalmente desacoplados que se comunican mediante mensajería asíncrona:

*   **API Gateway (AWS):** Punto de entrada único para el cliente, encargado del enrutamiento limpio hacia nuestros servicios.
*   **Producer Service (BFF):** Microservicio síncrono encargado de recibir las peticiones de los clientes, validar la seguridad y publicar eventos en las colas de mensajería.
*   **Consumer Service (Worker):** Microservicio asíncrono que procesa las tareas pesadas en segundo plano (escritura en base de datos, generación de documentos y cargas a almacenamiento cloud).
*   **RabbitMQ:** Broker de mensajería que gestiona el tráfico de eventos de forma resiliente.

---

## Stack Tecnológico

*   **Lenguaje & Framework:** Java 21 / Spring Boot 3
*   **Base de Datos:** Oracle Cloud Autonomous Database (Conexión mediante Wallet MBD)
*   **Cloud Storage:** AWS S3 (Para almacenamiento de materiales de estudio y comprobantes)
*   **Identity Provider (IdaaS):** Azure AD B2C (OAuth2 / JWT / RBAC)
*   **API Management:** AWS API Gateway (HTTP API)
*   **Contenedores:** Docker & Docker Compose
*   **CI/CD:** GitHub Actions (Pipelines automatizados de construcción y despliegue)

---

## Documentación Paso a Paso

Para los lineamientos de la evaluación, puedes acceder a la guía detalla de configuración de cada componente en el siguiente enlace:
