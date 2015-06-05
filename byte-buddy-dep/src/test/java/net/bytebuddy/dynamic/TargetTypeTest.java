package net.bytebuddy.dynamic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public class TargetTypeTest {

    private TypeDescription originalType, substitute;

    @Before
    public void setUp() throws Exception {
        originalType = new TypeDescription.ForLoadedType(OriginalType.class);
        substitute = new TypeDescription.ForLoadedType(Substitute.class);
    }

    @Test
    public void testSimpleResolution() throws Exception {
        assertThat(TargetType.resolve(originalType, substitute, ElementMatchers.is(originalType)), is(substitute));
    }

    @Test
    public void testSimpleSkippedResolution() throws Exception {
        assertThat(TargetType.resolve(originalType, substitute, ElementMatchers.none()), sameInstance(originalType));
    }

    @Test
    public void testArrayResolution() throws Exception {
        TypeDescription arrayType = TypeDescription.ArrayProjection.of(originalType, 2);
        assertThat(TargetType.resolve(arrayType, substitute, ElementMatchers.is(originalType)), is(TypeDescription.ArrayProjection.of(substitute, 2)));
    }

    @Test
    public void testArraySkippedResolution() throws Exception {
        TypeDescription arrayType = TypeDescription.ArrayProjection.of(originalType, 2);
        assertThat(TargetType.resolve(arrayType, substitute, ElementMatchers.none()), sameInstance(arrayType));
    }

    // TODO: Generic types!

    public static class OriginalType {
        /* empty */
    }

    public static class Substitute {
        /* empty */
    }
}
