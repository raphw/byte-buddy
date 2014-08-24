package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TrivialTypeTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassFileVersion classFileVersion;

    @Mock
    private AuxiliaryType.MethodAccessorFactory methodAccessorFactory;

    @Test
    public void testCreation() throws Exception {
        DynamicType dynamicType = TrivialType.INSTANCE.make(FOO, classFileVersion, methodAccessorFactory);
        assertThat(dynamicType.getDescription().getName(), is(FOO));
        assertThat(dynamicType.getDescription().getModifiers(), is(Opcodes.ACC_SYNTHETIC));
        assertThat(dynamicType.getRawAuxiliaryTypes().size(), is(0));
        assertThat(dynamicType.getTypeInitializers().get(dynamicType.getDescription()).isAlive(), is(false));
    }
}
