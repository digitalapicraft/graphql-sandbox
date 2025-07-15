# Metrics & Monitoring for GraphQL Sandbox

This guide explains how to enable, access, and use metrics from your GraphQL Sandbox server. It covers:
- Enabling and accessing metrics endpoints
- Scraping metrics with Prometheus
- Visualizing metrics in Grafana
- Example metrics and queries

---

## 1. Enabling Metrics in the Application

Metrics are enabled by default using [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html) and [Micrometer Prometheus](https://micrometer.io/docs/registry/prometheus).

**Key configuration in `graphql-app/src/main/resources/application.properties`:**

```
management.endpoints.web.exposure.include=prometheus,health,info
management.endpoint.prometheus.enabled=true
```

**Dependencies in `graphql-app/pom.xml`:**
- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`

---

## 2. Accessing the Metrics Endpoint

Start your application (see main README for details):

```
./run.sh
# or
./mvnw spring-boot:run -pl graphql-app
```

Then access the Prometheus metrics endpoint in your browser or with `curl`:

```
http://localhost:8080/actuator/prometheus
```

You should see output like:
```
# HELP schemas_loaded Number of loaded schemas
# TYPE schemas_loaded gauge
schemas_loaded 2.0
# HELP graphql_query_count Total number of GraphQL queries
# TYPE graphql_query_count counter
graphql_query_count{specName="employee",status="success"} 5.0
...
```

---

## 3. Scraping Metrics with Prometheus

1. **Install Prometheus** ([Download](https://prometheus.io/download/))
2. **Use the provided script to start Prometheus with the correct config:**

```
cd prometheus
chmod +x run-prometheus.sh  # Only needed once
./run-prometheus.sh
```

This script will:
- Create a default `prometheus.yml` if it doesn't exist
- Start Prometheus in Docker, mapping port 9090
- Use the config to scrape your GraphQL server at `localhost:8080/actuator/prometheus`

> The config file is provided as `prometheus/prometheus.yml` in this repo. You can edit it to match your environment if needed.

3. **Visit Prometheus UI:**
   - [http://localhost:9090](http://localhost:9090)
   - Try queries like `schemas_loaded`, `graphql_query_count`, etc.

---

## 4. Visualizing Metrics in Grafana

1. **Install Grafana** ([Download](https://grafana.com/grafana/download))
2. **Add Prometheus as a data source:**
   - URL: `http://localhost:9090`
3. **Create dashboards:**
   - Add panels for metrics like:
     - `schemas_loaded`
     - `graphql_query_count{specName="employee"}`
     - `graphql_query_latency{specName="employee"}`
     - `schema_upload_attempts`, `schema_upload_successes`, `schema_upload_failures`

---

## 5. Example Metrics

| Metric Name                | Type    | Description                                 | Labels           |
|---------------------------|---------|---------------------------------------------|------------------|
| `schemas_loaded`          | Gauge   | Number of loaded schemas                    |                  |
| `schema_upload_attempts`  | Counter | Number of schema upload attempts            |                  |
| `schema_upload_successes` | Counter | Number of successful schema uploads         |                  |
| `schema_upload_failures`  | Counter | Number of failed schema uploads             |                  |
| `graphql_query_count`     | Counter | Number of GraphQL queries                   | specName, status |
| `graphql_query_latency`   | Timer   | Latency of GraphQL queries (seconds)        | specName         |

---

## 6. Troubleshooting
- Ensure your app is running and `/actuator/prometheus` is accessible.
- Check Prometheus logs for scrape errors.
- Use `curl http://localhost:8080/actuator/prometheus` to verify output.
- For custom metrics, see the code in `graphql-app/src/main/java/com/dac/graphql/app/GraphqlServerApplication.java`, `MetricsController.java`, and `GraphQLMetricsAspect.java`.

---

## 7. References
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Prometheus](https://micrometer.io/docs/registry/prometheus)
- [Prometheus](https://prometheus.io/docs/introduction/overview/)
- [Grafana](https://grafana.com/docs/grafana/latest/) 