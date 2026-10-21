package com.diegoferreiracaetano.dbank.ledger.adapter.in.rest;

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
    OpenAPI ledgerOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Dbank Ledger API").version("1.0.0").description("""
                                Double-entry ledger: accounts, immutable entries and idempotent transactions.
                                Amounts are integers in cents. Errors follow RFC 9457 (application/problem+json).
                                """))
                .servers(List.of(new Server().url("http://localhost:8081").description("Local development")))
                .tags(List.of(
                        new Tag().name("Accounts").description("Open accounts and read balances and statements"),
                        new Tag().name("Transactions").description("Post double-entry transactions")));
    }
}
