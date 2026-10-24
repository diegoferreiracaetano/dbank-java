package com.diegoferreiracaetano.dbank.payments.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.diegoferreiracaetano.dbank.testsupport.DataAccessRules;
import com.diegoferreiracaetano.dbank.testsupport.HexagonalRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(
        packages = "com.diegoferreiracaetano.dbank.payments",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchTests HEXAGONAL = ArchTests.in(HexagonalRules.class);

    @ArchTest
    static final ArchTests DATA_ACCESS = ArchTests.in(DataAccessRules.class);

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
}
