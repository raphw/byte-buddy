package net.bytebuddy.agent;

import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteBuddyAgentInstallationTest {

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private static void resetField() throws Exception {
        Field instrumentation = Installer.class.getDeclaredField("instrumentation");
        instrumentation.setAccessible(true);
        instrumentation.set(null, null);
    }

    @Test
    @AgentAttachmentRule.Enforce
    public void testAgentInstallation() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
    }

    @Test
    @AgentAttachmentRule.Enforce
    @JavaVersionRule.Enforce(9) // To avoid unsupported duplicate binding of native library to two class loaders.
    public void testAgentInstallationOtherClassLoader() throws Exception {
        resetField();
        assertThat(new ClassLoader(null) {
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                InputStream in = getResourceAsStream(name.replace('.', '/') + ".class");
                if (in == null) {
                    throw new ClassNotFoundException(name);
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                try {
                    while ((length = in.read(buffer)) != -1) {
                        out.write(buffer, 0, length);
                    }
                } catch (IOException exception) {
                    throw new AssertionError(exception);
                }
                byte[] binaryRepresentation = out.toByteArray();
                return defineClass(name, binaryRepresentation, 0, binaryRepresentation.length);
            }

            public InputStream getResourceAsStream(String name) {
                return ByteBuddyAgentInstallationTest.class.getClassLoader().getResourceAsStream(name);
            }
        }.loadClass(ByteBuddyAgent.class.getName()).getDeclaredMethod("install").invoke(null), instanceOf(Instrumentation.class));
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(9) // To avoid unsupported duplicate binding of native library to two class loaders.
    public void testNoInstrumentation() throws Exception {
        resetField();
        ByteBuddyAgent.getInstrumentation();
    }
}
