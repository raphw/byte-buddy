package net.bytebuddy.test.precompiled;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class TypeAnnotationSamples<@TypeAnnotation(0) T,
        S,
        @TypeAnnotation(2) U extends @TypeAnnotation(3) Callable<@TypeAnnotation(4) ?> & @TypeAnnotation(5) List<@TypeAnnotation(6) ?>,
        @TypeAnnotation(7) V extends Map<@TypeAnnotation(8) ? extends @TypeAnnotation(9) String, @TypeAnnotation(10) Callable<@TypeAnnotation(11) ? super @TypeAnnotation(12) U>>>
        extends @TypeAnnotation(12) Object
        implements @TypeAnnotation(13) Callable<@TypeAnnotation(14) Object>, Map<@TypeAnnotation(15) String, Object> {

    @TypeAnnotation(16) Callable<@TypeAnnotation(17) ?> @TypeAnnotation(18) [] @TypeAnnotation(19) [] foo;

    abstract <@TypeAnnotation(20) T extends @TypeAnnotation(21) Exception> @TypeAnnotation(22) int foo(@TypeAnnotation(23) T @TypeAnnotation(24) [] @TypeAnnotation(25) [] v)
            throws @TypeAnnotation(26) T;
}
