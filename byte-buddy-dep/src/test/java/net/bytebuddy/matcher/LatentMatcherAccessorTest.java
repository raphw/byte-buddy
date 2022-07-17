package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class LatentMatcherAccessorTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private ElementMatcher<? super Object> matcher;

    @Mock
    private TypeDescription typeDescription;

    @Test
    @SuppressWarnings("unchecked")
    public void testManifestation() throws Exception {
        LatentMatcher<Object> matcher = new LatentMatcher.Resolved<Object>(this.matcher);
        assertThat(matcher.resolve(typeDescription), is((ElementMatcher) this.matcher));
        verifyNoMoreInteractions(this.matcher);
        verifyNoMoreInteractions(typeDescription);
    }
}
