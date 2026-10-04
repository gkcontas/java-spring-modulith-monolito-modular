package com.gkcontas.modulithsample.alpha.internal;

/**
 * Internal to the {@code alpha} module: it sits in a nested package, so no other module
 * may refer to it.
 */
public class HiddenDetail {

    public String value() {
        return "internal";
    }
}
