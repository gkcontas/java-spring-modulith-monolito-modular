package com.gkcontas.modulith;

import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks that the verification actually fails on a violation.
 *
 * <p>A green structural test says one of two things: the modules respect their boundaries,
 * or the verification is looking at the wrong package and has been passing vacuously since
 * the day it was written. Pointing the same check at code that breaks the rule settles
 * which one it is.
 */
class ModuleVerificationBitesTest {

    @Test
    void rejectsAModuleReachingIntoAnothersInternals() {
                // The sample lives in test sources, and the default import skips those — pass an
        // option that keeps them, otherwise the verification finds no classes at all and
        // the test fails for the wrong reason.
        ApplicationModules sample = ApplicationModules.of("com.gkcontas.modulithsample",
                new ImportOption.Predefined.OnlyIncludeTests());

        assertThatThrownBy(sample::verify)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("BetaService")
                .hasMessageContaining("HiddenDetail");
    }
}
