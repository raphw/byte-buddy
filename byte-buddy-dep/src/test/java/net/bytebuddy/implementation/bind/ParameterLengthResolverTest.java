package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.ParameterList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class ParameterLengthResolverTest extends AbstractAmbiguityResolverTest {

    @Mock
    private ParameterList<?> leftList, rightList;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();
        when(leftMethod.getParameters()).thenReturn((ParameterList) leftList);
        when(rightMethod.getParameters()).thenReturn((ParameterList) rightList);
    }

    @Test
    public void testAmbiguous() throws Exception {
        assertThat(ParameterLengthResolver.INSTANCE.resolve(source, left, right),
                is(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS));
    }

    @Test
    public void testLeft() throws Exception {
        when(leftList.size()).thenReturn(1);
        assertThat(ParameterLengthResolver.INSTANCE.resolve(source, left, right),
                is(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT));
    }

    @Test
    public void testRight() throws Exception {
        when(rightList.size()).thenReturn(1);
        assertThat(ParameterLengthResolver.INSTANCE.resolve(source, left, right),
                is(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT));
    }
}
