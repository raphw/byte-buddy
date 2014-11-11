package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.CallTraceable;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class ExceptionMethodTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testWithoutMessage() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, ExceptionMethod.throwing(RuntimeException.class));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        Foo instance = loaded.getLoaded().newInstance();
        assertNotEquals(Foo.class, instance.getClass());
        assertThat(instance, instanceOf(Foo.class));
        try {
            instance.foo();
            fail();
        } catch (RuntimeException e) {
            assertEquals(RuntimeException.class, e.getClass());
            assertThat(e.getMessage(), nullValue());
        }
        instance.assertZeroCalls();
    }

    @Test
    public void testWithMessage() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, ExceptionMethod.throwing(RuntimeException.class, BAR));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        Foo instance = loaded.getLoaded().newInstance();
        assertNotEquals(Foo.class, instance.getClass());
        assertThat(instance, instanceOf(Foo.class));
        try {
            instance.foo();
            fail();
        } catch (RuntimeException e) {
            assertEquals(RuntimeException.class, e.getClass());
            assertThat(e.getMessage(), is(BAR));
        }
        instance.assertZeroCalls();
    }

    @Test
    public void testWithNonDeclaredCheckedException() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, ExceptionMethod.throwing(Exception.class));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        Foo instance = loaded.getLoaded().newInstance();
        assertNotEquals(Foo.class, instance.getClass());
        assertThat(instance, instanceOf(Foo.class));
        try {
            instance.foo();
            fail();
        } catch (Exception e) {
            assertEquals(Exception.class, e.getClass());
            assertThat(e.getMessage(), nullValue());
        }
        instance.assertZeroCalls();
    }

    @Test
    public void testEqualsHashCode() throws Exception {
        ObjectPropertyAssertion.of(ExceptionMethod.class).apply();
        ObjectPropertyAssertion.of(ExceptionMethod.ConstructionDelegate.ForDefaultConstructor.class).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                when(mock.getDeclaredMethods()).thenReturn(new MethodList.ForLoadedType(Object.class));
            }
        }).apply();
        ObjectPropertyAssertion.of(ExceptionMethod.ConstructionDelegate.ForStringConstructor.class).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                when(mock.getDeclaredMethods()).thenReturn(new MethodList.ForLoadedType(String.class));
            }
        }).apply();
    }

    public static class Foo extends CallTraceable {

        public void foo() {
            register(FOO);
        }
    }
}
