package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodGraphCompilerDefaultHarmonizerForJavaMethodTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final int MODIFIERS = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription first, second;

    private MethodGraph.Compiler.Default.Harmonizer<MethodGraph.Compiler.Default.Harmonizer.ForJavaMethod.Token> harmonizer;

    @Before
    public void setUp() throws Exception {
        harmonizer = MethodGraph.Compiler.Default.Harmonizer.ForJavaMethod.INSTANCE;
    }

    @Test
    public void testMethodEqualityHashCode() throws Exception {
        assertThat(harmonizer.harmonize(new MethodDescription.TypeToken(first, Collections.singletonList(first))).hashCode(),
                is(harmonizer.harmonize(new MethodDescription.TypeToken(first, Collections.singletonList(first))).hashCode()));
    }

    @Test
    public void testMethodEquality() throws Exception {
        assertThat(harmonizer.harmonize(new MethodDescription.TypeToken(first, Collections.singletonList(first))),
                is(harmonizer.harmonize(new MethodDescription.TypeToken(first, Collections.singletonList(first)))));
    }

    @Test
    public void testMethodReturnTypeInequalityHashCode() throws Exception {
        assertThat(harmonizer.harmonize(new MethodDescription.TypeToken(first, Collections.singletonList(first))).hashCode(),
                is(harmonizer.harmonize(new MethodDescription.TypeToken(second, Collections.singletonList(first))).hashCode()));
    }

    @Test
    public void testMethodReturnTypeInequality() throws Exception {
        assertThat(harmonizer.harmonize(new MethodDescription.TypeToken(first, Collections.singletonList(first))),
                is(harmonizer.harmonize(new MethodDescription.TypeToken(second, Collections.singletonList(first)))));
    }

    @Test
    public void testMethodParameterTypesHashCode() throws Exception {
        assertThat(harmonizer.harmonize(new MethodDescription.TypeToken(first, Collections.singletonList(first))).hashCode(),
                not(harmonizer.harmonize(new MethodDescription.TypeToken(first, Collections.singletonList(second))).hashCode()));
    }

    @Test
    public void testMethodParameterTypesEquality() throws Exception {
        assertThat(harmonizer.harmonize(new MethodDescription.TypeToken(first, Collections.singletonList(first))),
                not(harmonizer.harmonize(new MethodDescription.TypeToken(first, Collections.singletonList(second)))));
    }

    @Test
    public void testFactory() throws Exception {
        assertThat(MethodGraph.Compiler.Default.forJavaHierarchy(), hasPrototype((MethodGraph.Compiler) new MethodGraph.Compiler
                .Default<MethodGraph.Compiler.Default.Harmonizer.ForJavaMethod.Token>(MethodGraph.Compiler.Default.Harmonizer.ForJavaMethod.INSTANCE,
                MethodGraph.Compiler.Default.Merger.Directional.LEFT, TypeDescription.Generic.Visitor.Reifying.INITIATING)));
    }
}
