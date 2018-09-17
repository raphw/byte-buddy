package net.bytebuddy.test.scope;

public class EnclosingType {

    public static final Class<?> INNER = Bar.class;

    public static final Class<?> NESTED = BarBaz.class;

    @SuppressWarnings("deprecation")
    public static final Class<?> DEPRECATED = DeprecatedClass.class;

    public static final Class<?> FINAL_INNER = FinalBar.class;

    public static final Class<?> FINAL_NESTED = FinalBarBaz.class;

    public static final Class<?> PRIVATE_INNER = PrivateBar.class;

    public static final Class<?> PRIVATE_NESTED = PrivateBarBaz.class;

    public static final Class<?> PACKAGE_INNER = PackageBar.class;

    public static final Class<?> PACKAGE_NESTED = PackageBarBaz.class;

    public static final Class<?> PROTECTED_INNER = ProtectedBar.class;

    public static final Class<?> PROTECTED_NESTED = ProtectedBarBaz.class;

    public static final Class<?> ANONYMOUS_INITIALIZER;

    public static final Class<?> LOCAL_INITIALIZER;

    public static final Class<?> ANONYMOUS_METHOD = anonymousStatic();

    public static final Class<?> LOCAL_METHOD = localStatic();

    static {
        ANONYMOUS_INITIALIZER = new Object() {
            /* empty*/
        }.getClass();
        class Foo {
            /* empty */
        }
        LOCAL_INITIALIZER = Foo.class;
    }

    private static Class<?> anonymousStatic() {
        return new Object() {
            /* empty */
        }.getClass();
    }

    private static Class<?> localStatic() {
        class FooBar {
            /* empty */
        }
        return FooBar.class;
    }

    public class Bar {
        /* empty */
    }

    public static class BarBaz {
        /* empty */
    }

    public final class FinalBar {
        /* empty */
    }

    public static final class FinalBarBaz {
        /* empty */
    }

    private class PrivateBar {
        /* empty */
    }

    private static class PrivateBarBaz {
        /* empty */
    }

    class PackageBar {
        /* empty */
    }

    static class PackageBarBaz {
        /* empty */
    }

    protected class ProtectedBar {
        /* empty */
    }

    protected static class ProtectedBarBaz {
        /* empty */
    }

    @Deprecated
    class DeprecatedClass {
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
        class Baz {
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
