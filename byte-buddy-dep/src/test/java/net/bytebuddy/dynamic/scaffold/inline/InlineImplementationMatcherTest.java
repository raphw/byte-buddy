package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMethodMatcher;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class InlineImplementationMatcherTest {

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
        latentMethodMatcher = new InliningImplementationMatcher(ignoredMethods, predefinedMethods);
        when(typeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(typeDescription.asErasure()).thenReturn(typeDescription);
        when(otherType.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(otherType.asErasure()).thenReturn(otherType);
    }

    @Test
    public void testMatchesVirtual() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(true);
        when(methodDescription.getModifiers()).thenReturn(0);
        when(ignoredMethods.matches(methodDescription)).thenReturn(false);
        when(predefinedMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(otherType);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(true));
    }

    @Test
    public void testNotMatchesVirtualAndFinal() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(true);
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_FINAL);
        when(ignoredMethods.matches(methodDescription)).thenReturn(false);
        when(predefinedMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(otherType);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(false));
    }

    @Test
    public void testMatchesDeclaredNotTargetType() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(false);
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_FINAL);
        when(ignoredMethods.matches(methodDescription)).thenReturn(false);
        when(predefinedMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(true));
    }

    @Test
    public void testMatchesDeclaredButIgnoredNotPredefined() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(false);
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_FINAL);
        when(ignoredMethods.matches(methodDescription)).thenReturn(true);
        when(predefinedMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(true));
    }

    @Test
    public void testMatchesDeclaredButIgnoredPredefined() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(false);
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_FINAL);
        when(ignoredMethods.matches(methodDescription)).thenReturn(true);
        when(predefinedMethods.matches(methodDescription)).thenReturn(true);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(false));
    }

    @Test
    public void testNotMatchesOverridableIgnored() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(true);
        when(methodDescription.getModifiers()).thenReturn(0);
        when(ignoredMethods.matches(methodDescription)).thenReturn(true);
        when(predefinedMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(otherType);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(InliningImplementationMatcher.class).apply();
    }
}
