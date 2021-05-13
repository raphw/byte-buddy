package net.bytebuddy.utility;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Collection;

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
                {"foo", TypeDescription.STRING, "foo"},
                {Object.class, TypeDescription.CLASS, Type.getType(Object.class)},
                {TypeDescription.OBJECT, TypeDescription.CLASS, Type.getType(Object.class)},
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
        assertThat(JavaConstant.Simple.wrap(value).asConstantPoolValue(), is(constant));
    }

    @Test
    public void testTypeMatchesConstant() {
        assertThat(JavaConstant.Simple.wrap(value).getTypeDescription(), is(typeDescription));
    }
}
