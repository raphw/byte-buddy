package net.bytebuddy.dynamic.scaffold.inline;

import jdk.nashorn.internal.codegen.types.*;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.description.modifier.MethodManifestation;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.AbstractDynamicTypeBuilderTest;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.MethodTransformer;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.scope.GenericType;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.RemappingMethodAdapter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static junit.framework.TestCase.assertEquals;
import static net.bytebuddy.matcher.ElementMatchers.isTypeInitializer;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public abstract class AbstractDynamicTypeBuilderForInliningTest extends AbstractDynamicTypeBuilderTest {

    private static final ProtectionDomain DEFAULT_PROTECTION_DOMAIN = null;

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
                ClassFileExtraction.of(GenericType.class), DEFAULT_PROTECTION_DOMAIN,
                AccessController.getContext(),
                ByteArrayClassLoader.PersistenceHandler.LATENT,
                PackageDefinitionStrategy.NoOp.INSTANCE);
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
    public void testBridgeMethodCreation() throws Exception {
        Class<?> dynamicType = create(BridgeRetention.Inner.class)
                .method(named(FOO)).intercept(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertEquals(String.class, dynamicType.getDeclaredMethod(FOO).getReturnType());
        assertThat(dynamicType.getDeclaredMethod(FOO).getGenericReturnType(), is((Type) String.class));
        BridgeRetention<String> bridgeRetention = (BridgeRetention<String>) dynamicType.newInstance();
        assertThat(bridgeRetention.foo(), is(FOO));
        bridgeRetention.assertZeroCalls();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBridgeMethodCreationForExistingBridgeMethod() throws Exception {
        Class<?> dynamicType = create(CallSuperMethod.Inner.class)
                .method(named(FOO)).intercept(net.bytebuddy.implementation.SuperMethodCall.INSTANCE)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethods().length, is(2));
        assertEquals(String.class, dynamicType.getDeclaredMethod(FOO, String.class).getReturnType());
        assertThat(dynamicType.getDeclaredMethod(FOO, String.class).getGenericReturnType(), is((Type) String.class));
        assertThat(dynamicType.getDeclaredMethod(FOO, String.class).isBridge(), is(false));
        assertEquals(Object.class, dynamicType.getDeclaredMethod(FOO, Object.class).getReturnType());
        assertThat(dynamicType.getDeclaredMethod(FOO, Object.class).getGenericReturnType(), is((Type) Object.class));
        assertThat(dynamicType.getDeclaredMethod(FOO, Object.class).isBridge(), is(true));
        CallSuperMethod<String> callSuperMethod = (CallSuperMethod<String>) dynamicType.newInstance();
        assertThat(callSuperMethod.foo(FOO), is(FOO));
        callSuperMethod.assertOnlyCall(FOO);
    }

    @Test
    public void testBridgeMethodForAbstractMethod() throws Exception {
        Class<?> dynamicType = create(AbstractGenericType.Inner.class)
                .modifiers(Opcodes.ACC_ABSTRACT | Opcodes.ACC_PUBLIC)
                .method(named(FOO)).withoutCode()
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethods().length, is(2));
        assertEquals(Void.class, dynamicType.getDeclaredMethod(FOO, Void.class).getReturnType());
        assertThat(dynamicType.getDeclaredMethod(FOO, Void.class).getGenericReturnType(), is((Type) Void.class));
        assertThat(dynamicType.getDeclaredMethod(FOO, Void.class).isBridge(), is(false));
        assertThat(Modifier.isAbstract(dynamicType.getDeclaredMethod(FOO, Void.class).getModifiers()), is(true));
        assertEquals(Object.class, dynamicType.getDeclaredMethod(FOO, Object.class).getReturnType());
        assertThat(dynamicType.getDeclaredMethod(FOO, Object.class).getGenericReturnType(), is((Type) Object.class));
        assertThat(dynamicType.getDeclaredMethod(FOO, Object.class).isBridge(), is(true));
        assertThat(Modifier.isAbstract(dynamicType.getDeclaredMethod(FOO, Object.class).getModifiers()), is(false));
    }

    @Test
    public void testVisibilityBridge() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(null,
                ClassFileExtraction.of(PackagePrivateVisibilityBridgeExtension.class, VisibilityBridge.class, FooBar.class),
                DEFAULT_PROTECTION_DOMAIN,
                AccessController.getContext(),
                ByteArrayClassLoader.PersistenceHandler.LATENT,
                PackageDefinitionStrategy.NoOp.INSTANCE);
        Class<?> type = create(PackagePrivateVisibilityBridgeExtension.class)
                .modifiers(Opcodes.ACC_PUBLIC)
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(1));
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThat(type.getDeclaredMethods().length, is(2));
        Method foo = type.getDeclaredMethod(FOO, String.class);
        foo.setAccessible(true);
        assertThat(foo.isBridge(), is(true));
        assertThat(foo.getDeclaredAnnotations().length, is(1));
        assertThat(foo.getDeclaredAnnotations()[0].annotationType().getName(), is(FooBar.class.getName()));
        assertThat(foo.invoke(constructor.newInstance(), BAR), is((Object) (FOO + BAR)));
        assertThat(foo.getParameterAnnotations()[0].length, is(1));
        assertThat(foo.getParameterAnnotations()[0][0].annotationType().getName(), is(FooBar.class.getName()));
        assertThat(foo.invoke(constructor.newInstance(), BAR), is((Object) (FOO + BAR)));
        Method bar = type.getDeclaredMethod(BAR, List.class);
        bar.setAccessible(true);
        assertThat(bar.isBridge(), is(true));
        assertThat(bar.getDeclaredAnnotations().length, is(0));
        List<?> list = new ArrayList<Object>();
        assertThat(bar.invoke(constructor.newInstance(), list), sameInstance((Object) list));
        assertThat(bar.getGenericReturnType(), instanceOf(Class.class));
        assertThat(bar.getGenericParameterTypes()[0], instanceOf(Class.class));
        assertThat(bar.getGenericExceptionTypes()[0], instanceOf(Class.class));
    }

    @Test
    public void testNoVisibilityBridgeForNonPublicType() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(null,
                ClassFileExtraction.of(PackagePrivateVisibilityBridgeExtension.class, VisibilityBridge.class, FooBar.class),
                DEFAULT_PROTECTION_DOMAIN,
                AccessController.getContext(),
                ByteArrayClassLoader.PersistenceHandler.LATENT,
                PackageDefinitionStrategy.NoOp.INSTANCE);
        Class<?> type = create(PackagePrivateVisibilityBridgeExtension.class)
                .modifiers(0)
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredMethods().length, is(0));
    }

    @Test
    public void testNoVisibilityBridgeForInheritedType() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(null,
                ClassFileExtraction.of(PublicVisibilityBridgeExtension.class, VisibilityBridge.class, FooBar.class),
                DEFAULT_PROTECTION_DOMAIN,
                AccessController.getContext(),
                ByteArrayClassLoader.PersistenceHandler.LATENT,
                PackageDefinitionStrategy.NoOp.INSTANCE);
        Class<?> type = new ByteBuddy().subclass(PublicVisibilityBridgeExtension.class)
                .modifiers(Opcodes.ACC_PUBLIC)
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredMethods().length, is(0));
    }

    @Test
    public void testNoVisibilityBridgeForAbstractMethod() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(null,
                ClassFileExtraction.of(PackagePrivateVisibilityBridgeExtensionAbstractMethod.class, VisibilityBridgeAbstractMethod.class),
                DEFAULT_PROTECTION_DOMAIN,
                AccessController.getContext(),
                ByteArrayClassLoader.PersistenceHandler.LATENT,
                PackageDefinitionStrategy.NoOp.INSTANCE);
        Class<?> type = create(PackagePrivateVisibilityBridgeExtensionAbstractMethod.class)
                .modifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT)
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredMethods().length, is(0));
    }

    @Test
    public void testMethodTransformationExistingMethod() throws Exception {
        Class<?> type = create(Transform.class)
                .method(named(FOO))
                .intercept(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE),
                        MethodTransformer.Simple.withModifiers(MethodManifestation.FINAL))
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Method foo = type.getDeclaredMethod(FOO);
        assertThat(foo.invoke(type.newInstance()), is((Object) FOO));
        assertThat(foo.getModifiers(), is(Opcodes.ACC_FINAL | Opcodes.ACC_PUBLIC));
    }

    @Test
    public void testReaderHint() throws Exception {
        ClassVisitorWrapper classVisitorWrapper = mock(ClassVisitorWrapper.class);
        when(classVisitorWrapper.wrap(any(ClassVisitor.class))).then(new Answer<ClassVisitor>() {
            @Override
            public ClassVisitor answer(InvocationOnMock invocationOnMock) throws Throwable {
                return new ClassVisitor(Opcodes.ASM5, (ClassVisitor) invocationOnMock.getArguments()[0]) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                        return new LocalVariablesSorter(access, desc, super.visitMethod(access, name, desc, signature, exceptions));
                    }
                };
            }
        });
        when(classVisitorWrapper.wrapWriter(0)).thenReturn(ClassWriter.COMPUTE_MAXS);
        when(classVisitorWrapper.wrapReader(0)).thenReturn(ClassReader.EXPAND_FRAMES);
        Class<?> type = create(StackMapFrames.class)
                .classVisitor(classVisitorWrapper)
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) BAR));
        verify(classVisitorWrapper).wrapWriter(0);
        verify(classVisitorWrapper).wrapReader(0);
        verify(classVisitorWrapper).wrap(any(ClassVisitor.class));
        verifyNoMoreInteractions(classVisitorWrapper);
    }

    public @interface Baz {

        String foo();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface FooBar {
        /* empty */
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

    @SuppressWarnings("unused")
    static class VisibilityBridge {

        @FooBar
        public String foo(@FooBar String value) {
            return FOO + value;
        }

        public <T extends Exception> List<String> bar(List<String> value) throws T {
            return value;
        }

        void qux() {
            /* empty */
        }

        protected void baz() {
            /* empty */
        }

        public final void foobar() {
            /* empty */
        }
    }

    static class PackagePrivateVisibilityBridgeExtension extends VisibilityBridge {
        /* empty */
    }

    public static class PublicVisibilityBridgeExtension extends VisibilityBridge {
        /* empty */
    }

    abstract static class VisibilityBridgeAbstractMethod {

        public abstract void foo();
    }

    abstract static class PackagePrivateVisibilityBridgeExtensionAbstractMethod extends VisibilityBridgeAbstractMethod {
        /* empty */
    }

    public static class Transform {

        public String foo() {
            return null;
        }
    }

    public abstract static class AbstractGenericType<T> {

        public abstract T foo(T t);

        public abstract static class Inner extends AbstractGenericType<Void> {
            /* empty */
        }
    }

    public static class StackMapFrames {

        public boolean foo;

        public String foo() {
            return foo
                    ? FOO
                    : BAR;
        }
    }
}