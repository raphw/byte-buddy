package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static net.bytebuddy.test.utility.CustomHamcrestMatchers.containsAllOf;
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
        MethodList invokableMethods = new MethodLookupEngine.Default(MethodLookupEngine.Default.DefaultMethodLookup.DISABLED)
                .process(target).getInvokableMethods();
        MethodList relevantMethods = invokableMethods.filter(not(isDeclaredBy(Object.class).or(isConstructor())));
        assertThat(relevantMethods.size(), is(2));
        BridgeMethodResolver bridgeMethodResolver = BridgeMethodResolver.Simple.of(invokableMethods, conflictHandler);
        assertThat(bridgeMethodResolver.resolve(relevantMethods.filter(isBridge()).getOnly()),
                is(relevantMethods.filter(not(isBridge())).getOnly()));
        verifyZeroInteractions(conflictHandler);
    }

    @Test
    public void testFindsBridgeMethodTwoStep() throws Exception {
        TypeDescription target = new TypeDescription.ForLoadedType(Qux.class);
        MethodList invokableMethods = new MethodLookupEngine.Default(MethodLookupEngine.Default.DefaultMethodLookup.DISABLED)
                .process(target).getInvokableMethods();
        MethodList relevantMethods = invokableMethods.filter(not(isDeclaredBy(Object.class).or(isConstructor())));
        assertThat(relevantMethods.size(), is(3));
        BridgeMethodResolver bridgeMethodResolver = BridgeMethodResolver.Simple.of(invokableMethods, conflictHandler);
        for (MethodDescription methodDescription : relevantMethods.filter(isBridge())) {
            assertThat(bridgeMethodResolver.resolve(methodDescription), is(relevantMethods.filter(not(isBridge())).getOnly()));
        }
        verifyZeroInteractions(conflictHandler);
    }

    @Test
    public void testFindsBridgeMethodConflictResolver() throws Exception {
        TypeDescription target = new TypeDescription.ForLoadedType(Baz.class);
        MethodList invokableMethods = new MethodLookupEngine.Default(MethodLookupEngine.Default.DefaultMethodLookup.DISABLED)
                .process(target).getInvokableMethods();
        MethodList relevantMethods = invokableMethods.filter(not(isDeclaredBy(Object.class).or(isConstructor())));
        assertThat(relevantMethods.size(), is(3));
        when(conflictHandler.choose(any(MethodDescription.class), any(MethodList.class))).thenReturn(bridgeTarget);
        when(bridgeTarget.isResolved()).thenReturn(true);
        when(bridgeTarget.extract()).thenReturn(methodDescription);
        BridgeMethodResolver bridgeMethodResolver = BridgeMethodResolver.Simple.of(invokableMethods, conflictHandler);
        assertThat(bridgeMethodResolver.resolve(relevantMethods.filter(isBridge()).getOnly()), is(methodDescription));
        ArgumentCaptor<MethodList> capturedConflictHandlerCandidates = ArgumentCaptor.forClass(MethodList.class);
        verify(conflictHandler).choose(eq(relevantMethods.filter(isBridge()).getOnly()), capturedConflictHandlerCandidates.capture());
        assertThat(capturedConflictHandlerCandidates.getValue().size(), is(2));
        assertThat(capturedConflictHandlerCandidates.getValue(), containsAllOf(relevantMethods.filter(not(isBridge()))));
        verifyNoMoreInteractions(conflictHandler);
        verify(bridgeTarget).isResolved();
        verify(bridgeTarget).extract();
        verifyNoMoreInteractions(bridgeTarget);
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
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(BridgeMethodResolver.Simple.class).create(new ObjectPropertyAssertion.Creator<MethodList>() {
            @Override
            public MethodList create() {
                return null;
            }
        }).apply();
        ObjectPropertyAssertion.of(BridgeMethodResolver.Simple.Factory.class).apply();
        ObjectPropertyAssertion.of(BridgeMethodResolver.Simple.BridgeTarget.Resolved.class).apply();
        ObjectPropertyAssertion.of(BridgeMethodResolver.Simple.ConflictHandler.Default.class).apply();
        ObjectPropertyAssertion.of(BridgeMethodResolver.Simple.BridgeTarget.Candidate.class).apply();
        ObjectPropertyAssertion.of(BridgeMethodResolver.Simple.BridgeTarget.Resolved.class).apply();
        ObjectPropertyAssertion.of(BridgeMethodResolver.Simple.BridgeTarget.Unknown.class).apply();
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
