package net.bytebuddy.test.precompiled;

import java.util.Map;
import java.util.concurrent.Callable;

public abstract class TypeAnnotationSamples<@TypeAnnotation(0) T,
        S,
        @TypeAnnotation(2) U extends @TypeAnnotation(3) Callable<@TypeAnnotation(4) ?>,
        @TypeAnnotation(5) V extends Map<@TypeAnnotation(6) ? extends @TypeAnnotation(7) String, @TypeAnnotation(8) Callable<@TypeAnnotation(9) ? super @TypeAnnotation(10) T>>> {

    @TypeAnnotation(11) Callable<@TypeAnnotation(12) ?> @TypeAnnotation(13) [] @TypeAnnotation(14) [] foo;

    abstract <@TypeAnnotation(15) T extends @TypeAnnotation(16) Object> @TypeAnnotation(17) int foo(@TypeAnnotation(18) T @TypeAnnotation(19) [] @TypeAnnotation(20) [] v);
}
