package net.bytebuddy.agent;

import net.bytebuddy.test.utility.AgentAttachmentRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.net.URL;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteBuddyAgentInstallationTest {

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    @Before
    public void setUp() throws Exception {
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
    public void testAgentInstallationOtherClassLoader() throws Exception {
        assertThat(new ClassLoader(null) {
            @Override
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

            @Override
            public InputStream getResourceAsStream(String name) {
                return ByteBuddyAgentInstallationTest.class.getClassLoader().getResourceAsStream(name);
            }
        }.loadClass(ByteBuddyAgent.class.getName()).getDeclaredMethod("install").invoke(null), instanceOf(Instrumentation.class));
    }
}
