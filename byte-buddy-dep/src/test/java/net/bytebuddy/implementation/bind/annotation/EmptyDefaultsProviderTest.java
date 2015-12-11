package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;

public class EmptyDefaultsProviderTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Implementation.Target implementationTarget;

    @Mock
    private MethodDescription left, right;

    @Test(expected = NoSuchElementException.class)
    public void testEmptyIteration() throws Exception {
        Iterator<?> iterator = TargetMethodAnnotationDrivenBinder.DefaultsProvider.Empty.INSTANCE
                .makeIterator(implementationTarget, left, right);
        assertThat(iterator.hasNext(), is(false));
        verifyZeroInteractions(implementationTarget);
        verifyZeroInteractions(left);
        verifyZeroInteractions(right);
        iterator.next();
    }
}
