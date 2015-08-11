package net.bytebuddy.dynamic.scaffold.subclass;

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

public class SubclassDynamicTypeBuilderInstrumentableMatcherTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private TypeDescription typeDescription, otherType;

    @Mock
    private ElementMatcher<? super MethodDescription> ignoredMethods;

    private LatentMethodMatcher latentMethodMatcher;

    @Before
    public void setUp() throws Exception {
        latentMethodMatcher = new SubclassDynamicTypeBuilder.InstrumentableMatcher(ignoredMethods);
        when(typeDescription.asErasure()).thenReturn(typeDescription);
        when(typeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(otherType.asErasure()).thenReturn(otherType);
        when(otherType.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
    }

    @Test
    public void testMatchesVirtual() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(true);
        when(methodDescription.getModifiers()).thenReturn(0);
        when(ignoredMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(otherType);
        when(methodDescription.isVisibleTo(typeDescription)).thenReturn(true);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(true));
    }

    @Test
    public void testNotMatchesVirtualIfNotVisible() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(true);
        when(methodDescription.getModifiers()).thenReturn(0);
        when(ignoredMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(otherType);
        when(methodDescription.isVisibleTo(typeDescription)).thenReturn(false);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(false));
    }

    @Test
    public void testNotMatchesVirtualIfFinal() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(true);
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_FINAL);
        when(ignoredMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(otherType);
        when(methodDescription.isVisibleTo(typeDescription)).thenReturn(false);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(false));
    }

    @Test
    public void testNotMatchesNonVirtualIfNotDeclared() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(false);
        when(methodDescription.getModifiers()).thenReturn(0);
        when(ignoredMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(otherType);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(false));
    }

    @Test
    public void testNotMatchesIgnoredMethodIfNotDeclared() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(true);
        when(methodDescription.getModifiers()).thenReturn(0);
        when(ignoredMethods.matches(methodDescription)).thenReturn(true);
        when(methodDescription.getDeclaringType()).thenReturn(otherType);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(false));
    }

    @Test
    public void testMatchesDeclaredMethod() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(true);
        when(methodDescription.getModifiers()).thenReturn(0);
        when(ignoredMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(true));
    }

    @Test
    public void testMatchesDeclaredMethodIfIgnored() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(true);
        when(methodDescription.getModifiers()).thenReturn(0);
        when(ignoredMethods.matches(methodDescription)).thenReturn(true);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(true));
    }

    @Test
    public void testMatchesDeclaredMethodIfNotVirtual() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(false);
        when(methodDescription.getModifiers()).thenReturn(0);
        when(ignoredMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(true));
    }

    @Test
    public void testMatchesDeclaredMethodIfFinal() throws Exception {
        when(methodDescription.isVirtual()).thenReturn(true);
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_FINAL);
        when(ignoredMethods.matches(methodDescription)).thenReturn(false);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        assertThat(latentMethodMatcher.resolve(typeDescription).matches(methodDescription), is(true));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(SubclassDynamicTypeBuilder.InstrumentableMatcher.class).apply();
    }
}
