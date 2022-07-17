package net.bytebuddy.description.type;

import net.bytebuddy.ClassFileVersion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class TypeDescriptionGenericOfParameterizedTypeRenderingDelegateTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription typeDescription, ownerErasure;

    @Mock
    private TypeDescription.Generic ownerType;

    @Before
    public void setUp() throws Exception {
        when(typeDescription.getName()).thenReturn(FOO + "." + BAR);
        when(typeDescription.getSimpleName()).thenReturn(BAR);
        when(ownerType.getTypeName()).thenReturn(QUX + "." + BAZ);
        when(ownerType.asErasure()).thenReturn(ownerErasure);
        when(ownerErasure.getName()).thenReturn(FOO + "." + BAR);
    }

    @Test
    public void testJava8OwnerTypeParameterized() throws Exception {
        when(ownerType.getSort()).thenReturn(TypeDefinition.Sort.PARAMETERIZED);
        StringBuilder stringBuilder = new StringBuilder();
        TypeDescription.Generic.OfParameterizedType.RenderingDelegate.FOR_JAVA_8_CAPABLE_VM.apply(stringBuilder, typeDescription, ownerType);
        assertThat(stringBuilder.toString(), is(QUX + "." + BAZ + "$" + FOO + "." + BAR));
    }

    @Test
    public void testJava8NonParameterized() throws Exception {
        when(ownerType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        StringBuilder stringBuilder = new StringBuilder();
        TypeDescription.Generic.OfParameterizedType.RenderingDelegate.FOR_JAVA_8_CAPABLE_VM.apply(stringBuilder, typeDescription, ownerType);
        assertThat(stringBuilder.toString(), is(QUX + "." + BAZ + "$" + BAR));
    }

    @Test
    public void testJava8NoOwner() throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        TypeDescription.Generic.OfParameterizedType.RenderingDelegate.FOR_JAVA_8_CAPABLE_VM.apply(stringBuilder, typeDescription, null);
        assertThat(stringBuilder.toString(), is(FOO + "." + BAR));
    }

    @Test
    public void testLegacyParameterized() throws Exception {
        when(ownerType.getSort()).thenReturn(TypeDefinition.Sort.PARAMETERIZED);
        StringBuilder stringBuilder = new StringBuilder();
        TypeDescription.Generic.OfParameterizedType.RenderingDelegate.FOR_LEGACY_VM.apply(stringBuilder, typeDescription, ownerType);
        assertThat(stringBuilder.toString(), is(QUX + "." + BAZ + "." + BAR));
    }

    @Test
    public void testLegacyNonParameterized() throws Exception {
        when(ownerType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        StringBuilder stringBuilder = new StringBuilder();
        TypeDescription.Generic.OfParameterizedType.RenderingDelegate.FOR_LEGACY_VM.apply(stringBuilder, typeDescription, ownerType);
        assertThat(stringBuilder.toString(), is(QUX + "." + BAZ + "." + FOO + "." + BAR));
    }

    @Test
    public void testLegacy8NoOwner() throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        TypeDescription.Generic.OfParameterizedType.RenderingDelegate.FOR_LEGACY_VM.apply(stringBuilder, typeDescription, null);
        assertThat(stringBuilder.toString(), is(FOO + "." + BAR));
    }

    @Test
    public void testCurrent() throws Exception {
        assertThat(TypeDescription.Generic.OfParameterizedType.RenderingDelegate.CURRENT, is(ClassFileVersion.ofThisVm().isAtLeast(ClassFileVersion.JAVA_V8)
                ? TypeDescription.Generic.OfParameterizedType.RenderingDelegate.FOR_JAVA_8_CAPABLE_VM
                : TypeDescription.Generic.OfParameterizedType.RenderingDelegate.FOR_LEGACY_VM));
    }
}
