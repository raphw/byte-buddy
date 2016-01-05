package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMatcher;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class InlineImplementationMatcherTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private TypeDescription rawTypeDescription, rawOtherType;

    @Mock
    private TypeDescription.Generic typeDescription, otherType;

    @Mock
    private LatentMatcher<? super MethodDescription> latentIgnoredMethods;

    @Mock
    private ElementMatcher<? super MethodDescription> predefinedMethods, ignoredMethods;

    private LatentMatcher<MethodDescription> matcher;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        matcher = new InliningImplementationMatcher(latentIgnoredMethods, predefinedMethods);
        when(rawTypeDescription.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(rawTypeDescription.asGenericType()).thenReturn(typeDescription);
        when(typeDescription.asErasure()).thenReturn(rawTypeDescription);
        when(typeDescription.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(otherType.asErasure()).thenReturn(rawOtherType);
        when(otherType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(rawOtherType.asGenericType()).thenReturn(otherType);
        when(latentIgnoredMethods.resolve(Mockito.any(TypeDescription.class))).thenReturn((ElementMatcher) ignoredMethods);
    }

    @Test
    public void testMatchesVirtual() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(true);
        when(methodDescription.getModifiers()).thenReturn(0);
        when(ignoredMethods.matches(methodDescription)).thenReturn(false);
        when(predefinedMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(rawOtherType);
        assertThat(matcher.resolve(rawTypeDescription).matches(methodDescription), is(true));
    }

    @Test
    public void testNotMatchesVirtualAndFinal() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(true);
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_FINAL);
        when(ignoredMethods.matches(methodDescription)).thenReturn(false);
        when(predefinedMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(rawOtherType);
        assertThat(matcher.resolve(rawTypeDescription).matches(methodDescription), is(false));
    }

    @Test
    public void testMatchesDeclaredNotTargetType() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(false);
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_FINAL);
        when(ignoredMethods.matches(methodDescription)).thenReturn(false);
        when(predefinedMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(rawTypeDescription);
        assertThat(matcher.resolve(rawTypeDescription).matches(methodDescription), is(true));
    }

    @Test
    public void testMatchesDeclaredButIgnoredNotPredefined() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(false);
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_FINAL);
        when(ignoredMethods.matches(methodDescription)).thenReturn(true);
        when(predefinedMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(rawTypeDescription);
        assertThat(matcher.resolve(rawTypeDescription).matches(methodDescription), is(true));
    }

    @Test
    public void testMatchesDeclaredButIgnoredPredefined() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(false);
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_FINAL);
        when(ignoredMethods.matches(methodDescription)).thenReturn(true);
        when(predefinedMethods.matches(methodDescription)).thenReturn(true);
        when(methodDescription.getDeclaringType()).thenReturn(rawTypeDescription);
        assertThat(matcher.resolve(rawTypeDescription).matches(methodDescription), is(false));
    }

    @Test
    public void testNotMatchesOverridableIgnored() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(true);
        when(methodDescription.getModifiers()).thenReturn(0);
        when(ignoredMethods.matches(methodDescription)).thenReturn(true);
        when(predefinedMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(rawOtherType);
        assertThat(matcher.resolve(rawTypeDescription).matches(methodDescription), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(InliningImplementationMatcher.class).apply();
    }
}
