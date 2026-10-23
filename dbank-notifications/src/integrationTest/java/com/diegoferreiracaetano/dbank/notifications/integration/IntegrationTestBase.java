package com.diegoferreiracaetano.dbank.notifications.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "management.endpoint.health.show-components=always")
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
abstract class IntegrationTestBase {}
