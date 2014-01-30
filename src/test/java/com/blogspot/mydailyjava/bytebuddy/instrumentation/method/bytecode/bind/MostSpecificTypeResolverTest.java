package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.*;

public class MostSpecificTypeResolverTest extends AbstractAmbiguityResolverTest {

    private static class IndexTokenMatcher extends BaseMatcher<MostSpecificTypeResolver.ParameterIndexToken> {

        private final int index;

        private IndexTokenMatcher(int index) {
            assert index >= 0;
            this.index = index;
        }

        @Override
        public boolean matches(Object item) {
            return new MostSpecificTypeResolver.ParameterIndexToken(index).equals(item);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Expected matching token for parameter index ").appendValue(index);
        }
    }

    private static Matcher<? super MostSpecificTypeResolver.ParameterIndexToken> describesArgument(int... index) {
        Matcher<? super MostSpecificTypeResolver.ParameterIndexToken> token = CoreMatchers.anything();
        for (int anIndex : index) {
            token = anyOf(new IndexTokenMatcher(anIndex), token);
        }
        return token;
    }

    private static class TokenAnswer implements Answer<Integer> {

        private final Map<MostSpecificTypeResolver.ParameterIndexToken, Integer> indexMapping;

        private TokenAnswer(int[][] mapping) {
            Map<MostSpecificTypeResolver.ParameterIndexToken, Integer> indexMapping = new HashMap<MostSpecificTypeResolver.ParameterIndexToken, Integer>();
            for (int[] entry : mapping) {
                assert entry.length == 2;
                assert entry[0] >= 0;
                assert entry[1] >= 0;
                Object override = indexMapping.put(new MostSpecificTypeResolver.ParameterIndexToken(entry[0]), entry[1]);
                assert override == null;
            }
            this.indexMapping = Collections.unmodifiableMap(indexMapping);
        }

        @Override
        public Integer answer(InvocationOnMock invocation) throws Throwable {
            assert invocation.getArguments().length == 1;
            return indexMapping.get(invocation.getArguments()[0]);
        }
    }

    @Test
    public void testMethodWithoutArguments() throws Exception {
        when(source.getParameterTypes()).thenReturn(makeTypeList());
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution = MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
        assertThat(resolution.isUnresolved(), is(true));
        verify(source, atLeast(1)).getParameterTypes();
        verifyZeroInteractions(left);
        verifyZeroInteractions(right);
    }

    @Test
    public void testMethodWithoutSeveralUnmappedArguments() throws Exception {
        when(source.getParameterTypes()).thenReturn(makeTypeList(Void.class, Void.class, Void.class));
        when(left.getTargetParameterIndex(any())).thenReturn(null);
        when(right.getTargetParameterIndex(any())).thenReturn(null);
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution = MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
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
    public void testMethodWithoutMappedArguments() throws Exception {
        when(source.getParameterTypes()).thenReturn(makeTypeList(Integer.class));
        when(leftMethod.getParameterTypes()).thenReturn(makeTypeList(Number.class));
        when(left.getTargetParameterIndex(argThat(describesArgument(0)))).thenReturn(null);
        when(rightMethod.getParameterTypes()).thenReturn(makeTypeList(Object.class));
        when(right.getTargetParameterIndex(argThat(describesArgument(0)))).thenReturn(null);
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution = MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
        assertThat(resolution, is(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS));
        verify(source, atLeast(1)).getParameterTypes();
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(left, never()).getTargetParameterIndex(argThat(not(describesArgument(0))));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(right, never()).getTargetParameterIndex(argThat(not(describesArgument(0))));
    }

    @Test
    public void testLeftMethodDominantByType() throws Exception {
        when(source.getParameterTypes()).thenReturn(makeTypeList(Integer.class));
        when(leftMethod.getParameterTypes()).thenReturn(makeTypeList(Number.class, null));
        when(left.getTargetParameterIndex(any())).thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        when(rightMethod.getParameterTypes()).thenReturn(makeTypeList(null, Object.class));
        when(right.getTargetParameterIndex(any())).thenAnswer(new TokenAnswer(new int[][]{{0, 1}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution = MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
        assertThat(resolution, is(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT));
        verify(source, atLeast(1)).getParameterTypes();
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(left, never()).getTargetParameterIndex(argThat(not(describesArgument(0))));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(right, never()).getTargetParameterIndex(argThat(not(describesArgument(0))));
    }

    @Test
    public void testRightMethodDominantByType() throws Exception {
        when(source.getParameterTypes()).thenReturn(makeTypeList(Integer.class));
        when(leftMethod.getParameterTypes()).thenReturn(makeTypeList(Object.class, null));
        when(left.getTargetParameterIndex(any())).thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        when(rightMethod.getParameterTypes()).thenReturn(makeTypeList(null, Number.class));
        when(right.getTargetParameterIndex(any())).thenAnswer(new TokenAnswer(new int[][]{{0, 1}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution = MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
        assertThat(resolution, is(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT));
        verify(source, atLeast(1)).getParameterTypes();
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(left, never()).getTargetParameterIndex(argThat(not(describesArgument(0))));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(right, never()).getTargetParameterIndex(argThat(not(describesArgument(0))));
    }

    @Test
    public void testAmbiguousByCrossAssignableType() throws Exception {
        when(source.getParameterTypes()).thenReturn(makeTypeList(Integer.class));
        when(leftMethod.getParameterTypes()).thenReturn(makeTypeList(Number.class));
        when(left.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class))).thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        when(rightMethod.getParameterTypes()).thenReturn(makeTypeList(Number.class));
        when(right.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class))).thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution = MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
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
        when(source.getParameterTypes()).thenReturn(makeTypeList(Class.class));
        when(leftMethod.getParameterTypes()).thenReturn(makeTypeList(GenericDeclaration.class));
        when(left.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class))).thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        when(rightMethod.getParameterTypes()).thenReturn(makeTypeList(Type.class));
        when(right.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class))).thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution = MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
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
        when(source.getParameterTypes()).thenReturn(makeTypeList(Integer.class, Integer.class));
        when(leftMethod.getParameterTypes()).thenReturn(makeTypeList(Integer.class, Object.class));
        when(left.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class))).thenAnswer(new TokenAnswer(new int[][]{{0, 0}, {1, 1}}));
        when(rightMethod.getParameterTypes()).thenReturn(makeTypeList(Object.class, Integer.class));
        when(right.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class))).thenAnswer(new TokenAnswer(new int[][]{{0, 0}, {1, 1}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution = MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
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
        when(source.getParameterTypes()).thenReturn(makeTypeList(Integer.class, Integer.class));
        when(leftMethod.getParameterTypes()).thenReturn(makeTypeList(Integer.class, Integer.class));
        when(left.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class))).thenAnswer(new TokenAnswer(new int[][]{{0, 0}, {1, 1}}));
        when(rightMethod.getParameterTypes()).thenReturn(makeTypeList(Integer.class, null));
        when(right.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class))).thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution = MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
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
        when(source.getParameterTypes()).thenReturn(makeTypeList(Integer.class, Integer.class));
        when(leftMethod.getParameterTypes()).thenReturn(makeTypeList(Integer.class, null));
        when(left.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class))).thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        when(rightMethod.getParameterTypes()).thenReturn(makeTypeList(Integer.class, Integer.class));
        when(right.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class))).thenAnswer(new TokenAnswer(new int[][]{{0, 0}, {1, 1}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution = MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
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

    @Test
    public void testLeftMethodDominantForPrimitiveType() throws Exception {
        when(source.getParameterTypes()).thenReturn(makeTypeList(int.class));
        when(leftMethod.getParameterTypes()).thenReturn(makeTypeList(int.class));
        when(left.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class))).thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        when(rightMethod.getParameterTypes()).thenReturn(makeTypeList(long.class));
        when(right.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class))).thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution = MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
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
    public void testRightMethodDominantForPrimitiveType() throws Exception {
        when(source.getParameterTypes()).thenReturn(makeTypeList(int.class));
        when(leftMethod.getParameterTypes()).thenReturn(makeTypeList(long.class));
        when(left.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class))).thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        when(rightMethod.getParameterTypes()).thenReturn(makeTypeList(int.class));
        when(right.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class))).thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution = MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
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

    private static TypeList makeTypeList(Class<?>... type) {
        return new TypeList.ForLoadedType(type);
    }
}
