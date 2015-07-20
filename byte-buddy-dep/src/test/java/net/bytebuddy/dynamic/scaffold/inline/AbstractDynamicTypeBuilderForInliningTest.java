package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.AbstractDynamicTypeBuilderTest;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.scope.GenericType;
import net.bytebuddy.test.utility.CallTraceable;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.hamcrest.core.Is;
import org.junit.*;
import org.junit.rules.MethodRule;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import static junit.framework.TestCase.assertEquals;
import static net.bytebuddy.matcher.ElementMatchers.isTypeInitializer;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public abstract class AbstractDynamicTypeBuilderForInliningTest extends AbstractDynamicTypeBuilderTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final String PARAMETER_NAME_CLASS = "net.bytebuddy.test.precompiled.ParameterNames";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    protected abstract DynamicType.Builder<?> create(Class<?> type);

    protected abstract DynamicType.Builder<?> create(TypeDescription typeDescription, ClassFileLocator classFileLocator);

    @Test
    public void testTypeInitializerRetention() throws Exception {
        Class<?> type = create(Qux.class)
                .invokable(isTypeInitializer()).intercept(MethodCall.invoke(Qux.class.getDeclaredMethod("invoke")))
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.newInstance(), notNullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
    }

    @Test
    public void testDefaultValue() throws Exception {
        Class<?> dynamicType = create(Baz.class)
                .method(named(FOO)).withDefaultValue(FOO)
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethods().length, is(1));
        assertThat(dynamicType.getDeclaredMethod(FOO).getDefaultValue(), is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testParameterMetaDataRetention() throws Exception {
        Class<?> dynamicType = create(typePool.describe(PARAMETER_NAME_CLASS).resolve(), ClassFileLocator.ForClassLoader.ofClassPath())
                .method(named(FOO)).intercept(StubMethod.INSTANCE)
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Class<?> executable = Class.forName("java.lang.reflect.Executable");
        Method getParameters = executable.getDeclaredMethod("getParameters");
        Class<?> parameter = Class.forName("java.lang.reflect.Parameter");
        Method getName = parameter.getDeclaredMethod("getName");
        Method getModifiers = parameter.getDeclaredMethod("getModifiers");
        Method first = dynamicType.getDeclaredMethod("foo", String.class, long.class, int.class);
        Object[] methodParameter = (Object[]) getParameters.invoke(first);
        assertThat(getName.invoke(methodParameter[0]), is((Object) "first"));
        assertThat(getName.invoke(methodParameter[1]), is((Object) "second"));
        assertThat(getName.invoke(methodParameter[2]), is((Object) "third"));
        assertThat(getModifiers.invoke(methodParameter[0]), is((Object) Opcodes.ACC_FINAL));
        assertThat(getModifiers.invoke(methodParameter[1]), is((Object) 0));
        assertThat(getModifiers.invoke(methodParameter[2]), is((Object) 0));
    }

    @Test
    public void testGenericType() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(null,
                Collections.singletonMap(GenericType.class.getName(), ClassFileExtraction.extract(GenericType.class)),
                null,
                ByteArrayClassLoader.PersistenceHandler.LATENT);
        Class<?> dynamicType = create(GenericType.Inner.class)
                .method(named(FOO)).intercept(StubMethod.INSTANCE)
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(dynamicType.getTypeParameters().length, is(2));
        assertThat(dynamicType.getTypeParameters()[0].getName(), is("T"));
        assertThat(dynamicType.getTypeParameters()[0].getBounds().length, is(1));
        assertThat(dynamicType.getTypeParameters()[0].getBounds()[0], instanceOf(Class.class));
        assertThat(dynamicType.getTypeParameters()[0].getBounds()[0], is((Type) String.class));
        assertThat(dynamicType.getTypeParameters()[1].getName(), is("S"));
        assertThat(dynamicType.getTypeParameters()[1].getBounds().length, is(1));
        assertThat(dynamicType.getTypeParameters()[1].getBounds()[0], is((Type) dynamicType.getTypeParameters()[0]));
        assertThat(dynamicType.getGenericSuperclass(), instanceOf(ParameterizedType.class));
        assertThat(((ParameterizedType) dynamicType.getGenericSuperclass()).getActualTypeArguments().length, is(1));
        assertThat(((ParameterizedType) dynamicType.getGenericSuperclass()).getActualTypeArguments()[0], instanceOf(ParameterizedType.class));
        ParameterizedType superType = (ParameterizedType) ((ParameterizedType) dynamicType.getGenericSuperclass()).getActualTypeArguments()[0];
        assertThat(superType.getActualTypeArguments().length, is(2));
        assertThat(superType.getActualTypeArguments()[0], is((Type) dynamicType.getTypeParameters()[0]));
        assertThat(superType.getActualTypeArguments()[1], is((Type) dynamicType.getTypeParameters()[1]));
        assertThat(superType.getOwnerType(), instanceOf(ParameterizedType.class));
        assertThat(((ParameterizedType) superType.getOwnerType()).getRawType(), instanceOf(Class.class));
        assertThat(((Class<?>) ((ParameterizedType) superType.getOwnerType()).getRawType()).getName(), is(GenericType.class.getName()));
        assertThat(((ParameterizedType) superType.getOwnerType()).getActualTypeArguments().length, is(1));
        assertThat(((ParameterizedType) superType.getOwnerType()).getActualTypeArguments()[0],
                is((Type) ((Class<?>) ((ParameterizedType) superType.getOwnerType()).getRawType()).getTypeParameters()[0]));
        assertThat(dynamicType.getGenericInterfaces().length, is(1));
        assertThat(dynamicType.getGenericInterfaces()[0], instanceOf(ParameterizedType.class));
        assertThat(((ParameterizedType) dynamicType.getGenericInterfaces()[0]).getActualTypeArguments()[0], instanceOf(ParameterizedType.class));
        assertThat(((ParameterizedType) dynamicType.getGenericInterfaces()[0]).getRawType(), is((Type) Callable.class));
        assertThat(((ParameterizedType) dynamicType.getGenericInterfaces()[0]).getOwnerType(), nullValue(Type.class));
        assertThat(((ParameterizedType) ((ParameterizedType) dynamicType.getGenericInterfaces()[0]).getActualTypeArguments()[0])
                .getActualTypeArguments().length, is(2));
        ParameterizedType interfaceType = (ParameterizedType) ((ParameterizedType) dynamicType.getGenericInterfaces()[0]).getActualTypeArguments()[0];
        assertThat(interfaceType.getRawType(), is((Type) Map.class));
        assertThat(interfaceType.getActualTypeArguments().length, is(2));
        assertThat(interfaceType.getActualTypeArguments()[0], instanceOf(WildcardType.class));
        assertThat(((WildcardType) interfaceType.getActualTypeArguments()[0]).getUpperBounds().length, is(1));
        assertThat(((WildcardType) interfaceType.getActualTypeArguments()[0]).getUpperBounds()[0], is((Type) Object.class));
        assertThat(((WildcardType) interfaceType.getActualTypeArguments()[0]).getLowerBounds().length, is(1));
        assertThat(((WildcardType) interfaceType.getActualTypeArguments()[0]).getLowerBounds()[0], is((Type) String.class));
        assertThat(interfaceType.getActualTypeArguments()[1], instanceOf(WildcardType.class));
        assertThat(((WildcardType) interfaceType.getActualTypeArguments()[1]).getUpperBounds().length, is(1));
        assertThat(((WildcardType) interfaceType.getActualTypeArguments()[1]).getUpperBounds()[0], is((Type) String.class));
        assertThat(((WildcardType) interfaceType.getActualTypeArguments()[1]).getLowerBounds().length, is(0));
        Method foo = dynamicType.getDeclaredMethod(FOO, String.class);
        assertThat(foo.getGenericReturnType(), instanceOf(ParameterizedType.class));
        assertThat(((ParameterizedType) foo.getGenericReturnType()).getActualTypeArguments().length, is(1));
        assertThat(((ParameterizedType) foo.getGenericReturnType()).getActualTypeArguments()[0], instanceOf(GenericArrayType.class));
        assertThat(((GenericArrayType) ((ParameterizedType) foo.getGenericReturnType()).getActualTypeArguments()[0]).getGenericComponentType(),
                is((Type) dynamicType.getTypeParameters()[0]));
        assertThat(foo.getTypeParameters().length, is(2));
        assertThat(foo.getTypeParameters()[0].getName(), is("V"));
        assertThat(foo.getTypeParameters()[0].getBounds().length, is(1));
        assertThat(foo.getTypeParameters()[0].getBounds()[0], is((Type) dynamicType.getTypeParameters()[0]));
        assertThat(foo.getTypeParameters()[1].getName(), is("W"));
        assertThat(foo.getTypeParameters()[1].getBounds().length, is(1));
        assertThat(foo.getTypeParameters()[1].getBounds()[0], is((Type) Exception.class));
        assertThat(foo.getGenericParameterTypes().length, is(1));
        assertThat(foo.getGenericParameterTypes()[0], is((Type) foo.getTypeParameters()[0]));
        assertThat(foo.getGenericExceptionTypes().length, is(1));
        assertThat(foo.getGenericExceptionTypes()[0], is((Type) foo.getTypeParameters()[1]));
        Method call = dynamicType.getDeclaredMethod("call");
        assertThat(call.getGenericReturnType(), is((Type) interfaceType));
    }

    @Test
    @SuppressWarnings("unchecked")
    @Ignore("Fails because of missing bridge method")
    public void testBridgeMethodCreation() throws Exception {
        Class<?> dynamicType = create(BridgeRetention.Inner.class)
                .method(named(FOO)).intercept(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertEquals(String.class, dynamicType.getDeclaredMethod(FOO).getReturnType());
        assertThat(dynamicType.getDeclaredMethod(FOO).getGenericReturnType(), is((Type) String.class));
        SuperCall<String> superCall = (SuperCall<String>) dynamicType.newInstance();
        assertThat(superCall.foo(FOO), is(FOO));
        superCall.assertOnlyCall(FOO);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBridgeMethodSuperTypeInvocation() throws Exception {
        Class<?> dynamicType = create(SuperCall.Inner.class)
                .method(named(FOO)).intercept(SuperMethodCall.INSTANCE)
                .classVisitor(new MethodCallValidator.ClassWrapper())
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertEquals(String.class, dynamicType.getDeclaredMethod(FOO, String.class).getReturnType());
        assertThat(dynamicType.getDeclaredMethod(FOO, String.class).getGenericReturnType(), is((Type) String.class));
        SuperCall<String> superCall = (SuperCall<String>) dynamicType.newInstance();
        assertThat(superCall.foo(FOO), is(FOO));
        superCall.assertOnlyCall(FOO);
    }

    public @interface Baz {

        String foo();
    }

    public static class Qux {

        public static final String foo;

        public static String bar;

        static {
            foo = FOO;
        }

        public static void invoke() {
            bar = BAR;
        }
    }
}