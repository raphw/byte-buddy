package net.bytebuddy.pool;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.asm.Type;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class TypePoolDefaultTypeTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {SimpleType.class, false, null},
                {SimpleType[].class, true, SimpleType.class},
                {SimpleType[][].class, true, SimpleType[].class}
        });
    }

    private final Class<?> type;

    private final boolean array;

    private final TypeDescription componentType;

    public TypePoolDefaultTypeTest(Class<?> type, boolean array, Class<?> componentType) {
        this.type = type;
        this.array = array;
        this.componentType = componentType == null ? null : new TypeDescription.ForLoadedType(componentType);
    }

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
    }

    @Test
    public void testTypeExtraction() throws Exception {
        TypeDescription typeDescription = typePool.describe(type.getName());
        assertThat(typeDescription.getName(), is(type.getName()));
        assertThat(typeDescription.getCanonicalName(), is(type.getCanonicalName()));
        assertThat(typeDescription.getSimpleName(), is(type.getSimpleName()));
        assertThat(typeDescription.getEnclosingClass(), is(array ? null : (TypeDescription) new TypeDescription.ForLoadedType(getClass())));
        assertThat(typeDescription.getInternalName(), is(Type.getInternalName(type)));
        assertThat(typeDescription.getComponentType(), is(componentType));
        assertThat(typeDescription.isArray(), is(array));
    }

    private static class SimpleType {
        /* empty */
    }
}
