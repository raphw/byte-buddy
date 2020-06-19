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
public class StringSetMatcherTest extends AbstractElementMatcherTest<StringSetMatcher> {

  private final String[] names;

  private final String matching, nonMatching;

  public StringSetMatcherTest(String[] names, String matching, String nonMatching) {
    super(StringSetMatcher.class, Arrays.toString(names));
    this.names = names;
    this.matching = matching;
    this.nonMatching = nonMatching;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
            {new String[] {"fo", "fo", "fo"}, "fo", "fooo"},
            {new String[] {"fo"}, "fo", "fooo"},
            {new String[] {"f", "fo", "foo"}, "fo", "fooo"}
    });
  }

  @Test
  public void testMatch() throws Exception {
    assertThat(new StringSetMatcher(names).matches(matching), is(true));
  }

  @Test
  public void testNoMatch() throws Exception {
    assertThat(new StringSetMatcher(names).matches(nonMatching), is(false));
  }

  @Test
  public void testStringRepresentation() {
    assertThat(new StringSetMatcher(names).toString(), startsWith("in("));
  }
}
