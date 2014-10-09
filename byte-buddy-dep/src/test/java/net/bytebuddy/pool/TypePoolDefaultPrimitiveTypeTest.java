package net.bytebuddy.pool;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class TypePoolDefaultPrimitiveTypeTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {boolean.class},
                {byte.class},
                {short.class},
                {char.class},
                {int.class},
                {long.class},
                {float.class},
                {double.class},
                {void.class}
        });
    }

    private final Class<?> primitiveType;

    public TypePoolDefaultPrimitiveTypeTest(Class<?> primitiveType) {
        this.primitiveType = primitiveType;
    }

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
    }

    @Test
    public void testPrimitiveLookup() throws Exception {
        assertThat(typePool.describe(primitiveType.getName()).represents(primitiveType), is(true));
    }
}
