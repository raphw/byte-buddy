package net.bytebuddy.description.type;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class TypeDescriptionGenericOfParameterizedTypeRenderingDelegateTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private TypeDescription.Generic ownerType;

    @Before
    public void setUp() throws Exception {
        when(typeDescription.getName()).thenReturn(FOO + "." + BAR);
        when(typeDescription.getSimpleName()).thenReturn(BAR);
    }

    @Test
    public void testJava9Capable() throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        TypeDescription.Generic.OfParameterizedType.RenderingDelegate.JAVA_9_CAPABLE_VM.apply(stringBuilder, typeDescription, ownerType);
        assertThat(stringBuilder.toString(), is("$" + BAR));
    }

    @Test
    public void testLegacyParameterized() throws Exception {
        when(ownerType.getSort()).thenReturn(TypeDefinition.Sort.PARAMETERIZED);
        StringBuilder stringBuilder = new StringBuilder();
        TypeDescription.Generic.OfParameterizedType.RenderingDelegate.LEGACY_VM.apply(stringBuilder, typeDescription, ownerType);
        assertThat(stringBuilder.toString(), is("." + BAR));
    }

    @Test
    public void testLegacyNonParameterized() throws Exception {
        when(ownerType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        StringBuilder stringBuilder = new StringBuilder();
        TypeDescription.Generic.OfParameterizedType.RenderingDelegate.LEGACY_VM.apply(stringBuilder, typeDescription, ownerType);
        assertThat(stringBuilder.toString(), is("." + FOO + "." + BAR));
    }

    @Test
    public void testCurrent() throws Exception {
        assertThat(TypeDescription.Generic.OfParameterizedType.RenderingDelegate.CURRENT, is(ClassFileVersion.ofThisVm().isAtLeast(ClassFileVersion.JAVA_V9)
                ? TypeDescription.Generic.OfParameterizedType.RenderingDelegate.JAVA_9_CAPABLE_VM
                : TypeDescription.Generic.OfParameterizedType.RenderingDelegate.LEGACY_VM));
    }
}
