package com.edu.unicauca.gasstation.backend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Keeps the sales public API (CashBalanceQuery) as the only door other modules use to read the cash balance table.
 */
class SalesPublicApiBoundaryTest {

    private static final String ROOT = "com.edu.unicauca.gasstation.backend";
    private static final String[] SALES_INTERNALS = {
            ROOT + ".sales.api..",
            ROOT + ".sales.application..",
            ROOT + ".sales.domain..",
            ROOT + ".sales.exception..",
            ROOT + ".sales.infrastructure.."
    };

    private static JavaClasses mainClasses;

    @BeforeAll
    static void importMainClasses() {
        mainClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(ROOT);
    }

    @Test
    void reportsDoNotUseTheInternalsOfSales() {
        noClasses().that().resideInAPackage(ROOT + ".reports..")
                .should().dependOnClassesThat().resideInAnyPackage(SALES_INTERNALS)
                .check(mainClasses);
    }

    @Test
    void theSalesPublicApiDoesNotExposeInternalClasses() {
        classes().that().resideInAPackage(ROOT + ".sales")
                .should().onlyDependOnClassesThat().resideOutsideOfPackages(SALES_INTERNALS)
                .check(mainClasses);
    }

    @Test
    @Disabled("Enable in the PR where reports adds its consumer of CashBalanceQuery")
    void reportsConsumeTheCashBalanceQuery() {
        boolean consumed = mainClasses.stream()
                .filter(javaClass -> javaClass.getPackageName().startsWith(ROOT + ".reports"))
                .flatMap(javaClass -> javaClass.getDirectDependenciesFromSelf().stream())
                .anyMatch(dependency -> dependency.getTargetClass().getFullName().equals(ROOT + ".sales.CashBalanceQuery"));

        assertTrue(consumed, "No class of reports uses sales.CashBalanceQuery yet");
    }
}
