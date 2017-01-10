package net.bytebuddy.utility;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class RandomStringTest {

    @Test
    public void testRandomStringLength() throws Exception {
        assertThat(new RandomString().nextString().length(), is(RandomString.DEFAULT_LENGTH));
        assertThat(RandomString.make().length(), is(RandomString.DEFAULT_LENGTH));
        assertThat(new RandomString(RandomString.DEFAULT_LENGTH * 2).nextString().length(), is(RandomString.DEFAULT_LENGTH * 2));
        assertThat(RandomString.make(RandomString.DEFAULT_LENGTH * 2).length(), is(RandomString.DEFAULT_LENGTH * 2));
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

    @Test
    public void testHashValueOnlyOneBits() throws Exception {
        assertThat(RandomString.hashOf(-1).length(), not(0));
    }

    @Test
    public void testHashValueOnlyZeroBits() throws Exception {
        assertThat(RandomString.hashOf(0).length(), not(0));
    }

    @Test
    public void testHashValueInequality() throws Exception {
        assertThat(RandomString.hashOf(0), is(RandomString.hashOf(0)));
        assertThat(RandomString.hashOf(0), not(RandomString.hashOf(-1)));
        assertThat(RandomString.hashOf(0), not(RandomString.hashOf(1)));
    }
}
