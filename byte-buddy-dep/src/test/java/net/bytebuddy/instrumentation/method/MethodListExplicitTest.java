package net.bytebuddy.instrumentation.method;

import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodListExplicitTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription firstMethodDescription, secondMethodDescription;

    private MethodList methodList;

    @Before
    public void setUp() throws Exception {
        methodList = new MethodList.Explicit(Arrays.asList(firstMethodDescription, secondMethodDescription));
    }

    @Test
    public void testMethodList() throws Exception {
        assertThat(methodList.size(), is(2));
        assertThat(methodList.get(0), is(firstMethodDescription));
        assertThat(methodList.get(1), is(secondMethodDescription));
    }

    @Test
    public void testMethodListFilter() throws Exception {
        MethodMatcher methodMatcher = mock(MethodMatcher.class);
        when(methodMatcher.matches(firstMethodDescription)).thenReturn(true);
        methodList = methodList.filter(methodMatcher);
        assertThat(methodList.size(), is(1));
        assertThat(methodList.getOnly(), is(firstMethodDescription));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetOnly() throws Exception {
        methodList.getOnly();
    }

    @Test
    public void testSubList() throws Exception {
        assertThat(methodList.subList(0, 1), is((MethodList) new MethodList.Explicit(Arrays.asList(firstMethodDescription))));
    }
}
