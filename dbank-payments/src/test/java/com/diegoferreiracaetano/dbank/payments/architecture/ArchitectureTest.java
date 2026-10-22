package com.diegoferreiracaetano.dbank.payments.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import jakarta.persistence.Entity;

@AnalyzeClasses(
        packages = "com.diegoferreiracaetano.dbank.payments",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    void givenDomain_whenCheckingDependencies_thenShouldOnlyUseItselfAndTheJdk(JavaClasses classes) {
        classes()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage("..domain..", "java..")
                .because("the domain is plain Java with no framework")
                .check(classes);
    }

    @ArchTest
    void givenApplication_whenCheckingDependencies_thenShouldOnlyUseDomainItselfAndTheJdk(JavaClasses classes) {
        classes()
                .that()
                .resideInAPackage("..application..")
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage("..domain..", "..application..", "java..")
                .because("use cases depend on the domain and on ports, never on adapters or frameworks")
                .check(classes);
    }

    @ArchTest
    void givenDomainPackages_whenCheckingDirection_thenSharedShouldNotKnowTransfer(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..domain.shared..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..domain.transfer..")
                .because("dependencies flow from transfer to shared, with no cycles")
                .check(classes);
    }

    @ArchTest
    void givenOutboundAdapters_whenCheckingDependencies_thenShouldNotKnowInboundAdapters(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..adapter.out..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..adapter.in..")
                .because("adapters talk to each other only through ports")
                .check(classes);
    }

    @ArchTest
    void givenAnyClass_whenCheckingDependencies_thenNothingOutsideConfigShouldDependOnIt(JavaClasses classes) {
        noClasses()
                .that()
                .resideOutsideOfPackage("..config..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..config..")
                .because("config is the composition root and nobody depends on it")
                .check(classes);
    }

    @ArchTest
    void givenAdapters_whenCheckingVisibility_thenShouldNotBePublic(JavaClasses classes) {
        classes()
                .that()
                .resideInAPackage("..adapter..")
                .should()
                .notBePublic()
                .because("adapters are reached only through ports and Spring wiring")
                .check(classes);
    }

    @ArchTest
    void givenJpaEntities_whenCheckingLocation_thenShouldLiveInThePersistenceAdapter(JavaClasses classes) {
        classes()
                .that()
                .areAnnotatedWith(Entity.class)
                .should()
                .resideInAPackage("..adapter.out.persistence..")
                .because("persistence mapping is an adapter concern")
                .check(classes);
    }

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
