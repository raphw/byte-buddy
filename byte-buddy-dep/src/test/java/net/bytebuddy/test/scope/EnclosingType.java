package net.bytebuddy.test.scope;

public class EnclosingType {

    public static final Class<?> INNER = Bar.class;

    public static final Class<?> ANONYMOUS_INITIALIZER;

    public static final Class<?> LOCAL_INITIALIZER;

    public static final Class<?> ANONYMOUS_METHOD = localStatic();

    public static final Class<?> LOCAL_METHOD = localAnonymous();

    static {
        ANONYMOUS_INITIALIZER = new Object() {
            /* empty*/
        }.getClass();
        class Foo {
            /* empty */
        }
        LOCAL_INITIALIZER = Foo.class;
    }

    private static Class<?> localStatic() {
        class FooBar {
            /* empty */
        }
        return FooBar.class;
    }

    private static Class<?> localAnonymous() {
        return new Object() {
            /* empty */
        }.getClass();
    }

    class Bar {
        /* empty */
    }

    public final Class<?> localConstructor;

    public final Class<?> anonymousConstructor;

    public final Class<?> localMethod;

    public final Class<?> anonymousMethod;

    public EnclosingType() {
        class Qux {
            /* empty */
        }
        localConstructor = Qux.class;
        anonymousConstructor = new Object() {
            /* empty */
        }.getClass();
        localMethod = local();
        anonymousMethod = anonymous();
    }

    private Class<?> local() {
        class Baz{
            /* empty */
        }
        return Baz.class;
    }

    private Class<?> anonymous() {
        return new Object() {
            /* empty */
        }.getClass();
    }
}
