package net.bytebuddy.build.gradle;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AbstractUserConfigurationPrefixIterableTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private File primary, other;

    @Test
    public void testIteration() throws Exception {
        Iterator<? extends File> iterator = new AbstractUserConfiguration.PrefixIterable(primary, Collections.singleton(other)).iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(primary));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(other));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoval() throws Exception {
        new AbstractUserConfiguration.PrefixIterable(primary, Collections.<File>emptySet()).iterator().remove();
    }
}
