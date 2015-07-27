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

    private static final int BAR = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassFileVersion classFileVersion;

    @Mock
    private AuxiliaryType.MethodAccessorFactory methodAccessorFactory;

    @Test
    public void testCreation() throws Exception {
        when(classFileVersion.getVersion()).thenReturn(BAR);
        DynamicType dynamicType = TrivialType.INSTANCE.make(FOO, classFileVersion, methodAccessorFactory);
        assertThat(dynamicType.getTypeDescription().getName(), is(FOO));
        assertThat(dynamicType.getTypeDescription().getModifiers(), is(Opcodes.ACC_SYNTHETIC));
        assertThat(dynamicType.getRawAuxiliaryTypes().size(), is(0));
        assertThat(dynamicType.getLoadedTypeInitializers().get(dynamicType.getTypeDescription()).isAlive(), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TrivialType.class).apply();
    }
}
