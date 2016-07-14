package net.bytebuddy.test.scope;

public class EnclosingType {

    public static final Class<?> INNER = Bar.class;

    public static final Class<?> ANONYMOUS = new Object() {
        /* empty */
    }.getClass();

    public static final Class<?> LOCAL = makeMember();

    private static Class<?> makeMember() {
        class Foo {
            /* empty */
        }
        return Foo.class;
    }

    class Bar {
        /* empty */
    }
}
