package net.bytebuddy.implementation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.test.utility.CallTraceable;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SuperMethodCallOtherTest extends AbstractImplementationTest {

    private static final String SINGLE_DEFAULT_METHOD = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";

    private static final String SINGLE_DEFAULT_METHOD_CLASS = "net.bytebuddy.test.precompiled.SingleDefaultMethodClass";

    private static final String CONFLICTING_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodConflictingInterface";

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

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
    private MethodList superClassMethods;

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
        verifyZeroInteractions(instrumentedType);
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
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
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
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, SuperMethodCall.INSTANCE
                .andThen(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE)));
        Foo foo = loaded.getLoaded().getConstructor().newInstance();
        assertThat(foo.foo(), is(FOO));
        foo.assertOnlyCall(FOO);
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testUnambiguousDirectDefaultMethod() throws Exception {
        DynamicType.Loaded<?> loaded = implement(Object.class,
                SuperMethodCall.INSTANCE,
                getClass().getClassLoader(),
                isMethod().and(not(isDeclaredBy(Object.class))),
                Class.forName(SINGLE_DEFAULT_METHOD));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Method method = loaded.getLoaded().getDeclaredMethod(FOO);
        Object instance = loaded.getLoaded().getConstructor().newInstance();
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testInheritedDefaultMethod() throws Exception {
        DynamicType.Loaded<?> loaded = implement(Class.forName(SINGLE_DEFAULT_METHOD_CLASS),
                SuperMethodCall.INSTANCE,
                getClass().getClassLoader(),
                isMethod().and(not(isDeclaredBy(Object.class))));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Method method = loaded.getLoaded().getDeclaredMethod(FOO);
        Object instance = loaded.getLoaded().getConstructor().newInstance();
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(8)
    public void testAmbiguousDirectDefaultMethodThrowsException() throws Exception {
        implement(Object.class,
                SuperMethodCall.INSTANCE,
                getClass().getClassLoader(),
                not(isDeclaredBy(Object.class)),
                Class.forName(SINGLE_DEFAULT_METHOD), Class.forName(CONFLICTING_INTERFACE));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(SuperMethodCall.class).apply();
        ObjectPropertyAssertion.of(SuperMethodCall.WithoutReturn.class).apply();
        ObjectPropertyAssertion.of(SuperMethodCall.Appender.class).apply();
        ObjectPropertyAssertion.of(SuperMethodCall.Appender.TerminationHandler.class).apply();
    }

    public static class Foo extends CallTraceable {

        public String foo() {
            register(FOO);
            return null;
        }
    }
}
