package com.gkcontas.modulith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/**
 * The application, and the root of the module structure.
 *
 * <p>Spring Modulith treats every direct sub-package of this one as an application module:
 * {@code order}, {@code inventory}, {@code payment}, {@code fulfillment} and
 * {@code notification}. Types sitting directly in a module's package are its public API;
 * anything in a nested package is internal to it, and reaching into it from another module
 * is what the verification test refuses.
 */
@Modulithic(systemName = "Modular Monolith")
@SpringBootApplication
public class ModulithApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModulithApplication.class, args);
    }
}
