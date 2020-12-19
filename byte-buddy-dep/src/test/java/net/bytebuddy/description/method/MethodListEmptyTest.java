package net.bytebuddy.description.method;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class MethodListEmptyTest {

    @Test
    public void testTokenListWithFilter() throws Exception {
        assertThat(new MethodList.Empty<MethodDescription>().asTokenList(none()).size(), is(0));
    }

    @Test
    public void testSignatureTokenList() throws Exception {
        assertThat(new MethodList.Empty<MethodDescription>().asSignatureTokenList().size(), is(0));
    }

    @Test
    public void testSignatureTokenListWithFilter() throws Exception {
        assertThat(new MethodList.Empty<MethodDescription>().asSignatureTokenList(none(), mock(TypeDescription.class)).size(), is(0));
    }

    @Test
    public void testDeclaredList() throws Exception {
        assertThat(new MethodList.Empty<MethodDescription>().asDefined().size(), is(0));
    }
}
