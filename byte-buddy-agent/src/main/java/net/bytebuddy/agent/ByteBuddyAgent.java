package net.bytebuddy.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Logger;

/**
 * <p>
 * The Byte Buddy agent provides a JVM {@link java.lang.instrument.Instrumentation} in order to allow Byte Buddy the
 * redefinition of already loaded classes. An agent must normally be specified via the command line via the
 * {@code javaagent} parameter. As an argument to this parameter, one must specify the location of this agent's jar
 * file such as for example in
 * </p>
 * <p>
 * <code>
 * java -javaagent:byte-buddy-agent.jar -jar app.jar
 * </code>
 * </p>
 * <p>
 * For JDK installations of the Java virtual machine, an agent can however also be installed after startup using the
 * <i>Attach API</i> which is contained in the JDK's <i>tools.jar</i>. As instrumentation is commonly used in unit
 * tests which are normally run on a JDK, the Byte Buddy agent provides a convenience installation method
 * {@link ByteBuddyAgent#installOnOpenJDK()} which is only guaranteed to work on the OpenJDK and compatible JDKs.
 * </p>
 * <p>
 * <b>Note</b>: This class's name is known to the Byte Buddy main application and must not be altered.
 * </p>
 */
public class ByteBuddyAgent {

    /**
     * The manifest property specifying the agent class.
     */
    private static final String AGENT_CLASS_PROPERTY = "Agent-Class";

    /**
     * The manifest property specifying the <i>can redefine</i> class property.
     */
    private static final String CAN_REDEFINE_CLASSES_PROPERTY = "Can-Redefine-Classes";

    /**
     * The manifest property specifying the <i>can retransform</i> class property.
     */
    private static final String CAN_RETRANSFORM_CLASSES_PROPERTY = "Can-Retransform-Classes";

    /**
     * The manifest property value for the manifest version.
     */
    private static final String MANIFEST_VERSION_VALUE = "1.0";

    /**
     * The JVM property for this JVM instance's home property.
     */
    private static final String JAVA_HOME_PROPERTY = "java.home";

    /**
     * The relative location of the tools.jar on the OpenJDK relatively to the
     * {@link net.bytebuddy.agent.ByteBuddyAgent#JAVA_HOME_PROPERTY}.
     */
    private static final String TOOLS_JAR_LOCATION = "/../lib/tools.jar";

    /**
     * The size of the buffer for copying the agent installer file into another jar.
     */
    private static final int BUFFER_SIZE = 1024;

    /**
     * Convenience indices for reading and writing to the buffer to make the code more readable.
     */
    private static final int START_INDEX = 0, END_OF_FILE = -1;

    /**
     * Base for access to a reflective member to make the code more readable.
     */
    private static final Object STATIC_MEMBER = null;

    /**
     * Representation of the bootstrap {@link java.lang.ClassLoader}.
     */
    private static final ClassLoader BOOTSTRAP_CLASS_LOADER = null;

    /**
     * Empty command line arguments.
     */
    private static final String WITHOUT_ARGUMENTS = "";

    /**
     * The default prefix of the Byte Buddy agent jar file.
     */
    private static final String AGENT_FILE_NAME = "byteBuddyAgent";

    /**
     * The jar file extension.
     */
    private static final String JAR_FILE_EXTENSION = ".jar";

    /**
     * The class file extension.
     */
    private static final String CLASS_FILE_EXTENSION = ".class";

    /**
     * The name of the <i>tools.jar</i>'s {@code VirtualMachine} class.
     */
    private static final String VIRTUAL_MACHINE_TYPE_NAME = "com.sun.tools.attach.VirtualMachine";

    /**
     * The name of the {@code attach} method of the  {@code VirtualMachine} class.
     */
    private static final String ATTACH_METHOD_NAME = "attach";

    /**
     * The name of the {@code loadAgent} method of the  {@code VirtualMachine} class.
     */
    private static final String LOAD_AGENT_METHOD_NAME = "loadAgent";

    /**
     * The name of the {@code detach} method of the  {@code VirtualMachine} class.
     */
    private static final String DETACH_METHOD_NAME = "detach";

    /**
     * The name of this class'S {@code instrumentation} field.
     */
    private static final String INSTRUMENTATION_FIELD_NAME = "instrumentation";

    /**
     * The agent provides only {@code static} utility methods and should not be instantiated.
     */
    private ByteBuddyAgent() {
        throw new UnsupportedOperationException();
    }

    /**
     * Installs the Byte Buddy agent using the <i>tools.jar</i>'s Attach API. This installation is only possible
     * on the OpenJDK and compatible JDKs as the <i>tools.jar</i> is not available on non-JDK JVMs or other JDKs.
     * Note that the installation is only performed if the Byte Buddy agent is not yet installed. However, this method
     * implies reflective lookup and reflective invocation such that the returned value should be cached rather than
     * calling this method several times.
     *
     * @return The {@link java.lang.instrument.Instrumentation} instance that is provided by the Byte Buddy agent.
     * @throws IllegalStateException If the currently running JVM does not support the runtime installation of
     *                               an agent.
     */
    public static Instrumentation installOnOpenJDK() {
        try {
            Instrumentation instrumentation = doGetInstrumentation();
            if (instrumentation != null) {
                return instrumentation;
            }
        } catch (Exception ignored) {
            // Ignore this exception as it only means that the agent type is not yet available on the class path.
        }
        try {
            doInstall();
        } catch (Exception e) {
            throw new IllegalStateException("The programmatic installation of the Byte Buddy agent is only " +
                    "possible on the OpenJDK and JDKs with a compatible 'tools.jar'", e);
        }
        return getInstrumentation();
    }

    /**
     * Performs the actual installation of the Byte Buddy agent.
     *
     * @throws Exception If the installation is not possible.
     */
    private static synchronized void doInstall() throws Exception {
        ClassLoader classLoader = new URLClassLoader(new URL[]{new File(System.getProperty(JAVA_HOME_PROPERTY)
                .replace('\\', '/') + TOOLS_JAR_LOCATION).toURI().toURL()}, BOOTSTRAP_CLASS_LOADER);
        Class<?> virtualMachine = classLoader.loadClass(VIRTUAL_MACHINE_TYPE_NAME);
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        Object virtualMachineInstance = virtualMachine.getDeclaredMethod(ATTACH_METHOD_NAME, String.class)
                .invoke(STATIC_MEMBER, runtimeName.substring(0, runtimeName.indexOf('@')));
        try {
            File agentFile = File.createTempFile(AGENT_FILE_NAME, JAR_FILE_EXTENSION);
            saveAgentJar(agentFile);
            try {
                virtualMachine.getDeclaredMethod(LOAD_AGENT_METHOD_NAME, String.class, String.class)
                        .invoke(virtualMachineInstance, agentFile.getAbsolutePath(), WITHOUT_ARGUMENTS);
            } finally {
                if (!agentFile.delete()) {
                    Logger.getAnonymousLogger().info("Cannot delete temporary file: " + agentFile);
                }
            }
        } finally {
            virtualMachine.getDeclaredMethod(DETACH_METHOD_NAME).invoke(virtualMachineInstance);
        }
    }

    /**
     * Saves all necessary classes for the Byte Buddy agent jar to the given file.
     *
     * @param agentFile The target file to which all required classes are to be written.
     * @throws Exception If the writing to the file is not possible.
     */
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
            manifest.getMainAttributes().put(new Attributes.Name(CAN_RETRANSFORM_CLASSES_PROPERTY), Boolean.TRUE.toString());
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

    /**
     * Looks up the {@link java.lang.instrument.Instrumentation} instance of an installed Byte Buddy agent. Note that
     * this method implies reflective lookup and reflective invocation such that the returned value should be cached
     * rather than calling this method several times.
     *
     * @return The {@link java.lang.instrument.Instrumentation} instance which is provided by an installed
     * Byte Buddy agent.
     * @throws java.lang.IllegalStateException If the Byte Buddy agent is not properly installed.
     */
    public static Instrumentation getInstrumentation() {
        Instrumentation instrumentation = doGetInstrumentation();
        if (instrumentation == null) {
            throw new IllegalStateException("The Byte Buddy agent is not initialized");
        }
        return instrumentation;
    }

    /**
     * Performs the actual lookup of the {@link java.lang.instrument.Instrumentation} from an installed
     * Byte Buddy agent.
     *
     * @return The Byte Buddy agent's {@link java.lang.instrument.Instrumentation} instance.
     */
    private static Instrumentation doGetInstrumentation() {
        try {
            // The lookup classes must not be cached as the agent might be loaded at a later point.
            Field field = ClassLoader.getSystemClassLoader()
                    .loadClass(ByteBuddyAgent.Installer.class.getName())
                    .getDeclaredField(INSTRUMENTATION_FIELD_NAME);
            field.setAccessible(true);
            return (Instrumentation) field.get(STATIC_MEMBER);
        } catch (Exception e) {
            throw new IllegalStateException("The Byte Buddy agent is not properly initialized", e);
        }
    }

    /**
     * An installer class which defined the hook-in methods that are required by the Java agent specification.
     */
    public static class Installer {

        /**
         * A field for carrying the {@link java.lang.instrument.Instrumentation} that was loaded by the Byte Buddy
         * agent. Note that this field must never be accessed directly as the agent is injected into the VM's
         * system class loader. This way, the field of this class might be {@code null} even after the installation
         * of the Byte Buddy agent as this class might be loaded by a different class loader than the system class
         * loader.
         */
        @SuppressWarnings("unused")
        private static volatile Instrumentation instrumentation;

        /**
         * The installer provides only {@code static} hook-in methods and should not be instantiated.
         */
        private Installer() {
            throw new UnsupportedOperationException();
        }

        /**
         * Allows the installation of this agent via a command line argument.
         *
         * @param agentArguments  The unused agent arguments.
         * @param instrumentation The instrumentation instance.
         */
        public static void premain(String agentArguments, Instrumentation instrumentation) {
            Installer.instrumentation = instrumentation;
        }

        /**
         * Allows the installation of this agent via the Attach API.
         *
         * @param agentArguments  The unused agent arguments.
         * @param instrumentation The instrumentation instance.
         */
        public static void agentmain(String agentArguments, Instrumentation instrumentation) {
            Installer.instrumentation = instrumentation;
        }
    }
}
