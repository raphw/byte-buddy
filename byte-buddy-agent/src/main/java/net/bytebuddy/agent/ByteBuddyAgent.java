package net.bytebuddy.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Logger;

public class ByteBuddyAgent {

    public static final String AGENT_CLASS_PROPERTY = "Agent-Class";
    public static final String CAN_REDEFINE_CLASSES_PROPERTY = "Can-Redefine-Classes";
    public static final String CAN_SET_NATIVE_METHOD_PREFIX_PROPERTY = "Can-Set-Native-Method-Prefix";
    public static final String JAVA_HOME_PROPERTY = "java.home";
    public static final String TOOLS_JAR_LOCATION = "/../lib/tools.jar";
    private static final int BUFFER_SIZE = 1024;
    private static final int START_INDEX = 0, END_OF_FILE = -1;
    private static final Object STATIC_MEMBER = null;
    private static final ClassLoader BOOTSTRAP_CLASS_LOADER = null;
    private static final String WITHOUT_ARGUMENTS = "";
    private static final String AGENT_FILE_NAME = "byteBuddyAgent";
    private static final String JAR_FILE_EXTENSION = ".jar";
    private static final String CLASS_FILE_EXTENSION = ".class";
    private static final String VIRTUAL_MACHINE_TYPE_NAME = "com.sun.tools.attach.VirtualMachine";
    private static final String ATTACH_METHOD_NAME = "attach";
    private static final String INSTRUMENTATION_FIELD_NAME = "instrumentation";
    private static final String LOAD_AGENT_METHOD_NAME = "loadAgent";
    private static final String DETACH_METHOD_NAME = "detach";
    private static final String MANIFEST_VERSION_VALUE = "1.0";
    @SuppressWarnings("unused")
    private static volatile Instrumentation instrumentation;

    private ByteBuddyAgent() {
        throw new UnsupportedOperationException();
    }

    public static Instrumentation installOnOpenJDK() throws Exception {
        Instrumentation instrumentation = doGetInstrumentation();
        if (instrumentation != null) {
            return instrumentation;
        }
        try {
            doInstall();
        } catch (Exception e) {
            throw new IllegalStateException("The programmatic installation of the Byte Buddy agent is only possible on the OpenJDK", e);
        }
        return getInstrumentation();
    }

    private static synchronized void doInstall() throws Exception {
        ClassLoader classLoader = new URLClassLoader(new URL[]{new File(System.getProperty(JAVA_HOME_PROPERTY)
                .replace('\\', '/') + TOOLS_JAR_LOCATION).toURI().toURL()}, BOOTSTRAP_CLASS_LOADER);
        Class<?> virtualMachine = classLoader.loadClass(VIRTUAL_MACHINE_TYPE_NAME);
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        Object virtualMachineInstance = virtualMachine.getDeclaredMethod(ATTACH_METHOD_NAME, String.class)
                .invoke(STATIC_MEMBER, runtimeName.substring(0, runtimeName.indexOf('@')));
        File agentFile = File.createTempFile(AGENT_FILE_NAME, JAR_FILE_EXTENSION);
        saveAgentJar(agentFile);
        try {
            virtualMachine.getDeclaredMethod(LOAD_AGENT_METHOD_NAME, String.class, String.class)
                    .invoke(virtualMachineInstance, agentFile.getAbsolutePath(), WITHOUT_ARGUMENTS);
            virtualMachine.getDeclaredMethod(DETACH_METHOD_NAME).invoke(virtualMachineInstance);
        } finally {
            if (!agentFile.delete()) {
                Logger.getAnonymousLogger().info("Cannot delete temporary file: " + agentFile);
            }
        }
    }

    private static void saveAgentJar(File agentFile) throws Exception {
        InputStream inputStream = ByteBuddyAgent.Installer.class.getResourceAsStream('/'
                + ByteBuddyAgent.Installer.class.getName().replace('.', '/') + CLASS_FILE_EXTENSION);
        if (inputStream == null) {
            throw new IllegalStateException("Cannot locate class file for Byte Buddy agent");
        }
        try {
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, MANIFEST_VERSION_VALUE);
            manifest.getMainAttributes().put(new Attributes.Name(AGENT_CLASS_PROPERTY), ByteBuddyAgent.Installer.class.getName());
            manifest.getMainAttributes().put(new Attributes.Name(CAN_REDEFINE_CLASSES_PROPERTY), Boolean.TRUE.toString());
            manifest.getMainAttributes().put(new Attributes.Name(CAN_SET_NATIVE_METHOD_PREFIX_PROPERTY), Boolean.TRUE.toString());
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(agentFile), manifest);
            try {
                jarOutputStream.putNextEntry(new JarEntry('/' + ByteBuddyAgent.Installer.class.getName().replace('.', '/') + CLASS_FILE_EXTENSION));
                byte[] buffer = new byte[BUFFER_SIZE];
                int index;
                while ((index = inputStream.read(buffer)) != END_OF_FILE) {
                    jarOutputStream.write(buffer, START_INDEX, index);
                }
                jarOutputStream.closeEntry();
            } finally {
                jarOutputStream.close();
            }
        } finally {
            inputStream.close();
        }
    }

    public static Instrumentation getInstrumentation() {
        Instrumentation instrumentation = doGetInstrumentation();
        if (instrumentation == null) {
            throw new IllegalStateException("The Byte Buddy agent is not initialized");
        }
        return instrumentation;
    }

    private static Instrumentation doGetInstrumentation() {
        try {
            // The lookup classes must not be cached as the agent might be loaded at a later point.
            return (Instrumentation) ClassLoader.getSystemClassLoader()
                    .loadClass(ByteBuddyAgent.class.getName())
                    .getDeclaredField(INSTRUMENTATION_FIELD_NAME)
                    .get(STATIC_MEMBER);
        } catch (Exception e) {
            throw new IllegalStateException("The Byte Buddy agent is not initialized", e);
        }
    }

    public static class Installer {

        private Installer() {
            throw new UnsupportedOperationException();
        }

        public static void premain(String agentArguments, Instrumentation instrumentation) {
            ByteBuddyAgent.instrumentation = instrumentation;
        }

        public static void agentmain(String agentArguments, Instrumentation instrumentation) {
            ByteBuddyAgent.instrumentation = instrumentation;
        }
    }
}
