package net.bytebuddy.utility;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class GraalImageCodeTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {GraalImageCode.NONE, false, false},
                {GraalImageCode.UNKNOWN, false, false},
                {GraalImageCode.AGENT, true, false},
                {GraalImageCode.BUILD, true, false},
                {GraalImageCode.RUNTIME, true, true}
        });
    }

    private final GraalImageCode code;

    private final boolean defined;

    private final boolean nativeImageExecution;

    public GraalImageCodeTest(GraalImageCode code, boolean defined, boolean nativeImageExecution) {
        this.code = code;
        this.defined = defined;
        this.nativeImageExecution = nativeImageExecution;
    }

    @Test
    public void testDefined() throws Exception {
        assertThat(code.isDefined(), is(defined));
    }

    @Test
    public void testNativeImageExecution() throws Exception {
        assertThat(code.isNativeImageExecution(), is(nativeImageExecution));
    }

    @Test
    public void testSort() {
        String[] value = {"b", "a"};
        assertThat(code.sorted(value, new Comparator<String>() {
            public int compare(String left, String right) {
                return left.compareTo(right);
            }
        }), sameInstance(value));
        assertThat(value[0], is(code.isDefined() ? "a" : "b"));
        assertThat(value[1], is(code.isDefined() ? "b" : "a"));
    }
}
