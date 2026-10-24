package com.diegoferreiracaetano.dbank.payments.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "dbank.test.dead-letter-topic=ledger.transfer-result.v1.payments.dlt")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(ContainersConfig.class)
abstract class IntegrationTestBase {}
