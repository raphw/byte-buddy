package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

public class InstrumentedTypeFactoryDefaultTest {

    @Test
    public void testSubclassModifiable() throws Exception {
        assertThat(InstrumentedType.Factory.Default.MODIFIABLE.subclass("foo", 0, TypeDescription.Generic.OBJECT), instanceOf(InstrumentedType.Default.class));
    }

    @Test
    public void testSubclassFrozen() throws Exception {
        assertThat(InstrumentedType.Factory.Default.FROZEN.subclass("foo", 0, TypeDescription.Generic.OBJECT), instanceOf(InstrumentedType.Default.class));
    }

    @Test
    public void testRepresentModifiable() throws Exception {
        assertThat(InstrumentedType.Factory.Default.MODIFIABLE.represent(TypeDescription.OBJECT), instanceOf(InstrumentedType.Default.class));
    }

    @Test
    public void testRepresentFrozen() throws Exception {
        assertThat(InstrumentedType.Factory.Default.FROZEN.represent(TypeDescription.OBJECT), instanceOf(InstrumentedType.Frozen.class));
    }
}
