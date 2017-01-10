package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class MethodGraphCompilerDefaultKeyTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private SampleKey foo, bar, qux;

    @Test
    public void testEqualsSimilar() throws Exception {
        assertThat(new PseudoKey(FOO, Collections.singleton(foo)).hashCode(),
                is(new PseudoKey(FOO, Collections.singleton(foo)).hashCode()));
        assertThat(new PseudoKey(FOO, Collections.singleton(foo)),
                is(new PseudoKey(FOO, Collections.singleton(foo))));
    }

    @Test
    public void testNotEqualsDifferentName() throws Exception {
        assertThat(new PseudoKey(FOO, Collections.singleton(foo)).hashCode(),
                not(new PseudoKey(BAR, Collections.singleton(foo)).hashCode()));
        assertThat(new PseudoKey(FOO, Collections.singleton(foo)),
                not(new PseudoKey(BAR, Collections.singleton(foo))));
    }

    @Test
    public void testNotEqualDifferentToken() throws Exception {
        assertThat(new PseudoKey(FOO, Collections.singleton(foo)).hashCode(),
                is(new PseudoKey(FOO, Collections.singleton(bar)).hashCode()));
        assertThat(new PseudoKey(FOO, Collections.singleton(foo)),
                not(new PseudoKey(BAR, Collections.singleton(bar))));
    }

    @Test
    public void testEqualsSuperSet() throws Exception {
        assertThat(new PseudoKey(FOO, new HashSet<SampleKey>(Arrays.asList(foo, bar))).hashCode(),
                is(new PseudoKey(FOO, Collections.singleton(bar)).hashCode()));
        assertThat(new PseudoKey(FOO, new HashSet<SampleKey>(Arrays.asList(foo, bar))),
                is(new PseudoKey(FOO, Collections.singleton(foo))));
    }

    @Test
    public void testEqualsSubSet() throws Exception {
        assertThat(new PseudoKey(FOO, Collections.singleton(foo)).hashCode(),
                is(new PseudoKey(FOO, new HashSet<SampleKey>(Arrays.asList(foo, bar))).hashCode()));
        assertThat(new PseudoKey(FOO, Collections.singleton(foo)),
                is(new PseudoKey(FOO, new HashSet<SampleKey>(Arrays.asList(foo, bar)))));
    }

    @Test
    public void testNotEqualsDistinctSet() throws Exception {
        assertThat(new PseudoKey(FOO, new HashSet<SampleKey>(Arrays.asList(foo, bar))).hashCode(),
                is(new PseudoKey(FOO, Collections.singleton(qux)).hashCode()));
        assertThat(new PseudoKey(FOO, new HashSet<SampleKey>(Arrays.asList(foo, bar))),
                not(new PseudoKey(FOO, Collections.singleton(qux))));
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void testInitialEntryCannotInject() throws Exception {
        new MethodGraph.Compiler.Default.Key.Store.Entry.Initial(new MethodGraph.Compiler.Default.Key.Harmonized(FOO, Collections.emptyMap()))
                .inject(mock(MethodGraph.Compiler.Default.Key.Harmonized.class), Visibility.PUBLIC);
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void testInitialEntryCannotBeTransformed() throws Exception {
        new MethodGraph.Compiler.Default.Key.Store.Entry.Initial(new MethodGraph.Compiler.Default.Key.Harmonized(FOO, Collections.emptyMap()))
                .asNode(mock(MethodGraph.Compiler.Default.Merger.class));
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void testInitialEntryCannotExposeKey() throws Exception {
        new MethodGraph.Compiler.Default.Key.Store.Entry.Initial(new MethodGraph.Compiler.Default.Key.Harmonized(FOO, Collections.emptyMap()))
                .getKey();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodGraph.Compiler.Default.Key.Store.class).apply();
        ObjectPropertyAssertion.of(MethodGraph.Compiler.Default.Key.Store.class).apply();
        ObjectPropertyAssertion.of(MethodGraph.Compiler.Default.Key.Store.Graph.class).apply();
        ObjectPropertyAssertion.of(MethodGraph.Compiler.Default.Key.Store.Entry.Initial.class).apply();
        ObjectPropertyAssertion.of(MethodGraph.Compiler.Default.Key.Store.Entry.Ambiguous.class).apply();
        ObjectPropertyAssertion.of(MethodGraph.Compiler.Default.Key.Store.Entry.Ambiguous.Node.class).apply();
        ObjectPropertyAssertion.of(MethodGraph.Compiler.Default.Key.Store.Entry.Resolved.class).apply();
        ObjectPropertyAssertion.of(MethodGraph.Compiler.Default.Key.Store.Entry.Resolved.Node.class).apply();
    }

    protected static class PseudoKey extends MethodGraph.Compiler.Default.Key<SampleKey> {

        private final Set<SampleKey> identifiers;

        public PseudoKey(String internalName, Set<SampleKey> identifiers) {
            super(internalName);
            this.identifiers = identifiers;
        }

        @Override
        protected Set<SampleKey> getIdentifiers() {
            return identifiers;
        }
    }

    public static class SampleKey {
        /* empty */
    }
}
