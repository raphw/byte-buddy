package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PluginEngineSummaryTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private Throwable throwable;

    @Test
    public void testGetters() {
        Plugin.Engine.Summary summary = new Plugin.Engine.Summary(Collections.singletonList(typeDescription),
                Collections.singletonMap(typeDescription, Collections.singletonList(throwable)),
                Collections.singletonList(FOO));
        assertThat(summary.getTransformed(), is(Collections.singletonList(typeDescription)));
        assertThat(summary.getFailed(), is(Collections.singletonMap(typeDescription, Collections.singletonList(throwable))));
        assertThat(summary.getUnresolved(), is(Collections.singletonList(FOO)));
    }

    @Test
    public void testSummaryObjectProperties() {
        Plugin.Engine.Summary summary = new Plugin.Engine.Summary(Collections.singletonList(typeDescription),
                Collections.singletonMap(typeDescription, Collections.singletonList(throwable)),
                Collections.singletonList(FOO));
        assertThat(summary.hashCode(), is(new Plugin.Engine.Summary(Collections.singletonList(typeDescription),
                Collections.singletonMap(typeDescription, Collections.singletonList(throwable)),
                Collections.singletonList(FOO)).hashCode()));
        assertThat(summary, is(summary));
        assertThat(summary, not(new Object()));
        assertThat(summary.equals(null), is(false));
        assertThat(summary, is(new Plugin.Engine.Summary(Collections.singletonList(typeDescription),
                Collections.singletonMap(typeDescription, Collections.singletonList(throwable)),
                Collections.singletonList(FOO))));
    }
}
