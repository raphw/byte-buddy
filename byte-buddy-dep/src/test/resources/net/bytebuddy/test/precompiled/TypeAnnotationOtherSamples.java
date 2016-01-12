package net.bytebuddy.test.precompiled;

public abstract class TypeAnnotationOtherSamples<T> {

    @TypeAnnotation(0)
    Void foo;

    @TypeAnnotation(1) TypeAnnotationOtherSamples<@TypeAnnotation(2) Void>.@TypeAnnotation(3) Bar<@TypeAnnotation(4) Void> bar;

    @TypeAnnotation(5) @OtherTypeAnnotation(6) Void qux;

    @TypeAnnotation(7)
    abstract Void foo(@TypeAnnotation(8) Void v) throws @TypeAnnotation(9) Exception;

    class Bar<S> {
        /* empty */
    }
}
