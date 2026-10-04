package com.gkcontas.modulithsample.beta;

import com.gkcontas.modulithsample.alpha.internal.HiddenDetail;

/**
 * Reaches into another module's internals on purpose, so the verification can be shown to
 * catch it.
 *
 * <p>The package is {@code com.gkcontas.modulithsample}, outside the application's base
 * package: a class annotated for Spring inside {@code com.gkcontas.modulith} would be
 * picked up by the component scan of every test that starts a context.
 */
public class BetaService {

    private final HiddenDetail detail = new HiddenDetail();

    public String describe() {
        return detail.value();
    }
}
