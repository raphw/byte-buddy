package net.bytebuddy.test.packaging;

@SuppressWarnings("unused")
public class PackagePrivateMethod {

    public static final String PROTECTED_METHOD_NAME = "foo";

    public static final String PACKAGE_PRIVATE_METHOD_NAME = "bar";

    public static final String PRIVATE_METHOD_NAME = "qux";

    protected void foo() {
        /* do nothing */
    }

    void bar() {
        /* do nothing */
    }

    private void qux() {
        /* do nothing */
    }
}
