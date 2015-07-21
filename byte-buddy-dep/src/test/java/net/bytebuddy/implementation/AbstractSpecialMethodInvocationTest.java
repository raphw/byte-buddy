package net.bytebuddy.implementation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public abstract class AbstractSpecialMethodInvocationTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription, otherMethod;

    @Mock
    private MethodDescription.Token methodToken, otherToken;

    @Mock
    private TypeDescription typeDescription, otherType;

    @Mock
    private StackManipulation stackManipulation;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.asToken()).thenReturn(methodToken);
        when(otherMethod.asToken()).thenReturn(otherToken);
    }

    protected abstract Implementation.SpecialMethodInvocation make(MethodDescription methodDescription, TypeDescription typeDescription);

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
}
