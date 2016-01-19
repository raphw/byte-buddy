package net.bytebuddy.test.precompiled;

public abstract class ReceiverTypeSample {

    abstract void foo(@TypeAnnotation(0) ReceiverTypeSample this);

    ReceiverTypeSample() {
        /* empty */
    }

    abstract class Inner {

        abstract void foo(@TypeAnnotation(1) Inner this);

        Inner(@TypeAnnotation(2) ReceiverTypeSample ReceiverTypeSample.this) {
            /* empty */
        }
    }

    abstract static class Nested {

        abstract void foo(@TypeAnnotation(3) Nested this);

        Nested() {
            /* empty */
        }
    }

    abstract static class Generic<T> {

        abstract void foo(@TypeAnnotation(4) Generic<@TypeAnnotation(5) T> this);

        Generic() {
            /* empty */
        }

        abstract class Inner<S> {

            abstract void foo(@TypeAnnotation(6) Generic<@TypeAnnotation(7) T>.@TypeAnnotation(8) Inner<@TypeAnnotation(9) S> this);

            Inner(@TypeAnnotation(10) Generic<@TypeAnnotation(11) T> Generic.this) {
                /* empty */
            }
        }

        abstract static class Nested<S> {

            abstract void foo(@TypeAnnotation(12) Nested<@TypeAnnotation(13) S> this);

            Nested() {
                /* empty */
            }
        }
    }
}
