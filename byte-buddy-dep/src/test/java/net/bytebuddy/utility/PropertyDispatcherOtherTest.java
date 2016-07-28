package net.bytebuddy.utility;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static javafx.scene.input.KeyCode.O;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PropertyDispatcherOtherTest {

    @Test
    public void testTypeRendererAdjustment() throws Exception {
        assertThat(PropertyDispatcher.TypeRenderer.FOR_LEGACY_VM.adjust('0'), is('0'));
        assertThat(PropertyDispatcher.TypeRenderer.FOR_JAVA9_CAPABLE_VM.adjust('0'), is((char) ('0' + '{' - '[')));
    }

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
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(PropertyDispatcher.class).apply();
        ObjectPropertyAssertion.of(PropertyDispatcher.TypeRenderer.class).apply();
    }
}
