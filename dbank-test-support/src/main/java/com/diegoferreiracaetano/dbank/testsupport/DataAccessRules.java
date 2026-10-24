package com.diegoferreiracaetano.dbank.testsupport;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.ArchTest;

public class DataAccessRules {

    @ArchTest
    void givenProductionCode_whenCheckingDataAccess_thenShouldNotUseJdbcDirectly(JavaClasses classes) {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("java.sql..", "javax.sql..", "org.springframework.jdbc..")
                .because("data access goes through Spring Data JPA, and native SQL is declared with @Query")
                .check(classes);
    }
}
