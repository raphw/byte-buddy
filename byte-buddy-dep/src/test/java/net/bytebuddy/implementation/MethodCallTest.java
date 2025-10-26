package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.CallTraceable;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.isEquals;
import static net.bytebuddy.matcher.ElementMatchers.isTypeInitializer;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class MethodCallTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", INVOKE_FOO = "invokeFoo";

    private static final String SINGLE_DEFAULT_METHOD = "net.bytebuddy.test.precompiled.v8.SingleDefaultMethodInterface";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Mock
    private Assigner nonAssigner;

    private static Object makeMethodType(Class<?> returnType, Class<?>... parameterType) throws Exception {
        return JavaType.METHOD_TYPE.load().getDeclaredMethod("methodType", Class.class, Class[].class).invoke(null, returnType, parameterType);
    }

    private static Object makeMethodHandle() throws Exception {
        Object lookup = Class.forName("java.lang.invoke.MethodHandles").getDeclaredMethod("publicLookup").invoke(null);
        return JavaType.METHOD_HANDLES_LOOKUP.load().getDeclaredMethod("findStatic", Class.class, String.class, JavaType.METHOD_TYPE.load())
                .invoke(lookup, Foo.class, BAR, makeMethodType(String.class, Object.class, Object.class));
    }

    @Before
    public void setUp() throws Exception {
        when(nonAssigner.assign(Mockito.any(TypeDescription.Generic.class),
                Mockito.any(TypeDescription.Generic.class),
                Mockito.any(Assigner.Typing.class))).thenReturn(StackManipulation.Illegal.INSTANCE);
    }

    @Test
    public void testStaticMethodInvocationWithoutArguments() throws Exception {
        DynamicType.Loaded<SimpleMethod> loaded = new ByteBuddy()
                .subclass(SimpleMethod.class)
                .method(named(FOO))
                .intercept(MethodCall.invoke(SimpleMethod.class.getDeclaredMethod(BAR)))
                .make()
                .load(SimpleMethod.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        SimpleMethod instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), is(BAR));
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(SimpleMethod.class)));
        assertThat(instance, instanceOf(SimpleMethod.class));
    }

    @Test
    public void testExternalStaticMethodInvocationWithoutArguments() throws Exception {
        DynamicType.Loaded<SimpleMethod> loaded = new ByteBuddy()
                .subclass(SimpleMethod.class)
                .method(named(FOO))
                .intercept(MethodCall.invoke(StaticExternalMethod.class.getDeclaredMethod(BAR)))
                .make()
                .load(SimpleMethod.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        SimpleMethod instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), is(BAR));
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(SimpleMethod.class)));
        assertThat(instance, instanceOf(SimpleMethod.class));
    }

    @Test
    public void testInstanceMethodInvocationWithoutArguments() throws Exception {
        DynamicType.Loaded<InstanceMethod> loaded = new ByteBuddy()
                .subclass(InstanceMethod.class)
                .method(named(FOO))
                .intercept(MethodCall.invoke(InstanceMethod.class.getDeclaredMethod(BAR)))
                .make()
                .load(SimpleMethod.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        InstanceMethod instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), is(BAR));
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(InstanceMethod.class)));
        assertThat(instance, instanceOf(InstanceMethod.class));
    }

    @Test
    public void testInstanceMethodInvocationWithoutArgumentsByMatcher() throws Exception {
        DynamicType.Loaded<InstanceMethod> loaded = new ByteBuddy()
                .subclass(InstanceMethod.class)
                .method(named(FOO))
                .intercept(MethodCall.invoke(named(BAR)))
                .make()
                .load(SimpleMethod.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        InstanceMethod instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), is(BAR));
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(InstanceMethod.class)));
        assertThat(instance, instanceOf(InstanceMethod.class));
    }

    @Test
    public void testMethodInvocationUsingStackManipulation() throws Exception {
        DynamicType.Loaded<SimpleMethod> loaded = new ByteBuddy()
                .subclass(SimpleMethod.class)
                .method(named(FOO))
                .intercept(MethodCall.invoke(String.class.getMethod("toUpperCase")).on(new TextConstant(FOO), String.class))
                .make()
                .load(SimpleMethod.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        SimpleMethod instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), is(FOO.toUpperCase()));
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(SimpleMethod.class)));
        assertThat(instance, instanceOf(SimpleMethod.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testMatchedCallAmbiguous() throws Exception {
        new ByteBuddy()
                .subclass(InstanceMethod.class)
                .method(named(FOO))
                .intercept(MethodCall.invoke(ElementMatchers.any()))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOnArgumentInvocationNegativeArgument() throws Exception {
        MethodCall.invoke(Object.class.getDeclaredMethod("toString")).onArgument(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOnArgumentInvocationNonExisting() throws Exception {
        new ByteBuddy()
                .subclass(ArgumentCall.class)
                .method(isDeclaredBy(ArgumentCall.class))
                .intercept(MethodCall.invoke(Object.class.getDeclaredMethod("toString")).onArgument(10))
                .make();
    }

    @Test
    public void testInvokeOnArgument() throws Exception {
        DynamicType.Loaded<ArgumentCall> loaded = new ByteBuddy()
                .subclass(ArgumentCall.class)
                .method(isDeclaredBy(ArgumentCall.class))
                .intercept(MethodCall.invoke(ArgumentCall.Target.class.getDeclaredMethod("foo")).onArgument(0))
                .make()
                .load(ArgumentCall.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        ArgumentCall instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(new ArgumentCall.Target(BAR)), is(BAR));
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(InstanceMethod.class)));
        assertThat(instance, instanceOf(ArgumentCall.class));
    }

    @Test
    public void testInvokeOnArgumentUsingMatcher() throws Exception {
        DynamicType.Loaded<ArgumentCall> loaded = new ByteBuddy()
                .subclass(ArgumentCall.class)
                .method(named("foo"))
                .intercept(MethodCall.invoke(named("toUpperCase").and(takesArguments(0))).onMethodCall(MethodCall.invoke(named("foo")).onArgument(0)))
                .make()
                .load(ArgumentCall.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        ArgumentCall instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(new ArgumentCall.Target("foo")), is("FOO"));
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(InstanceMethod.class)));
        assertThat(instance, instanceOf(ArgumentCall.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testInvokeOnArgumentNonAssignable() throws Exception {
        new ByteBuddy()
                .subclass(ArgumentCallDynamic.class)
                .method(isDeclaredBy(ArgumentCallDynamic.class))
                .intercept(MethodCall.invoke(ArgumentCallDynamic.Target.class.getDeclaredMethod("foo")).onArgument(0))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testInvokeOnArgumentNonVirtual() throws Exception {
        new ByteBuddy()
                .subclass(ArgumentCallDynamic.class)
                .method(isDeclaredBy(ArgumentCallDynamic.class))
                .intercept(MethodCall.invoke(NonVirtual.class.getDeclaredMethod("foo")).onArgument(0))
                .make();
    }

    @Test
    public void testInvokeOnArgumentDynamic() throws Exception {
        DynamicType.Loaded<ArgumentCallDynamic> loaded = new ByteBuddy()
                .subclass(ArgumentCallDynamic.class)
                .method(isDeclaredBy(ArgumentCallDynamic.class))
                .intercept(MethodCall.invoke(ArgumentCallDynamic.Target.class.getDeclaredMethod("foo")).onArgument(0)
                        .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                .make()
                .load(ArgumentCallDynamic.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        ArgumentCallDynamic instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(new ArgumentCallDynamic.Target(BAR)), is(BAR));
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(InstanceMethod.class)));
        assertThat(instance, instanceOf(ArgumentCallDynamic.class));
    }

    @Test
    public void testInvokeOnMethodCall() throws Exception {
        DynamicType.Loaded<MethodCallChaining> loaded = new ByteBuddy()
                .subclass(MethodCallChaining.class)
                .method(named("foobar"))
                .intercept(MethodCall.invoke(String.class.getMethod("toUpperCase"))
                        .onMethodCall(MethodCall.invoke(named("bar"))))
                .make()
                .load(MethodCallChaining.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallChaining instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foobar(), is("BAR"));
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(InstanceMethod.class)));
        assertThat(instance, instanceOf(MethodCallChaining.class));
    }

    @Test
    public void testInvokeOnMethodCallUsingMatcher() throws Exception {
        DynamicType.Loaded<MethodCallChaining> loaded = new ByteBuddy()
                .subclass(MethodCallChaining.class)
                .method(named("foobar"))
                .intercept(MethodCall.invoke(named("toUpperCase").and(takesArguments(0)))
                        .onMethodCall(MethodCall.invoke(named("bar"))))
                .make()
                .load(MethodCallChaining.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallChaining instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foobar(), is("BAR"));
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(InstanceMethod.class)));
        assertThat(instance, instanceOf(MethodCallChaining.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testInvokeOnIncompatibleMethodCall() throws Exception {
        new ByteBuddy()
                .subclass(MethodCallChaining.class)
                .method(named("foobar"))
                .intercept(MethodCall.invoke(Integer.class.getMethod("toString"))
                        .onMethodCall(MethodCall.invoke(named("bar"))))
                .make();
    }

    @Test
    public void testStaticOnStaticFieldFromAnotherClass() throws Exception {
        DynamicType.Loaded<Object> loaded = new ByteBuddy()
                .subclass(Object.class)
                .invokable(isTypeInitializer())
                .intercept(MethodCall.invoke(isEquals())
                        .onField("out", new FieldLocator.ForExactType.Factory(TypeDescription.ForLoadedType.of(System.class)))
                        .with(""))
                .make()
                .load(Object.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(0));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance, instanceOf(Object.class));
    }

    @Test
    public void testStaticFieldExplicit() throws Exception {
        DynamicType.Loaded<Object> loaded = new ByteBuddy()
                .subclass(Object.class)
                .invokable(isTypeInitializer())
                .intercept(MethodCall.invoke(isEquals())
                        .onField(System.class.getField("out"))
                        .with(""))
                .make()
                .load(Object.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(0));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance, instanceOf(Object.class));
    }

    @Test
    public void testOnStaticFieldFromAnotherClass() throws Exception {
        DynamicType.Loaded<Object> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod("foo", void.class)
                .intercept(MethodCall.invoke(isEquals())
                        .onField("out", new FieldLocator.ForExactType.Factory(TypeDescription.ForLoadedType.of(System.class)))
                        .with("fooCall"))
                .make()
                .load(Object.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance, instanceOf(Object.class));
    }

    @Test
    public void testOnFieldUsingMatcher() throws Exception {
        DynamicType.Loaded<MethodCallOnField> loaded = new ByteBuddy()
                .subclass(MethodCallOnField.class)
                .method(named("foo"))
                .intercept(MethodCall.invoke(named("trim")).onField("foo"))
                .make()
                .load(MethodCallOnField.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallOnField instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo = " abc ";
        assertThat(instance, instanceOf(MethodCallOnField.class));
        assertThat(instance.foo(), is("abc"));
    }

    @Test
    public void testSuperConstructorInvocationWithoutArguments() throws Exception {
        DynamicType.Loaded<Object> loaded = new ByteBuddy()
                .subclass(Object.class)
                .constructor(ElementMatchers.any())
                .intercept(MethodCall.invoke(Object.class.getDeclaredConstructor()).onSuper())
                .make()
                .load(Object.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(0));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(Object.class)));
        assertThat(instance, instanceOf(Object.class));
    }

    @Test
    public void testSuperConstructorInvocationUsingMatcher() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .defineConstructor(Visibility.PUBLIC)
                .intercept(MethodCall.invoke(isConstructor().and(isDeclaredBy(Object.class))).onSuper())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getConstructor()
                .newInstance(), notNullValue(Object.class));
    }

    @Test
    public void testObjectConstruction() throws Exception {
        DynamicType.Loaded<SelfReference> loaded = new ByteBuddy()
                .subclass(SelfReference.class)
                .method(isDeclaredBy(SelfReference.class))
                .intercept(MethodCall.construct(SelfReference.class.getDeclaredConstructor()))
                .make()
                .load(SelfReference.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        SelfReference instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        SelfReference created = instance.foo();
        assertThat(created.getClass(), CoreMatchers.<Class<?>>is(SelfReference.class));
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(SelfReference.class)));
        assertThat(instance, instanceOf(SelfReference.class));
        assertThat(created, not(instance));
    }

    @Test
    public void testSelfInvocation() throws Exception {
        SuperMethodInvocation delegate = mock(SuperMethodInvocation.class);
        when(delegate.foo()).thenReturn(FOO);
        DynamicType.Loaded<SuperMethodInvocation> loaded = new ByteBuddy()
                .subclass(SuperMethodInvocation.class)
                .method(takesArguments(0).and(named(FOO)))
                .intercept(MethodCall.invokeSelf().on(delegate))
                .make()
                .load(SuperMethodInvocation.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        SuperMethodInvocation instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(SuperMethodInvocation.class)));
        assertThat(instance, instanceOf(SuperMethodInvocation.class));
        assertThat(instance.foo(), is(FOO));
        verify(delegate).foo();
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testSelfInvocationViaMatcher() throws Exception {
        SuperMethodInvocation delegate = mock(SuperMethodInvocation.class);
        when(delegate.foo()).thenReturn(FOO);
        DynamicType.Loaded<SuperMethodInvocation> loaded = new ByteBuddy()
                .subclass(SuperMethodInvocation.class)
                .method(takesArguments(0).and(named(FOO)))
                .intercept(MethodCall.invoke(takesArguments(0).and(named(FOO))).on(delegate, SuperMethodInvocation.class))
                .make()
                .load(SuperMethodInvocation.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        SuperMethodInvocation instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(SuperMethodInvocation.class)));
        assertThat(instance, instanceOf(SuperMethodInvocation.class));
        assertThat(instance.foo(), is(FOO));
        verify(delegate).foo();
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testSuperInvocation() throws Exception {
        DynamicType.Loaded<SuperMethodInvocation> loaded = new ByteBuddy()
                .subclass(SuperMethodInvocation.class)
                .method(takesArguments(0).and(named(FOO)))
                .intercept(MethodCall.invokeSuper())
                .make()
                .load(SuperMethodInvocation.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        SuperMethodInvocation instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(SuperMethodInvocation.class)));
        assertThat(instance, instanceOf(SuperMethodInvocation.class));
        assertThat(instance.foo(), is(FOO));
    }

    @Test
    public void testWithExplicitArgumentConstantPool() throws Exception {
        DynamicType.Loaded<MethodCallWithExplicitArgument> loaded = new ByteBuddy()
                .subclass(MethodCallWithExplicitArgument.class)
                .method(isDeclaredBy(MethodCallWithExplicitArgument.class))
                .intercept(MethodCall.invokeSuper().with(FOO))
                .make()
                .load(MethodCallWithExplicitArgument.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, String.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallWithExplicitArgument instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(MethodCallWithExplicitArgument.class)));
        assertThat(instance, instanceOf(MethodCallWithExplicitArgument.class));
        assertThat(instance.foo(BAR), is(FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testWithExplicitArgumentConstantPoolNonAssignable() throws Exception {
        new ByteBuddy()
                .subclass(MethodCallWithExplicitArgument.class)
                .method(isDeclaredBy(MethodCallWithExplicitArgument.class))
                .intercept(MethodCall.invokeSuper().with(FOO).withAssigner(nonAssigner, Assigner.Typing.STATIC))
                .make();
    }

    @Test
    public void testWithExplicitArgumentStackManipulation() throws Exception {
        DynamicType.Loaded<MethodCallWithExplicitArgument> loaded = new ByteBuddy()
                .subclass(MethodCallWithExplicitArgument.class)
                .method(isDeclaredBy(MethodCallWithExplicitArgument.class))
                .intercept(MethodCall.invokeSuper().with(new TextConstant(FOO), String.class))
                .make()
                .load(MethodCallWithExplicitArgument.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, String.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallWithExplicitArgument instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(MethodCallWithExplicitArgument.class)));
        assertThat(instance, instanceOf(MethodCallWithExplicitArgument.class));
        assertThat(instance.foo(BAR), is(FOO));
    }

    @Test
    public void testWithExplicitArgumentField() throws Exception {
        DynamicType.Loaded<MethodCallWithExplicitArgument> loaded = new ByteBuddy()
                .subclass(MethodCallWithExplicitArgument.class)
                .method(isDeclaredBy(MethodCallWithExplicitArgument.class))
                .intercept(MethodCall.invokeSuper().withReference(FOO))
                .make()
                .load(MethodCallWithExplicitArgument.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, String.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        MethodCallWithExplicitArgument instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(MethodCallWithExplicitArgument.class)));
        assertThat(instance, instanceOf(MethodCallWithExplicitArgument.class));
        assertThat(instance.foo(BAR), is(FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testWithExplicitArgumentFieldNonAssignable() throws Exception {
        new ByteBuddy()
                .subclass(MethodCallWithExplicitArgument.class)
                .method(isDeclaredBy(MethodCallWithExplicitArgument.class))
                .intercept(MethodCall.invokeSuper().withReference(FOO).withAssigner(nonAssigner, Assigner.Typing.STATIC))
                .make();
    }

    @Test
    public void testWithArgument() throws Exception {
        DynamicType.Loaded<MethodCallWithExplicitArgument> loaded = new ByteBuddy()
                .subclass(MethodCallWithExplicitArgument.class)
                .method(isDeclaredBy(MethodCallWithExplicitArgument.class))
                .intercept(MethodCall.invokeSuper().withArgument(0))
                .make()
                .load(MethodCallWithExplicitArgument.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, String.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallWithExplicitArgument instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(MethodCallWithExplicitArgument.class)));
        assertThat(instance, instanceOf(MethodCallWithExplicitArgument.class));
        assertThat(instance.foo(BAR), is(BAR));
    }

    @Test
    public void testWithAllArguments() throws Exception {
        DynamicType.Loaded<MethodCallWithExplicitArgument> loaded = new ByteBuddy()
                .subclass(MethodCallWithExplicitArgument.class)
                .method(isDeclaredBy(MethodCallWithExplicitArgument.class))
                .intercept(MethodCall.invokeSuper().withAllArguments())
                .make()
                .load(MethodCallWithExplicitArgument.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, String.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallWithExplicitArgument instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(MethodCallWithExplicitArgument.class)));
        assertThat(instance, instanceOf(MethodCallWithExplicitArgument.class));
        assertThat(instance.foo(BAR), is(BAR));
    }

    @Test
    public void testWithAllArgumentsTwoArguments() throws Exception {
        DynamicType.Loaded<MethodCallWithTwoExplicitArguments> loaded = new ByteBuddy()
                .subclass(MethodCallWithTwoExplicitArguments.class)
                .method(isDeclaredBy(MethodCallWithTwoExplicitArguments.class))
                .intercept(MethodCall.invokeSuper().withAllArguments())
                .make()
                .load(MethodCallWithTwoExplicitArguments.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, String.class, String.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallWithTwoExplicitArguments instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(MethodCallWithTwoExplicitArguments.class)));
        assertThat(instance, instanceOf(MethodCallWithTwoExplicitArguments.class));
        assertThat(instance.foo(FOO, BAR), is(FOO + BAR));
    }

    @Test
    public void testWithArgumentsAsArray() throws Exception {
        DynamicType.Loaded<ArrayConsuming> loaded = new ByteBuddy()
                .subclass(ArrayConsuming.class)
                .method(named(FOO))
                .intercept(MethodCall.invoke(ArrayConsuming.class.getDeclaredMethod(BAR, String[].class)).withArgumentArray())
                .make()
                .load(ArrayConsuming.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, String.class, String.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        ArrayConsuming instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(ArrayConsuming.class)));
        assertThat(instance, instanceOf(ArrayConsuming.class));
        assertThat(instance.foo(FOO, BAR), is(FOO + BAR));
    }

    @Test
    public void testWithArgumentsFromArray() throws Exception {
        DynamicType.Loaded<MethodCallWithExplicitArgument> loaded = new ByteBuddy()
                .subclass(MethodCallWithExplicitArgument.class)
                .implement(MethodCallDelegator.class)
                .intercept(MethodCall.invoke(MethodCallWithExplicitArgument.class.getDeclaredMethod("foo", String.class)).withArgumentArrayElements(0, 1))
                .make()
                .load(MethodCallDelegator.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(INVOKE_FOO, String[].class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallDelegator instance = (MethodCallDelegator) loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(MethodCallWithExplicitArgument.class)));
        assertThat(instance, instanceOf(MethodCallDelegator.class));
        assertThat(instance.invokeFoo(BAR), is(BAR));
    }

    @Test
    public void testWithArgumentsFromArrayComplete() throws Exception {
        DynamicType.Loaded<MethodCallWithExplicitArgument> loaded = new ByteBuddy()
                .subclass(MethodCallWithExplicitArgument.class)
                .implement(MethodCallDelegator.class)
                .intercept(MethodCall.invoke(MethodCallWithExplicitArgument.class.getDeclaredMethod("foo", String.class)).withArgumentArrayElements(0))
                .make()
                .load(MethodCallDelegator.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(INVOKE_FOO, String[].class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallDelegator instance = (MethodCallDelegator) loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(MethodCallWithExplicitArgument.class)));
        assertThat(instance, instanceOf(MethodCallDelegator.class));
        assertThat(instance.invokeFoo(BAR), is(BAR));
    }

    @Test
    public void testWithArgumentsFromArrayExplicitSize() throws Exception {
        DynamicType.Loaded<MethodCallWithExplicitArgument> loaded = new ByteBuddy()
                .subclass(MethodCallWithExplicitArgument.class)
                .implement(MethodCallDelegator.class)
                .intercept(MethodCall.invoke(MethodCallWithExplicitArgument.class.getDeclaredMethod("foo", String.class)).withArgumentArrayElements(0, 1, 1))
                .make()
                .load(MethodCallDelegator.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(INVOKE_FOO, String[].class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallDelegator instance = (MethodCallDelegator) loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(MethodCallWithExplicitArgument.class)));
        assertThat(instance, instanceOf(MethodCallDelegator.class));
        assertThat(instance.invokeFoo(FOO, BAR, FOO), is(BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testWithArgumentsFromArrayDoesNotExist() throws Exception {
        new ByteBuddy()
                .subclass(MethodCallWithExplicitArgument.class)
                .implement(MethodCallDelegator.class)
                .intercept(MethodCall.invoke(MethodCallWithExplicitArgument.class.getDeclaredMethod("foo", String.class)).withArgumentArrayElements(1, 1))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testWithArgumentsFromArrayDoesNotExistCompleteArray() throws Exception {
        new ByteBuddy()
                .subclass(MethodCallWithExplicitArgument.class)
                .implement(MethodCallDelegator.class)
                .intercept(MethodCall.invoke(MethodCallWithExplicitArgument.class.getDeclaredMethod("foo", String.class)).withArgumentArrayElements(1))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testWithArgumentsFromArrayIllegalType() throws Exception {
        new ByteBuddy()
                .subclass(MethodCallWithExplicitArgument.class)
                .implement(IllegalMethodCallDelegator.class)
                .intercept(MethodCall.invoke(MethodCallWithExplicitArgument.class.getDeclaredMethod("foo", String.class)).withArgumentArrayElements(0, 1))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testWithArgumentsFromArrayIllegalTypeCompleteArray() throws Exception {
        new ByteBuddy()
                .subclass(MethodCallWithExplicitArgument.class)
                .implement(IllegalMethodCallDelegator.class)
                .intercept(MethodCall.invoke(MethodCallWithExplicitArgument.class.getDeclaredMethod("foo", String.class)).withArgumentArrayElements(0))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeIndex() throws Exception {
        MethodCall.invoke(MethodCallWithExplicitArgument.class.getDeclaredMethod("foo", String.class)).withArgumentArrayElements(-1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeIndexComplete() throws Exception {
        MethodCall.invoke(MethodCallWithExplicitArgument.class.getDeclaredMethod("foo", String.class)).withArgumentArrayElements(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeStartIndex() throws Exception {
        MethodCall.invoke(MethodCallWithExplicitArgument.class.getDeclaredMethod("foo", String.class)).withArgumentArrayElements(0, -1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeSize() throws Exception {
        MethodCall.invoke(MethodCallWithExplicitArgument.class.getDeclaredMethod("foo", String.class)).withArgumentArrayElements(0, 1, -1);
    }

    @Test
    public void testSameSize() throws Exception {
        MethodCall methodCall = MethodCall.invoke(MethodCallWithExplicitArgument.class.getDeclaredMethod("foo", String.class));
        assertThat(methodCall.withArgumentArrayElements(0, 0), sameInstance(methodCall));
    }

    @Test(expected = IllegalStateException.class)
    public void testWithTooBigParameter() throws Exception {
        new ByteBuddy()
                .subclass(MethodCallWithExplicitArgument.class)
                .method(isDeclaredBy(MethodCallWithExplicitArgument.class))
                .intercept(MethodCall.invokeSuper().withArgument(1))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testWithParameterNonAssignable() throws Exception {
        new ByteBuddy()
                .subclass(MethodCallWithExplicitArgument.class)
                .method(isDeclaredBy(MethodCallWithExplicitArgument.class))
                .intercept(MethodCall.invokeSuper().withArgument(0).withAssigner(nonAssigner, Assigner.Typing.STATIC))
                .make();
    }

    @Test
    public void testWithField() throws Exception {
        DynamicType.Loaded<MethodCallWithField> loaded = new ByteBuddy()
                .subclass(MethodCallWithField.class)
                .method(isDeclaredBy(MethodCallWithField.class))
                .intercept(MethodCall.invokeSuper().withField(FOO))
                .make()
                .load(MethodCallWithField.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, String.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallWithField instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo = FOO;
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(MethodCallWithField.class)));
        assertThat(instance, instanceOf(MethodCallWithField.class));
        assertThat(instance.foo(BAR), is(FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testWithFieldNotExist() throws Exception {
        new ByteBuddy()
                .subclass(MethodCallWithField.class)
                .method(isDeclaredBy(MethodCallWithField.class))
                .intercept(MethodCall.invokeSuper().withField(BAR))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testWithFieldNonAssignable() throws Exception {
        new ByteBuddy()
                .subclass(MethodCallWithField.class)
                .method(isDeclaredBy(MethodCallWithField.class))
                .intercept(MethodCall.invokeSuper().withField(FOO).withAssigner(nonAssigner, Assigner.Typing.STATIC))
                .make();
    }

    @Test
    public void testWithFieldHierarchyVisibility() throws Exception {
        DynamicType.Loaded<InvisibleMethodCallWithField> loaded = new ByteBuddy()
                .subclass(InvisibleMethodCallWithField.class)
                .method(isDeclaredBy(InvisibleMethodCallWithField.class))
                .intercept(MethodCall.invokeSuper().withField(FOO))
                .make()
                .load(InvisibleMethodCallWithField.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, String.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        InvisibleMethodCallWithField instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        ((InvisibleBase) instance).foo = FOO;
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(InvisibleMethodCallWithField.class)));
        assertThat(instance, instanceOf(InvisibleMethodCallWithField.class));
        assertThat(instance.foo(BAR), is(FOO));
    }

    @Test
    public void testWithThis() throws Exception {
        DynamicType.Loaded<MethodCallWithThis> loaded = new ByteBuddy()
                .subclass(MethodCallWithThis.class)
                .method(isDeclaredBy(MethodCallWithThis.class))
                .intercept(MethodCall.invokeSuper().withThis())
                .make()
                .load(MethodCallWithThis.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, MethodCallWithThis.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallWithThis instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(MethodCallWithThis.class)));
        assertThat(instance, instanceOf(MethodCallWithThis.class));
        assertThat(instance.foo(null), is(instance));
    }

    @Test
    public void testWithOwnType() throws Exception {
        DynamicType.Loaded<MethodCallWithOwnType> loaded = new ByteBuddy()
                .subclass(MethodCallWithOwnType.class)
                .method(isDeclaredBy(MethodCallWithOwnType.class))
                .intercept(MethodCall.invokeSuper().withOwnType())
                .make()
                .load(MethodCallWithOwnType.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, Class.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallWithOwnType instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(MethodCallWithThis.class)));
        assertThat(instance, instanceOf(MethodCallWithOwnType.class));
        assertThat(instance.foo(null), CoreMatchers.<Class<?>>is(loaded.getLoaded()));
    }

    @Test
    public void testInvokeWithMethodCall() throws Exception {
        DynamicType.Loaded<MethodCallChaining> loaded = new ByteBuddy()
                .subclass(MethodCallChaining.class)
                .method(named("foobar"))
                .intercept(MethodCall.invoke(named("foo")).withMethodCall(MethodCall.invoke(named("bar"))))
                .make()
                .load(MethodCallChaining.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        MethodCallChaining instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod("foobar"), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        assertThat(instance.foobar(), is("foobar"));
    }

    @Test(expected = IllegalStateException.class)
    public void testStaticInvokeWithMethodCall() throws Exception {
        new ByteBuddy()
                .subclass(MethodCallChaining.class)
                .defineMethod("staticFoobar", String.class, Ownership.STATIC)
                .intercept(MethodCall.invoke(named("foo")).withMethodCall(MethodCall.invoke(named("bar"))))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testInvokeWithIncompatibleMethodCall() throws Exception {
        new ByteBuddy()
                .subclass(MethodCallChaining.class)
                .method(named("foobar"))
                .intercept(MethodCall.invoke(named("foo")).withMethodCall(MethodCall.invoke(named("someInt"))))
                .make();
    }

    @Test
    public void testImplementationAppendingMethod() throws Exception {
        DynamicType.Loaded<MethodCallAppending> loaded = new ByteBuddy()
                .subclass(MethodCallAppending.class)
                .method(isDeclaredBy(MethodCallAppending.class))
                .intercept(MethodCall.invokeSuper().andThen(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE)))
                .make()
                .load(MethodCallAppending.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallAppending instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(MethodCallAppending.class)));
        assertThat(instance, instanceOf(MethodCallAppending.class));
        assertThat(instance.foo(), is((Object) FOO));
        instance.assertOnlyCall(FOO);
    }

    @Test
    public void testImplementationAppendingConstructor() throws Exception {
        DynamicType.Loaded<MethodCallAppending> loaded = new ByteBuddy()
                .subclass(MethodCallAppending.class)
                .method(isDeclaredBy(MethodCallAppending.class))
                .intercept(MethodCall.construct(Object.class.getDeclaredConstructor())
                        .andThen(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE)))
                .make()
                .load(MethodCallAppending.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        MethodCallAppending instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(MethodCallAppending.class)));
        assertThat(instance, instanceOf(MethodCallAppending.class));
        assertThat(instance.foo(), is((Object) FOO));
        instance.assertZeroCalls();
    }

    @Test
    public void testWithExplicitTarget() throws Exception {
        Object target = new Object();
        DynamicType.Loaded<ExplicitTarget> loaded = new ByteBuddy()
                .subclass(ExplicitTarget.class)
                .method(isDeclaredBy(ExplicitTarget.class))
                .intercept(MethodCall.invoke(Object.class.getDeclaredMethod("toString")).on(target))
                .make()
                .load(ExplicitTarget.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        ExplicitTarget instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(ExplicitTarget.class)));
        assertThat(instance, instanceOf(ExplicitTarget.class));
        assertThat(instance.foo(), is(target.toString()));
    }

    @Test
    public void testWithFieldTarget() throws Exception {
        Object target = new Object();
        DynamicType.Loaded<ExplicitTarget> loaded = new ByteBuddy()
                .subclass(ExplicitTarget.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isDeclaredBy(ExplicitTarget.class))
                .intercept(MethodCall.invoke(Object.class.getDeclaredMethod("toString")).onField(FOO))
                .make()
                .load(ExplicitTarget.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        ExplicitTarget instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        loaded.getLoaded().getDeclaredField(FOO).set(instance, target);
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(ExplicitTarget.class)));
        assertThat(instance, instanceOf(ExplicitTarget.class));
        assertThat(instance.foo(), is(target.toString()));
    }

    @Test
    public void testUnloadedType() throws Exception {
        DynamicType.Loaded<SimpleMethod> loaded = new ByteBuddy()
                .subclass(SimpleMethod.class)
                .method(named(FOO))
                .intercept(MethodCall.invoke(Foo.class.getDeclaredMethod(BAR, Object.class, Object.class)).with(TypeDescription.ForLoadedType.of(Object.class), TypeDescription.ForLoadedType.of(String.class)))
                .make()
                .load(SimpleMethod.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        SimpleMethod instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), is("" + Object.class + String.class));
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(SimpleMethod.class)));
        assertThat(instance, instanceOf(SimpleMethod.class));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, atMost = 7, j9 = false)
    public void testJava7Types() throws Exception {
        DynamicType.Loaded<SimpleMethod> loaded = new ByteBuddy()
                .subclass(SimpleMethod.class)
                .method(named(FOO))
                .intercept(MethodCall.invoke(Foo.class.getDeclaredMethod(BAR, Object.class, Object.class)).with(makeMethodHandle(), makeMethodType(void.class)))
                .make()
                .load(SimpleMethod.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        SimpleMethod instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), is("" + makeMethodHandle() + makeMethodType(void.class)));
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(SimpleMethod.class)));
        assertThat(instance, instanceOf(SimpleMethod.class));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, atMost = 7, j9 = false)
    public void testJava7TypesExplicit() throws Exception {
        DynamicType.Loaded<SimpleMethod> loaded = new ByteBuddy()
                .subclass(SimpleMethod.class)
                .method(named(FOO))
                .intercept(MethodCall.invoke(Foo.class.getDeclaredMethod(BAR, Object.class, Object.class))
                        .with(JavaConstant.MethodHandle.ofLoaded(makeMethodHandle()), JavaConstant.MethodType.ofLoaded(makeMethodType(void.class))))
                .make()
                .load(SimpleMethod.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        SimpleMethod instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), is("" + makeMethodHandle() + makeMethodType(void.class)));
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(SimpleMethod.class)));
        assertThat(instance, instanceOf(SimpleMethod.class));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethod() throws Exception {
        DynamicType.Loaded<Object> loaded = new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(SINGLE_DEFAULT_METHOD))
                .method(ElementMatchers.not(isDeclaredBy(Object.class)))
                .intercept(MethodCall.invoke(Class.forName(SINGLE_DEFAULT_METHOD).getDeclaredMethod(FOO)).onDefault())
                .make()
                .load(Class.forName(SINGLE_DEFAULT_METHOD).getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Method method = loaded.getLoaded().getDeclaredMethod(FOO);
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test
    public void testCallable() throws Exception {
        Traceable traceable = new Traceable();
        DynamicType.Loaded<SimpleStringMethod> loaded = new ByteBuddy()
                .subclass(SimpleStringMethod.class)
                .method(isDeclaredBy(SimpleStringMethod.class))
                .intercept(MethodCall.call(traceable))
                .make()
                .load(SimpleStringMethod.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoaded().getDeclaredConstructor().newInstance().foo(), is(FOO));
        traceable.assertOnlyCall(FOO);
    }

    @Test
    public void testRunnable() throws Exception {
        Traceable traceable = new Traceable();
        DynamicType.Loaded<SimpleStringMethod> loaded = new ByteBuddy()
                .subclass(SimpleStringMethod.class)
                .method(isDeclaredBy(SimpleStringMethod.class))
                .intercept(MethodCall.run(traceable))
                .make()
                .load(SimpleStringMethod.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoaded().getDeclaredConstructor().newInstance().foo(), nullValue(String.class));
        traceable.assertOnlyCall(FOO);
    }

    @Test
    public void testFieldSetting() throws Exception {
        DynamicType.Loaded<FieldSetting> loaded = new ByteBuddy()
                .subclass(FieldSetting.class)
                .method(named(FOO))
                .intercept(MethodCall.invoke(FieldSetting.class.getMethod(BAR)).setsField(FieldSetting.class.getField(FOO)))
                .make()
                .load(FieldSetting.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        FieldSetting instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo();
        assertThat(instance.foo, is((Object) instance.bar()));
    }

    @Test
    public void testFieldSettingWithMatcher() throws Exception {
        DynamicType.Loaded<FieldSetting> loaded = new ByteBuddy()
                .subclass(FieldSetting.class)
                .method(named(FOO))
                .intercept(MethodCall.invoke(FieldSetting.class.getMethod(BAR)).setsField(ElementMatchers.is(FieldSetting.class.getField(FOO))))
                .make()
                .load(FieldSetting.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        FieldSetting instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo();
        assertThat(instance.foo, is((Object) instance.bar()));
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldSettingLast() throws Exception {
        new ByteBuddy()
                .subclass(FieldSetting.class)
                .method(named(QUX))
                .intercept(MethodCall.invoke(FieldSetting.class.getMethod(BAR)).setsField(FieldSetting.class.getField(FOO)))
                .make();
    }

    @Test
    public void testFieldSettingLastChained() throws Exception {
        DynamicType.Loaded<FieldSetting> loaded = new ByteBuddy()
                .subclass(FieldSetting.class)
                .method(named(QUX))
                .intercept(MethodCall.invoke(FieldSetting.class.getMethod(BAR))
                        .setsField(ElementMatchers.is(FieldSetting.class.getField(FOO)))
                        .andThen(StubMethod.INSTANCE))
                .make()
                .load(FieldSetting.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        FieldSetting instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.qux(), nullValue(Object.class));
        assertThat(instance.foo, is((Object) instance.bar()));
    }

    @Test
    public void testConstructorIsAccessibleFromDifferentPackage() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(ProtectedConstructor.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name("foo.Bar")
                .defineConstructor(Visibility.PUBLIC)
                .intercept(MethodCall.invoke(ProtectedConstructor.class.getDeclaredConstructor()).onSuper())
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getConstructor()
                .newInstance(), instanceOf(ProtectedConstructor.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testDefaultMethodNotCompatible() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .method(isDeclaredBy(Object.class))
                .intercept(MethodCall.invoke(String.class.getDeclaredMethod("toString")).onDefault())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodTypeIncompatible() throws Exception {
        new ByteBuddy()
                .subclass(InstanceMethod.class)
                .method(isDeclaredBy(InstanceMethod.class))
                .intercept(MethodCall.invoke(String.class.getDeclaredMethod("toLowerCase")))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testArgumentIncompatibleTooFew() throws Exception {
        new ByteBuddy()
                .subclass(InstanceMethod.class)
                .method(named(FOO))
                .intercept(MethodCall.invoke(StaticIncompatibleExternalMethod.class.getDeclaredMethod("bar", String.class)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testArgumentIncompatibleTooMany() throws Exception {
        new ByteBuddy()
                .subclass(InstanceMethod.class)
                .method(named(FOO))
                .intercept(MethodCall.invoke(StaticIncompatibleExternalMethod.class.getDeclaredMethod("bar", String.class)).with(FOO, BAR))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testArgumentIncompatibleNotAssignable() throws Exception {
        new ByteBuddy()
                .subclass(InstanceMethod.class)
                .method(named(FOO))
                .intercept(MethodCall.invoke(StaticIncompatibleExternalMethod.class.getDeclaredMethod("bar", String.class)).with(new Object()))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructNonConstructorThrowsException() throws Exception {
        MethodCall.construct(mock(MethodDescription.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalIndex() throws Exception {
        MethodCall.invokeSuper().withArgument(-1);
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodCallNonVirtual() throws Exception {
        new ByteBuddy()
                .subclass(InstanceMethod.class)
                .method(named(FOO))
                .intercept(MethodCall.invoke(StaticIncompatibleExternalMethod.class.getDeclaredMethod("bar", String.class)).on(new StaticIncompatibleExternalMethod()).with(FOO))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodCallIncompatibleInstance() throws Exception {
        new ByteBuddy()
                .subclass(InstanceMethod.class)
                .method(named(FOO))
                .intercept(MethodCall.invoke(StaticIncompatibleExternalMethod.class.getDeclaredMethod("bar", String.class)).on(new Object()).with(FOO))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodCallNonVisibleType() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .method(isDeclaredBy(Object.class))
                .intercept(MethodCall.invoke(PackagePrivateType.class.getDeclaredMethod("foo")).on(new PackagePrivateType()))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodCallStaticTargetNonVisibleType() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .method(isDeclaredBy(Object.class))
                .intercept(MethodCall.invoke(PackagePrivateType.class.getDeclaredMethod("bar")))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodCallSuperCallNonInvokable() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .method(isDeclaredBy(Object.class))
                .intercept(MethodCall.invoke(Bar.class.getDeclaredMethod("bar")).onSuper())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodCallDefaultCallNonInvokable() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .method(isDeclaredBy(Object.class))
                .intercept(MethodCall.invoke(Bar.class.getDeclaredMethod("bar")).onDefault())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodCallFieldDoesNotExist() throws Exception {
        new ByteBuddy()
                .subclass(ExplicitTarget.class)
                .method(isDeclaredBy(ExplicitTarget.class))
                .intercept(MethodCall.invoke(Object.class.getDeclaredMethod("toString")).onField(FOO))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodCallIsNotAccessible() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class)
                .method(isDeclaredBy(Object.class))
                .intercept(MethodCall.invokeSelf().onField(FOO).withAllArguments())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testInstanceCallFromStaticMethod() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, void.class)
                .intercept(StubMethod.INSTANCE)
                .defineMethod(BAR, void.class, Ownership.STATIC)
                .intercept(MethodCall.invoke(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testCallConstructorFromMethod() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, void.class)
                .intercept(MethodCall.invoke(Object.class.getConstructor()))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testConstructorCallFromNonRelatedConstructor() throws Exception {
        new ByteBuddy()
                .subclass(Number.class)
                .constructor(ElementMatchers.any())
                .intercept(MethodCall.invoke(Object.class.getConstructor()))
                .make();
    }

    @Test
    public void testConstructorCallResultIntoMethod() throws Exception {
        DynamicType.Loaded<ConstructorResult> loaded = new ByteBuddy()
                .subclass(ConstructorResult.class)
                .method(ElementMatchers.named(FOO))
                .intercept(MethodCall.invoke(ElementMatchers.named("method"))
                        .withMethodCall(MethodCall.construct(ConstructorResult.Target.class.getConstructor())))
                .make()
                .load(ConstructorResult.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        ConstructorResult instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(ConstructorResult.class)));
        assertThat(instance, instanceOf(ConstructorResult.class));
        assertThat(instance.foo(), instanceOf(ConstructorResult.Target.class));
    }

    @Test
    public void testConstructorCallResultIntoField() throws Exception {
        DynamicType.Loaded<ConstructorResult> loaded = new ByteBuddy()
                .subclass(ConstructorResult.class)
                .method(ElementMatchers.named(FOO))
                .intercept(MethodCall.construct(ConstructorResult.Target.class.getConstructor())
                        .setsField(ElementMatchers.named("field"))
                        .andThen(FieldAccessor.ofField("field")))
                .make()
                .load(ConstructorResult.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        ConstructorResult instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(ConstructorResult.class)));
        assertThat(instance, instanceOf(ConstructorResult.class));
        ConstructorResult.Target foo = instance.foo();
        assertThat(foo, instanceOf(ConstructorResult.Target.class));
        assertThat(instance.field, equalTo(foo));
    }

    public static class ConstructorResult {

        public Target field;

        public Target method(Target args) {
            return args;
        }

        public Target foo() {
            return null;
        }

        public static class Target {

        }
    }

    public static class SimpleMethod {

        public String foo() {
            return null;
        }

        public String bar() {
            return BAR;
        }
    }

    public static class StaticExternalMethod {

        public static String bar() {
            return BAR;
        }
    }

    public static class InstanceMethod {

        public String foo() {
            return null;
        }

        public String bar() {
            return BAR;
        }
    }

    public abstract static class ArgumentCall {

        public abstract String foo(Target target);

        public static class Target {

            private final String value;

            public Target(String value) {
                this.value = value;
            }

            public String foo() {
                return value;
            }
        }
    }

    public abstract static class ArgumentCallDynamic {

        public abstract String foo(Object target);

        public static class Target {

            private final String value;

            public Target(String value) {
                this.value = value;
            }

            public String foo() {
                return value;
            }
        }
    }

    public static class SelfReference {

        public SelfReference foo() {
            return null;
        }
    }

    public static class SuperMethodInvocation {

        public String foo() {
            return FOO;
        }
    }

    public static class MethodCallWithExplicitArgument {

        public String foo(String value) {
            return value;
        }
    }

    public static class MethodCallWithTwoExplicitArguments {

        public String foo(String first, String second) {
            return first + second;
        }
    }

    public interface MethodCallDelegator {

        String invokeFoo(String... argument);
    }

    public interface IllegalMethodCallDelegator {

        String invokeFoo(String argument);
    }

    @SuppressWarnings("unused")
    public static class MethodCallWithField {

        public String foo;

        public String foo(String value) {
            return value;
        }
    }

    public static class MethodCallOnField {

        public String foo;

        public String foo() {
            return foo;
        }
    }

    @SuppressWarnings("unused")
    public static class InvisibleMethodCallWithField extends InvisibleBase {

        private String foo;

        public String foo(String value) {
            return value;
        }
    }

    public static class InvisibleBase {

        public String foo;
    }

    public static class MethodCallWithThis {

        public MethodCallWithThis foo(MethodCallWithThis value) {
            return value;
        }
    }

    public static class MethodCallWithOwnType {

        public Class<?> foo(Class<?> value) {
            return value;
        }
    }

    public static abstract class MethodCallChaining {

        public String foo(String bar) {
            return "foo" + bar;
        }

        public String bar() {
            return "bar";
        }

        public abstract String foobar();

        public int someInt() {
            return 0xCAFEBABE;
        }
    }

    public static class MethodCallAppending extends CallTraceable {

        public Object foo() {
            register(FOO);
            return null;
        }
    }

    public static class ExplicitTarget {

        public String foo() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static class StaticIncompatibleExternalMethod {

        public static String bar(String value) {
            return null;
        }
    }

    public static class Foo {

        public static String bar(Object arg0, Object arg1) {
            return "" + arg0 + arg1;
        }
    }

    static class PackagePrivateType {

        public String foo() {
            return FOO;
        }

        public static String bar() {
            return BAR;
        }
    }

    public static class Bar {

        public void bar() {
            /* empty */
        }
    }

    public static class SimpleStringMethod {

        public String foo() {
            return null;
        }
    }

    public static class Traceable extends CallTraceable implements Runnable, Callable<String> {

        public String call() throws Exception {
            register(FOO);
            return FOO;
        }

        public void run() {
            register(FOO);
        }
    }

    public static class NonVirtual {

        public static void foo() {
            /* empty */
        }
    }

    public abstract static class ArrayConsuming {

        public abstract String foo(String arg1, String arg2);

        public String bar(String[] arg) {
            return arg[0] + arg[1];
        }
    }

    public static class FieldSetting {

        public String foo;

        public void foo() {
            throw new AssertionError();
        }

        public String bar() {
            return FOO;
        }

        public String qux() {
            throw new AssertionError();
        }
    }

    public static class ProtectedConstructor {

        protected ProtectedConstructor() {
            /* empty */
        }
    }
}
