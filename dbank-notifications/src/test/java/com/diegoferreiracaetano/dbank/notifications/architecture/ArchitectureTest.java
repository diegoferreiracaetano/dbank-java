package com.diegoferreiracaetano.dbank.notifications.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.diegoferreiracaetano.dbank.testsupport.DataAccessRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
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
    static final ArchTests DATA_ACCESS = ArchTests.in(DataAccessRules.class);

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
