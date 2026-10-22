package com.diegoferreiracaetano.dbank.payments.adapter.in.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    @Bean
    OpenAPI paymentsOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Dbank Payments API").version("1.0.0").description("""
                                Transfer requests, accepted for asynchronous processing and idempotent by key.
                                Amounts are integers in cents. Errors follow RFC 9457 (application/problem+json).
                                """))
                .servers(List.of(new Server().url("http://localhost:8082").description("Local development")))
                .tags(List.of(new Tag().name("Transfers").description("Request and read transfers")));
    }
}
