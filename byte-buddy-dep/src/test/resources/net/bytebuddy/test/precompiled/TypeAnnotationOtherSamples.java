package net.bytebuddy.test.precompiled;

public abstract class TypeAnnotationOtherSamples<T> {

    @TypeAnnotation(0)
    Void foo;

    @TypeAnnotation(1) TypeAnnotationOtherSamples<@TypeAnnotation(2) Void>.@TypeAnnotation(3) Bar<@TypeAnnotation(4) Void> bar;

    @TypeAnnotation(5) @OtherTypeAnnotation(6) Void qux;

    @TypeAnnotation(7) Qux.@TypeAnnotation(8) Baz baz;

    @TypeAnnotation(9)
    abstract Void foo(@TypeAnnotation(10) Void v) throws @TypeAnnotation(11) Exception;

    class Bar<S> {
        /* empty */
    }

    static class Qux {

        class Baz {
            /* empty */
        }
    }
}
