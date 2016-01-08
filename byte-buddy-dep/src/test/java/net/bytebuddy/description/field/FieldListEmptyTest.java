package net.bytebuddy.description.field;

import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldListEmptyTest {

    @Test
    public void testTokenListWithFilter() throws Exception {
        assertThat(new FieldList.Empty<FieldDescription>().asTokenList(none()).size(), is(0));
    }

    @Test
    public void testDeclaredList() throws Exception {
        assertThat(new FieldList.Empty<FieldDescription>().asDefined().size(), is(0));
    }
}
