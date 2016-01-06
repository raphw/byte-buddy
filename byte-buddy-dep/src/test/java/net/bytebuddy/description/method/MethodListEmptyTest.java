package net.bytebuddy.description.method;

import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodListEmptyTest {

    @Test
    public void testTokenListWithFilter() throws Exception {
        assertThat(new MethodList.Empty<MethodDescription>().asTokenList(none()).size(), is(0));
    }

    @Test
    public void testDeclaredList() throws Exception {
        assertThat(new MethodList.Empty<MethodDescription>().asDefined().size(), is(0));
    }
}
