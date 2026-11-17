package com.pixid.joboffer.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
        packages = "com.pixid.joboffer",
        importOptions = {ImportOption.DoNotIncludeTests.class}
)
class ArchitectureTest {

    private static final String DOMAIN = "..domain..";
    private static final String APPLICATION = "..application..";
    private static final String INFRASTRUCTURE = "..infrastructure..";
    private static final String PRESENTATION = "..presentation..";

    @ArchTest
    static final ArchRule domain_should_not_depend_on_outer_layers =
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            APPLICATION, INFRASTRUCTURE, PRESENTATION, "..springframework.."
                    )
                    .because("Le cœur du domaine doit être pur et totalement agnostique des frameworks et couches externes.");

    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure_or_presentation =
            noClasses().that().resideInAPackage(APPLICATION)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            INFRASTRUCTURE, PRESENTATION
                    )
                    .because("La couche application ne doit connaître ni l'infrastructure ni la présentation.");

    @ArchTest
    static final ArchRule infrastructure_should_not_depend_on_presentation =
            noClasses().that().resideInAPackage(INFRASTRUCTURE)
                    .should().dependOnClassesThat().resideInAPackage(PRESENTATION)
                    .because("L'infrastructure ne doit pas dépendre des contrôleurs ou adaptateurs de présentation.");

    @ArchTest
    static final ArchRule no_cyclic_dependencies_between_packages =
            slices().matching("com.pixid.joboffer.(*)..")
                    .should().beFreeOfCycles()
                    .because("L'architecture globale ne doit pas contenir de cycles de dépendances.");
}