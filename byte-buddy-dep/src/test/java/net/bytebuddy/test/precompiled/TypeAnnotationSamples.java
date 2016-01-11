package net.bytebuddy.test.precompiled;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class TypeAnnotationSamples<@TypeAnnotation(0) T,
        S,
        @TypeAnnotation(2) U extends @TypeAnnotation(3) Callable<@TypeAnnotation(4) ?> & @TypeAnnotation(5) List<@TypeAnnotation(6) ?>,
        @TypeAnnotation(7) V extends Map<@TypeAnnotation(8) ? extends @TypeAnnotation(9) String, @TypeAnnotation(10) Callable<@TypeAnnotation(11) ? super @TypeAnnotation(12) T>>> {

    @TypeAnnotation(13) Callable<@TypeAnnotation(14) ?> @TypeAnnotation(15) [] @TypeAnnotation(16) [] foo;

    abstract <@TypeAnnotation(17) T extends @TypeAnnotation(18) Object> @TypeAnnotation(19) int foo(@TypeAnnotation(20) T @TypeAnnotation(21) [] @TypeAnnotation(22) [] v);
}
