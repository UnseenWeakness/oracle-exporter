# Spring Boot Oracle 11g Monitoring

A Spring Boot project to monitor Oracle 11g database metrics using Micrometer and expose them to Prometheus.

## Prerequisites

- Java 8+
- Maven 3.6+
- Oracle 11g database running on `localhost:1521/orcl` (or adjust `application.yml`)
- Prometheus installed (optional, for testing)

## Setup

1. **Configure Oracle Connection**
   Edit `src/main/resources/application.yml`:
    - Update `spring.datasource.url`, `username`, and `password` to match your Oracle 11g instance.

2. **Build the Project**
   ```bash
   mvn clean package
   
3. **get info**
   curl http://localhost:9226/actuator/prometheus

4. **update prometheus.yml** 
scrape_configs:
   - job_name: 'oracle-exporter'
     metrics_path: '/actuator/prometheus' # 指定metrics_path
     static_configs:
      - targets: ['localhost:9226']