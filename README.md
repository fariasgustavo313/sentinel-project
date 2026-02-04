# Sentinel: Microservices Self-Healing Monitor

**Sentinel** es una solución de monitoreo y resiliencia activa desarrollada con **Java 17** y **Spring Boot**. El sistema está diseñado para supervisar infraestructuras de **Docker** en tiempo real, detectando interrupciones en los servicios y ejecutando protocolos de recuperación automática (*Self-Healing*) sin intervención humana.


## 🌟 Características Destacadas

- **Monitoreo Basado en Eventos**: Supervisión constante del estado de los contenedores mediante el SDK de Docker.
- **Self-Healing Selectivo**: Recuperación automática de contenedores basada en **Docker Labels** (`sentinel.auto-heal=true`), evitando reinicios accidentales en servicios no críticos.
- **Dashboard en Tiempo Real**: Interfaz web dinámica que utiliza **WebSockets (STOMP)** para reflejar cambios de estado instantáneamente sin recargar la página.
- **Observabilidad y Notificaciones**: Integración con **Slack API** mediante Webhooks para el reporte proactivo de incidentes y recuperaciones exitosas.
- **Control Remoto**: Capacidad de forzar la detención de servicios desde el dashboard para pruebas de resiliencia.

## 🛠️ Stack Tecnológico

- **Backend**: Java 17, Spring Boot 3.x, Spring WebFlux (WebClient).
- **Comunicación**: Spring WebSocket + SockJS + STOMP.
- **Infraestructura**: Docker API SDK for Java.
- **Frontend**: HTML5, Tailwind CSS (Dark Mode Design).
- **Notificaciones**: Slack Webhooks.


## 📋 Requisitos Previos

- **Java 17** o superior.
- **Docker Desktop** activo.
- **Maven** para la gestión de dependencias.

## 🚀 Configuración e Instalación

1. **Clonar el repositorio:**
   ```bash
   git clone [https://github.com/tu-usuario/sentinel-project.git](https://github.com/tu-usuario/sentinel-project.git)
   cd sentinel-project```


2. **Configurar variables de entorno:**
En el archivo `src/main/resources/application.properties`, configura tu Webhook de Slack:
```properties
sentinel.slack.webhook=[https://hooks.slack.com/services/TU/TOKEN/AQUI](https://hooks.slack.com/services/TU/TOKEN/AQUI)
```

3. **Preparar servicios para monitoreo:**
Asegúrate de que tus servicios de Docker tengan la etiqueta habilitada en tu `docker-compose.yml`:
```yaml
services:
  mi-servicio:
    image: nginx
    labels:
      - "sentinel.auto-heal=true"
```

4. **Ejecutar la aplicación:**
```bash
mvn spring-boot:run
```

5. **Acceder al Dashboard:**
Abre tu navegador en `http://localhost:8080/index.html`.

## 📸 Arquitectura del Proyecto

El sistema opera bajo un ciclo de vida cerrado de monitoreo:

1. **Detección**: El `ContainerService` consulta el estado de los contenedores cada 5 segundos.
2. **Difusión**: Se envía el estado actualizado al Dashboard vía WebSockets.
3. **Acción**: Si un contenedor marcado falla, Sentinel ejecuta el comando de reinicio.
4. **Notificación**: Se envía un reporte detallado al canal de Slack configurado.

---
