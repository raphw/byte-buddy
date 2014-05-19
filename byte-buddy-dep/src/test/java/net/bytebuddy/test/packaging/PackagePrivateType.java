package net.bytebuddy.test.packaging;

@SuppressWarnings("unused")
public class PackagePrivateType {

    public static final Class<?> TYPE = Type.class;

    static class Type {
        /* empty */
    }
}
