package com.diegoferreiracaetano.dbank.ledger.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import jakarta.persistence.Entity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@AnalyzeClasses(
        packages = "com.diegoferreiracaetano.dbank.ledger",
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
    void givenDomainPackages_whenCheckingDirection_thenSharedShouldNotKnowAccountOrLedger(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..domain.shared..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..domain.account..", "..domain.ledger..")
                .because("dependencies flow from ledger to account to shared, with no cycles")
                .check(classes);
    }

    @ArchTest
    void givenDomainPackages_whenCheckingDirection_thenAccountShouldNotKnowLedger(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..domain.account..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..domain.ledger..")
                .because("dependencies flow from ledger to account to shared, with no cycles")
                .check(classes);
    }

    @ArchTest
    void givenInboundAdapters_whenCheckingDependencies_thenShouldNotKnowOutboundAdapters(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..adapter.in..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..adapter.out..")
                .because("adapters talk to each other only through ports")
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
    void givenRestComponents_whenCheckingLocation_thenShouldLiveInTheRestAdapter(JavaClasses classes) {
        classes()
                .that()
                .areAnnotatedWith(RestController.class)
                .or()
                .areAnnotatedWith(RestControllerAdvice.class)
                .should()
                .resideInAPackage("..adapter.in.rest..")
                .because("HTTP is an adapter concern")
                .check(classes);
    }
}
