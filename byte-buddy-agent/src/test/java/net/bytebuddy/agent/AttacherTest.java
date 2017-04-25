package net.bytebuddy.agent;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.fail;

public class AttacherTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Test
    public void testPseudoAttachment() throws Exception {
        Attacher.main(new String[]{PseudoAttacher.class.getName(), FOO, "/" + BAR, "=" + QUX, BAZ});
    }

    @Test
    public void testPseudoAttachmentNoArgument() throws Exception {
        Attacher.main(new String[]{PseudoAttacherNoArgument.class.getName(), FOO, "/" + BAR, ""});
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConstructorThrowsException() throws Exception {
        Constructor<?> constructor = Attacher.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            throw (Exception) exception.getCause();
        }
    }

    @SuppressWarnings("unused")
    public static class PseudoAttacher {

        public static PseudoAttacher attach(String processId) {
            if (!processId.equals(FOO)) {
                throw new AssertionError();
            }
            return new PseudoAttacher();
        }

        public void loadAgent(String path, String argument) {
            if (!path.equals("/" + BAR) || !argument.equals(QUX + " " + BAZ)) {
                throw new AssertionError();
            }
        }

        public void detach() {
        }
    }

    @SuppressWarnings("unused")
    public static class PseudoAttacherNoArgument {

        public static PseudoAttacherNoArgument attach(String processId) {
            if (!processId.equals(FOO)) {
                throw new AssertionError();
            }
            return new PseudoAttacherNoArgument();
        }

        public void loadAgent(String path, String argument) {
            if (!path.equals("/" + BAR) || argument != null) {
                throw new AssertionError();
            }
        }

        public void detach() {
        }
    }
}