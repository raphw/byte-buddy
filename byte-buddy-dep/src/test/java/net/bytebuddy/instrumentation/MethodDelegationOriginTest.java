package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Origin;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;

public class MethodDelegationOriginTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo";

    @Test
    public void testOriginClass() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(OriginClass.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), instanceOf(Class.class));
        assertEquals(Foo.class, ((Class<?>) instance.foo()).getSuperclass());
    }

    @Test
    public void testOriginMethod() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(OriginMethod.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), instanceOf(Method.class));
        assertThat(instance.foo(), is((Object) Foo.class.getDeclaredMethod(FOO)));
    }

    @Test
    public void testOriginString() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(OriginString.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), instanceOf(String.class));
        assertThat(instance.foo(),
                is((Object) new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod(FOO)).getUniqueSignature()));
    }

    @Test(expected = IllegalStateException.class)
    public void testOriginIllegal() throws Exception {
        instrument(Foo.class, MethodDelegation.to(OriginIllegal.class));
    }

    public static class Foo {

        public Object foo() {
            return null;
        }
    }

    public static class OriginClass {

        public static Object foo(@Origin Class<?> type) {
            return type;
        }
    }

    public static class OriginMethod {

        public static Object foo(@Origin Method method) {
            return method;
        }
    }

    public static class OriginString {

        public static Object foo(@Origin String string) {
            return string;
        }
    }

    public static class OriginIllegal {

        public static Object foo(@Origin Object object) {
            return object;
        }
    }
}
