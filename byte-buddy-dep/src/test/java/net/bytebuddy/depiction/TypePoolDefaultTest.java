package net.bytebuddy.depiction;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isConstructor;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypePoolDefaultTest {

    private static final String F1 = "f1", M1 = "m1";

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    @Test
    public void testTypeDescription() throws Exception {
        TypeDescription typeDescription = typePool.describe(Foo.class.getName());
        assertThat(typeDescription, notNullValue());
        assertThat(typeDescription.getName(), is(Foo.class.getName()));
        assertThat(typeDescription.getDescriptor(), is(Type.getDescriptor(Foo.class)));
        assertThat(typeDescription.getModifiers(), is(Foo.class.getModifiers()));
//        assertThat(typeDescription.getDeclaredFields().size(), is(1));
        assertThat(typeDescription.getDeclaredFields().named(F1).getName(), is(F1));
        assertThat(typeDescription.getDeclaredFields().named(F1).getModifiers(), is(Foo.class.getDeclaredField(F1).getModifiers()));
        assertThat(typeDescription.getDeclaredFields().named(F1).getFieldType(), is((TypeDescription) new TypeDescription.ForLoadedType(boolean.class)));
        assertThat(typeDescription.getDeclaredMethods().size(), is(2));
        assertThat(typeDescription.getDeclaredMethods().filter(isConstructor()).getOnly().getName(), is(Foo.class.getName()));
        assertThat(typeDescription.getDeclaredMethods().filter(isConstructor()).getOnly().getModifiers(), is(Foo.class.getDeclaredConstructor().getModifiers()));
        assertThat(typeDescription.getDeclaredMethods().filter(named(M1)).getOnly().getName(), is(M1));
        assertThat(typeDescription.getDeclaredMethods().filter(named(M1)).getOnly().getModifiers(), is(Foo.class.getDeclaredMethod(M1).getModifiers()));
    }

    @SuppressWarnings("unused")
    private static class Foo {

        private boolean f1;

        private byte f2;

        private short f3;

        private char f4;

        private int f5;

        private long f6;

        private float f7;

        private double f8;

        private Object f9;

        private Object[] f10;

        private boolean m1() {
            return false;
        }
    }
}
