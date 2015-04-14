package net.bytebuddy.benchmark;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.utility.RandomString;
import org.junit.Before;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.reflect.Method;
import java.util.Collections;

/**
 * Unfortunately, the JMH is not very test friendly. Thus, we need to do some tricks to run test cases. Fortunately,
 * we are testing a code generation framework such that we already have the tools to generate the required classes
 * on our class path.
 */
public abstract class AbstractBlackHoleTest {

    private static final String BLACK_HOLE_METHOD = "_jmh_tryInit_";

    protected Blackhole blackHole;

    @Before
    public void setUpBlackHole() throws Exception {
        Class<?> blackHoleGenerator = new ByteBuddy()
                .subclass(Object.class)
                .name(String.format("C%s$generated", RandomString.make()))
                .defineMethod(BLACK_HOLE_METHOD, Blackhole.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .intercept(MethodDelegation.toConstructor(Blackhole.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Method method = blackHoleGenerator.getDeclaredMethod(BLACK_HOLE_METHOD);
        blackHole = (Blackhole) method.invoke(blackHoleGenerator.newInstance());
    }
}
