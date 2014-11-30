package net.bytebuddy;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteBuddyDefinableTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testUndefined() throws Exception {
        assertThat(new ByteBuddy.Definable.Undefined<String>().isDefined(), is(false));
        assertThat(new ByteBuddy.Definable.Undefined<String>().resolve(BAR), is(BAR));
    }

    @Test
    public void testDefined() throws Exception {
        assertThat(new ByteBuddy.Definable.Defined<String>(FOO).isDefined(), is(true));
        assertThat(new ByteBuddy.Definable.Defined<String>(FOO).resolve(BAR), is(FOO));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteBuddy.Definable.Defined.class).apply();
        ObjectPropertyAssertion.of(ByteBuddy.Definable.Undefined.class).apply();
    }
}
