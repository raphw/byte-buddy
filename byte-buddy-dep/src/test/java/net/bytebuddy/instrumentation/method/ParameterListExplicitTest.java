package net.bytebuddy.instrumentation.method;

import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.test.utility.MockitoRule;
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

public class ParameterListExplicitTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ParameterDescription firstParameterDescription, secondParameterDescription;

    private ParameterList parameterList;

    @Before
    public void setUp() throws Exception {
        parameterList = new ParameterList.Explicit(Arrays.asList(firstParameterDescription, secondParameterDescription));
    }

    @Test
    public void testMethodList() throws Exception {
        assertThat(parameterList.size(), is(2));
        assertThat(parameterList.get(0), is(firstParameterDescription));
        assertThat(parameterList.get(1), is(secondParameterDescription));
    }

    @Test
    public void testMethodListFilter() throws Exception {
        @SuppressWarnings("unchecked")
        ElementMatcher<? super ParameterDescription> methodMatcher = mock(ElementMatcher.class);
        when(methodMatcher.matches(firstParameterDescription)).thenReturn(true);
        parameterList = parameterList.filter(methodMatcher);
        assertThat(parameterList.size(), is(1));
        assertThat(parameterList.getOnly(), is(firstParameterDescription));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetOnly() throws Exception {
        parameterList.getOnly();
    }

    @Test
    public void testSubList() throws Exception {
        assertThat(parameterList.subList(0, 1), is((ParameterList) new ParameterList.Explicit(Arrays.asList(firstParameterDescription))));
    }

    @Test
    public void testHasExplicitMetaDataTrue() throws Exception {
        when(firstParameterDescription.hasModifiers()).thenReturn(true);
        when(secondParameterDescription.hasModifiers()).thenReturn(true);
        when(firstParameterDescription.isNamed()).thenReturn(true);
        when(secondParameterDescription.isNamed()).thenReturn(true);
        assertThat(parameterList.hasExplicitMetaData(), is(true));
    }

    @Test
    public void testHasExplicitMetaDataFalse() throws Exception {
        assertThat(parameterList.hasExplicitMetaData(), is(false));
    }
}
