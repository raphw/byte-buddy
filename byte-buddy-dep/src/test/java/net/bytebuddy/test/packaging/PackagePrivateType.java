package net.bytebuddy.test.packaging;

@SuppressWarnings("unused")
public class PackagePrivateType {

    public static final Class<?> TYPE = Type.class;

    public static final Class<?> EXCEPTION_TYPE = ExceptionType.class;

    public static final Class<?> INTERFACE_TYPE = InterfaceType.class;

    static class Type {
        /* empty */
    }

    static interface InterfaceType {
        /* empty */
    }

    static class ExceptionType extends Exception {
        /* empty */
    }
}
