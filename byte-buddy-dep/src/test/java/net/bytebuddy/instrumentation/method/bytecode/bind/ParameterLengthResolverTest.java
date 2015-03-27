package net.bytebuddy.instrumentation.method.bytecode.bind;

import net.bytebuddy.instrumentation.method.ParameterList;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class ParameterLengthResolverTest extends AbstractAmbiguityResolverTest {

    @Mock
    private ParameterList leftList, rightList;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(leftMethod.getParameters()).thenReturn(leftList);
        when(rightMethod.getParameters()).thenReturn(rightList);
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

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ParameterLengthResolver.class).apply();
    }
}
