package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.dynamic.scaffold.subclass.SubclassDynamicTypeBuilder;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMethodMatcher;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class InlineInstrumentationMatcherTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private TypeDescription typeDescription, otherType;

    @Mock
    private ElementMatcher<? super MethodDescription> ignoredMethods, predefinedMethods;

    private LatentMethodMatcher latentMethodMatcher;

    @Before
    public void setUp() throws Exception {
        latentMethodMatcher = new InlineInstrumentationMatcher(ignoredMethods, predefinedMethods);
    }

    @Test
    public void testMatchesOverridable() throws Exception {
        when(methodDescription.isOverridable()).thenReturn(true);
        when(ignoredMethods.matches(methodDescription)).thenReturn(false);
        when(predefinedMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(otherType);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(true));
    }

    @Test
    public void testMatchesDeclaredNotTargetType() throws Exception {
        when(methodDescription.isOverridable()).thenReturn(false);
        when(ignoredMethods.matches(methodDescription)).thenReturn(false);
        when(predefinedMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(true));
    }

    @Test
    public void testMatchesDeclaredButIgnoredNotPredefined() throws Exception {
        when(methodDescription.isOverridable()).thenReturn(false);
        when(ignoredMethods.matches(methodDescription)).thenReturn(true);
        when(predefinedMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(true));
    }

    @Test
    public void testMatchesDeclaredButIgnoredPredefined() throws Exception {
        when(methodDescription.isOverridable()).thenReturn(false);
        when(ignoredMethods.matches(methodDescription)).thenReturn(true);
        when(predefinedMethods.matches(methodDescription)).thenReturn(true);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(false));
    }

    @Test
    public void testNotMatchesOverridableIgnored() throws Exception {
        when(methodDescription.isOverridable()).thenReturn(true);
        when(ignoredMethods.matches(methodDescription)).thenReturn(true);
        when(predefinedMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(otherType);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(InlineInstrumentationMatcher.class).apply();
    }
}
