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
        assertThat(PropertyDispatcher.TypeRenderer.FOR_LEGACY_VM.render(Object.class), is(Object.class.toString()));
        assertThat(PropertyDispatcher.TypeRenderer.FOR_LEGACY_VM.render(TypeDescription.OBJECT), is(Object.class.toString()));
    }

    @Test
    public void testModernVm() throws Exception {
        assertThat(PropertyDispatcher.TypeRenderer.FOR_JAVA9_CAPABLE_VM.render(Object.class), is(Object.class.getName() + ".class"));
        assertThat(PropertyDispatcher.TypeRenderer.FOR_JAVA9_CAPABLE_VM.render(TypeDescription.OBJECT), is(Object.class.getName() + ".class"));
    }

    @Test
    public void testLegacyVmArray() throws Exception {
        assertThat(PropertyDispatcher.TypeRenderer.FOR_LEGACY_VM.render(new Class<?>[]{Object.class}), is("[" + Object.class.toString() + "]"));
        assertThat(PropertyDispatcher.TypeRenderer.FOR_LEGACY_VM.render(new TypeDescription[]{TypeDescription.OBJECT}), is("[" + Object.class.toString() + "]"));
    }

    @Test
    public void testModernVmArray() throws Exception {
        assertThat(PropertyDispatcher.TypeRenderer.FOR_JAVA9_CAPABLE_VM.render(new Class<?>[]{Object.class}), is("{" + Object.class.getName() + ".class}"));
        assertThat(PropertyDispatcher.TypeRenderer.FOR_JAVA9_CAPABLE_VM.render(new TypeDescription[]{TypeDescription.OBJECT}), is("{" + Object.class.getName() + ".class}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLegacyVmIllegalArgument() throws Exception {
        PropertyDispatcher.TypeRenderer.FOR_LEGACY_VM.render(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testModernVmIllegalArgument() throws Exception {
        PropertyDispatcher.TypeRenderer.FOR_LEGACY_VM.render(new Object());
    }

    @Test
    public void testLegacyVmBrace() throws Exception {
        assertThat(PropertyDispatcher.TypeRenderer.FOR_LEGACY_VM.getOpen(), is('['));
        assertThat(PropertyDispatcher.TypeRenderer.FOR_LEGACY_VM.getClose(), is(']'));
    }

    @Test
    public void testModernVmBrace() throws Exception {
        assertThat(PropertyDispatcher.TypeRenderer.FOR_JAVA9_CAPABLE_VM.getOpen(), is('{'));
        assertThat(PropertyDispatcher.TypeRenderer.FOR_JAVA9_CAPABLE_VM.getClose(), is('}'));
    }

    @Test
    public void testCurrent() throws Exception {
        assertThat(PropertyDispatcher.TypeRenderer.CURRENT, is(ClassFileVersion.ofThisVm().isAtLeast(ClassFileVersion.JAVA_V9)
                ? PropertyDispatcher.TypeRenderer.FOR_JAVA9_CAPABLE_VM
                : PropertyDispatcher.TypeRenderer.FOR_LEGACY_VM));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(PropertyDispatcher.class).apply();
        ObjectPropertyAssertion.of(PropertyDispatcher.TypeRenderer.class).apply();
    }
}
