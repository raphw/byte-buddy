package net.bytebuddy.utility;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PropertyDispatcherOtherTest {

    @Test
    public void testLegacyVm() throws Exception {
        assertThat(PropertyDispatcher.RenderingDispatcher.FOR_LEGACY_VM.toSourceString(Object.class), is(Object.class.toString()));
        assertThat(PropertyDispatcher.RenderingDispatcher.FOR_LEGACY_VM.toSourceString(TypeDescription.OBJECT), is(Object.class.toString()));
    }

    @Test
    public void testModernVm() throws Exception {
        assertThat(PropertyDispatcher.RenderingDispatcher.FOR_JAVA9_CAPABLE_VM.toSourceString(Object.class), is(Object.class.getName() + ".class"));
        assertThat(PropertyDispatcher.RenderingDispatcher.FOR_JAVA9_CAPABLE_VM.toSourceString(TypeDescription.OBJECT), is(Object.class.getName() + ".class"));
    }

    @Test
    public void testLegacyVmArray() throws Exception {
        assertThat(PropertyDispatcher.RenderingDispatcher.FOR_LEGACY_VM.toSourceString(new Class<?>[]{Object.class}), is("[" + Object.class.toString() + "]"));
        assertThat(PropertyDispatcher.RenderingDispatcher.FOR_LEGACY_VM.toSourceString(new TypeDescription[]{TypeDescription.OBJECT}), is("[" + Object.class.toString() + "]"));
    }

    @Test
    public void testModernVmArray() throws Exception {
        assertThat(PropertyDispatcher.RenderingDispatcher.FOR_JAVA9_CAPABLE_VM.toSourceString(new Class<?>[]{Object.class}), is("{" + Object.class.getName() + ".class}"));
        assertThat(PropertyDispatcher.RenderingDispatcher.FOR_JAVA9_CAPABLE_VM.toSourceString(new TypeDescription[]{TypeDescription.OBJECT}), is("{" + Object.class.getName() + ".class}"));
    }

    @Test
    public void testLegacyVmBrace() throws Exception {
        assertThat(PropertyDispatcher.RenderingDispatcher.FOR_LEGACY_VM.getOpeningBrace(), is('['));
        assertThat(PropertyDispatcher.RenderingDispatcher.FOR_LEGACY_VM.getClosingBrace(), is(']'));
    }

    @Test
    public void testModernVmBrace() throws Exception {
        assertThat(PropertyDispatcher.RenderingDispatcher.FOR_JAVA9_CAPABLE_VM.getOpeningBrace(), is('{'));
        assertThat(PropertyDispatcher.RenderingDispatcher.FOR_JAVA9_CAPABLE_VM.getClosingBrace(), is('}'));
    }

    @Test
    public void testCurrent() throws Exception {
        assertThat(PropertyDispatcher.RenderingDispatcher.CURRENT, is(ClassFileVersion.ofThisVm().isAtLeast(ClassFileVersion.JAVA_V9)
                ? PropertyDispatcher.RenderingDispatcher.FOR_JAVA9_CAPABLE_VM
                : PropertyDispatcher.RenderingDispatcher.FOR_LEGACY_VM));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(PropertyDispatcher.class).apply();
        ObjectPropertyAssertion.of(PropertyDispatcher.RenderingDispatcher.class).apply();
    }
}
