package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verifyZeroInteractions;

public class EmptyDefaultsProviderTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Instrumentation.Target instrumentationTarget;

    @Mock
    private MethodDescription left, right;

    @Test(expected = NoSuchElementException.class)
    public void testEmptyIteration() throws Exception {
        Iterator<?> iterator = TargetMethodAnnotationDrivenBinder.DefaultsProvider.Empty.INSTANCE
                .makeIterator(instrumentationTarget, left, right);
        assertThat(iterator.hasNext(), is(false));
        verifyZeroInteractions(instrumentationTarget);
        verifyZeroInteractions(left);
        verifyZeroInteractions(right);
        iterator.next();
    }
}
