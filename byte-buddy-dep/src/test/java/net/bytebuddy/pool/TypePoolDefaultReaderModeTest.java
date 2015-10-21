package net.bytebuddy.pool;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypePoolDefaultReaderModeTest {

    @Test
    public void testDefinition() throws Exception {
        assertThat(TypePool.Default.ReaderMode.EXTENDED.isExtended(), is(true));
        assertThat(TypePool.Default.ReaderMode.FAST.isExtended(), is(false));
    }

    @Test
    public void testFlags() throws Exception {
        assertThat(TypePool.Default.ReaderMode.EXTENDED.getFlags(), is(ClassReader.SKIP_FRAMES));
        assertThat(TypePool.Default.ReaderMode.FAST.getFlags(), is(ClassReader.SKIP_CODE));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.Default.ReaderMode.class).apply();
    }
}