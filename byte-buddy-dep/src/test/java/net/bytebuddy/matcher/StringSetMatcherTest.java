package net.bytebuddy.matcher;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class StringSetMatcherTest extends AbstractElementMatcherTest<StringSetMatcher> {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new String[]{"fo", "fo", "fo"}, "fo", "fooo"},
                {new String[]{"fo"}, "fo", "fooo"},
                {new String[]{"f", "fo", "foo"}, "fo", "fooo"}
        });
    }

    private final Set<String> values;

    private final String matching, nonMatching;

    public StringSetMatcherTest(String[] values, String matching, String nonMatching) {
        super(StringSetMatcher.class, Arrays.toString(values));
        this.values = new HashSet<String>(Arrays.asList(values));
        this.matching = matching;
        this.nonMatching = nonMatching;
    }

    @Test
    public void testMatch() throws Exception {
        assertThat(new StringSetMatcher(values).matches(matching), is(true));
    }

    @Test
    public void testNoMatch() throws Exception {
        assertThat(new StringSetMatcher(values).matches(nonMatching), is(false));
    }

    @Test
    public void testStringRepresentation() {
        assertThat(new StringSetMatcher(values).toString(), startsWith("in("));
    }
}
