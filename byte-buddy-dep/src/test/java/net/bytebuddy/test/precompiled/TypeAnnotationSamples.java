package net.bytebuddy.test.precompiled;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class TypeAnnotationSamples<@TypeAnnotation(0) T,
        S,
        @TypeAnnotation(2) U extends String & @TypeAnnotation(3) Callable<@TypeAnnotation(4) ?> & @TypeAnnotation(5) List<@TypeAnnotation(6) ?>,
        @TypeAnnotation(7) V extends Map<@TypeAnnotation(8) ? extends @TypeAnnotation(9) String, @TypeAnnotation(10) Callable<@TypeAnnotation(11) ? super @TypeAnnotation(12) U>>,
        @TypeAnnotation(13) W extends @TypeAnnotation(14) V>
        extends @TypeAnnotation(15) Object
        implements @TypeAnnotation(16) Callable<@TypeAnnotation(17) Object>, Map<@TypeAnnotation(18) String, Object> {

    @TypeAnnotation(19) Callable<@TypeAnnotation(20) ?> @TypeAnnotation(21) [] @TypeAnnotation(22) [] foo;

    abstract <@TypeAnnotation(23) T extends @TypeAnnotation(24) Exception> @TypeAnnotation(25) int foo(@TypeAnnotation(26) T @TypeAnnotation(27) [] @TypeAnnotation(28) [] v)
            throws @TypeAnnotation(29) T, @TypeAnnotation(30) RuntimeException;
}
