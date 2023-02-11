package net.bytebuddy.implementation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaConstantMethodHandleTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public abstract class AbstractSpecialMethodInvocationTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodDescription.InDefinedShape methodDescription, otherMethod;

    @Mock
    private MethodDescription.SignatureToken token, otherToken;

    @Mock
    private TypeDescription typeDescription, otherType;

    @Mock
    private StackManipulation stackManipulation;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.asDefined()).thenReturn(methodDescription);
        when(methodDescription.asSignatureToken()).thenReturn(token);
        when(otherMethod.asSignatureToken()).thenReturn(otherToken);
    }

    protected abstract Implementation.SpecialMethodInvocation make(MethodDescription.InDefinedShape methodDescription, TypeDescription typeDescription);

    @Test
    public void testEquality() throws Exception {
        assertThat(make(methodDescription, typeDescription).hashCode(),
                is(new Implementation.SpecialMethodInvocation.Simple(methodDescription, typeDescription, stackManipulation).hashCode()));
        assertThat(make(methodDescription, typeDescription),
                is((Implementation.SpecialMethodInvocation) new Implementation.SpecialMethodInvocation.Simple(methodDescription,
                        typeDescription,
                        stackManipulation)));
    }

    @Test
    public void testTypeInequality() throws Exception {
        assertThat(make(methodDescription, typeDescription).hashCode(),
                not(new Implementation.SpecialMethodInvocation.Simple(methodDescription, otherType, stackManipulation).hashCode()));
        assertThat(make(methodDescription, typeDescription),
                not((Implementation.SpecialMethodInvocation) new Implementation.SpecialMethodInvocation.Simple(methodDescription,
                        otherType,
                        stackManipulation)));
    }

    @Test
    public void testTokenInequality() throws Exception {
        assertThat(make(methodDescription, typeDescription).hashCode(),
                not(new Implementation.SpecialMethodInvocation.Simple(otherMethod, typeDescription, stackManipulation).hashCode()));
        assertThat(make(methodDescription, typeDescription),
                not((Implementation.SpecialMethodInvocation) new Implementation.SpecialMethodInvocation.Simple(otherMethod,
                        typeDescription,
                        stackManipulation)));
    }

    @Test
    public void testValidity() throws Exception {
        assertThat(make(methodDescription, typeDescription).isValid(), is(true));
    }

    @Test
    public void testMethodHandle() {
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getDeclaringType()).thenReturn(TypeDescription.ForLoadedType.of(Object.class));
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.ForLoadedType.of(void.class).asGenericType());
        when(methodDescription.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InDefinedShape>());
        when(methodDescription.isSpecializableFor(typeDescription)).thenReturn(true);
        assertThat(make(methodDescription, typeDescription).toMethodHandle(), is(JavaConstant.MethodHandle.ofSpecial(methodDescription, typeDescription)));
    }
}
