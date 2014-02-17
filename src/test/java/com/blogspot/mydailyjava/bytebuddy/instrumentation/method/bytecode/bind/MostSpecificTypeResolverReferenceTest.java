package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.*;

public class MostSpecificTypeResolverReferenceTest extends AbstractMostSpecificTypeResolverTest {

    @Mock
    private TypeDescription weakTargetType;
    @Mock
    private TypeDescription dominantTargetType;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(weakTargetType.isAssignableFrom(dominantTargetType)).thenReturn(true);
        when(weakTargetType.isAssignableFrom(weakTargetType)).thenReturn(true);
        when(weakTargetType.isAssignableTo(weakTargetType)).thenReturn(true);
        when(dominantTargetType.isAssignableTo(weakTargetType)).thenReturn(true);
    }

    @Test
    public void testMethodWithoutArguments() throws Exception {
        when(source.getParameterTypes()).thenReturn(sourceTypeList);
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution =
                MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
        assertThat(resolution.isUnresolved(), is(true));
        verify(source, atLeast(1)).getParameterTypes();
        verifyZeroInteractions(left);
        verifyZeroInteractions(right);
    }

    @Test
    public void testMethodWithoutMappedArguments() throws Exception {
        when(sourceTypeList.size()).thenReturn(1);
        when(left.getTargetParameterIndex(argThat(describesArgument(0)))).thenReturn(null);
        when(right.getTargetParameterIndex(argThat(describesArgument(0)))).thenReturn(null);
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution =
                MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
        assertThat(resolution, is(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS));
        verify(source, atLeast(1)).getParameterTypes();
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(left, never()).getTargetParameterIndex(argThat(not(describesArgument(0))));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(right, never()).getTargetParameterIndex(argThat(not(describesArgument(0))));
    }

    @Test
    public void testMethodWithoutSeveralUnmappedArguments() throws Exception {
        when(sourceTypeList.size()).thenReturn(3);
        when(left.getTargetParameterIndex(any())).thenReturn(null);
        when(right.getTargetParameterIndex(any())).thenReturn(null);
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution =
                MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
        assertThat(resolution, is(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS));
        verify(source, atLeast(1)).getParameterTypes();
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(1)));
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(2)));
        verify(left, never()).getTargetParameterIndex(argThat(not(describesArgument(0, 1, 2))));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(1)));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(2)));
        verify(right, never()).getTargetParameterIndex(argThat(not(describesArgument(0, 1, 2))));
    }

    @Test
    public void testLeftMethodDominantByType() throws Exception {
        when(sourceTypeList.size()).thenReturn(1);
        when(leftTypeList.get(0)).thenReturn(dominantTargetType);
        when(left.getTargetParameterIndex(any())).thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        when(rightTypeList.get(1)).thenReturn(weakTargetType);
        when(right.getTargetParameterIndex(any())).thenAnswer(new TokenAnswer(new int[][]{{0, 1}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution =
                MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
        assertThat(resolution, is(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT));
        verify(source, atLeast(1)).getParameterTypes();
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(left, never()).getTargetParameterIndex(argThat(not(describesArgument(0))));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(right, never()).getTargetParameterIndex(argThat(not(describesArgument(0))));
    }

    @Test
    public void testRightMethodDominantByType() throws Exception {
        when(sourceTypeList.size()).thenReturn(1);
        when(leftTypeList.get(0)).thenReturn(weakTargetType);
        when(left.getTargetParameterIndex(any())).thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        when(rightTypeList.get(1)).thenReturn(dominantTargetType);
        when(right.getTargetParameterIndex(any())).thenAnswer(new TokenAnswer(new int[][]{{0, 1}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution =
                MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
        assertThat(resolution, is(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT));
        verify(source, atLeast(1)).getParameterTypes();
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(left, never()).getTargetParameterIndex(argThat(not(describesArgument(0))));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(right, never()).getTargetParameterIndex(argThat(not(describesArgument(0))));
    }

    @Test
    public void testAmbiguousByCrossAssignableType() throws Exception {
        when(sourceTypeList.size()).thenReturn(1);
        when(leftTypeList.get(0)).thenReturn(weakTargetType);
        when(left.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class)))
                .thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        when(rightTypeList.get(0)).thenReturn(weakTargetType);
        when(right.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class)))
                .thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution =
                MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
        assertThat(resolution, is(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS));
        verify(source, atLeast(1)).getParameterTypes();
        verify(leftMethod, atLeast(1)).getParameterTypes();
        verify(rightMethod, atLeast(1)).getParameterTypes();
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(left, never()).getTargetParameterIndex(argThat(not(describesArgument(0))));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(right, never()).getTargetParameterIndex(argThat(not(describesArgument(0))));
    }

    @Test
    public void testAmbiguousByNonCrossAssignableType() throws Exception {
        when(sourceTypeList.size()).thenReturn(1);
        when(leftTypeList.get(0)).thenReturn(dominantTargetType);
        when(left.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class)))
                .thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        when(rightTypeList.get(0)).thenReturn(dominantTargetType);
        when(right.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class)))
                .thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution =
                MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
        assertThat(resolution, is(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS));
        verify(source, atLeast(1)).getParameterTypes();
        verify(leftMethod, atLeast(1)).getParameterTypes();
        verify(rightMethod, atLeast(1)).getParameterTypes();
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(left, never()).getTargetParameterIndex(argThat(not(describesArgument(0))));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(right, never()).getTargetParameterIndex(argThat(not(describesArgument(0))));
    }

    @Test
    public void testAmbiguousByDifferentIndexedAssignableType() throws Exception {
        when(sourceTypeList.size()).thenReturn(2);
        when(leftTypeList.get(0)).thenReturn(dominantTargetType);
        when(leftTypeList.get(1)).thenReturn(weakTargetType);
        when(left.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class)))
                .thenAnswer(new TokenAnswer(new int[][]{{0, 0}, {1, 1}}));
        when(rightTypeList.get(0)).thenReturn(weakTargetType);
        when(rightTypeList.get(1)).thenReturn(dominantTargetType);
        when(right.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class)))
                .thenAnswer(new TokenAnswer(new int[][]{{0, 0}, {1, 1}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution =
                MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
        assertThat(resolution, is(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS));
        verify(source, atLeast(1)).getParameterTypes();
        verify(leftMethod, atLeast(1)).getParameterTypes();
        verify(rightMethod, atLeast(1)).getParameterTypes();
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(1)));
        verify(left, never()).getTargetParameterIndex(argThat(not(describesArgument(0, 1))));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(1)));
        verify(right, never()).getTargetParameterIndex(argThat(not(describesArgument(0, 1))));
    }

    @Test
    public void testLeftMethodDominantByScore() throws Exception {
        when(sourceTypeList.size()).thenReturn(2);
        when(leftTypeList.get(0)).thenReturn(dominantTargetType);
        when(leftTypeList.get(1)).thenReturn(weakTargetType);
        when(left.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class)))
                .thenAnswer(new TokenAnswer(new int[][]{{0, 0}, {1, 1}}));
        when(rightTypeList.get(0)).thenReturn(dominantTargetType);
        when(right.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class)))
                .thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution =
                MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
        assertThat(resolution, is(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT));
        verify(source, atLeast(1)).getParameterTypes();
        verify(leftMethod, atLeast(1)).getParameterTypes();
        verify(rightMethod, atLeast(1)).getParameterTypes();
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(1)));
        verify(left, never()).getTargetParameterIndex(argThat(not(describesArgument(0, 1))));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(1)));
        verify(right, never()).getTargetParameterIndex(argThat(not(describesArgument(0, 1))));
    }

    @Test
    public void testRightMethodDominantByScore() throws Exception {
        when(sourceTypeList.size()).thenReturn(2);
        when(leftTypeList.get(0)).thenReturn(dominantTargetType);
        when(left.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class)))
                .thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        when(rightTypeList.get(0)).thenReturn(dominantTargetType);
        when(rightTypeList.get(1)).thenReturn(dominantTargetType);
        when(right.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class)))
                .thenAnswer(new TokenAnswer(new int[][]{{0, 0}, {1, 1}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution =
                MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
        assertThat(resolution, is(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT));
        verify(source, atLeast(1)).getParameterTypes();
        verify(leftMethod, atLeast(1)).getParameterTypes();
        verify(rightMethod, atLeast(1)).getParameterTypes();
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(1)));
        verify(left, never()).getTargetParameterIndex(argThat(not(describesArgument(0, 1))));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(1)));
        verify(right, never()).getTargetParameterIndex(argThat(not(describesArgument(0, 1))));
    }
}
