package net.bytebuddy.utility;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class JavaConstantSimpleTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {0, TypeDescription.ForLoadedType.of(int.class), 0},
                {0L, TypeDescription.ForLoadedType.of(long.class), 0L},
                {0f, TypeDescription.ForLoadedType.of(float.class), 0f},
                {0d, TypeDescription.ForLoadedType.of(double.class), 0d},
                {"foo", TypeDescription.ForLoadedType.of(String.class), "foo"},
                {Object.class, TypeDescription.ForLoadedType.of(Class.class), TypeDescription.ForLoadedType.of(Object.class)},
                {TypeDescription.ForLoadedType.of(Object.class), TypeDescription.ForLoadedType.of(Class.class), TypeDescription.ForLoadedType.of(Object.class)},
                {JavaConstant.Simple.ofLoaded(0), TypeDescription.ForLoadedType.of(int.class), 0}
        });
    }

    private final Object value;

    private final TypeDescription typeDescription;

    private final Object constant;

    public JavaConstantSimpleTest(Object value, TypeDescription typeDescription, Object constant) {
        this.value = value;
        this.typeDescription = typeDescription;
        this.constant = constant;
    }

    @Test
    public void testValueWrap() {
        assertThat(JavaConstant.Simple.wrap(value), instanceOf(JavaConstant.Simple.class));
        assertThat(((JavaConstant.Simple<?>) JavaConstant.Simple.wrap(value)).getValue(), is(constant));
    }

    @Test
    public void testTypeMatchesConstant() {
        assertThat(JavaConstant.Simple.wrap(value).getTypeDescription(), is(typeDescription));
    }

    @Test
    public void testHashCode() {
        assertThat(JavaConstant.Simple.wrap(value).hashCode(), is((value instanceof Class<?>
                ? TypeDescription.ForLoadedType.of((Class<?>) value)
                : value).hashCode()));
    }

    @Test
    public void testEquals() {
        assertThat(JavaConstant.Simple.wrap(value), is(JavaConstant.Simple.wrap(value)));
    }

    @Test
    public void testToString() {
        assertThat(JavaConstant.Simple.wrap(value).toString(), is(value.toString()));
    }
}
