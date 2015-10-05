package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.dynamic.AbstractDynamicTypeBuilderTest;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.test.scope.GenericType;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotEquals;

public class SubclassDynamicTypeBuilderTest extends AbstractDynamicTypeBuilderTest {

    private static final ProtectionDomain DEFAULT_PROTECTION_DOMAIN = null;

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    private static final String DEFAULT_METHOD_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";

    private static final String PARAMETER_NAME_CLASS = "net.bytebuddy.test.precompiled.ParameterNames";

    private static final Object STATIC_FIELD = null;

    private static final String INTERFACE_STATIC_FIELD_NAME = "FOO";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Override
    protected DynamicType.Builder<?> createPlain() {
        return new ByteBuddy().subclass(Object.class);
    }

    protected DynamicType.Builder<?> create(Class<?> type) {
        return new ByteBuddy().subclass(type);
    }

    @Test
    public void testSimpleSubclass() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(0));
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredConstructor(), notNullValue(Constructor.class));
        assertThat(Object.class.isAssignableFrom(type), is(true));
        assertNotEquals(Object.class, type);
        assertThat(type.newInstance(), notNullValue(Object.class));
        assertThat(type.isInterface(), is(false));
        assertThat(type.isAnnotation(), is(false));
    }

    @Test
    public void testSimpleSubclassWithoutConstructor() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(0));
        assertThat(type.getDeclaredConstructors().length, is(0));
        assertThat(Object.class.isAssignableFrom(type), is(true));
        assertNotEquals(Object.class, type);
        assertThat(type.isInterface(), is(false));
        assertThat(type.isAnnotation(), is(false));
    }

    @Test
    public void testSimpleSubclassWithDefaultConstructor() throws Exception {
        Class<? extends DefaultConstructor> type = new ByteBuddy()
                .subclass(DefaultConstructor.class, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(0));
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredConstructor(), notNullValue(Constructor.class));
        assertThat(DefaultConstructor.class.isAssignableFrom(type), is(true));
        assertNotEquals(DefaultConstructor.class, type);
        assertThat(type.newInstance(), notNullValue(DefaultConstructor.class));
        assertThat(type.isInterface(), is(false));
        assertThat(type.isAnnotation(), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonExtendableIsIllegal() throws Exception {
        new ByteBuddy().subclass(String.class);
    }

    @Test
    public void testInterfaceDefinition() throws Exception {
        Class<? extends SimpleInterface> type = new ByteBuddy()
                .makeInterface(SimpleInterface.class)
                .defineMethod(FOO, void.class, Collections.<Class<?>>singletonList(Void.class), Visibility.PUBLIC)
                .withoutCode()
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(1));
        assertThat(type.getDeclaredMethod(FOO, Void.class), notNullValue(Method.class));
        assertThat(type.getDeclaredConstructors().length, is(0));
        assertThat(SimpleInterface.class.isAssignableFrom(type), is(true));
        assertNotEquals(SimpleInterface.class, type);
        assertThat(type.isInterface(), is(true));
        assertThat(type.isAnnotation(), is(false));
    }

    @Test
    public void testAnnotationDefinition() throws Exception {
        Class<? extends Annotation> type = new ByteBuddy()
                .makeAnnotation()
                .defineMethod(FOO, int.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .withoutCode()
                .defineMethod(BAR, String.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .withDefaultValue(FOO)
                .defineMethod(QUX, SimpleEnum.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .withDefaultValue(SimpleEnum.FIRST, SimpleEnum.class)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(3));
        assertThat(type.getDeclaredMethod(FOO), notNullValue(Method.class));
        assertThat(type.getDeclaredMethod(BAR).getDefaultValue(), is((Object) FOO));
        assertThat(type.getDeclaredMethod(QUX).getDefaultValue(), is((Object) SimpleEnum.FIRST));
        assertThat(type.getDeclaredConstructors().length, is(0));
        assertThat(Annotation.class.isAssignableFrom(type), is(true));
        assertNotEquals(Annotation.class, type);
        assertThat(type.isInterface(), is(true));
        assertThat(type.isAnnotation(), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEnumerationDefinition() throws Exception {
        Class<? extends Enum<?>> type = new ByteBuddy()
                .makeEnumeration(FOO, BAR)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(2));
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredFields().length, is(3));
        assertThat(Enum.class.isAssignableFrom(type), is(true));
        assertNotEquals(Enum.class, type);
        assertThat(type.isInterface(), is(false));
        assertThat(type.isAnnotation(), is(false));
        assertThat(type.isEnum(), is(true));
        Enum foo = Enum.valueOf((Class) type, FOO);
        assertThat(foo.name(), is(FOO));
        assertThat(foo.ordinal(), is(0));
        Enum bar = Enum.valueOf((Class) type, BAR);
        assertThat(bar.name(), is(BAR));
        assertThat(bar.ordinal(), is(1));
    }

    @Test
    public void testPackageDefinition() throws Exception {
        Class<?> packageType = new ByteBuddy()
                .makePackage(FOO)
                .annotateType(AnnotationDescription.Builder.forType(Foo.class).make())
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(packageType.getSimpleName(), is(PackageDescription.PACKAGE_CLASS_NAME));
        assertThat(packageType.getName(), is(FOO + "." + PackageDescription.PACKAGE_CLASS_NAME));
        assertThat(packageType.getModifiers(), is(PackageDescription.PACKAGE_MODIFIERS));
        assertThat(packageType.getDeclaredFields().length, is(0));
        assertThat(packageType.getDeclaredMethods().length, is(0));
        assertThat(packageType.getDeclaredAnnotations().length, is(1));
        assertThat(packageType.getAnnotation(Foo.class), notNullValue(Foo.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEnumWithoutValuesIsIllegal() throws Exception {
        new ByteBuddy().makeEnumeration();
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethodNonOverridden() throws Exception {
        Class<?> interfaceType = Class.forName(DEFAULT_METHOD_INTERFACE);
        Object interfaceMarker = interfaceType.getDeclaredField(INTERFACE_STATIC_FIELD_NAME).get(STATIC_FIELD);
        Method interfaceMethod = interfaceType.getDeclaredMethod(FOO);
        Class<?> dynamicType = new ByteBuddy()
                .subclass(interfaceType)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredFields().length, is(0));
        assertThat(dynamicType.getDeclaredMethods().length, is(0));
        assertThat(interfaceMethod.invoke(dynamicType.newInstance()), is(interfaceMarker));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethodOverridden() throws Exception {
        Class<?> interfaceType = Class.forName(DEFAULT_METHOD_INTERFACE);
        Method interfaceMethod = interfaceType.getDeclaredMethod(FOO);
        Class<?> dynamicType = new ByteBuddy()
                .subclass(interfaceType)
                .method(isDeclaredBy(interfaceType)).intercept(FixedValue.value(BAR))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredFields().length, is(0));
        assertThat(dynamicType.getDeclaredMethods().length, is(1));
        assertThat(interfaceMethod.invoke(dynamicType.newInstance()), is((Object) BAR));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testParameterMetaDataSubclassForLoaded() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .subclass(Class.forName(PARAMETER_NAME_CLASS))
                .method(named(FOO)).intercept(StubMethod.INSTANCE)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethods().length, is(1));
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
    @JavaVersionRule.Enforce(8)
    public void testDefaultInterfaceSubInterface() throws Exception {
        Class<?> interfaceType = Class.forName(DEFAULT_METHOD_INTERFACE);
        Class<?> dynamicInterfaceType = new ByteBuddy()
                .subclass(interfaceType)
                .modifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT)
                .method(named(FOO)).intercept(MethodDelegation.to(InterfaceOverrideInterceptor.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Class<?> dynamicClassType = new ByteBuddy()
                .subclass(dynamicInterfaceType)
                .make()
                .load(dynamicInterfaceType.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicClassType.getMethod(FOO).invoke(dynamicClassType.newInstance()), is((Object) (FOO + BAR)));
        assertThat(dynamicInterfaceType.getDeclaredMethods().length, is(2));
        assertThat(dynamicClassType.getDeclaredMethods().length, is(0));
    }

    @Test
    public void testDoesNotOverrideMethodWithPackagePrivateReturnType() throws Exception {
        Class<?> type = create(PackagePrivateReturnType.class)
                .name("net.bytebuddy.test.generated." + FOO)
                .method(isDeclaredBy(PackagePrivateReturnType.class))
                .intercept(StubMethod.INSTANCE)
                .make()
                .load(new ByteArrayClassLoader(null,
                        ClassFileExtraction.of(PackagePrivateReturnType.class, PackagePrivateReturnType.Argument.class),
                        DEFAULT_PROTECTION_DOMAIN,
                        AccessController.getContext(),
                        ByteArrayClassLoader.PersistenceHandler.LATENT,
                        PackageDefinitionStrategy.NoOp.INSTANCE), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(0));
    }

    @Test
    public void testDoesNotOverrideMethodWithPackagePrivateArgumentType() throws Exception {
        Class<?> type = create(PackagePrivateArgumentType.class)
                .name("net.bytebuddy.test.generated." + FOO)
                .method(isDeclaredBy(PackagePrivateArgumentType.class))
                .intercept(StubMethod.INSTANCE)
                .make()
                .load(new ByteArrayClassLoader(null,
                        ClassFileExtraction.of(PackagePrivateArgumentType.class, PackagePrivateArgumentType.Argument.class),
                        DEFAULT_PROTECTION_DOMAIN,
                        AccessController.getContext(),
                        ByteArrayClassLoader.PersistenceHandler.LATENT,
                        PackageDefinitionStrategy.NoOp.INSTANCE), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(0));
    }

    @Test
    public void testDoesNotOverridePrivateMethod() throws Exception {
        Class<?> type = create(PrivateMethod.class)
                .method(isDeclaredBy(PrivateMethod.class))
                .intercept(StubMethod.INSTANCE)
                .make()
                .load(new ByteArrayClassLoader(null,
                        ClassFileExtraction.of(PrivateMethod.class),
                        DEFAULT_PROTECTION_DOMAIN,
                        AccessController.getContext(),
                        ByteArrayClassLoader.PersistenceHandler.LATENT,
                        PackageDefinitionStrategy.NoOp.INSTANCE), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(0));
    }

    @Test
    public void testGenericType() throws Exception {
        Class<?> dynamicType = create(GenericType.Inner.class)
                .method(named(FOO).or(named("call"))).intercept(StubMethod.INSTANCE)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(dynamicType.getTypeParameters().length, is(0));
        assertThat(dynamicType.getGenericSuperclass(), instanceOf(Class.class));
        assertThat(dynamicType.getGenericSuperclass(), is((Type) GenericType.Inner.class));
        assertThat(dynamicType.getGenericInterfaces().length, is(0));
        Method foo = dynamicType.getDeclaredMethod(FOO, String.class);
        assertThat(foo.getTypeParameters().length, is(2));
        assertThat(foo.getTypeParameters()[0].getName(), is("V"));
        assertThat(foo.getTypeParameters()[0].getBounds().length, is(1));
        assertThat(foo.getTypeParameters()[0].getBounds()[0], is((Type) String.class));
        assertThat(foo.getTypeParameters()[1].getName(), is("W"));
        assertThat(foo.getTypeParameters()[1].getBounds().length, is(1));
        assertThat(foo.getTypeParameters()[1].getBounds()[0], is((Type) Exception.class));
        assertThat(foo.getGenericReturnType(), instanceOf(ParameterizedType.class));
        assertThat(((ParameterizedType) foo.getGenericReturnType()).getActualTypeArguments().length, is(1));
        Type parameterType = ((ParameterizedType) foo.getGenericReturnType()).getActualTypeArguments()[0];
        // Before Java 7, non-generic array types returned from methods of the generic reflection API returned generic arrays.
        if (ClassFileVersion.forCurrentJavaVersion().compareTo(ClassFileVersion.JAVA_V7) < 0) {
            assertThat(parameterType, instanceOf(GenericArrayType.class));
            assertThat(((GenericArrayType) parameterType).getGenericComponentType(), is((Type) String.class));
        } else {
            assertThat(parameterType, is((Type) String[].class));
        }
        assertThat(foo.getGenericParameterTypes().length, is(1));
        assertThat(foo.getGenericParameterTypes()[0], is((Type) foo.getTypeParameters()[0]));
        assertThat(foo.getGenericExceptionTypes().length, is(1));
        assertThat(foo.getGenericExceptionTypes()[0], is((Type) foo.getTypeParameters()[1]));
        Method call = dynamicType.getDeclaredMethod("call");
        assertThat(call.getGenericReturnType(), is(((ParameterizedType) GenericType.Inner.class.getGenericInterfaces()[0]).getActualTypeArguments()[0]));
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
        Class<?> type = create(VisibilityBridge.class)
                .modifiers(Opcodes.ACC_PUBLIC)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredMethods().length, is(2));
        Method foo = type.getDeclaredMethod(FOO, String.class);
        assertThat(foo.isBridge(), is(true));
        assertThat(foo.getDeclaredAnnotations().length, is(1));
        assertThat(foo.getAnnotation(Foo.class), notNullValue(Foo.class));
        assertThat(foo.invoke(type.newInstance(), BAR), is((Object) (FOO + BAR)));
        Method bar = type.getDeclaredMethod(BAR, List.class);
        assertThat(bar.isBridge(), is(true));
        assertThat(bar.getDeclaredAnnotations().length, is(0));
        List<?> list = new ArrayList<Object>();
        assertThat(bar.invoke(type.newInstance(), list), sameInstance((Object) list));
        assertThat(bar.getGenericReturnType(), instanceOf(Class.class));
        assertThat(bar.getGenericParameterTypes()[0], instanceOf(Class.class));
        assertThat(bar.getGenericExceptionTypes()[0], instanceOf(Class.class));
    }

    @Test
    public void testNoVisibilityBridgeForNonPublicType() throws Exception {
        Class<?> type = create(VisibilityBridge.class)
                .modifiers(0)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredMethods().length, is(0));
    }

    @Test
    public void testNoVisibilityBridgeForInheritedType() throws Exception {
        Class<?> type = create(VisibilityBridgeExtension.class)
                .modifiers(Opcodes.ACC_PUBLIC)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredMethods().length, is(0));
    }

    @Test
    public void testNoVisibilityBridgeForAbstractMethod() throws Exception {
        Class<?> type = create(VisibilityBridgeAbstractMethod.class)
                .modifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredMethods().length, is(0));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(SubclassDynamicTypeBuilder.class).create(new ObjectPropertyAssertion.Creator<List<?>>() {
            @Override
            public List<?> create() {
                return Collections.singletonList(new Object());
            }
        }).apply();
    }

    @SuppressWarnings("unused")
    public enum SimpleEnum {
        FIRST,
        SECOND
    }

    public interface SimpleInterface {

        void bar(Void arg);
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Foo {
        /* empty */
    }

    @SuppressWarnings("unused")
    public static class DefaultConstructor {

        public DefaultConstructor() {
            /* empty */
        }

        public DefaultConstructor(Void arg) {
            /* empty */
        }
    }

    public static class PackagePrivateReturnType {

        public Argument foo() {
            return null;
        }

        static class Argument {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class PackagePrivateArgumentType {

        public void foo(Argument argument) {
            /* empty */
        }

        static class Argument {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class PrivateMethod {

        private void foo() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    static class VisibilityBridge {

        @Foo
        public String foo(@Foo String value) {
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

    abstract static class VisibilityBridgeAbstractMethod {

        public abstract void foo();
    }

    public static class VisibilityBridgeExtension extends VisibilityBridge {
        /* empty */
    }

    public abstract static class AbstractGenericType<T> {

        public abstract T foo(T t);

        public abstract static class Inner extends AbstractGenericType<Void> {
            /* empty */
        }
    }
}
