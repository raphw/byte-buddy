package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Origin;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class MethodDelegationOriginTest extends AbstractInstrumentationTest {

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

    @Test
    public void testOriginClass() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(OriginClass.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), instanceOf(Class.class));
    }

    public static class OriginMethod {

        public static Object foo(@Origin Method method) {
            return method;
        }
    }

    @Test
    public void testOriginMethod() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(OriginMethod.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), instanceOf(Method.class));
    }

    public static class OriginIllegal {

        public static Object foo(@Origin Object object) {
            return object;
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testOriginIllegal() throws Exception {
        instrument(Foo.class, MethodDelegation.to(OriginIllegal.class));
    }
}
