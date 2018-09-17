package net.bytebuddy.test.precompiled;

public abstract class SimpleTypeAnnotatedType<@TypeAnnotation(42) foo> implements @TypeAnnotation(84) Runnable {
    /* empty */
}
