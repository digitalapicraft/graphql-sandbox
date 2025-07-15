package com.dac.graphql.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ComponentScan;
import io.micrometer.core.instrument.MeterRegistry;
import com.dac.graphql.core.service.SchemaRegistry;
import java.util.Map;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableAspectJAutoProxy
@ComponentScan(basePackages = {
    "com.dac.graphql.core",
    "com.dac.graphql.sqlite",
    "com.dac.graphql.postgres",
    "com.dac.graphql.app.controller",
    "com.dac.graphql.app.aspect"
})
public class GraphqlServerApplication {
    private final MeterRegistry meterRegistry;
    private final SchemaRegistry schemaRegistry;

    public GraphqlServerApplication(MeterRegistry meterRegistry, SchemaRegistry schemaRegistry) {
        this.meterRegistry = meterRegistry;
        this.schemaRegistry = schemaRegistry;
    }

    public static void main(String[] args) {
        SpringApplication.run(GraphqlServerApplication.class, args);
    }

    @PostConstruct
    public void registerSchemaGauge() {
        meterRegistry.gauge("schemas.loaded", schemaRegistry.getAllSchemas(), Map::size);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("*")
                        .allowedHeaders("*");
            }
        };
    }
} 