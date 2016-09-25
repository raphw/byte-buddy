package net.bytebuddy.benchmark;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.utility.RandomString;
import org.junit.Before;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.reflect.Method;

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
