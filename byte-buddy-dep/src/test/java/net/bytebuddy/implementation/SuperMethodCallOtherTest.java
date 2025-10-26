package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.test.utility.CallTraceable;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SuperMethodCallOtherTest {

    private static final String SINGLE_DEFAULT_METHOD = "net.bytebuddy.test.precompiled.v8.SingleDefaultMethodInterface";

    private static final String SINGLE_DEFAULT_METHOD_CLASS = "net.bytebuddy.test.precompiled.v8.SingleDefaultMethodClass";

    private static final String CONFLICTING_INTERFACE = "net.bytebuddy.test.precompiled.v8.SingleDefaultMethodConflictingInterface";

    private static final String FOO = "foo";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Mock
    private InstrumentedType instrumentedType;

    @Mock
    private TypeDescription typeDescription, rawSuperClass, returnType, declaringType;

    @Mock
    private TypeDescription.Generic superClass, genericReturnType;

    @Mock
    private Implementation.Target implementationTarget;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private MethodDescription.SignatureToken token;

    @Mock
    private MethodList<MethodDescription.InDefinedShape> superClassMethods;

    @Before
    public void setUp() throws Exception {
        when(implementationTarget.getInstrumentedType()).thenReturn(typeDescription);
        when(methodDescription.asSignatureToken()).thenReturn(token);
        when(genericReturnType.asErasure()).thenReturn(returnType);
        when(superClass.asErasure()).thenReturn(rawSuperClass);
    }

    @Test
    public void testPreparation() throws Exception {
        assertThat(SuperMethodCall.INSTANCE.prepare(instrumentedType), is(instrumentedType));
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void testConstructor() throws Exception {
        when(typeDescription.getSuperClass()).thenReturn(superClass);
        when(methodDescription.isConstructor()).thenReturn(true);
        when(rawSuperClass.getDeclaredMethods()).thenReturn(superClassMethods);
        when(superClassMethods.filter(any(ElementMatcher.class))).thenReturn(superClassMethods);
        when(implementationTarget.invokeDominant(token)).thenReturn(Implementation.SpecialMethodInvocation.Illegal.INSTANCE);
        SuperMethodCall.INSTANCE.appender(implementationTarget).apply(methodVisitor, implementationContext, methodDescription);
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testStaticMethod() throws Exception {
        when(typeDescription.getSuperClass()).thenReturn(superClass);
        when(methodDescription.isStatic()).thenReturn(true);
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(genericReturnType);
        when(returnType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(rawSuperClass.getDeclaredMethods()).thenReturn(superClassMethods);
        when(superClassMethods.filter(any(ElementMatcher.class))).thenReturn(superClassMethods);
        when(implementationTarget.invokeDominant(token)).thenReturn(Implementation.SpecialMethodInvocation.Illegal.INSTANCE);
        SuperMethodCall.INSTANCE.appender(implementationTarget).apply(methodVisitor, implementationContext, methodDescription);
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testNoSuper() throws Exception {
        when(typeDescription.getSuperClass()).thenReturn(superClass);
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
        when(methodDescription.getReturnType()).thenReturn(genericReturnType);
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(declaringType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(returnType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(rawSuperClass.getDeclaredMethods()).thenReturn(superClassMethods);
        when(superClassMethods.filter(any(ElementMatcher.class))).thenReturn(superClassMethods);
        when(implementationTarget.invokeDominant(token)).thenReturn(Implementation.SpecialMethodInvocation.Illegal.INSTANCE);
        SuperMethodCall.INSTANCE.appender(implementationTarget).apply(methodVisitor, implementationContext, methodDescription);
    }

    @Test
    public void testAndThen() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(SuperMethodCall.INSTANCE.andThen(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Foo foo = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(foo.foo(), is(FOO));
        foo.assertOnlyCall(FOO);
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testUnambiguousDirectDefaultMethod() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(SINGLE_DEFAULT_METHOD))
                .intercept(SuperMethodCall.INSTANCE)
                .make()
                .load(Class.forName(SINGLE_DEFAULT_METHOD).getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Method method = loaded.getLoaded().getDeclaredMethod(FOO);
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testInheritedDefaultMethod() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Class.forName(SINGLE_DEFAULT_METHOD_CLASS))
                .method(not(isDeclaredBy(Object.class)))
                .intercept(SuperMethodCall.INSTANCE)
                .make()
                .load(Class.forName(SINGLE_DEFAULT_METHOD_CLASS).getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Method method = loaded.getLoaded().getDeclaredMethod(FOO);
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(8)
    public void testAmbiguousDirectDefaultMethodThrowsException() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(SINGLE_DEFAULT_METHOD), Class.forName(CONFLICTING_INTERFACE))
                .intercept(SuperMethodCall.INSTANCE)
                .make()
                .load(Class.forName(SINGLE_DEFAULT_METHOD).getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
    }

    public static class Foo extends CallTraceable {

        public String foo() {
            register(FOO);
            return null;
        }
    }
}
