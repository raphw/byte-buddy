package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.AbstractDynamicTypeBuilderTest;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
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
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.*;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class SubclassDynamicTypeBuilderTest extends AbstractDynamicTypeBuilderTest {

    private static final ProtectionDomain DEFAULT_PROTECTION_DOMAIN = null;

    private static final String TYPE_VARIABLE_NAME = "net.bytebuddy.test.precompiled.TypeAnnotation", VALUE = "value";

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    private static final int BAZ = 42;

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

    @Override
    protected DynamicType.Builder<?> createPlainWithoutValidation() {
        return new ByteBuddy().with(TypeValidation.DISABLED).subclass(Object.class);
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
        assertThat(type, not(CoreMatchers.<Class<?>>is(Object.class)));
        assertThat(type.getConstructor().newInstance(), notNullValue(Object.class));
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
        assertThat(type, not(CoreMatchers.<Class<?>>is(Object.class)));
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
        assertThat(type, not(CoreMatchers.<Class<?>>is(DefaultConstructor.class)));
        assertThat(type.getConstructor().newInstance(), notNullValue(DefaultConstructor.class));
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
                .defineMethod(FOO, void.class, Visibility.PUBLIC).withParameters(Void.class)
                .withoutCode()
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(1));
        assertThat(type.getDeclaredMethod(FOO, Void.class), notNullValue(Method.class));
        assertThat(type.getDeclaredConstructors().length, is(0));
        assertThat(SimpleInterface.class.isAssignableFrom(type), is(true));
        assertThat(type, not(CoreMatchers.<Class<?>>is(SimpleInterface.class)));
        assertThat(type.isInterface(), is(true));
        assertThat(type.isAnnotation(), is(false));
    }

    @Test
    public void testAnnotationDefinition() throws Exception {
        Class<? extends Annotation> type = new ByteBuddy()
                .makeAnnotation()
                .defineMethod(FOO, int.class, Visibility.PUBLIC)
                .withoutCode()
                .defineMethod(BAR, String.class, Visibility.PUBLIC)
                .defaultValue(FOO)
                .defineMethod(QUX, SimpleEnum.class, Visibility.PUBLIC)
                .defaultValue(SimpleEnum.FIRST, SimpleEnum.class)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(3));
        assertThat(type.getDeclaredMethod(FOO), notNullValue(Method.class));
        assertThat(type.getDeclaredMethod(BAR).getDefaultValue(), is((Object) FOO));
        assertThat(type.getDeclaredMethod(QUX).getDefaultValue(), is((Object) SimpleEnum.FIRST));
        assertThat(type.getDeclaredConstructors().length, is(0));
        assertThat(Annotation.class.isAssignableFrom(type), is(true));
        assertThat(type, not(CoreMatchers.<Class<?>>is(Annotation.class)));
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
        assertThat(type, not(CoreMatchers.<Class<?>>is(Enum.class)));
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
                .annotateType(AnnotationDescription.Builder.ofType(Foo.class).build())
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
        assertThat(interfaceMethod.invoke(dynamicType.getConstructor().newInstance()), is(interfaceMarker));
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
        assertThat(interfaceMethod.invoke(dynamicType.getConstructor().newInstance()), is((Object) BAR));
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
        assertThat(dynamicClassType.getMethod(FOO).invoke(dynamicClassType.getConstructor().newInstance()), is((Object) (FOO + BAR)));
        assertThat(dynamicInterfaceType.getDeclaredMethods().length, is(2));
        assertThat(dynamicClassType.getDeclaredMethods().length, is(0));
    }

    @Test
    public void testDoesNotOverrideMethodWithPackagePrivateReturnType() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(PackagePrivateReturnType.class)
                .name("net.bytebuddy.test.generated." + FOO)
                .method(isDeclaredBy(PackagePrivateReturnType.class))
                .intercept(StubMethod.INSTANCE)
                .make()
                .load(new ByteArrayClassLoader(null,
                        ClassFileExtraction.of(PackagePrivateReturnType.class, PackagePrivateReturnType.Argument.class),
                        DEFAULT_PROTECTION_DOMAIN,
                        ByteArrayClassLoader.PersistenceHandler.LATENT,
                        PackageDefinitionStrategy.NoOp.INSTANCE), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(0));
    }

    @Test
    public void testDoesNotOverrideMethodWithPackagePrivateArgumentType() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(PackagePrivateArgumentType.class)
                .name("net.bytebuddy.test.generated." + FOO)
                .method(isDeclaredBy(PackagePrivateArgumentType.class))
                .intercept(StubMethod.INSTANCE)
                .make()
                .load(new ByteArrayClassLoader(null,
                        ClassFileExtraction.of(PackagePrivateArgumentType.class, PackagePrivateArgumentType.Argument.class),
                        DEFAULT_PROTECTION_DOMAIN,
                        ByteArrayClassLoader.PersistenceHandler.LATENT,
                        PackageDefinitionStrategy.NoOp.INSTANCE), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(0));
    }

    @Test
    public void testDoesNotOverridePrivateMethod() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(PrivateMethod.class)
                .method(isDeclaredBy(PrivateMethod.class))
                .intercept(StubMethod.INSTANCE)
                .make()
                .load(new ByteArrayClassLoader(null,
                        ClassFileExtraction.of(PrivateMethod.class),
                        DEFAULT_PROTECTION_DOMAIN,
                        ByteArrayClassLoader.PersistenceHandler.LATENT,
                        PackageDefinitionStrategy.NoOp.INSTANCE), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(0));
    }

    @Test
    public void testGenericTypeRawExtension() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .subclass(GenericType.Inner.class)
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
        assertThat(foo.getGenericReturnType(), is((Object) List.class));
        Method call = dynamicType.getDeclaredMethod("call");
        assertThat(call.getGenericReturnType(), is(((ParameterizedType) GenericType.Inner.class.getGenericInterfaces()[0]).getActualTypeArguments()[0]));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBridgeMethodCreation() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .subclass(BridgeRetention.Inner.class)
                .method(named(FOO)).intercept(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertEquals(String.class, dynamicType.getDeclaredMethod(FOO).getReturnType());
        assertThat(dynamicType.getDeclaredMethod(FOO).getGenericReturnType(), is((Type) String.class));
        BridgeRetention<String> bridgeRetention = (BridgeRetention<String>) dynamicType.getConstructor().newInstance();
        assertThat(bridgeRetention.foo(), is(FOO));
        bridgeRetention.assertZeroCalls();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBridgeMethodCreationForExistingBridgeMethod() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .subclass(CallSuperMethod.Inner.class)
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
        CallSuperMethod<String> callSuperMethod = (CallSuperMethod<String>) dynamicType.getConstructor().newInstance();
        assertThat(callSuperMethod.foo(FOO), is(FOO));
        callSuperMethod.assertOnlyCall(FOO);
    }

    @Test
    public void testBridgeMethodForAbstractMethod() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .subclass(AbstractGenericType.Inner.class)
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
        Class<?> type = new ByteBuddy()
                .subclass(VisibilityBridge.class)
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
        assertThat(foo.invoke(type.getConstructor().newInstance(), BAR), is((Object) (FOO + BAR)));
        Method bar = type.getDeclaredMethod(BAR, List.class);
        assertThat(bar.isBridge(), is(true));
        assertThat(bar.getDeclaredAnnotations().length, is(0));
        List<?> list = new ArrayList<Object>();
        assertThat(bar.invoke(type.getConstructor().newInstance(), list), sameInstance((Object) list));
        assertThat(bar.getGenericReturnType(), instanceOf(Class.class));
        assertThat(bar.getGenericParameterTypes()[0], instanceOf(Class.class));
        assertThat(bar.getGenericExceptionTypes()[0], instanceOf(Class.class));
    }

    @Test
    public void testNoVisibilityBridgeForNonPublicType() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(VisibilityBridge.class)
                .modifiers(0)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredMethods().length, is(0));
    }

    @Test
    public void testNoVisibilityBridgeForInheritedType() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(VisibilityBridgeExtension.class)
                .modifiers(Opcodes.ACC_PUBLIC)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredMethods().length, is(0));
    }

    @Test
    public void testNoVisibilityBridgeForAbstractMethod() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(VisibilityBridgeAbstractMethod.class)
                .modifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredMethods().length, is(0));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testAnnotationTypeOnSuperClass() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Class<?> type = new ByteBuddy()
                .subclass(TypeDescription.Generic.Builder.rawType(Object.class)
                        .build(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, BAZ).build()))
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getSuperclass(), is((Object) Object.class));
        assertThat(TypeDescription.Generic.AnnotationReader.DISPATCHER.resolveSuperClassType(type).asList().size(), is(1));
        assertThat(TypeDescription.Generic.AnnotationReader.DISPATCHER.resolveSuperClassType(type).asList().ofType(typeAnnotationType)
                .getValue(value, Integer.class), is(BAZ));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testReceiverTypeDefinition() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Method method = createPlain()
                .defineMethod(FOO, void.class)
                .intercept(StubMethod.INSTANCE)
                .receiverType(TypeDescription.Generic.Builder.rawType(TargetType.class)
                        .annotate(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, BAZ).build())
                        .build())
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredMethod(FOO);
        assertThat(TypeDescription.Generic.AnnotationReader.DISPATCHER.resolveReceiverType(method).getDeclaredAnnotations().size(), is(1));
        assertThat(TypeDescription.Generic.AnnotationReader.DISPATCHER.resolveReceiverType(method).getDeclaredAnnotations()
                .ofType(typeAnnotationType).getValue(value, Integer.class), is(BAZ));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testReceiverTypeInterception() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Method method = createPlain()
                .method(named("toString"))
                .intercept(StubMethod.INSTANCE)
                .receiverType(TypeDescription.Generic.Builder.rawType(TargetType.class)
                        .annotate(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, BAZ).build())
                        .build())
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredMethod("toString");
        assertThat(TypeDescription.Generic.AnnotationReader.DISPATCHER.resolveReceiverType(method).getDeclaredAnnotations().size(), is(1));
        assertThat(TypeDescription.Generic.AnnotationReader.DISPATCHER.resolveReceiverType(method).getDeclaredAnnotations()
                .ofType(typeAnnotationType).getValue(value, Integer.class), is(BAZ));
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
