# Sentinel: Microservices Self-Healing Monitor

**Sentinel** es una solución de monitoreo y resiliencia activa desarrollada con **Java 17** y **Spring Boot 3.4**. El sistema está diseñado para supervisar infraestructuras de **Docker** en tiempo real, detectando interrupciones en los servicios y ejecutando protocolos de recuperación automática (*Self-Healing*) mientras mantiene un registro histórico de auditoría.


## 🌟 Características Destacadas

- **Monitoreo Basado en Eventos**: Supervisión constante del estado de los contenedores mediante el SDK de Docker (TCP 2375).
- **Self-Healing Selectivo**: Recuperación automática de servicios basada en **Docker Labels** (`sentinel.auto-heal=true`), garantizando que solo se reinicien los servicios críticos.
- **Trazabilidad y Persistencia (Fase 2)**: Registro histórico de cada fallo y recuperación exitosa en una base de datos **H2**, visualizable directamente en el dashboard.
- **Dashboard en Tiempo Real**: Interfaz web reactiva que utiliza **WebSockets (STOMP)** para reflejar cambios de estado y logs instantáneamente.
- **Notificaciones Proactivas**: Integración con **Slack API** para alertar sobre incidentes y acciones de recuperación en tiempo real.

## 🛠️ Stack Tecnológico

- **Backend**: Java 17, Spring Boot 3.4.2, Spring Data JPA.
- **Comunicación**: Spring WebSocket + SockJS + STOMP.
- **Base de Datos**: H2 (In-memory) para persistencia de eventos.
- **Infraestructura**: Docker API SDK + Apache HttpClient 5.4.
- **Frontend**: HTML5, Tailwind CSS (Dark Mode), Vanilla JS.
- **Notificaciones**: Slack Webhooks.

## 📋 Requisitos Previos

1. **Java 17** o superior.
2. **Docker Desktop** activo.
3. **Configuración de Docker**: Es necesario habilitar la opción:
   * `Settings` > `General` > `Expose daemon on tcp://localhost:2375 without TLS`.
4. **Maven** para la gestión de dependencias.

## 🚀 Configuración e Instalación

1. **Clonar el repositorio:**
   ```bash
   git clone [https://github.com/tu-usuario/sentinel.git](https://github.com/tu-usuario/sentinel.git)
   cd sentinel```

2. **Configurar el Webhook de Slack:**
En el archivo `src/main/resources/application.properties`, agrega tu URL real:
```properties
    slack.webhook.url=[https://hooks.slack.com/services/TU/TOKEN/AQUI](https://hooks.slack.com/services/TU/TOKEN/AQUI)
```

3. **Compilar y Ejecutar:**
```bash
    mvn clean install
    mvn spring-boot:run
```

4. **Acceso:**
Dashboard: `http://localhost:8080`
Consola de DB: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:sentineldb`)

## 📸 Ciclo de Resiliencia de Sentinel

Sentinel opera bajo un flujo de "Detección - Registro - Acción":

1. **Detección**: Escaneo de contenedores cada 5 segundos.
2. **Registro de Incidente**: Si un contenedor `exited` es detectado, se guarda el evento `FAILURE` en la base de datos.
3. **Recuperación Automática**: Sentinel envía el comando de reinicio al contenedor mediante la API de Docker.
4. **Cierre de Ciclo**: Una vez recuperado, se registra el evento `RECOVERY` y se dispara la notificación a Slack.

## 🛤️ Roadmap del Proyecto

* [x] **Fase 1**: Dashboard en tiempo real y Self-healing básico.
* [x] **Fase 2**: Persistencia de logs de incidentes y notificaciones Slack.
* [ ] **Fase 3**: Monitoreo de recursos (CPU/RAM) y lógica "Anti-Loop" para prevenir reinicios infinitos.
* [ ] **Fase 4**: Soporte para orquestadores (Docker Swarm / Kubernetes).

---
