package net.bytebuddy.benchmark;

import org.junit.Before;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Unfortunately, the JMH is not very test friendly. Thus, we need to do some tricks to run test cases. Fortunately,
 * we are testing a code generation framework such that we already have the tools to generate the required classes
 * on our class path.
 */
public abstract class AbstractBlackHoleTest {

    protected Blackhole blackHole;

    @Before
    public void setUpBlackHole() throws Exception {
        blackHole = new Blackhole("Today\'s password is swordfish. I understand instantiating Blackholes directly is dangerous.");
    }
}
