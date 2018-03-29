package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldLocatorResolutionTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private FieldDescription fieldDescription;

    @Test
    public void testSimpleResolutionResolved() throws Exception {
        assertThat(new FieldLocator.Resolution.Simple(fieldDescription).isResolved(), is(true));
    }

    @Test
    public void testSimpleResolutionFieldDescription() throws Exception {
        assertThat(new FieldLocator.Resolution.Simple(fieldDescription).getField(), is(fieldDescription));
    }

    @Test
    public void testIllegalResolutionUnresolved() throws Exception {
        assertThat(FieldLocator.Resolution.Illegal.INSTANCE.isResolved(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalResolutionFieldDescription() throws Exception {
        FieldLocator.Resolution.Illegal.INSTANCE.getField();
    }
}
