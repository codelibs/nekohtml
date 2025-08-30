package org.codelibs.nekohtml.testclasses;

/**
 * Class whose constructor intentionally throws to exercise error wrapping.
 */
public class ThrowingConstructor {
    public ThrowingConstructor() {
        throw new RuntimeException("boom");
    }
}
