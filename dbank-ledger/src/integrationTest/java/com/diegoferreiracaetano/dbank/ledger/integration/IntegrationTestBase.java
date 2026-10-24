package com.diegoferreiracaetano.dbank.ledger.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "dbank.test.dead-letter-topic=payments.transfer-requested.v1.ledger.dlt")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(PostgresContainerConfig.class)
abstract class IntegrationTestBase {}
