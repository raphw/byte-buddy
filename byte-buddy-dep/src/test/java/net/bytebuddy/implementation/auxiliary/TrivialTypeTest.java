package net.bytebuddy.implementation.auxiliary;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class TrivialTypeTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassFileVersion classFileVersion;

    @Mock
    private AuxiliaryType.MethodAccessorFactory methodAccessorFactory;

    @Test
    public void testPlain() throws Exception {
        when(classFileVersion.getMinorMajorVersion()).thenReturn(ClassFileVersion.JAVA_V5.getMinorMajorVersion());
        DynamicType dynamicType = TrivialType.PLAIN.make(FOO, classFileVersion, methodAccessorFactory);
        assertThat(dynamicType.getTypeDescription().getName(), is(FOO));
        assertThat(dynamicType.getTypeDescription().getModifiers(), is(Opcodes.ACC_SYNTHETIC));
        assertThat(dynamicType.getTypeDescription().getDeclaredAnnotations().size(), is(0));
        assertThat(dynamicType.getAuxiliaryTypes().size(), is(0));
        assertThat(dynamicType.getLoadedTypeInitializers().get(dynamicType.getTypeDescription()).isAlive(), is(false));
    }

    @Test
    public void testEager() throws Exception {
        when(classFileVersion.getMinorMajorVersion()).thenReturn(ClassFileVersion.JAVA_V5.getMinorMajorVersion());
        DynamicType dynamicType = TrivialType.SIGNATURE_RELEVANT.make(FOO, classFileVersion, methodAccessorFactory);
        assertThat(dynamicType.getTypeDescription().getName(), is(FOO));
        assertThat(dynamicType.getTypeDescription().getModifiers(), is(Opcodes.ACC_SYNTHETIC));
        assertThat(dynamicType.getTypeDescription().getDeclaredAnnotations().size(), is(1));
        assertThat(dynamicType.getTypeDescription().getDeclaredAnnotations().isAnnotationPresent(AuxiliaryType.SignatureRelevant.class), is(true));
        assertThat(dynamicType.getAuxiliaryTypes().size(), is(0));
        assertThat(dynamicType.getLoadedTypeInitializers().get(dynamicType.getTypeDescription()).isAlive(), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TrivialType.class).apply();
    }
}
