package net.bytebuddy.instrumentation.method.bytecode.bind;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

public class AbstractMostSpecificTypeResolverTest extends AbstractAmbiguityResolverTest {

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

    protected static Matcher<? super MostSpecificTypeResolver.ParameterIndexToken> describesArgument(int... index) {
        Matcher<? super MostSpecificTypeResolver.ParameterIndexToken> token = CoreMatchers.anything();
        for (int anIndex : index) {
            token = anyOf(new IndexTokenMatcher(anIndex), token);
        }
        return token;
    }

    protected static class TokenAnswer implements Answer<Integer> {

        private final Map<MostSpecificTypeResolver.ParameterIndexToken, Integer> indexMapping;

        protected TokenAnswer(int[][] mapping) {
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

    @Mock
    protected TypeList sourceTypeList, leftTypeList, rightTypeList;
    @Mock
    protected TypeDescription sourceType;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(source.getParameterTypes()).thenReturn(sourceTypeList);
        when(sourceTypeList.get(anyInt())).thenReturn(sourceType);
        when(leftMethod.getParameterTypes()).thenReturn(leftTypeList);
        when(rightMethod.getParameterTypes()).thenReturn(rightTypeList);
    }
}
