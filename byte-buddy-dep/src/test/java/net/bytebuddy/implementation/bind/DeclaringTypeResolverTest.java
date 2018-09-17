package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class DeclaringTypeResolverTest extends AbstractAmbiguityResolverTest {

    @Mock
    private TypeDescription leftType, rightType;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(leftMethod.getDeclaringType()).thenReturn(leftType);
        when(rightMethod.getDeclaringType()).thenReturn(rightType);
        when(leftType.asErasure()).thenReturn(leftType);
        when(rightType.asErasure()).thenReturn(rightType);
    }

    @Test
    public void testEquals() throws Exception {
        when(leftType.isAssignableFrom(rightType)).thenReturn(true);
        assertThat(DeclaringTypeResolver.INSTANCE.resolve(source, left, left), is(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS));
        verify(leftType, times(2)).asErasure();
        verifyNoMoreInteractions(leftType);
    }

    @Test
    public void testLeftAssignable() throws Exception {
        when(leftType.isAssignableFrom(rightType)).thenReturn(true);
        assertThat(DeclaringTypeResolver.INSTANCE.resolve(source, left, right), is(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT));
        verify(leftType).isAssignableFrom(rightType);
        verify(leftType).asErasure();
        verifyNoMoreInteractions(leftType);
        verify(rightType).asErasure();
        verifyNoMoreInteractions(rightType);
    }

    @Test
    public void testRightAssignable() throws Exception {
        when(leftType.isAssignableTo(rightType)).thenReturn(true);
        assertThat(DeclaringTypeResolver.INSTANCE.resolve(source, left, right), is(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT));
        verify(leftType).isAssignableFrom(rightType);
        verify(leftType).isAssignableTo(rightType);
        verify(leftType).asErasure();
        verifyNoMoreInteractions(leftType);
        verify(rightType).asErasure();
        verifyNoMoreInteractions(rightType);
    }

    @Test
    public void testNonAssignable() throws Exception {
        assertThat(DeclaringTypeResolver.INSTANCE.resolve(source, left, right), is(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS));
        verify(leftType).isAssignableFrom(rightType);
        verify(leftType).isAssignableTo(rightType);
        verify(leftType).asErasure();
        verifyNoMoreInteractions(leftType);
        verify(rightType).asErasure();
        verifyNoMoreInteractions(rightType);
    }
}
