package net.bytebuddy.utility;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class RandomStringTest {

    private static final int LENGTH = RandomString.DEFAULT_LENGTH * 2;

    @Test
    public void testRandomStringLength() throws Exception {
        assertThat(new RandomString().nextString().length(), is(RandomString.DEFAULT_LENGTH));
        assertThat(RandomString.make().length(), is(RandomString.DEFAULT_LENGTH));
        assertThat(new RandomString(LENGTH).nextString().length(), is(LENGTH));
        assertThat(RandomString.make(LENGTH).length(), is(LENGTH));
    }

    @Test
    public void testRandom() throws Exception {
        RandomString randomString = new RandomString();
        assertThat(randomString.nextString(), not(randomString.nextString()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeLengthThrowsException() throws Exception {
        new RandomString(-1);
    }
}
