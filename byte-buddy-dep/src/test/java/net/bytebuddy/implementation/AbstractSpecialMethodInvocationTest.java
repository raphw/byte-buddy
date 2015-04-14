package net.bytebuddy.implementation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractSpecialMethodInvocationTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription returnType, parameterType, targetType;

    private TypeList parameterTypes;

    @Before
    public void setUp() throws Exception {
        when(parameterType.getStackSize()).thenReturn(StackSize.ZERO);
        parameterTypes = new TypeList.Explicit(Collections.singletonList(parameterType));
    }

    protected abstract Implementation.SpecialMethodInvocation make(String name,
                                                                    TypeDescription returnType,
                                                                    List<TypeDescription> parameterTypes,
                                                                    TypeDescription targetType);

    @Test
    public void testEquals() throws Exception {
        Implementation.SpecialMethodInvocation identical = make(FOO, returnType, parameterTypes, targetType);
        assertThat(identical, is(identical));
        assertThat(make(FOO, returnType, parameterTypes, targetType), is(make(FOO, returnType, parameterTypes, targetType)));
        Implementation.SpecialMethodInvocation equal = mock(Implementation.SpecialMethodInvocation.class);
        when(equal.getTypeDescription()).thenReturn(targetType);
        MethodDescription equalMethod = mock(MethodDescription.class);
        when(equal.getMethodDescription()).thenReturn(equalMethod);
        when(equalMethod.getInternalName()).thenReturn(FOO);
        when(equalMethod.getReturnType()).thenReturn(returnType);
        ParameterList equalMethodParameters = ParameterList.Explicit.latent(equalMethod, parameterTypes);
        when(equalMethod.getParameters()).thenReturn(equalMethodParameters);
        assertThat(make(FOO, returnType, parameterTypes, targetType), is(equal));
        Implementation.SpecialMethodInvocation equalButType = mock(Implementation.SpecialMethodInvocation.class);
        when(equalButType.getTypeDescription()).thenReturn(mock(TypeDescription.class));
        when(equalButType.getMethodDescription()).thenReturn(equalMethod);
        assertThat(make(FOO, returnType, parameterTypes, targetType), not(is(equalButType)));
        Implementation.SpecialMethodInvocation equalButName = mock(Implementation.SpecialMethodInvocation.class);
        when(equalButName.getTypeDescription()).thenReturn(targetType);
        MethodDescription equalMethodButName = mock(MethodDescription.class);
        when(equalButName.getMethodDescription()).thenReturn(equalMethodButName);
        when(equalMethodButName.getInternalName()).thenReturn(BAR);
        when(equalMethodButName.getReturnType()).thenReturn(returnType);
        ParameterList equalMethodButNameParameters = ParameterList.Explicit.latent(equalMethodButName, parameterTypes);
        when(equalMethodButName.getParameters()).thenReturn(equalMethodButNameParameters);
        assertThat(make(FOO, returnType, parameterTypes, targetType), not(is(equalButName)));
        Implementation.SpecialMethodInvocation equalButReturn = mock(Implementation.SpecialMethodInvocation.class);
        when(equalButName.getTypeDescription()).thenReturn(targetType);
        MethodDescription equalMethodButReturn = mock(MethodDescription.class);
        when(equalButName.getMethodDescription()).thenReturn(equalMethodButReturn);
        when(equalMethodButReturn.getInternalName()).thenReturn(FOO);
        when(equalMethodButReturn.getReturnType()).thenReturn(mock(TypeDescription.class));
        ParameterList equalMethodButReturnParameters = ParameterList.Explicit.latent(equalMethodButReturn, parameterTypes);
        when(equalMethodButReturn.getParameters()).thenReturn(equalMethodButReturnParameters);
        assertThat(make(FOO, returnType, parameterTypes, targetType), not(is(equalButReturn)));
        Implementation.SpecialMethodInvocation equalButParameter = mock(Implementation.SpecialMethodInvocation.class);
        when(equalButParameter.getTypeDescription()).thenReturn(targetType);
        MethodDescription equalMethodButParameter = mock(MethodDescription.class);
        when(equalButParameter.getMethodDescription()).thenReturn(equalMethodButParameter);
        when(equalMethodButParameter.getInternalName()).thenReturn(FOO);
        when(equalMethodButParameter.getReturnType()).thenReturn(returnType);
        TypeDescription parameterType = mock(TypeDescription.class);
        when(parameterType.getStackSize()).thenReturn(StackSize.ZERO);
        ParameterList equalMethodButParameterParameters = ParameterList.Explicit.latent(equalMethodButParameter, Collections.singletonList(parameterType));
        when(equalMethodButParameter.getParameters()).thenReturn(equalMethodButParameterParameters);
        assertThat(make(FOO, returnType, parameterTypes, targetType), not(is(equalButParameter)));
        assertThat(make(FOO, returnType, parameterTypes, targetType), not(is(new Object())));
        assertThat(make(FOO, returnType, parameterTypes, targetType), not(is((Object) null)));
    }
}
