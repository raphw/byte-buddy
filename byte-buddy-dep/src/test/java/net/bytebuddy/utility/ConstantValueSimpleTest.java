package net.bytebuddy.utility;

import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class ConstantValueSimpleTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {false, TypeDescription.ForLoadedType.of(boolean.class)},
                {(byte) 0, TypeDescription.ForLoadedType.of(byte.class)},
                {(short) 0, TypeDescription.ForLoadedType.of(short.class)},
                {(char) 0, TypeDescription.ForLoadedType.of(char.class)},
                {0, TypeDescription.ForLoadedType.of(int.class)},
                {0L, TypeDescription.ForLoadedType.of(long.class)},
                {0f, TypeDescription.ForLoadedType.of(float.class)},
                {0d, TypeDescription.ForLoadedType.of(double.class)},
                {"foo", TypeDescription.ForLoadedType.of(String.class)},
                {Object.class, TypeDescription.ForLoadedType.of(Class.class)},
                {int.class, TypeDescription.ForLoadedType.of(Class.class)},
                {Sample.VALUE, TypeDescription.ForLoadedType.of(Sample.class)},
                {TypeDescription.ForLoadedType.of(Object.class), TypeDescription.ForLoadedType.of(Class.class)},
                {new EnumerationDescription.ForLoadedEnumeration(Sample.VALUE), TypeDescription.ForLoadedType.of(Sample.class)},
                {ConstantValue.Simple.wrap(false), TypeDescription.ForLoadedType.of(boolean.class)},
                {ConstantValue.Simple.wrap(0), TypeDescription.ForLoadedType.of(int.class)}
        });
    }

    private final Object value;

    private final TypeDescription typeDescription;

    public ConstantValueSimpleTest(Object value, TypeDescription typeDescription) {
        this.value = value;
        this.typeDescription = typeDescription;
    }

    @Test
    public void testValueWrap() {
        assertThat(ConstantValue.Simple.wrapOrNull(value), notNullValue(ConstantValue.class));
    }

    @Test
    public void testTypeMatchesConstant() {
        assertThat(ConstantValue.Simple.wrap(value).getTypeDescription(), is(typeDescription));
    }

    public enum Sample {
        VALUE
    }
}
