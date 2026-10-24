package com.diegoferreiracaetano.dbank.ledger.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.diegoferreiracaetano.dbank.testsupport.DataAccessRules;
import com.diegoferreiracaetano.dbank.testsupport.HexagonalRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(
        packages = "com.diegoferreiracaetano.dbank.ledger",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchTests HEXAGONAL = ArchTests.in(HexagonalRules.class);

    @ArchTest
    static final ArchTests DATA_ACCESS = ArchTests.in(DataAccessRules.class);

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
}
