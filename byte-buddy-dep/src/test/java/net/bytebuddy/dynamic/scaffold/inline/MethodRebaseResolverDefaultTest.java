package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class MethodRebaseResolverDefaultTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodMatcher methodMatcher;
    @Mock
    private TypeDescription instrumentedType, placeholderType, returnType;
    @Mock
    private MethodRebaseResolver.MethodNameTransformer methodNameTransformer, otherMethodNameTransformer;
    @Mock
    private MethodDescription methodDescription;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getDeclaringType()).thenReturn(instrumentedType);
        when(methodDescription.getParameterTypes()).thenReturn(new TypeList.Empty());
        when(methodDescription.getReturnType()).thenReturn(returnType);
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(methodNameTransformer.transform(FOO)).thenReturn(BAR);
    }

    @Test
    public void testNonIgnoredMethodIsRebased() throws Exception {
        MethodRebaseResolver.Resolution resolution = new MethodRebaseResolver.Default(methodMatcher, placeholderType, methodNameTransformer)
                .resolve(methodDescription);
        assertThat(resolution.isRebased(), is(true));
        assertThat(resolution.getResolvedMethod().getInternalName(), is(BAR));
        assertThat(resolution.getResolvedMethod().getModifiers(), is(MethodRebaseResolver.REBASED_METHOD_MODIFIER));
        assertThat(resolution.getResolvedMethod().getDeclaringType(), is(instrumentedType));
        assertThat(resolution.getResolvedMethod().getParameterTypes(), is((TypeList) new TypeList.Empty()));
        assertThat(resolution.getResolvedMethod().getReturnType(), is(returnType));
        assertThat(resolution.getAdditionalArguments().isValid(), is(true));
        StackManipulation.Size size = resolution.getAdditionalArguments().apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testNonIgnoredConstructorIsRebased() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        MethodRebaseResolver.Resolution resolution = new MethodRebaseResolver.Default(methodMatcher, placeholderType, methodNameTransformer)
                .resolve(methodDescription);
        assertThat(resolution.isRebased(), is(true));
        assertThat(resolution.getResolvedMethod().getInternalName(), is(FOO));
        assertThat(resolution.getResolvedMethod().getModifiers(), is(MethodRebaseResolver.REBASED_METHOD_MODIFIER));
        assertThat(resolution.getResolvedMethod().getDeclaringType(), is(instrumentedType));
        assertThat(resolution.getResolvedMethod().getParameterTypes(), is((TypeList) new TypeList.Explicit(Arrays.asList(placeholderType))));
        assertThat(resolution.getResolvedMethod().getReturnType(), is(returnType));
        assertThat(resolution.getAdditionalArguments().isValid(), is(true));
        StackManipulation.Size size = resolution.getAdditionalArguments().apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.ACONST_NULL);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testIgnoredMethodIsNotRebased() throws Exception {
        when(methodMatcher.matches(methodDescription)).thenReturn(true);
        MethodRebaseResolver.Resolution resolution = new MethodRebaseResolver.Default(methodMatcher, placeholderType, methodNameTransformer)
                .resolve(methodDescription);
        assertThat(resolution.isRebased(), is(false));
        assertThat(resolution.getResolvedMethod(), is(methodDescription));
        try {
            resolution.getAdditionalArguments();
            fail();
        } catch (IllegalStateException ignored) {
            // expected
        }
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(new MethodRebaseResolver.Default(methodMatcher, placeholderType, methodNameTransformer).hashCode(),
                is(new MethodRebaseResolver.Default(methodMatcher, placeholderType, methodNameTransformer).hashCode()));
        assertThat(new MethodRebaseResolver.Default(methodMatcher, placeholderType, methodNameTransformer),
                is(new MethodRebaseResolver.Default(methodMatcher, placeholderType, methodNameTransformer)));
        assertThat(new MethodRebaseResolver.Default(methodMatcher, placeholderType, methodNameTransformer).hashCode(),
                not(is(new MethodRebaseResolver.Default(methodMatcher, placeholderType, otherMethodNameTransformer).hashCode())));
        assertThat(new MethodRebaseResolver.Default(methodMatcher, placeholderType, methodNameTransformer),
                not(is(new MethodRebaseResolver.Default(methodMatcher, placeholderType, otherMethodNameTransformer))));
    }
}
