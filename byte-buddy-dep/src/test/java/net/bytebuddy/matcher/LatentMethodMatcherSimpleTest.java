package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;

public class LatentMethodMatcherSimpleTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ElementMatcher<? super MethodDescription> elementMatcher;

    @Mock
    private TypeDescription typeDescription;

    @Test
    @SuppressWarnings("unchecked")
    public void testManifestation() throws Exception {
        LatentMethodMatcher latentMethodMatcher = new LatentMethodMatcher.Resolved(elementMatcher);
        assertThat(latentMethodMatcher.resolve(typeDescription), is((ElementMatcher) elementMatcher));
        verifyZeroInteractions(elementMatcher);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(LatentMethodMatcher.Resolved.class).apply();
    }
}
