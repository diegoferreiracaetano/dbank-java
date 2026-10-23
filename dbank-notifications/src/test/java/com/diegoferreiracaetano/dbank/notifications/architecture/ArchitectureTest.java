package com.diegoferreiracaetano.dbank.notifications.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import jakarta.persistence.Entity;

@AnalyzeClasses(
        packages = "com.diegoferreiracaetano.dbank.notifications",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    void givenLayers_whenCheckingDependencies_thenShouldOnlyGoDownwards(JavaClasses classes) {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Messaging")
                .definedBy("..messaging..")
                .layer("Service")
                .definedBy("..service..")
                .layer("Persistence")
                .definedBy("..persistence..")
                .whereLayer("Messaging")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Service")
                .mayOnlyBeAccessedByLayers("Messaging")
                .whereLayer("Persistence")
                .mayOnlyBeAccessedByLayers("Service")
                .check(classes);
    }

    @ArchTest
    void givenPackages_whenCheckingSlices_thenShouldBeFreeOfCycles(JavaClasses classes) {
        slices().matching("com.diegoferreiracaetano.dbank.notifications.(*)..")
                .should()
                .beFreeOfCycles()
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

    @ArchTest
    void givenJpaEntities_whenCheckingLocation_thenShouldLiveInThePersistenceLayer(JavaClasses classes) {
        classes()
                .that()
                .areAnnotatedWith(Entity.class)
                .should()
                .resideInAPackage("..persistence..")
                .because("persistence mapping belongs to the persistence layer")
                .check(classes);
    }
}
