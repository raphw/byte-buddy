package net.bytebuddy.description.method;

import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ParameterListEmptyTest {

    @Test
    public void testTokenListWithFilter() throws Exception {
        assertThat(new ParameterList.Empty().asTokenList(none()).size(), is(0));
    }

    @Test
    public void testTokenListWithoutFilter() throws Exception {
        assertThat(new ParameterList.Empty().asTokenList().size(), is(0));
    }

    @Test
    public void testTokenListMetaData() throws Exception {
        assertThat(new ParameterList.Empty().hasExplicitMetaData(), is(true));
    }

    @Test
    public void testTypeList() throws Exception {
        assertThat(new ParameterList.Empty().asTypeList().size(), is(0));
    }

    @Test
    public void testDeclaredList() throws Exception {
        assertThat(new ParameterList.Empty().asDefined().size(), is(0));
    }
}
