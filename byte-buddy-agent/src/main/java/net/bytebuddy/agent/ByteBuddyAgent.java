package net.bytebuddy.agent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

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
 * <b>Note</b>: The runtime installation of a Java agent is not possible on all JVMs. See the documentation for
 * {@link ByteBuddyAgent#install()} for details on JVMs that are supported out of the box.
 * </p>
 * <p>
 * <b>Important</b>: This class's name is known to the Byte Buddy main application and must not be altered.
 * </p>
 * <p>
 * <b>Note</b>: Byte Buddy does not execute code using an {@link java.security.AccessController}. If a security manager
 * is present, the user of this class is responsible for assuring any required privileges.
 * </p>
 */
public class ByteBuddyAgent {

    /**
     * The manifest property specifying the agent class.
     */
    private static final String AGENT_CLASS_PROPERTY = "Agent-Class";

    /**
     * The manifest property specifying the <i>can redefine</i> property.
     */
    private static final String CAN_REDEFINE_CLASSES_PROPERTY = "Can-Redefine-Classes";

    /**
     * The manifest property specifying the <i>can retransform</i> property.
     */
    private static final String CAN_RETRANSFORM_CLASSES_PROPERTY = "Can-Retransform-Classes";

    /**
     * The manifest property specifying the <i>can set native method prefix</i> property.
     */
    private static final String CAN_SET_NATIVE_METHOD_PREFIX = "Can-Set-Native-Method-Prefix";

    /**
     * The manifest property value for the manifest version.
     */
    private static final String MANIFEST_VERSION_VALUE = "1.0";

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
     * The class file extension.
     */
    private static final String CLASS_FILE_EXTENSION = ".class";

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
     * An indicator variable to express that no instrumentation is available.
     */
    private static final Instrumentation UNAVAILABLE = null;

    /**
     * The agent provides only {@code static} utility methods and should not be instantiated.
     */
    private ByteBuddyAgent() {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Looks up the {@link java.lang.instrument.Instrumentation} instance of an installed Byte Buddy agent. Note that
     * this method implies reflective lookup and reflective invocation such that the returned value should be cached
     * rather than calling this method several times.
     * </p>
     * <p>
     * <b>Note</b>: This method throws an {@link java.lang.IllegalStateException} If the Byte Buddy agent is not
     * properly installed.
     * </p>
     *
     * @return The {@link java.lang.instrument.Instrumentation} instance which is provided by an installed
     * Byte Buddy agent.
     */
    public static Instrumentation getInstrumentation() {
        Instrumentation instrumentation = doGetInstrumentation();
        if (instrumentation == null) {
            throw new IllegalStateException("The Byte Buddy agent is not initialized");
        }
        return instrumentation;
    }

    /**
     * Attaches the given agent Jar on the target process which must be a virtual machine process. The default attachment provider
     * is used for applying the attachment. This operation blocks until the attachment is complete. If the current VM does not supply
     * any known form of attachment to a remote VM, an {@link IllegalStateException} is thrown.
     *
     * @param agentJar  The agent jar file.
     * @param processId The target process id.
     */
    public static void attach(File agentJar, String processId) {
        attach(agentJar, processId, AttachmentProvider.DEFAULT);
    }

    /**
     * Attaches the given agent Jar on the target process which must be a virtual machine process. This operation blocks until the
     * attachment is complete.
     *
     * @param agentJar           The agent jar file.
     * @param processId          The target process id.
     * @param attachmentProvider The attachment provider to use.
     */
    public static void attach(File agentJar, String processId, AttachmentProvider attachmentProvider) {
        install(attachmentProvider, processId, new AgentProvider.ForExistingAgent(agentJar));
    }

    /**
     * <p>
     * Installs an agent on the currently running Java virtual machine. Unfortunately, this does
     * not always work. The runtime installation of a Java agent is supported for:
     * </p>
     * <ul>
     * <li><b>JVM version 9+</b>: For Java VM of at least version 9, the attachment API was merged
     * into a Jigsaw module and the runtime installation is always possible.</li>
     * <li><b>OpenJDK / Oracle JDK / IBM J9 versions 8-</b>: The installation for HotSpot is only
     * possible when bundled with a JDK up until Java version 8. It is not possible for runtime-only
     * installations of HotSpot or J9 for these versions.</li>
     * </ul>
     * <p>
     * If an agent cannot be installed, an {@link IllegalStateException} is thrown.
     * </p>
     * <p>
     * <b>Important</b>: This is a rather computation-heavy operation. Therefore, this operation is
     * not repeated after an agent was successfully installed for the first time. Instead, the previous
     * instrumentation instance is returned. However, invoking this method requires synchronization
     * such that subsequently to an installation, {@link ByteBuddyAgent#getInstrumentation()} should
     * be invoked instead.
     * </p>
     *
     * @return An instrumentation instance representing the currently running JVM.
     */
    public static Instrumentation install() {
        return install(AttachmentProvider.DEFAULT);
    }

    /**
     * Installs a Java agent using the Java attach API. This API is available under different
     * access routes for different JVMs and JVM versions or it might not be available at all.
     * If a Java agent cannot be installed by using the supplied attachment provider, an
     * {@link IllegalStateException} is thrown. The same happens if the default process provider
     * cannot resolve a process id for the current VM.
     *
     * @param attachmentProvider The attachment provider to use for the installation.
     * @return An instrumentation instance representing the currently running JVM.
     */
    public static Instrumentation install(AttachmentProvider attachmentProvider) {
        return install(attachmentProvider, ProcessProvider.ForCurrentVm.INSTANCE);
    }

    /**
     * Installs a Java agent using the Java attach API. This API is available under different
     * access routes for different JVMs and JVM versions or it might not be available at all.
     * If a Java agent cannot be installed by using the supplied process provider, an
     * {@link IllegalStateException} is thrown. The same happens if the default attachment
     * provider cannot be used.
     *
     * @param processProvider The provider for the current JVM's process id.
     * @return An instrumentation instance representing the currently running JVM.
     */
    public static Instrumentation install(ProcessProvider processProvider) {
        return install(AttachmentProvider.DEFAULT, processProvider);
    }

    /**
     * Installs a Java agent using the Java attach API. This API is available under different
     * access routes for different JVMs and JVM versions or it might not be available at all.
     * If a Java agent cannot be installed by using the supplied attachment provider and process
     * provider, an {@link IllegalStateException} is thrown.
     *
     * @param attachmentProvider The attachment provider to use for the installation.
     * @param processProvider    The provider for the current JVM's process id.
     * @return An instrumentation instance representing the currently running JVM.
     */
    public static synchronized Instrumentation install(AttachmentProvider attachmentProvider, ProcessProvider processProvider) {
        Instrumentation instrumentation = doGetInstrumentation();
        if (instrumentation != null) {
            return instrumentation;
        }
        install(attachmentProvider, processProvider.resolve(), AgentProvider.ForByteBuddyAgent.INSTANCE);
        return doGetInstrumentation();
    }

    /**
     * Installs a Java agent on a target VM.
     *
     * @param attachmentProvider The attachment provider to use.
     * @param processId          The process id of the target JVM process.
     * @param agentProvider      The agent provider for the agent jar.
     */
    private static void install(AttachmentProvider attachmentProvider, String processId, AgentProvider agentProvider) {
        AttachmentProvider.Accessor attachmentAccessor = attachmentProvider.attempt();
        if (!attachmentAccessor.isAvailable()) {
            throw new IllegalStateException();
        }
        try {
            Object virtualMachineInstance = attachmentAccessor.getVirtualMachineType()
                    .getDeclaredMethod(ATTACH_METHOD_NAME, String.class)
                    .invoke(STATIC_MEMBER, processId);
            try {
                attachmentAccessor.getVirtualMachineType()
                        .getDeclaredMethod(LOAD_AGENT_METHOD_NAME, String.class, String.class)
                        .invoke(virtualMachineInstance, agentProvider.resolve().getAbsolutePath(), WITHOUT_ARGUMENTS);
            } finally {
                attachmentAccessor.getVirtualMachineType().getDeclaredMethod(DETACH_METHOD_NAME).invoke(virtualMachineInstance);
            }
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Error during attachment using: " + attachmentProvider, exception);
        }
    }

    /**
     * Performs the actual lookup of the {@link java.lang.instrument.Instrumentation} from an installed
     * Byte Buddy agent.
     *
     * @return The Byte Buddy agent's {@link java.lang.instrument.Instrumentation} instance.
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Legal state where reflection communicates errors by exception")
    private static Instrumentation doGetInstrumentation() {
        try {
            return (Instrumentation) ClassLoader.getSystemClassLoader()
                    .loadClass(Installer.class.getName())
                    .getDeclaredField(INSTRUMENTATION_FIELD_NAME)
                    .get(STATIC_MEMBER);
        } catch (Exception ignored) {
            return UNAVAILABLE;
        }
    }

    /**
     * An attachment provider is responsible for making the Java attachment API available.
     */
    @SuppressFBWarnings(value = "IC_SUPERCLASS_USES_SUBCLASS_DURING_INITIALIZATION", justification = "No circularity, initialization is safe")
    public interface AttachmentProvider {

        /**
         * The default attachment provider to be used.
         */
        AttachmentProvider DEFAULT = new Compound(ForJigsawVm.INSTANCE,
                ForJ9Vm.INSTANCE,
                ForToolsJarVm.JVM_ROOT,
                ForToolsJarVm.JDK_ROOT,
                ForToolsJarVm.MACINTOSH);

        /**
         * Attempts the creation of an accessor for a specific JVM's attachment API.
         *
         * @return The accessor this attachment provider can supply for the currently running JVM.
         */
        Accessor attempt();

        /**
         * An accessor for a JVM's attachment API.
         */
        interface Accessor {

            /**
             * The name of the {@code VirtualMachine} class on any OpenJDK or Oracle JDK implementation.
             */
            String VIRTUAL_MACHINE_TYPE_NAME = "com.sun.tools.attach.VirtualMachine";

            /**
             * The name of the {@code VirtualMachine} class on IBM J9 VMs.
             */
            String VIRTUAL_MACHINE_TYPE_NAME_J9 = "com.ibm.tools.attach.VirtualMachine";

            /**
             * Determines if this accessor is applicable for the currently running JVM.
             *
             * @return {@code true} if this accessor is available.
             */
            boolean isAvailable();

            /**
             * Returns the {@code com.sun.tools.attach.VirtualMachine} class. This method must only be called
             * for available accessors.
             *
             * @return The virtual machine type.
             */
            Class<?> getVirtualMachineType();

            /**
             * A canonical implementation of an unavailable accessor.
             */
            enum Unavailable implements Accessor {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public boolean isAvailable() {
                    return false;
                }

                @Override
                public Class<?> getVirtualMachineType() {
                    throw new IllegalStateException("Cannot read the virtual machine type for an unavailable accessor");
                }

                @Override
                public String toString() {
                    return "ByteBuddyAgent.AttachmentProvider.Accessor.Unavailable." + name();
                }
            }

            /**
             * A simple implementation of an accessible accessor.
             */
            class Simple implements Accessor {

                /**
                 * The {@code com.sun.tools.attach.VirtualMachine} class.
                 */
                private final Class<?> virtualMachineType;

                /**
                 * Creates a new simple accessor.
                 *
                 * @param virtualMachineType The {@code com.sun.tools.attach.VirtualMachine} class.
                 */
                protected Simple(Class<?> virtualMachineType) {
                    this.virtualMachineType = virtualMachineType;
                }

                /**
                 * <p>
                 * Creates an accessor by reading the process id from the JMX runtime bean and by attempting
                 * to load the {@code com.sun.tools.attach.VirtualMachine} class from the provided class loader.
                 * </p>
                 * <p>
                 * This accessor is supposed to work on any implementation of the OpenJDK or Oracle JDK.
                 * </p>
                 *
                 * @param classLoader A class loader that is capable of loading the virtual machine type.
                 * @return An appropriate accessor.
                 */
                public static Accessor of(ClassLoader classLoader) {
                    try {
                        return new Simple(classLoader.loadClass(VIRTUAL_MACHINE_TYPE_NAME));
                    } catch (ClassNotFoundException ignored) {
                        return Unavailable.INSTANCE;
                    }
                }

                /**
                 * <p>
                 * Creates an accessor by reading the process id from the JMX runtime bean and by attempting
                 * to load the {@code com.ibm.tools.attach.VirtualMachine} class from the provided class loader.
                 * </p>
                 * <p>
                 * This accessor is supposed to work on any implementation of IBM's J9.
                 * </p>
                 *
                 * @return An appropriate accessor.
                 */
                public static Accessor ofJ9() {
                    try {
                        return new Simple(ClassLoader.getSystemClassLoader().loadClass(VIRTUAL_MACHINE_TYPE_NAME_J9));
                    } catch (ClassNotFoundException ignored) {
                        return Unavailable.INSTANCE;
                    }
                }

                @Override
                public boolean isAvailable() {
                    return true;
                }

                @Override
                public Class<?> getVirtualMachineType() {
                    return virtualMachineType;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Simple simple = (Simple) other;
                    return virtualMachineType.equals(simple.virtualMachineType);
                }

                @Override
                public int hashCode() {
                    return virtualMachineType.hashCode();
                }

                @Override
                public String toString() {
                    return "ByteBuddyAgent.AttachmentProvider.Accessor.Simple{" +
                            "virtualMachineType=" + virtualMachineType +
                            '}';
                }
            }
        }

        /**
         * An attachment provider that locates the attach API directly from the system class loader.
         */
        enum ForJigsawVm implements AttachmentProvider {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Accessor attempt() {
                return Accessor.Simple.of(ClassLoader.getSystemClassLoader());
            }

            @Override
            public String toString() {
                return "ByteBuddyAgent.AttachmentProvider.ForJigsawVm." + name();
            }
        }

        /**
         * An attachment provider that locates the attach API directly from the system class loader expecting
         * an IBM J9 VM.
         */
        enum ForJ9Vm implements AttachmentProvider {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Accessor attempt() {
                return Accessor.Simple.ofJ9();
            }

            @Override
            public String toString() {
                return "ByteBuddyAgent.AttachmentProvider.ForJ9Vm." + name();
            }
        }

        /**
         * An attachment provider that is dependant on the existence of a <i>tools.jar</i> file on the local
         * file system.
         */
        enum ForToolsJarVm implements AttachmentProvider {

            /**
             * An attachment provider that locates the <i>tools.jar</i> from a Java home directory.
             */
            JVM_ROOT("../lib/tools.jar"),

            /**
             * An attachment provider that locates the <i>tools.jar</i> from a Java installation directory.
             * In practice, several virtual machines do not return the JRE's location for the
             * <i>java.home</i> property against the property's specification.
             */
            JDK_ROOT("lib/tools.jar"),

            /**
             * An attachment provider that locates the <i>tools.jar</i> as it is set for several JVM
             * installations on Apple Macintosh computers.
             */
            MACINTOSH("../Classes/classes.jar");

            /**
             * The Java home system property.
             */
            private static final String JAVA_HOME_PROPERTY = "java.home";

            /**
             * The path to the <i>tools.jar</i> file, starting from the Java home directory.
             */
            private final String toolsJarPath;

            /**
             * Creates a new attachment provider that loads the virtual machine class from the <i>tools.jar</i>.
             *
             * @param toolsJarPath The path to the <i>tools.jar</i> file, starting from the Java home directory.
             */
            ForToolsJarVm(String toolsJarPath) {
                this.toolsJarPath = toolsJarPath;
            }

            @Override
            @SuppressFBWarnings(value = "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED", justification = "Privilege should be provided by user")
            public Accessor attempt() {
                File toolsJar = new File(System.getProperty(JAVA_HOME_PROPERTY).replace('\\', '/') + "/../" + toolsJarPath);
                try {
                    return toolsJar.isFile() && toolsJar.canRead()
                            ? Accessor.Simple.of(new URLClassLoader(new URL[]{toolsJar.toURI().toURL()}, BOOTSTRAP_CLASS_LOADER))
                            : Accessor.Unavailable.INSTANCE;
                } catch (MalformedURLException exception) {
                    throw new IllegalStateException("Could not represent " + toolsJar + " as URL");
                }
            }

            @Override
            public String toString() {
                return "ByteBuddyAgent.AttachmentProvider.ForToolsJarVm." + name();
            }
        }

        /**
         * A compound attachment provider that attempts the attachment by delegation to other providers. If
         * none of the providers of this compound provider is capable of providing a valid accessor, an
         * non-available accessor is returned.
         */
        class Compound implements AttachmentProvider {

            /**
             * A list of attachment providers in the order of their application.
             */
            private final List<? extends AttachmentProvider> attachmentProviders;

            /**
             * Creates a new compound attachment provider.
             *
             * @param attachmentProvider A list of attachment providers in the order of their application.
             */
            public Compound(AttachmentProvider... attachmentProvider) {
                this(Arrays.asList(attachmentProvider));
            }

            /**
             * Creates a new compound attachment provider.
             *
             * @param attachmentProviders A list of attachment providers in the order of their application.
             */
            public Compound(List<? extends AttachmentProvider> attachmentProviders) {
                this.attachmentProviders = attachmentProviders;
            }

            @Override
            public Accessor attempt() {
                for (AttachmentProvider attachmentProvider : attachmentProviders) {
                    Accessor accessor = attachmentProvider.attempt();
                    if (accessor.isAvailable()) {
                        return accessor;
                    }
                }
                return Accessor.Unavailable.INSTANCE;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Compound compound = (Compound) other;
                return attachmentProviders.equals(compound.attachmentProviders);
            }

            @Override
            public int hashCode() {
                return attachmentProviders.hashCode();
            }

            @Override
            public String toString() {
                return "ByteBuddyAgent.AttachmentProvider.Compound{" +
                        "attachmentProviders=" + attachmentProviders +
                        '}';
            }
        }
    }

    /**
     * A process provider is responsible for providing the process id of the current VM.
     */
    public interface ProcessProvider {

        /**
         * Resolves a process id for the current JVM.
         *
         * @return The resolved process id.
         */
        String resolve();

        /**
         * Supplies the current VM's process id.
         */
        enum ForCurrentVm implements ProcessProvider {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * The best process provider for the current VM.
             */
            private final ProcessProvider dispatcher;

            /**
             * Creates a process provider that supplies the current VM's process id.
             */
            ForCurrentVm() {
                dispatcher = ForJava9CapableVm.make();
            }

            @Override
            public String resolve() {
                return dispatcher.resolve();
            }

            @Override
            public String toString() {
                return "ByteBuddyAgent.ProcessProvider.ForCurrentVm." + name();
            }

            /**
             * A process provider for a legacy VM that reads the process id from its JMX properties.
             */
            protected enum ForLegacyVm implements ProcessProvider {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public String resolve() {
                    String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
                    int processIdIndex = runtimeName.indexOf('@');
                    if (processIdIndex == -1) {
                        throw new IllegalStateException("Cannot extract process id from runtime management bean");
                    } else {
                        return runtimeName.substring(0, processIdIndex);
                    }
                }

                @Override
                public String toString() {
                    return "ByteBuddyAgent.ProcessProvider.ForCurrentVm.ForLegacyVm." + name();
                }
            }

            /**
             * A process provider for a Java 9 capable VM with access to the introduced process API.
             */
            protected static class ForJava9CapableVm implements ProcessProvider {

                /**
                 * The {@code java.lang.ProcessHandle#current()} method.
                 */
                private final Method current;

                /**
                 * The {@code java.lang.ProcessHandle#getPid()} method.
                 */
                private final Method getPid;

                /**
                 * Creates a new Java 9 capable dispatcher for reading the current process's id.
                 *
                 * @param current The {@code java.lang.ProcessHandle#current()} method.
                 * @param getPid  The {@code java.lang.ProcessHandle#getPid()} method.
                 */
                protected ForJava9CapableVm(Method current, Method getPid) {
                    this.current = current;
                    this.getPid = getPid;
                }

                /**
                 * Attempts to create a dispatcher for a Java 9 VM and falls back to a legacy dispatcher
                 * if this is not possible.
                 *
                 * @return A dispatcher for the current VM.
                 */
                @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
                public static ProcessProvider make() {
                    try {
                        return new ForJava9CapableVm(Class.forName("java.lang.ProcessHandle").getDeclaredMethod("current"),
                                Class.forName("java.lang.ProcessHandle").getDeclaredMethod("getPid"));
                    } catch (Exception ignored) {
                        return ForLegacyVm.INSTANCE;
                    }
                }

                @Override
                public String resolve() {
                    try {
                        return getPid.invoke(current.invoke(STATIC_MEMBER)).toString();
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access Java 9 process API", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error when accessing Java 9 process API", exception.getCause());
                    }
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    ForJava9CapableVm that = (ForJava9CapableVm) other;
                    return current.equals(that.current) && getPid.equals(that.getPid);
                }

                @Override
                public int hashCode() {
                    int result = current.hashCode();
                    result = 31 * result + getPid.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "ByteBuddyAgent.ProcessProvider.ForCurrentVm.ForJava9CapableVm{" +
                            "current=" + current +
                            ", getPid=" + getPid +
                            '}';
                }
            }
        }
    }

    /**
     * An agent provider is responsible for handling and providing the jar file of an agent that is being attached.
     */
    protected interface AgentProvider {

        /**
         * Provides an agent jar file for attachment.
         *
         * @return The provided agent.
         * @throws IOException If the agent cannot be written to disk.
         */
        File resolve() throws IOException;

        /**
         * An agent provider for a temporary Byte Buddy agent.
         */
        enum ForByteBuddyAgent implements AgentProvider {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * The default prefix of the Byte Buddy agent jar file.
             */
            private static final String AGENT_FILE_NAME = "byteBuddyAgent";

            /**
             * The jar file extension.
             */
            private static final String JAR_FILE_EXTENSION = ".jar";

            @Override
            public File resolve() throws IOException {
                File agentJar;
                InputStream inputStream = Installer.class.getResourceAsStream('/' + Installer.class.getName().replace('.', '/') + CLASS_FILE_EXTENSION);
                if (inputStream == null) {
                    throw new IllegalStateException("Cannot locate class file for Byte Buddy installer");
                }
                try {
                    agentJar = File.createTempFile(AGENT_FILE_NAME, JAR_FILE_EXTENSION);
                    agentJar.deleteOnExit(); // Agent jar is required until VM shutdown due to lazy class loading.
                    Manifest manifest = new Manifest();
                    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, MANIFEST_VERSION_VALUE);
                    manifest.getMainAttributes().put(new Attributes.Name(AGENT_CLASS_PROPERTY), Installer.class.getName());
                    manifest.getMainAttributes().put(new Attributes.Name(CAN_REDEFINE_CLASSES_PROPERTY), Boolean.TRUE.toString());
                    manifest.getMainAttributes().put(new Attributes.Name(CAN_RETRANSFORM_CLASSES_PROPERTY), Boolean.TRUE.toString());
                    manifest.getMainAttributes().put(new Attributes.Name(CAN_SET_NATIVE_METHOD_PREFIX), Boolean.TRUE.toString());
                    JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(agentJar), manifest);
                    try {
                        jarOutputStream.putNextEntry(new JarEntry(Installer.class.getName().replace('.', '/') + CLASS_FILE_EXTENSION));
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
                return agentJar;
            }

            @Override
            public String toString() {
                return "ByteBuddyAgent.AgentProvider.ForByteBuddyAgent." + name();
            }
        }

        /**
         * An agent provider that supplies an existing agent that is not deleted after attachment.
         */
        class ForExistingAgent implements AgentProvider {

            /**
             * The supplied agent.
             */
            private File agent;

            /**
             * Creates an agent provider for an existing agent.
             *
             * @param agent The supplied agent.
             */
            protected ForExistingAgent(File agent) {
                this.agent = agent;
            }

            @Override
            public File resolve() {
                return agent;
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                ForExistingAgent that = (ForExistingAgent) object;
                return agent.equals(that.agent);
            }

            @Override
            public int hashCode() {
                return agent.hashCode();
            }

            @Override
            public String toString() {
                return "ByteBuddyAgent.AgentProvider.ForExistingAgent{" +
                        "agent='" + agent + '\'' +
                        '}';
            }
        }
    }
}
