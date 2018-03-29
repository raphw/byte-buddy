package net.bytebuddy.matcher;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class StringMatcherTest extends AbstractElementMatcherTest<StringMatcher> {

    private static final String FOO = "foo";

    private final StringMatcher.Mode mode;

    private final String matching, nonMatching;

    public StringMatcherTest(StringMatcher.Mode mode, String matching, String nonMatching) {
        super(StringMatcher.class, mode.getDescription());
        this.mode = mode;
        this.matching = matching;
        this.nonMatching = nonMatching;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {StringMatcher.Mode.CONTAINS, "fo", "fooo"},
                {StringMatcher.Mode.CONTAINS_IGNORE_CASE, "FO", "fooo"},
                {StringMatcher.Mode.ENDS_WITH, "oo", "f"},
                {StringMatcher.Mode.ENDS_WITH_IGNORE_CASE, "OO", "f"},
                {StringMatcher.Mode.EQUALS_FULLY, "foo", "bar"},
                {StringMatcher.Mode.EQUALS_FULLY_IGNORE_CASE, "FOO", "bar"},
                {StringMatcher.Mode.MATCHES, "[a-z]{3}", "bar"},
                {StringMatcher.Mode.STARTS_WITH, "fo", "fooo"},
                {StringMatcher.Mode.STARTS_WITH_IGNORE_CASE, "FO", "fooo"},
        });
    }

    @Test
    public void testMatch() throws Exception {
        assertThat(new StringMatcher(matching, mode).matches(FOO), is(true));
    }

    @Test
    public void testNoMatch() throws Exception {
        assertThat(new StringMatcher(nonMatching, mode).matches(FOO), is(false));
    }

    @Test
    @Override
    public void testStringRepresentation() throws Exception {
        assertThat(new StringMatcher(FOO, mode).toString(), startsWith(mode.getDescription()));
    }
}
