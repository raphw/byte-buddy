package net.bytebuddy.description.type;

import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class RecordComponentListEmptyTest {

    @Test
    public void testTokenListWithFilter() throws Exception {
        assertThat(new RecordComponentList.Empty<RecordComponentDescription>().asTokenList(none()).size(), is(0));
    }

    @Test
    public void testDeclaredList() throws Exception {
        assertThat(new RecordComponentList.Empty<RecordComponentDescription>().asDefined().size(), is(0));
    }
}
