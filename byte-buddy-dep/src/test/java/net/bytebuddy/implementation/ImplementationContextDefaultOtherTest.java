package net.bytebuddy.implementation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

public class ImplementationContextDefaultOtherTest {

    @Test
    public void testFactory() throws Exception {
        assertThat(Implementation.Context.Default.Factory.INSTANCE.make(mock(TypeDescription.class),
                        mock(AuxiliaryType.NamingStrategy.class),
                        mock(InstrumentedType.TypeInitializer.class),
                        mock(ClassFileVersion.class)), instanceOf(Implementation.Context.Default.class));
    }

    @Test
    public void testTypeInitializerNotRetained() throws Exception {
        assertThat(new Implementation.Context.Default(mock(TypeDescription.class),
                mock(AuxiliaryType.NamingStrategy.class),
                mock(InstrumentedType.TypeInitializer.class),
                mock(ClassFileVersion.class)).isRetainTypeInitializer(), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Implementation.Context.Default.class).applyBasic();
        ObjectPropertyAssertion.of(Implementation.Context.Default.FieldCacheEntry.class).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.AccessorMethodDelegation.class).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.FieldSetterDelegation.class).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.FieldGetterDelegation.class).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.Factory.class).apply();
    }
}
