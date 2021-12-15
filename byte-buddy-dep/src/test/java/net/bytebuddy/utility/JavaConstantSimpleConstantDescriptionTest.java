package net.bytebuddy.utility;

import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.objectweb.asm.Type;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JavaConstantSimpleConstantDescriptionTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    public void testInteger() throws Exception {
        assertThat(JavaConstant.Simple.ofDescription(42, (ClassLoader) null).toDescription(), is((Object) 42));
    }

    @Test
    public void testLong() throws Exception {
        assertThat(JavaConstant.Simple.ofDescription(42L, (ClassLoader) null).toDescription(), is((Object) 42L));
    }

    @Test
    public void testFloat() throws Exception {
        assertThat(JavaConstant.Simple.ofDescription(42f, (ClassLoader) null).toDescription(), is((Object) 42f));
    }

    @Test
    public void testDouble() throws Exception {
        assertThat(JavaConstant.Simple.ofDescription(42d, (ClassLoader) null).toDescription(), is((Object) 42d));
    }

    @Test
    public void testString() throws Exception {
        assertThat(JavaConstant.Simple.ofDescription("foo", (ClassLoader) null).toDescription(), is((Object) "foo"));
    }

    @Test
    @JavaVersionRule.Enforce(12)
    public void testType() throws Exception {
        assertThat(JavaConstant.Simple.ofDescription(Class.forName("java.lang.constant.ClassDesc")
                        .getMethod("of", String.class)
                        .invoke(null, Object.class.getName()), (ClassLoader) null).toDescription(),
                is(describe(Object.class)));
    }

    @Test
    @JavaVersionRule.Enforce(12)
    public void testMethodType() throws Exception {
        assertThat(JavaConstant.Simple.ofDescription(Class.forName("java.lang.constant.MethodTypeDesc")
                        .getMethod("ofDescriptor", String.class)
                        .invoke(null, Type.getMethodDescriptor(Object.class.getMethod("toString"))), (ClassLoader) null).toDescription(),
                is(describe(Class.forName("java.lang.invoke.MethodType").getMethod("methodType", Class.class).invoke(null, String.class))));
    }

    @Test
    @JavaVersionRule.Enforce(12)
    public void testMethodHandle() throws Exception {
        Method ofClassDef = Class.forName("java.lang.constant.ClassDesc").getMethod("of", String.class);
        assertThat(JavaConstant.Simple.ofDescription(Class.forName("java.lang.constant.MethodHandleDesc")
                        .getMethod("ofConstructor", Class.forName("java.lang.constant.ClassDesc"), Class.forName("[Ljava.lang.constant.ClassDesc;"))
                        .invoke(null, ofClassDef.invoke(null, Object.class.getName()), Array.newInstance(Class.forName("java.lang.constant.ClassDesc"), 0)), (ClassLoader) null).toDescription(),
                is(describe(Class.forName("java.lang.invoke.MethodHandles$Lookup")
                        .getMethod("findConstructor", Class.class, Class.forName("java.lang.invoke.MethodType"))
                        .invoke(Class.forName("java.lang.invoke.MethodHandles").getMethod("lookup").invoke(null),
                                Object.class,
                                Class.forName("java.lang.invoke.MethodType").getMethod("methodType", Class.class).invoke(null, void.class)))));
    }

    @Test
    @JavaVersionRule.Enforce(12)
    public void testDynamic() throws Exception {
        Method ofClassDef = Class.forName("java.lang.constant.ClassDesc").getMethod("of", String.class);
        assertThat(JavaConstant.Simple.ofDescription(Class.forName("java.lang.constant.DynamicConstantDesc")
                        .getMethod("of", Class.forName("java.lang.constant.DirectMethodHandleDesc"))
                        .invoke(null, Class.forName("java.lang.constant.MethodHandleDesc")
                                .getMethod("ofConstructor", Class.forName("java.lang.constant.ClassDesc"), Class.forName("[Ljava.lang.constant.ClassDesc;"))
                                .invoke(null, ofClassDef.invoke(null, Object.class.getName()), Array.newInstance(Class.forName("java.lang.constant.ClassDesc"), 0))), (ClassLoader) null).toDescription(),
                is(Class.forName("java.lang.constant.DynamicConstantDesc")
                        .getMethod("of", Class.forName("java.lang.constant.DirectMethodHandleDesc"))
                        .invoke(null, describe(Class.forName("java.lang.invoke.MethodHandles$Lookup")
                                .getMethod("findConstructor", Class.class, Class.forName("java.lang.invoke.MethodType"))
                                .invoke(Class.forName("java.lang.invoke.MethodHandles").getMethod("lookup").invoke(null),
                                        Object.class,
                                        Class.forName("java.lang.invoke.MethodType").getMethod("methodType", Class.class).invoke(null, void.class))))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegal() throws Exception {
        JavaConstant.Simple.ofDescription(new Object(), (ClassLoader) null);
    }

    private static Object describe(Object value) throws Exception {
        return Class.forName("java.util.Optional")
                .getMethod("get")
                .invoke(Class.forName("java.lang.constant.Constable").getMethod("describeConstable").invoke(value));
    }
}
