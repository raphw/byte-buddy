package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MethodLookupEngineOverriddenClassMethodTest {

    private static final int MODIFIERS = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription firstType, secondType;

    @Mock
    private MethodDescription first, second;

    @Before
    public void setUp() throws Exception {
        when(first.getDeclaringType()).thenReturn(firstType);
        when(second.getDeclaringType()).thenReturn(secondType);
        when(first.getModifiers()).thenReturn(MODIFIERS);
    }

    @Test
    public void testOverridingMethodDominates() throws Exception {
        MethodDescription overriddenClassMethod = MethodLookupEngine.OverriddenClassMethod.of(first, second);
        assertThat(overriddenClassMethod.getDeclaringType(), is(firstType));
        assertThat(overriddenClassMethod.getModifiers(), is(MODIFIERS));
    }

    @Test
    public void testOverridenMethodIsSpecializableCascades() throws Exception {
        when(second.isSpecializableFor(firstType)).thenReturn(true);
        MethodDescription overriddenClassMethod = MethodLookupEngine.OverriddenClassMethod.of(first, second);
        assertThat(overriddenClassMethod.isSpecializableFor(firstType), is(true));
        verify(first).isSpecializableFor(firstType);
        verify(second).isSpecializableFor(firstType);
    }
}
