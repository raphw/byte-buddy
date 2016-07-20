package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MethodOverrideMatcherTest extends AbstractElementMatcherTest<MethodOverrideMatcher<?>> {

    @Mock
    private ElementMatcher<? super TypeDescription.Generic> typeMatcher;

    @Mock
    private TypeDescription.Generic declaringType, superType, interfaceType;

    @Mock
    private TypeDescription rawDeclaringType, rawSuperType, rawInterfaceType;

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private MethodDescription.InGenericShape declaredTypeMethod, superTypeMethod, interfaceTypeMethod;

    @Mock
    private MethodDescription.SignatureToken token, otherToken;

    @SuppressWarnings("unchecked")
    public MethodOverrideMatcherTest() {
        super((Class<? extends MethodOverrideMatcher<?>>) (Object) MethodOverrideMatcher.class, "returns");
    }

    @Before
    public void setUp() throws Exception {
        when(declaredTypeMethod.isVirtual()).thenReturn(true);
        when(superTypeMethod.isVirtual()).thenReturn(true);
        when(interfaceTypeMethod.isVirtual()).thenReturn(true);
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(methodDescription.asSignatureToken()).thenReturn(token);
        when(declaringType.asGenericType()).thenReturn(declaringType);
        when(superType.asGenericType()).thenReturn(superType);
        when(interfaceType.asGenericType()).thenReturn(interfaceType);
        when(declaringType.asErasure()).thenReturn(rawDeclaringType);
        when(superType.asErasure()).thenReturn(rawSuperType);
        when(interfaceType.asErasure()).thenReturn(rawInterfaceType);
        when(declaringType.iterator()).thenReturn(Arrays.<TypeDefinition>asList(declaringType, superType).iterator());
        when(declaringType.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(interfaceType));
        when(superType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(interfaceType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
        when(declaringType.getDeclaredMethods()).thenReturn(new MethodList.Explicit<MethodDescription.InGenericShape>(declaredTypeMethod));
        when(superType.getDeclaredMethods()).thenReturn(new MethodList.Explicit<MethodDescription.InGenericShape>(superTypeMethod));
        when(interfaceType.getDeclaredMethods()).thenReturn(new MethodList.Explicit<MethodDescription.InGenericShape>(interfaceTypeMethod));
    }

    @Test
    public void testDirectMatch() throws Exception {
        when(declaredTypeMethod.asSignatureToken()).thenReturn(token);
        when(typeMatcher.matches(declaringType)).thenReturn(true);
        assertThat(new MethodOverrideMatcher<MethodDescription>(typeMatcher).matches(methodDescription), is(true));
        verify(typeMatcher).matches(declaringType);
        verifyNoMoreInteractions(typeMatcher);
    }

    @Test
    public void testSuperTypeMatch() throws Exception {
        when(declaredTypeMethod.asSignatureToken()).thenReturn(otherToken);
        when(interfaceTypeMethod.asSignatureToken()).thenReturn(otherToken);
        when(superTypeMethod.asSignatureToken()).thenReturn(token);
        when(typeMatcher.matches(superType)).thenReturn(true);
        assertThat(new MethodOverrideMatcher<MethodDescription>(typeMatcher).matches(methodDescription), is(true));
        verify(typeMatcher).matches(superType);
        verifyNoMoreInteractions(typeMatcher);
    }

    @Test
    public void testInterfaceTypeMatch() throws Exception {
        when(declaredTypeMethod.asSignatureToken()).thenReturn(otherToken);
        when(superTypeMethod.asSignatureToken()).thenReturn(otherToken);
        when(interfaceTypeMethod.asSignatureToken()).thenReturn(token);
        when(typeMatcher.matches(interfaceType)).thenReturn(true);
        assertThat(new MethodOverrideMatcher<MethodDescription>(typeMatcher).matches(methodDescription), is(true));
        verify(typeMatcher).matches(interfaceType);
        verifyNoMoreInteractions(typeMatcher);
    }

    @Test
    public void testNoMatchMatcher() throws Exception {
        when(declaredTypeMethod.asSignatureToken()).thenReturn(token);
        when(superTypeMethod.asSignatureToken()).thenReturn(token);
        when(interfaceTypeMethod.asSignatureToken()).thenReturn(token);
        assertThat(new MethodOverrideMatcher<MethodDescription>(typeMatcher).matches(methodDescription), is(false));
        verify(typeMatcher).matches(declaringType);
        verify(typeMatcher).matches(superType);
        verify(typeMatcher).matches(interfaceType);
        verifyNoMoreInteractions(typeMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(declaredTypeMethod.asSignatureToken()).thenReturn(otherToken);
        when(superTypeMethod.asSignatureToken()).thenReturn(otherToken);
        when(interfaceTypeMethod.asSignatureToken()).thenReturn(otherToken);
        assertThat(new MethodOverrideMatcher<MethodDescription>(typeMatcher).matches(methodDescription), is(false));
        verifyZeroInteractions(typeMatcher);
    }
}
