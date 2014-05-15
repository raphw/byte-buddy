package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class BridgeMethodResolverSimpleTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private BridgeMethodResolver.Simple.ConflictHandler conflictHandler;
    @Mock
    private BridgeMethodResolver.Simple.BridgeTarget bridgeTarget;
    @Mock
    private MethodDescription methodDescription;

    @Test
    public void testFindsBridgeMethodSingleStep() throws Exception {
        TypeDescription target = new TypeDescription.ForLoadedType(Bar.class);
        MethodList reachableMethods = new MethodLookupEngine.Default().getReachableMethods(target);
        MethodList relevantMethods = reachableMethods.filter(not(isConstructor().or(isDeclaredBy(Object.class))));
        assertThat(relevantMethods.size(), is(2));
        BridgeMethodResolver bridgeMethodResolver = new BridgeMethodResolver.Simple(reachableMethods, conflictHandler);
        assertThat(bridgeMethodResolver.resolve(relevantMethods.filter(isBridge()).getOnly()),
                is(relevantMethods.filter(not(isBridge())).getOnly()));
        verifyZeroInteractions(conflictHandler);
    }

    @Test
    public void testFindsBridgeMethodTwoStep() throws Exception {
        TypeDescription target = new TypeDescription.ForLoadedType(Qux.class);
        MethodList reachableMethods = new MethodLookupEngine.Default().getReachableMethods(target);
        MethodList relevantMethods = reachableMethods.filter(not(isConstructor().or(isDeclaredBy(Object.class))));
        assertThat(relevantMethods.size(), is(3));
        BridgeMethodResolver bridgeMethodResolver = new BridgeMethodResolver.Simple(reachableMethods, conflictHandler);
        for (MethodDescription methodDescription : relevantMethods.filter(isBridge())) {
            assertThat(bridgeMethodResolver.resolve(methodDescription), is(relevantMethods.filter(not(isBridge())).getOnly()));
        }
        verifyZeroInteractions(conflictHandler);
    }

    @Test
    public void testFindsBridgeMethodConflictResolver() throws Exception {
        TypeDescription target = new TypeDescription.ForLoadedType(Baz.class);
        MethodList reachableMethods = new MethodLookupEngine.Default().getReachableMethods(target);
        MethodList relevantMethods = reachableMethods.filter(not(isConstructor().or(isDeclaredBy(Object.class))));
        assertThat(relevantMethods.size(), is(3));
        when(conflictHandler.choose(any(MethodDescription.class), any(MethodList.class))).thenReturn(bridgeTarget);
        when(bridgeTarget.isResolved()).thenReturn(true);
        when(bridgeTarget.extract()).thenReturn(methodDescription);
        BridgeMethodResolver bridgeMethodResolver = new BridgeMethodResolver.Simple(reachableMethods, conflictHandler);
        assertThat(bridgeMethodResolver.resolve(relevantMethods.filter(isBridge()).getOnly()), is(methodDescription));
        verify(conflictHandler).choose(relevantMethods.filter(isBridge()).getOnly(), relevantMethods.filter(not(isBridge())));
        verifyNoMoreInteractions(conflictHandler);
        verify(bridgeTarget).isResolved();
        verify(bridgeTarget).extract();
        verifyNoMoreInteractions(bridgeTarget);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        MethodList first = new MethodList.ForLoadedType(Baz.class);
        MethodList second = new MethodList.ForLoadedType(Bar.class);
        assertThat(new BridgeMethodResolver.Simple(first, BridgeMethodResolver.Simple.ConflictHandler.Default.CALL_BRIDGE).hashCode(),
                is(new BridgeMethodResolver.Simple(first, BridgeMethodResolver.Simple.ConflictHandler.Default.CALL_BRIDGE).hashCode()));
        assertThat(new BridgeMethodResolver.Simple(first, BridgeMethodResolver.Simple.ConflictHandler.Default.CALL_BRIDGE),
                is(new BridgeMethodResolver.Simple(first, BridgeMethodResolver.Simple.ConflictHandler.Default.CALL_BRIDGE)));
        assertThat(new BridgeMethodResolver.Simple(first, BridgeMethodResolver.Simple.ConflictHandler.Default.CALL_BRIDGE).hashCode(),
                CoreMatchers.not(is(new BridgeMethodResolver.Simple(first, BridgeMethodResolver.Simple.ConflictHandler.Default.FAIL_ON_REQUEST).hashCode())));
        assertThat(new BridgeMethodResolver.Simple(first, BridgeMethodResolver.Simple.ConflictHandler.Default.CALL_BRIDGE),
                CoreMatchers.not(is(new BridgeMethodResolver.Simple(first, BridgeMethodResolver.Simple.ConflictHandler.Default.FAIL_ON_REQUEST))));
        assertThat(new BridgeMethodResolver.Simple(first, BridgeMethodResolver.Simple.ConflictHandler.Default.CALL_BRIDGE).hashCode(),
                CoreMatchers.not(is(new BridgeMethodResolver.Simple(second, BridgeMethodResolver.Simple.ConflictHandler.Default.CALL_BRIDGE).hashCode())));
        assertThat(new BridgeMethodResolver.Simple(first, BridgeMethodResolver.Simple.ConflictHandler.Default.CALL_BRIDGE),
                CoreMatchers.not(is(new BridgeMethodResolver.Simple(second, BridgeMethodResolver.Simple.ConflictHandler.Default.CALL_BRIDGE))));
    }

    @Test
    public void testBridgeTargetCandidate() throws Exception {
        assertThat(new BridgeMethodResolver.Simple.BridgeTarget.Candidate(methodDescription).isResolved(), is(false));
        assertThat(new BridgeMethodResolver.Simple.BridgeTarget.Candidate(methodDescription).extract(), is(methodDescription));
    }

    @Test
    public void testBridgeTargetResolved() throws Exception {
        assertThat(new BridgeMethodResolver.Simple.BridgeTarget.Resolved(methodDescription).isResolved(), is(true));
        assertThat(new BridgeMethodResolver.Simple.BridgeTarget.Resolved(methodDescription).extract(), is(methodDescription));
    }

    @Test(expected = IllegalStateException.class)
    public void testBridgeTargetUnknown() throws Exception {
        assertThat(BridgeMethodResolver.Simple.BridgeTarget.Unknown.INSTANCE.isResolved(), is(true));
        BridgeMethodResolver.Simple.BridgeTarget.Unknown.INSTANCE.extract();
    }

    @Test
    public void testBridgeTargetHashCodeEquals() throws Exception {
        MethodDescription otherMethod = mock(MethodDescription.class);
        assertThat(new BridgeMethodResolver.Simple.BridgeTarget.Candidate(methodDescription).hashCode(),
                is(new BridgeMethodResolver.Simple.BridgeTarget.Candidate(methodDescription).hashCode()));
        assertThat(new BridgeMethodResolver.Simple.BridgeTarget.Candidate(methodDescription),
                is(new BridgeMethodResolver.Simple.BridgeTarget.Candidate(methodDescription)));
        assertThat(new BridgeMethodResolver.Simple.BridgeTarget.Candidate(methodDescription).hashCode(),
                CoreMatchers.not(is(new BridgeMethodResolver.Simple.BridgeTarget.Candidate(otherMethod).hashCode())));
        assertThat(new BridgeMethodResolver.Simple.BridgeTarget.Candidate(methodDescription),
                CoreMatchers.not(is(new BridgeMethodResolver.Simple.BridgeTarget.Candidate(otherMethod))));
        assertThat(new BridgeMethodResolver.Simple.BridgeTarget.Resolved(methodDescription).hashCode(),
                is(new BridgeMethodResolver.Simple.BridgeTarget.Resolved(methodDescription).hashCode()));
        assertThat(new BridgeMethodResolver.Simple.BridgeTarget.Resolved(methodDescription),
                is(new BridgeMethodResolver.Simple.BridgeTarget.Resolved(methodDescription)));
        assertThat(new BridgeMethodResolver.Simple.BridgeTarget.Resolved(methodDescription).hashCode(),
                CoreMatchers.not(is(new BridgeMethodResolver.Simple.BridgeTarget.Resolved(otherMethod).hashCode())));
        assertThat(new BridgeMethodResolver.Simple.BridgeTarget.Resolved(methodDescription),
                CoreMatchers.not(is(new BridgeMethodResolver.Simple.BridgeTarget.Resolved(otherMethod))));

    }

    private static class Foo<T> {

        public T foo(T t) {
            return null;
        }
    }

    private static class Bar<T extends Number> extends Foo<T> {

        @Override
        public T foo(T t) {
            return null;
        }
    }

    private static class Qux extends Bar<Integer> {

        @Override
        public Integer foo(Integer integer) {
            return null;
        }
    }

    private static class Baz extends Foo<Integer> {

        @Override
        public Integer foo(Integer i) {
            return null;
        }

        public String foo(String s) {
            return null;
        }
    }
}
