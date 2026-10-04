package com.gkcontas.modulith;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * The structure, verified and documented in the same test.
 *
 * <p>{@code verify()} is the whole point of adopting Spring Modulith: it reads the package
 * structure, derives the modules, and fails when one reaches into another's internals or
 * when a cycle appears. Without it the boundaries are a naming convention, and a naming
 * convention is respected until the first deadline.
 */
class ModuleStructureTest {

    static final ApplicationModules MODULES = ApplicationModules.of(ModulithApplication.class);

    @Test
    void modulesRespectTheirBoundaries() {
        MODULES.forEach(System.out::println);
        MODULES.verify();
    }

    /**
     * Writes the diagrams and the module canvas from the code itself.
     *
     * <p>Generated documentation cannot drift: it is derived from the same model that the
     * verification uses, so a module added today appears in the diagram without anybody
     * remembering to update it.
     */
    @Test
    void writesDocumentation() {
        // Under Gradle the default output folder is the Maven one, so it is set
        // explicitly; otherwise the diagrams land in a `target` directory nobody cleans.
        new Documenter(MODULES, Documenter.Options.defaults()
                .withOutputFolder("build/spring-modulith-docs"))
                .writeDocumentation()
                .writeIndividualModulesAsPlantUml();
    }
}
