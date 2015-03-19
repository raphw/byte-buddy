package net.bytebuddy.test.precompiled;

public abstract class ParameterNames {

    public ParameterNames(String first, final int second) {
    }

    public void foo(final String first, long second, int third) {

    }

    public abstract void bar(String first, final long second, int third);
}
