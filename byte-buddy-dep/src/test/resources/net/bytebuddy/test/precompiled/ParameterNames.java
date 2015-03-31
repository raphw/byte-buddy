package net.bytebuddy.test.precompiled;

/**
 * This class must be compiled with enabling {@code -parameters} for the related tests to work!
 */
public abstract class ParameterNames {

    public ParameterNames(String first, final int second) {
    }

    public void foo(final String first, long second, int third) {

    }

    public abstract void bar(String first, final long second, int third);
}
