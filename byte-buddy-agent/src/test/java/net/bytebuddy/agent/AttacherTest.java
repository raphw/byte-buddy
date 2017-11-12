package net.bytebuddy.agent;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.fail;

public class AttacherTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";


    @Test
    public void testPseudoAttachment() throws Exception {
        PseudoAttacher.ERROR.set(null);
        Attacher.main(new String[]{PseudoAttacher.class.getName(), FOO, BAR, "=" + QUX, BAZ});
        if (PseudoAttacher.ERROR.get() != null) {
            throw new AssertionError(PseudoAttacher.ERROR.get());
        }
    }

    @Test
    public void testPseudoAttachmentEmptyArgument() throws Exception {
        PseudoAttacherNoArgument.ERROR.set(null);
        Attacher.main(new String[]{PseudoAttacherNoArgument.class.getName(), FOO, BAR, ""});
        if (PseudoAttacherNoArgument.ERROR.get() != null) {
            throw new AssertionError(PseudoAttacherNoArgument.ERROR.get());
        }
    }

    @Test
    public void testPseudoAttachmentMissingArgument() throws Exception {
        PseudoAttacherNoArgument.ERROR.set(null);
        Attacher.main(new String[]{PseudoAttacherNoArgument.class.getName(), FOO, BAR});
        if (PseudoAttacherNoArgument.ERROR.get() != null) {
            throw new AssertionError(PseudoAttacherNoArgument.ERROR.get());
        }
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

        static final ThreadLocal<String> ERROR = new ThreadLocal<String>();

        public static PseudoAttacher attach(String processId) {
            if (!processId.equals(FOO)) {
                ERROR.set("Unexpected process id: " + processId);
            }
            return new PseudoAttacher();
        }

        public void loadAgent(String path, String argument) {
            if (!path.equals(BAR)) {
                ERROR.set("Unexpected file: " + path);
            } else if (!argument.equals(QUX + " " + BAZ)) {
                ERROR.set("Unexpected argument: " + argument);
            }
        }

        public void detach() {
        }
    }

    @SuppressWarnings("unused")
    public static class PseudoAttacherNoArgument {

        static final ThreadLocal<String> ERROR = new ThreadLocal<String>();

        public static PseudoAttacherNoArgument attach(String processId) {
            if (!processId.equals(FOO)) {
                ERROR.set("Unexpected process id: " + processId);
            }
            return new PseudoAttacherNoArgument();
        }

        public void loadAgent(String path, String argument) {
            if (!path.equals(BAR)) {
                ERROR.set("Unexpected file: " + path);
            } else if (argument != null) {
                ERROR.set("Unexpected argument: " + argument);
            }
        }

        public void detach() {
        }
    }
}