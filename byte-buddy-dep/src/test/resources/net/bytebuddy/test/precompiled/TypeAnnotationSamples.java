package net.bytebuddy.test.precompiled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class TypeAnnotationSamples<@TypeAnnotation(0) T,
        S,
        @TypeAnnotation(2) U extends String & @TypeAnnotation(3) Callable<@TypeAnnotation(4) ?> & @TypeAnnotation(5) List<@TypeAnnotation(6) ?>,
        @TypeAnnotation(7) V extends Map<@TypeAnnotation(8) ? extends @TypeAnnotation(9) String, @TypeAnnotation(10) Callable<@TypeAnnotation(11) ? super @TypeAnnotation(12) U>>,
        @TypeAnnotation(13) W extends @TypeAnnotation(14) V,
        @TypeAnnotation(15) X extends @TypeAnnotation(16) ArrayList<@TypeAnnotation(17) ?>>
        extends @TypeAnnotation(18) Object
        implements @TypeAnnotation(19) Callable<@TypeAnnotation(20) Object>, Map<@TypeAnnotation(21) String, Object> {

    @TypeAnnotation(22) Callable<@TypeAnnotation(23) ?> @TypeAnnotation(24) [] @TypeAnnotation(25) [] foo;

    abstract <@TypeAnnotation(26) T extends @TypeAnnotation(27) Exception> @TypeAnnotation(28) int foo(@TypeAnnotation(29) T @TypeAnnotation(30) [] @TypeAnnotation(31) [] v)
            throws @TypeAnnotation(32) T, @TypeAnnotation(33) RuntimeException;

    abstract @TypeAnnotation(34) int @TypeAnnotation(35) [] @TypeAnnotation(36) [] bar(@TypeAnnotation(37) Void @TypeAnnotation(38) [] @TypeAnnotation(39) [] v);

    abstract <T> @TypeAnnotation(40) int @TypeAnnotation(41) [] @TypeAnnotation(42) [] qux(@TypeAnnotation(43) Void @TypeAnnotation(44) [] @TypeAnnotation(45) [] v);
}
