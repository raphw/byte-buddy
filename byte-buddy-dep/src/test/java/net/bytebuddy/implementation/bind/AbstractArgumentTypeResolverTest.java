package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.when;

public class AbstractArgumentTypeResolverTest extends AbstractAmbiguityResolverTest {

    @Mock
    protected ParameterList<?> sourceParameterList, leftParameterList, rightParameterList;

    @Mock
    protected TypeDescription sourceType;

    @Mock
    protected TypeDescription.Generic genericSourceType;

    @Mock
    private ParameterDescription sourceParameter;

    protected static ArgumentMatcher<? super ArgumentTypeResolver.ParameterIndexToken> describesArgument(final int... index) {
        return new ArgumentMatcher<ArgumentTypeResolver.ParameterIndexToken>() {
            @Override
            public boolean matches(ArgumentTypeResolver.ParameterIndexToken parameterIndexToken) {
                for (int anIndex : index) {
                    if (parameterIndexToken.equals(new ArgumentTypeResolver.ParameterIndexToken(anIndex))) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    @Override
    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();
        when(source.getParameters()).thenReturn((ParameterList) sourceParameterList);
        when(sourceParameterList.get(anyInt())).thenReturn(sourceParameter);
        when(sourceParameter.getType()).thenReturn(genericSourceType);
        when(genericSourceType.asErasure()).thenReturn(sourceType);
        when(leftMethod.getParameters()).thenReturn((ParameterList) leftParameterList);
        when(rightMethod.getParameters()).thenReturn((ParameterList) rightParameterList);
    }

    protected static class TokenAnswer implements Answer<Integer> {

        private final Map<ArgumentTypeResolver.ParameterIndexToken, Integer> indexMapping;

        protected TokenAnswer(int[][] mapping) {
            Map<ArgumentTypeResolver.ParameterIndexToken, Integer> indexMapping = new HashMap<ArgumentTypeResolver.ParameterIndexToken, Integer>();
            for (int[] entry : mapping) {
                assert entry.length == 2;
                assert entry[0] >= 0;
                assert entry[1] >= 0;
                Object override = indexMapping.put(new ArgumentTypeResolver.ParameterIndexToken(entry[0]), entry[1]);
                assert override == null;
            }
            this.indexMapping = Collections.unmodifiableMap(indexMapping);
        }

        @Override
        public Integer answer(InvocationOnMock invocation) throws Throwable {
            assert invocation.getArguments().length == 1;
            return indexMapping.get((ArgumentTypeResolver.ParameterIndexToken) invocation.getArguments()[0]);
        }
    }
}
