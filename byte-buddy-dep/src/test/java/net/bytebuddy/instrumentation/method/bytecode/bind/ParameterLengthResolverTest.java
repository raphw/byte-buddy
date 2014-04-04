package net.bytebuddy.instrumentation.method.bytecode.bind;

import net.bytebuddy.instrumentation.type.TypeList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class ParameterLengthResolverTest extends AbstractAmbiguityResolverTest {

    @Mock
    private TypeList leftList, rightList;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(leftMethod.getParameterTypes()).thenReturn(leftList);
        when(rightMethod.getParameterTypes()).thenReturn(rightList);
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
