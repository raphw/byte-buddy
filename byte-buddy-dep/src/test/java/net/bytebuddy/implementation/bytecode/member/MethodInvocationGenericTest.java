package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodInvocationGenericTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription.InDefinedShape declaredMethod;

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private TypeDescription.Generic methodReturnType, declaredReturnType;

    @Mock
    private TypeDescription declaredErasure, declaringType, targetType, otherType;

    @Mock
    private MethodDescription.SignatureToken token;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.asDefined()).thenReturn(declaredMethod);
        when(methodDescription.getReturnType()).thenReturn(methodReturnType);
        when(declaredMethod.getReturnType()).thenReturn(declaredReturnType);
        when(declaredReturnType.asErasure()).thenReturn(declaredErasure);
        when(declaredMethod.getDeclaringType()).thenReturn(declaringType);
        when(declaringType.asErasure()).thenReturn(declaringType);
        when(declaredMethod.asSignatureToken()).thenReturn(token);
        when(declaredMethod.isSpecializableFor(targetType)).thenReturn(true);
        when(declaredMethod.asDefined()).thenReturn(declaredMethod);
    }

    @Test
    public void testGenericMethod() throws Exception {
        TypeDescription genericErasure = mock(TypeDescription.class);
        when(methodReturnType.asErasure()).thenReturn(genericErasure);
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription);
        assertThat(stackManipulation.isValid(), is(true));
        assertThat(stackManipulation, hasPrototype((StackManipulation) new MethodInvocation.OfGenericMethod(genericErasure, MethodInvocation.invoke(declaredMethod))));
    }

    @Test
    public void testGenericMethodErasureEqual() throws Exception {
        when(methodReturnType.asErasure()).thenReturn(declaredErasure);
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription);
        assertThat(stackManipulation.isValid(), is(true));
        assertThat(stackManipulation, hasPrototype((StackManipulation) MethodInvocation.invoke(declaredMethod)));
    }

    @Test
    public void testGenericMethodVirtual() throws Exception {
        TypeDescription genericErasure = mock(TypeDescription.class);
        when(methodReturnType.asErasure()).thenReturn(genericErasure);
        when(genericErasure.asErasure()).thenReturn(genericErasure);
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription).virtual(targetType);
        assertThat(stackManipulation.isValid(), is(true));
        assertThat(stackManipulation, hasPrototype((StackManipulation) new StackManipulation.Compound(MethodInvocation.invoke(declaredMethod).virtual(targetType),
                TypeCasting.to(genericErasure))));
    }

    @Test
    public void testGenericMethodVirtualErasureEqual() throws Exception {
        when(methodReturnType.asErasure()).thenReturn(declaredErasure);
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription).virtual(targetType);
        assertThat(stackManipulation.isValid(), is(true));
        assertThat(stackManipulation, hasPrototype(MethodInvocation.invoke(declaredMethod).virtual(targetType)));
    }

    @Test
    public void testGenericMethodSpecial() throws Exception {
        TypeDescription genericErasure = mock(TypeDescription.class);
        when(methodReturnType.asErasure()).thenReturn(genericErasure);
        when(genericErasure.asErasure()).thenReturn(genericErasure);
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription).special(targetType);
        assertThat(stackManipulation.isValid(), is(true));
        assertThat(stackManipulation, hasPrototype((StackManipulation) new StackManipulation.Compound(MethodInvocation.invoke(declaredMethod).special(targetType),
                TypeCasting.to(genericErasure))));
    }

    @Test
    public void testGenericMethodSpecialErasureEqual() throws Exception {
        when(methodReturnType.asErasure()).thenReturn(declaredErasure);
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription).special(targetType);
        assertThat(stackManipulation.isValid(), is(true));
        assertThat(stackManipulation, hasPrototype(MethodInvocation.invoke(declaredMethod).special(targetType)));
    }

    @Test
    public void testGenericMethodDynamic() throws Exception {
        TypeDescription genericErasure = mock(TypeDescription.class);
        when(methodReturnType.asErasure()).thenReturn(genericErasure);
        when(declaredMethod.isBootstrap()).thenReturn(true);
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription).dynamic(FOO,
                otherType,
                Collections.<TypeDescription>emptyList(),
                Collections.emptyList());
        assertThat(stackManipulation.isValid(), is(true));
        assertThat(stackManipulation, hasPrototype(MethodInvocation.invoke(declaredMethod).dynamic(FOO,
                otherType,
                Collections.<TypeDescription>emptyList(),
                Collections.emptyList())));
    }

    @Test
    public void testGenericMethodDynamicErasureEqual() throws Exception {
        when(methodReturnType.asErasure()).thenReturn(declaredErasure);
        when(declaredMethod.isBootstrap()).thenReturn(true);
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription).dynamic(FOO,
                otherType,
                Collections.<TypeDescription>emptyList(),
                Collections.emptyList());
        assertThat(stackManipulation.isValid(), is(true));
        assertThat(stackManipulation, hasPrototype(MethodInvocation.invoke(declaredMethod).dynamic(FOO,
                otherType,
                Collections.<TypeDescription>emptyList(),
                Collections.emptyList())));
    }

    @Test
    public void testIllegal() throws Exception {
        TypeDescription genericErasure = mock(TypeDescription.class);
        when(methodReturnType.asErasure()).thenReturn(genericErasure);
        when(declaredMethod.isTypeInitializer()).thenReturn(true);
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription);
        assertThat(stackManipulation.isValid(), is(false));
    }
}
