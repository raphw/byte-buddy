/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.agent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.*;

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
     * Indicates that the agent should not resolve it's own code location for a self-attachment.
     */
    public static final String LATENT_RESOLVE = "net.bytebuddy.agent.latent";

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
     * The status code expected as a result of a successful attachment.
     */
    private static final int SUCCESSFUL_ATTACH = 0;

    /**
     * Base for access to a reflective member to make the code more readable.
     */
    private static final Object STATIC_MEMBER = null;

    /**
     * Representation of the bootstrap {@link java.lang.ClassLoader}.
     */
    private static final ClassLoader BOOTSTRAP_CLASS_LOADER = null;

    /**
     * Represents a no-op argument for a dynamic agent attachment.
     */
    private static final String WITHOUT_ARGUMENT = null;

    /**
     * The naming prefix of all artifacts for an attacher jar.
     */
    private static final String ATTACHER_FILE_NAME = "byteBuddyAttacher";

    /**
     * The file extension for a class file.
     */
    private static final String CLASS_FILE_EXTENSION = ".class";

    /**
     * The file extension for a jar file.
     */
    private static final String JAR_FILE_EXTENSION = ".jar";

    /**
     * The class path argument to specify the class path elements.
     */
    private static final String CLASS_PATH_ARGUMENT = "-cp";

    /**
     * The Java property denoting the Java home directory.
     */
    private static final String JAVA_HOME = "java.home";

    /**
     * The Java property denoting the operating system name.
     */
    private static final String OS_NAME = "os.name";

    /**
     * The name of the method for reading the installer's instrumentation.
     */
    private static final String INSTRUMENTATION_METHOD = "getInstrumentation";

    /**
     * Represents the {@code file} URL protocol.
     */
    private static final String FILE_PROTOCOL = "file";

    /**
     * An indicator variable to express that no instrumentation is available.
     */
    private static final Instrumentation UNAVAILABLE = null;

    /**
     * Represents a failed attempt to self-resolve a jar file location.
     */
    private static final File CANNOT_SELF_RESOLVE = null;

    /**
     * The attachment type evaluator to be used for determining if an attachment requires an external process.
     */
    private static final AttachmentTypeEvaluator ATTACHMENT_TYPE_EVALUATOR = AccessController.doPrivileged(AttachmentTypeEvaluator.InstallationAction.INSTANCE);

    /**
     * The agent provides only {@code static} utility methods and should not be instantiated.
     */
    private ByteBuddyAgent() {
        throw new UnsupportedOperationException("This class is a utility class and not supposed to be instantiated");
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
     * <p>
     * Attaches the given agent Jar on the target process which must be a virtual machine process. The default attachment provider
     * is used for applying the attachment. This operation blocks until the attachment is complete. If the current VM does not supply
     * any known form of attachment to a remote VM, an {@link IllegalStateException} is thrown. The agent is not provided an argument.
     * </p>
     * <p>
     * <b>Important</b>: It is only possible to attach to processes that are executed by the same operating system user.
     * </p>
     *
     * @param agentJar  The agent jar file.
     * @param processId The target process id.
     */
    public static void attach(File agentJar, String processId) {
        attach(agentJar, processId, WITHOUT_ARGUMENT);
    }

    /**
     * <p>
     * Attaches the given agent Jar on the target process which must be a virtual machine process. The default attachment provider
     * is used for applying the attachment. This operation blocks until the attachment is complete. If the current VM does not supply
     * any known form of attachment to a remote VM, an {@link IllegalStateException} is thrown.
     * </p>
     * <p>
     * <b>Important</b>: It is only possible to attach to processes that are executed by the same operating system user.
     * </p>
     *
     * @param agentJar  The agent jar file.
     * @param processId The target process id.
     * @param argument  The argument to provide to the agent.
     */
    public static void attach(File agentJar, String processId, String argument) {
        attach(agentJar, processId, argument, AttachmentProvider.DEFAULT);
    }

    /**
     * <p>
     * Attaches the given agent Jar on the target process which must be a virtual machine process. This operation blocks until the
     * attachment is complete. The agent is not provided an argument.
     * </p>
     * <p>
     * <b>Important</b>: It is only possible to attach to processes that are executed by the same operating system user.
     * </p>
     *
     * @param agentJar           The agent jar file.
     * @param processId          The target process id.
     * @param attachmentProvider The attachment provider to use.
     */
    public static void attach(File agentJar, String processId, AttachmentProvider attachmentProvider) {
        attach(agentJar, processId, WITHOUT_ARGUMENT, attachmentProvider);
    }

    /**
     * <p>
     * Attaches the given agent Jar on the target process which must be a virtual machine process. This operation blocks until the
     * attachment is complete.
     * </p>
     * <p>
     * <b>Important</b>: It is only possible to attach to processes that are executed by the same operating system user.
     * </p>
     *
     * @param agentJar           The agent jar file.
     * @param processId          The target process id.
     * @param argument           The argument to provide to the agent.
     * @param attachmentProvider The attachment provider to use.
     */
    public static void attach(File agentJar, String processId, String argument, AttachmentProvider attachmentProvider) {
        install(attachmentProvider, processId, argument, new AgentProvider.ForExistingAgent(agentJar), false);
    }

    /**
     * <p>
     * Attaches the given agent Jar on the target process which must be a virtual machine process. The default attachment provider
     * is used for applying the attachment. This operation blocks until the attachment is complete. If the current VM does not supply
     * any known form of attachment to a remote VM, an {@link IllegalStateException} is thrown. The agent is not provided an argument.
     * </p>
     * <p>
     * <b>Important</b>: It is only possible to attach to processes that are executed by the same operating system user.
     * </p>
     *
     * @param agentJar        The agent jar file.
     * @param processProvider A provider of the target process id.
     */
    public static void attach(File agentJar, ProcessProvider processProvider) {
        attach(agentJar, processProvider, WITHOUT_ARGUMENT);
    }

    /**
     * <p>
     * Attaches the given agent Jar on the target process which must be a virtual machine process. The default attachment provider
     * is used for applying the attachment. This operation blocks until the attachment is complete. If the current VM does not supply
     * any known form of attachment to a remote VM, an {@link IllegalStateException} is thrown.
     * </p>
     * <p>
     * <b>Important</b>: It is only possible to attach to processes that are executed by the same operating system user.
     * </p>
     *
     * @param agentJar        The agent jar file.
     * @param processProvider A provider of the target process id.
     * @param argument        The argument to provide to the agent.
     */
    public static void attach(File agentJar, ProcessProvider processProvider, String argument) {
        attach(agentJar, processProvider, argument, AttachmentProvider.DEFAULT);
    }

    /**
     * <p>
     * Attaches the given agent Jar on the target process which must be a virtual machine process. This operation blocks until the
     * attachment is complete. The agent is not provided an argument.
     * </p>
     * <p>
     * <b>Important</b>: It is only possible to attach to processes that are executed by the same operating system user.
     * </p>
     *
     * @param agentJar           The agent jar file.
     * @param processProvider    A provider of the target process id.
     * @param attachmentProvider The attachment provider to use.
     */
    public static void attach(File agentJar, ProcessProvider processProvider, AttachmentProvider attachmentProvider) {
        attach(agentJar, processProvider, WITHOUT_ARGUMENT, attachmentProvider);
    }

    /**
     * <p>
     * Attaches the given agent Jar on the target process which must be a virtual machine process. This operation blocks until the
     * attachment is complete.
     * </p>
     * <p>
     * <b>Important</b>: It is only possible to attach to processes that are executed by the same operating system user.
     * </p>
     *
     * @param agentJar           The agent jar file.
     * @param processProvider    A provider of the target process id.
     * @param argument           The argument to provide to the agent.
     * @param attachmentProvider The attachment provider to use.
     */
    public static void attach(File agentJar, ProcessProvider processProvider, String argument, AttachmentProvider attachmentProvider) {
        install(attachmentProvider, processProvider.resolve(), argument, new AgentProvider.ForExistingAgent(agentJar), false);
    }

    /**
     * <p>
     * Attaches the given agent library on the target process which must be a virtual machine process. The default attachment provider
     * is used for applying the attachment. This operation blocks until the attachment is complete. If the current VM does not supply
     * any known form of attachment to a remote VM, an {@link IllegalStateException} is thrown. The agent is not provided an argument.
     * </p>
     * <p>
     * <b>Important</b>: It is only possible to attach to processes that are executed by the same operating system user.
     * </p>
     *
     * @param agentLibrary The agent jar file.
     * @param processId    The target process id.
     */
    public static void attachNative(File agentLibrary, String processId) {
        attachNative(agentLibrary, processId, WITHOUT_ARGUMENT);
    }

    /**
     * <p>
     * Attaches the given agent library on the target process which must be a virtual machine process. The default attachment provider
     * is used for applying the attachment. This operation blocks until the attachment is complete. If the current VM does not supply
     * any known form of attachment to a remote VM, an {@link IllegalStateException} is thrown.
     * </p>
     * <p>
     * <b>Important</b>: It is only possible to attach to processes that are executed by the same operating system user.
     * </p>
     *
     * @param agentLibrary The agent library.
     * @param processId    The target process id.
     * @param argument     The argument to provide to the agent.
     */
    public static void attachNative(File agentLibrary, String processId, String argument) {
        attachNative(agentLibrary, processId, argument, AttachmentProvider.DEFAULT);
    }

    /**
     * <p>
     * Attaches the given agent library on the target process which must be a virtual machine process. This operation blocks until the
     * attachment is complete. The agent is not provided an argument.
     * </p>
     * <p>
     * <b>Important</b>: It is only possible to attach to processes that are executed by the same operating system user.
     * </p>
     *
     * @param agentLibrary       The agent library.
     * @param processId          The target process id.
     * @param attachmentProvider The attachment provider to use.
     */
    public static void attachNative(File agentLibrary, String processId, AttachmentProvider attachmentProvider) {
        attachNative(agentLibrary, processId, WITHOUT_ARGUMENT, attachmentProvider);
    }

    /**
     * <p>
     * Attaches the given agent library on the target process which must be a virtual machine process. This operation blocks until the
     * attachment is complete.
     * </p>
     * <p>
     * <b>Important</b>: It is only possible to attach to processes that are executed by the same operating system user.
     * </p>
     *
     * @param agentLibrary       The agent library.
     * @param processId          The target process id.
     * @param argument           The argument to provide to the agent.
     * @param attachmentProvider The attachment provider to use.
     */
    public static void attachNative(File agentLibrary, String processId, String argument, AttachmentProvider attachmentProvider) {
        install(attachmentProvider, processId, argument, new AgentProvider.ForExistingAgent(agentLibrary), true);
    }

    /**
     * <p>
     * Attaches the given agent library on the target process which must be a virtual machine process. The default attachment provider
     * is used for applying the attachment. This operation blocks until the attachment is complete. If the current VM does not supply
     * any known form of attachment to a remote VM, an {@link IllegalStateException} is thrown. The agent is not provided an argument.
     * </p>
     * <p>
     * <b>Important</b>: It is only possible to attach to processes that are executed by the same operating system user.
     * </p>
     *
     * @param agentLibrary    The agent library.
     * @param processProvider A provider of the target process id.
     */
    public static void attachNative(File agentLibrary, ProcessProvider processProvider) {
        attachNative(agentLibrary, processProvider, WITHOUT_ARGUMENT);
    }

    /**
     * <p>
     * Attaches the given agent library on the target process which must be a virtual machine process. The default attachment provider
     * is used for applying the attachment. This operation blocks until the attachment is complete. If the current VM does not supply
     * any known form of attachment to a remote VM, an {@link IllegalStateException} is thrown.
     * </p>
     * <p>
     * <b>Important</b>: It is only possible to attach to processes that are executed by the same operating system user.
     * </p>
     *
     * @param agentLibrary    The agent library.
     * @param processProvider A provider of the target process id.
     * @param argument        The argument to provide to the agent.
     */
    public static void attachNative(File agentLibrary, ProcessProvider processProvider, String argument) {
        attachNative(agentLibrary, processProvider, argument, AttachmentProvider.DEFAULT);
    }

    /**
     * <p>
     * Attaches the given agent library on the target process which must be a virtual machine process. This operation blocks until the
     * attachment is complete. The agent is not provided an argument.
     * </p>
     * <p>
     * <b>Important</b>: It is only possible to attach to processes that are executed by the same operating system user.
     * </p>
     *
     * @param agentLibrary       The agent library.
     * @param processProvider    A provider of the target process id.
     * @param attachmentProvider The attachment provider to use.
     */
    public static void attachNative(File agentLibrary, ProcessProvider processProvider, AttachmentProvider attachmentProvider) {
        attachNative(agentLibrary, processProvider, WITHOUT_ARGUMENT, attachmentProvider);
    }

    /**
     * <p>
     * Attaches the given agent library on the target process which must be a virtual machine process. This operation blocks until the
     * attachment is complete.
     * </p>
     * <p>
     * <b>Important</b>: It is only possible to attach to processes that are executed by the same operating system user.
     * </p>
     *
     * @param agentLibrary       The agent library.
     * @param processProvider    A provider of the target process id.
     * @param argument           The argument to provide to the agent.
     * @param attachmentProvider The attachment provider to use.
     */
    public static void attachNative(File agentLibrary, ProcessProvider processProvider, String argument, AttachmentProvider attachmentProvider) {
        install(attachmentProvider, processProvider.resolve(), argument, new AgentProvider.ForExistingAgent(agentLibrary), true);
    }

    /**
     * <p>
     * Installs an agent on the currently running Java virtual machine. Unfortunately, this does
     * not always work. The runtime installation of a Java agent is supported for:
     * </p>
     * <ul>
     * <li><b>JVM version 9+</b>: For Java VM of at least version 9, the attachment API was moved
     * into a module and the runtime installation is possible if the {@code jdk.attach} module is
     * available to Byte Buddy which is typically only available for VMs shipped with a JDK.</li>
     * <li><b>OpenJDK / Oracle JDK / IBM J9 versions 8-</b>: The installation for HotSpot is only
     * possible when bundled with a JDK and requires a {@code tools.jar} bundled with the VM which
     * is typically only available for JDK-versions of the JVM.</li>
     * <li>When running Linux and including the optional <i>junixsocket-native-common</i> depedency,
     * Byte Buddy emulates a Unix socket connection to attach to the target VM.</li>
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
     * <p>
     * Installs an agent on the currently running Java virtual machine using the supplied
     * attachment provider.
     * </p>
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
     * @param attachmentProvider The attachment provider to use for the installation.
     * @return An instrumentation instance representing the currently running JVM.
     */
    public static Instrumentation install(AttachmentProvider attachmentProvider) {
        return install(attachmentProvider, ProcessProvider.ForCurrentVm.INSTANCE);
    }


    /**
     * <p>
     * Installs an agent on the Java virtual machine resolved by the process provider. Unfortunately, this does
     * not always work. The runtime installation of a Java agent is supported for:
     * </p>
     * <ul>
     * <li><b>JVM version 9+</b>: For Java VM of at least version 9, the attachment API was moved
     * into a module and the runtime installation is possible if the {@code jdk.attach} module is
     * available to Byte Buddy which is typically only available for VMs shipped with a JDK.</li>
     * <li><b>OpenJDK / Oracle JDK / IBM J9 versions 8-</b>: The installation for HotSpot is only
     * possible when bundled with a JDK and requires a {@code tools.jar} bundled with the VM which
     * is typically only available for JDK-versions of the JVM.</li>
     * <li>When running Linux and including the optional <i>junixsocket-native-common</i> depedency,
     * Byte Buddy emulates a Unix socket connection to attach to the target VM.</li>
     * </ul>
     * <p>
     * If an agent cannot be installed, an {@link IllegalStateException} is thrown.
     * </p>
     *
     * @param processProvider The provider for the current JVM's process id.
     * @return An instrumentation instance representing the currently running JVM.
     */
    public static Instrumentation install(ProcessProvider processProvider) {
        return install(AttachmentProvider.DEFAULT, processProvider);
    }

    /**
     * <p>
     * Installs an agent on the currently running Java virtual machine using the supplied
     * attachment provider and process provider.
     * </p>
     * <p>
     * If an agent cannot be installed, an {@link IllegalStateException} is thrown.
     * </p>
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
        install(attachmentProvider, processProvider.resolve(), WITHOUT_ARGUMENT, AgentProvider.ForByteBuddyAgent.INSTANCE, false);
        return doGetInstrumentation();
    }

    /**
     * Installs a Java agent on a target VM.
     *
     * @param attachmentProvider The attachment provider to use.
     * @param processId          The process id of the target JVM process.
     * @param argument           The argument to provide to the agent.
     * @param agentProvider      The agent provider for the agent jar or library.
     * @param isNative           {@code true} if the agent is native.
     */
    private static void install(AttachmentProvider attachmentProvider, String processId, String argument, AgentProvider agentProvider, boolean isNative) {
        AttachmentProvider.Accessor attachmentAccessor = attachmentProvider.attempt();
        if (!attachmentAccessor.isAvailable()) {
            throw new IllegalStateException("No compatible attachment provider is available");
        }
        try {
            if (attachmentAccessor.isExternalAttachmentRequired() && ATTACHMENT_TYPE_EVALUATOR.requiresExternalAttachment(processId)) {
                installExternal(attachmentAccessor.getExternalAttachment(), processId, agentProvider.resolve(), isNative, argument);
            } else {
                Attacher.install(attachmentAccessor.getVirtualMachineType(), processId, agentProvider.resolve().getAbsolutePath(), isNative, argument);
            }
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Error during attachment using: " + attachmentProvider, exception);
        }
    }

    /**
     * Installs a Java agent to the current VM via an external process. This is typically required starting with OpenJDK 9
     * when the {@code jdk.attach.allowAttachSelf} property is set to {@code false} what is the default setting.
     *
     * @param externalAttachment A description of the external attachment.
     * @param processId          The process id of the current process.
     * @param agent              The Java agent to install.
     * @param isNative           {@code true} if the agent is native.
     * @param argument           The argument to provide to the agent or {@code null} if no argument should be supplied.
     * @throws Exception If an exception occurs during the attachment or the external process fails the attachment.
     */
    private static void installExternal(AttachmentProvider.Accessor.ExternalAttachment externalAttachment,
                                        String processId,
                                        File agent,
                                        boolean isNative,
                                        String argument) throws Exception {
        File selfResolvedJar = trySelfResolve(), attachmentJar = null;
        try {
            if (selfResolvedJar == null) {
                InputStream inputStream = Attacher.class.getResourceAsStream('/' + Attacher.class.getName().replace('.', '/') + CLASS_FILE_EXTENSION);
                if (inputStream == null) {
                    throw new IllegalStateException("Cannot locate class file for Byte Buddy installation process");
                }
                try {
                    attachmentJar = File.createTempFile(ATTACHER_FILE_NAME, JAR_FILE_EXTENSION);
                    JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(attachmentJar));
                    try {
                        jarOutputStream.putNextEntry(new JarEntry(Attacher.class.getName().replace('.', '/') + CLASS_FILE_EXTENSION));
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
            StringBuilder classPath = new StringBuilder().append(quote((selfResolvedJar == null
                    ? attachmentJar
                    : selfResolvedJar).getCanonicalPath()));
            for (File jar : externalAttachment.getClassPath()) {
                classPath.append(File.pathSeparatorChar).append(quote(jar.getCanonicalPath()));
            }
            if (new ProcessBuilder(System.getProperty(JAVA_HOME)
                    + File.separatorChar + "bin"
                    + File.separatorChar + (System.getProperty(OS_NAME, "").toLowerCase(Locale.US).contains("windows") ? "java.exe" : "java"),
                    CLASS_PATH_ARGUMENT,
                    classPath.toString(),
                    Attacher.class.getName(),
                    externalAttachment.getVirtualMachineType(),
                    processId,
                    quote(agent.getAbsolutePath()),
                    Boolean.toString(isNative),
                    argument == null ? "" : ("=" + argument)).start().waitFor() != SUCCESSFUL_ATTACH) {
                throw new IllegalStateException("Could not self-attach to current VM using external process");
            }
        } finally {
            if (attachmentJar != null) {
                if (!attachmentJar.delete()) {
                    attachmentJar.deleteOnExit();
                }
            }
        }
    }

    /**
     * Attempts to resolve the location of the {@link Attacher} class for a self-attachment. Doing so avoids the creation of a temporary jar file.
     *
     * @return The self-resolved jar file or {@code null} if the jar file cannot be located.
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
    private static File trySelfResolve() {
        try {
            if (Boolean.getBoolean(LATENT_RESOLVE)) {
                return CANNOT_SELF_RESOLVE;
            }
            ProtectionDomain protectionDomain = Attacher.class.getProtectionDomain();
            if (protectionDomain == null) {
                return CANNOT_SELF_RESOLVE;
            }
            CodeSource codeSource = protectionDomain.getCodeSource();
            if (codeSource == null) {
                return CANNOT_SELF_RESOLVE;
            }
            URL location = codeSource.getLocation();
            if (!location.getProtocol().equals(FILE_PROTOCOL)) {
                return CANNOT_SELF_RESOLVE;
            }
            try {
                return new File(location.toURI());
            } catch (URISyntaxException ignored) {
                return new File(location.getPath());
            }
        } catch (Exception ignored) {
            return CANNOT_SELF_RESOLVE;
        }
    }

    /**
     * Quotes a value if it contains a white space.
     *
     * @param value The value to quote.
     * @return The value being quoted if necessary.
     */
    private static String quote(String value) {
        return value.contains(" ")
                ? '"' + value + '"'
                : value;
    }

    /**
     * Performs the actual lookup of the {@link java.lang.instrument.Instrumentation} from an installed
     * Byte Buddy agent.
     *
     * @return The Byte Buddy agent's {@link java.lang.instrument.Instrumentation} instance.
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Legal outcome where reflection communicates errors by throwing an exception")
    private static Instrumentation doGetInstrumentation() {
        try {
            return (Instrumentation) ClassLoader.getSystemClassLoader()
                    .loadClass(Installer.class.getName())
                    .getMethod(INSTRUMENTATION_METHOD)
                    .invoke(STATIC_MEMBER);
        } catch (Exception ignored) {
            return UNAVAILABLE;
        }
    }

    /**
     * An attachment provider is responsible for making the Java attachment API available.
     */
    @SuppressFBWarnings(value = "IC_SUPERCLASS_USES_SUBCLASS_DURING_INITIALIZATION", justification = "Safe initialization is implied")
    public interface AttachmentProvider {

        /**
         * The default attachment provider to be used.
         */
        AttachmentProvider DEFAULT = new Compound(ForModularizedVm.INSTANCE,
                ForJ9Vm.INSTANCE,
                ForStandardToolsJarVm.JVM_ROOT,
                ForStandardToolsJarVm.JDK_ROOT,
                ForStandardToolsJarVm.MACINTOSH,
                ForUserDefinedToolsJar.INSTANCE,
                ForEmulatedAttachment.INSTANCE);

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
             * Returns {@code true} if this accessor prohibits attachment to the same virtual machine in Java 9 and later.
             *
             * @return {@code true} if this accessor prohibits attachment to the same virtual machine in Java 9 and later.
             */
            boolean isExternalAttachmentRequired();

            /**
             * Returns a {@code VirtualMachine} class. This method must only be called for available accessors.
             *
             * @return The virtual machine type.
             */
            Class<?> getVirtualMachineType();

            /**
             * Returns a description of a virtual machine class for an external attachment.
             *
             * @return A description of the external attachment.
             */
            ExternalAttachment getExternalAttachment();

            /**
             * A canonical implementation of an unavailable accessor.
             */
            enum Unavailable implements Accessor {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public boolean isAvailable() {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isExternalAttachmentRequired() {
                    throw new IllegalStateException("Cannot read the virtual machine type for an unavailable accessor");
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> getVirtualMachineType() {
                    throw new IllegalStateException("Cannot read the virtual machine type for an unavailable accessor");
                }

                /**
                 * {@inheritDoc}
                 */
                public ExternalAttachment getExternalAttachment() {
                    throw new IllegalStateException("Cannot read the virtual machine type for an unavailable accessor");
                }
            }

            /**
             * Describes an external attachment to a Java virtual machine.
             */
            class ExternalAttachment {

                /**
                 * The fully-qualified binary name of the virtual machine type.
                 */
                private final String virtualMachineType;

                /**
                 * The class path elements required for loading the supplied virtual machine type.
                 */
                private final List<File> classPath;

                /**
                 * Creates an external attachment.
                 *
                 * @param virtualMachineType The fully-qualified binary name of the virtual machine type.
                 * @param classPath          The class path elements required for loading the supplied virtual machine type.
                 */
                public ExternalAttachment(String virtualMachineType, List<File> classPath) {
                    this.virtualMachineType = virtualMachineType;
                    this.classPath = classPath;
                }

                /**
                 * Returns the fully-qualified binary name of the virtual machine type.
                 *
                 * @return The fully-qualified binary name of the virtual machine type.
                 */
                public String getVirtualMachineType() {
                    return virtualMachineType;
                }

                /**
                 * Returns the class path elements required for loading the supplied virtual machine type.
                 *
                 * @return The class path elements required for loading the supplied virtual machine type.
                 */
                public List<File> getClassPath() {
                    return classPath;
                }
            }

            /**
             * A simple implementation of an accessible accessor.
             */
            abstract class Simple implements Accessor {

                /**
                 * A {@code VirtualMachine} class.
                 */
                protected final Class<?> virtualMachineType;

                /**
                 * Creates a new simple accessor.
                 *
                 * @param virtualMachineType A {@code VirtualMachine} class.
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
                 * @param classPath   The class path required to load the virtual machine class.
                 * @return An appropriate accessor.
                 */
                public static Accessor of(ClassLoader classLoader, File... classPath) {
                    try {
                        return new Simple.WithExternalAttachment(classLoader.loadClass(VIRTUAL_MACHINE_TYPE_NAME),
                                Arrays.asList(classPath));
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
                        return new Simple.WithExternalAttachment(ClassLoader.getSystemClassLoader().loadClass(VIRTUAL_MACHINE_TYPE_NAME_J9),
                                Collections.<File>emptyList());
                    } catch (ClassNotFoundException ignored) {
                        return Unavailable.INSTANCE;
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isAvailable() {
                    return true;
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> getVirtualMachineType() {
                    return virtualMachineType;
                }

                /**
                 * A simple implementation of an accessible accessor that allows for external attachment.
                 */
                protected static class WithExternalAttachment extends Simple {

                    /**
                     * The class path required for loading the virtual machine type.
                     */
                    private final List<File> classPath;

                    /**
                     * Creates a new simple accessor that allows for external attachment.
                     *
                     * @param virtualMachineType The {@code com.sun.tools.attach.VirtualMachine} class.
                     * @param classPath          The class path required for loading the virtual machine type.
                     */
                    public WithExternalAttachment(Class<?> virtualMachineType, List<File> classPath) {
                        super(virtualMachineType);
                        this.classPath = classPath;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public boolean isExternalAttachmentRequired() {
                        return true;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ExternalAttachment getExternalAttachment() {
                        return new ExternalAttachment(virtualMachineType.getName(), classPath);
                    }
                }

                /**
                 * A simple implementation of an accessible accessor that attaches using a virtual machine emulation that does not require external attachment.
                 */
                protected static class WithDirectAttachment extends Simple {

                    /**
                     * Creates a new simple accessor that implements direct attachment.
                     *
                     * @param virtualMachineType A {@code VirtualMachine} class.
                     */
                    public WithDirectAttachment(Class<?> virtualMachineType) {
                        super(virtualMachineType);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public boolean isExternalAttachmentRequired() {
                        return false;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ExternalAttachment getExternalAttachment() {
                        throw new IllegalStateException("Cannot apply external attachment");
                    }
                }
            }
        }

        /**
         * An attachment provider that locates the attach API directly from the system class loader, as possible since
         * introducing the Java module system via the {@code jdk.attach} module.
         */
        enum ForModularizedVm implements AttachmentProvider {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public Accessor attempt() {
                return Accessor.Simple.of(ClassLoader.getSystemClassLoader());
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

            /**
             * {@inheritDoc}
             */
            public Accessor attempt() {
                return Accessor.Simple.ofJ9();
            }
        }

        /**
         * An attachment provider that is dependant on the existence of a <i>tools.jar</i> file on the local
         * file system.
         */
        enum ForStandardToolsJarVm implements AttachmentProvider {

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
            ForStandardToolsJarVm(String toolsJarPath) {
                this.toolsJarPath = toolsJarPath;
            }

            /**
             * {@inheritDoc}
             */
            @SuppressFBWarnings(value = "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED", justification = "Privilege is explicit user responsibility")
            public Accessor attempt() {
                File toolsJar = new File(System.getProperty(JAVA_HOME_PROPERTY), toolsJarPath);
                try {
                    return toolsJar.isFile() && toolsJar.canRead()
                            ? Accessor.Simple.of(new URLClassLoader(new URL[]{toolsJar.toURI().toURL()}, BOOTSTRAP_CLASS_LOADER), toolsJar)
                            : Accessor.Unavailable.INSTANCE;
                } catch (MalformedURLException exception) {
                    throw new IllegalStateException("Could not represent " + toolsJar + " as URL");
                }
            }
        }

        /**
         * An attachment provider that attempts to locate a {@code tools.jar} from a custom location set via a system property.
         */
        enum ForUserDefinedToolsJar implements AttachmentProvider {

            /**
             * The singelton instance.
             */
            INSTANCE;

            /**
             * The property being read for locating {@code tools.jar}.
             */
            public static final String PROPERTY = "net.bytebuddy.agent.toolsjar";

            /**
             * {@inheritDoc}
             */
            @SuppressFBWarnings(value = "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED", justification = "Privilege is explicit user responsibility")
            public Accessor attempt() {
                String location = System.getProperty(PROPERTY);
                if (location == null) {
                    return Accessor.Unavailable.INSTANCE;
                } else {
                    File toolsJar = new File(location);
                    try {
                        return Accessor.Simple.of(new URLClassLoader(new URL[]{toolsJar.toURI().toURL()}, BOOTSTRAP_CLASS_LOADER), toolsJar);
                    } catch (MalformedURLException exception) {
                        throw new IllegalStateException("Could not represent " + toolsJar + " as URL");
                    }
                }
            }
        }

        /**
         * An attachment provider that uses Byte Buddy's attachment API emulation. To use this feature, JNA is required.
         */
        enum ForEmulatedAttachment implements AttachmentProvider {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public Accessor attempt() {
                try {
                    return new Accessor.Simple.WithDirectAttachment(AccessController.doPrivileged(VirtualMachine.Resolver.INSTANCE));
                } catch (Throwable ignored) {
                    return Accessor.Unavailable.INSTANCE;
                }
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
            private final List<AttachmentProvider> attachmentProviders;

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
                this.attachmentProviders = new ArrayList<AttachmentProvider>();
                for (AttachmentProvider attachmentProvider : attachmentProviders) {
                    if (attachmentProvider instanceof Compound) {
                        this.attachmentProviders.addAll(((Compound) attachmentProvider).attachmentProviders);
                    } else {
                        this.attachmentProviders.add(attachmentProvider);
                    }
                }
            }

            /**
             * {@inheritDoc}
             */
            public Accessor attempt() {
                for (AttachmentProvider attachmentProvider : attachmentProviders) {
                    Accessor accessor = attachmentProvider.attempt();
                    if (accessor.isAvailable()) {
                        return accessor;
                    }
                }
                return Accessor.Unavailable.INSTANCE;
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

            /**
             * {@inheritDoc}
             */
            public String resolve() {
                return dispatcher.resolve();
            }

            /**
             * A process provider for a legacy VM that reads the process id from its JMX properties.
             */
            protected enum ForLegacyVm implements ProcessProvider {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public String resolve() {
                    String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
                    int processIdIndex = runtimeName.indexOf('@');
                    if (processIdIndex == -1) {
                        throw new IllegalStateException("Cannot extract process id from runtime management bean");
                    } else {
                        return runtimeName.substring(0, processIdIndex);
                    }
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
                 * The {@code java.lang.ProcessHandle#pid()} method.
                 */
                private final Method pid;

                /**
                 * Creates a new Java 9 capable dispatcher for reading the current process's id.
                 *
                 * @param current The {@code java.lang.ProcessHandle#current()} method.
                 * @param pid     The {@code java.lang.ProcessHandle#pid()} method.
                 */
                protected ForJava9CapableVm(Method current, Method pid) {
                    this.current = current;
                    this.pid = pid;
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
                        return new ForJava9CapableVm(Class.forName("java.lang.ProcessHandle").getMethod("current"),
                                Class.forName("java.lang.ProcessHandle").getMethod("pid"));
                    } catch (Exception ignored) {
                        return ForLegacyVm.INSTANCE;
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public String resolve() {
                    try {
                        return pid.invoke(current.invoke(STATIC_MEMBER)).toString();
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access Java 9 process API", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error when accessing Java 9 process API", exception.getCause());
                    }
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
             * Attempts to resolve the {@link Installer} class from this jar file if it can be located. Doing so, it is possible
             * to avoid the creation of a temporary jar file which can remain undeleted on Windows operating systems where the agent
             * is linked by a class loader such that {@link File#deleteOnExit()} does not have an effect.
             *
             * @return This jar file's location or {@code null} if this jar file's location is inaccessible.
             * @throws IOException If an I/O exception occurs.
             */
            private static File trySelfResolve() throws IOException {
                ProtectionDomain protectionDomain = Installer.class.getProtectionDomain();
                if (Boolean.getBoolean(LATENT_RESOLVE)) {
                    return CANNOT_SELF_RESOLVE;
                }
                if (protectionDomain == null) {
                    return CANNOT_SELF_RESOLVE;
                }
                CodeSource codeSource = protectionDomain.getCodeSource();
                if (codeSource == null) {
                    return CANNOT_SELF_RESOLVE;
                }
                URL location = codeSource.getLocation();
                if (!location.getProtocol().equals(FILE_PROTOCOL)) {
                    return CANNOT_SELF_RESOLVE;
                }
                File agentJar;
                try {
                    agentJar = new File(location.toURI());
                } catch (URISyntaxException ignored) {
                    agentJar = new File(location.getPath());
                }
                if (!agentJar.isFile() || !agentJar.canRead()) {
                    return CANNOT_SELF_RESOLVE;
                }
                // It is necessary to check the manifest of the containing file as this code can be shaded into another artifact.
                JarInputStream jarInputStream = new JarInputStream(new FileInputStream(agentJar));
                try {
                    Manifest manifest = jarInputStream.getManifest();
                    if (manifest == null) {
                        return CANNOT_SELF_RESOLVE;
                    }
                    Attributes attributes = manifest.getMainAttributes();
                    if (attributes == null) {
                        return CANNOT_SELF_RESOLVE;
                    }
                    if (Installer.class.getName().equals(attributes.getValue(AGENT_CLASS_PROPERTY))
                            && Boolean.parseBoolean(attributes.getValue(CAN_REDEFINE_CLASSES_PROPERTY))
                            && Boolean.parseBoolean(attributes.getValue(CAN_RETRANSFORM_CLASSES_PROPERTY))
                            && Boolean.parseBoolean(attributes.getValue(CAN_SET_NATIVE_METHOD_PREFIX))) {
                        return agentJar;
                    } else {
                        return CANNOT_SELF_RESOLVE;
                    }
                } finally {
                    jarInputStream.close();
                }
            }

            /**
             * Creates an agent jar file containing the {@link Installer} class.
             *
             * @return The agent jar file.
             * @throws IOException If an I/O exception occurs.
             */
            private static File createJarFile() throws IOException {
                InputStream inputStream = Installer.class.getResourceAsStream('/' + Installer.class.getName().replace('.', '/') + CLASS_FILE_EXTENSION);
                if (inputStream == null) {
                    throw new IllegalStateException("Cannot locate class file for Byte Buddy installer");
                }
                try {
                    File agentJar = File.createTempFile(AGENT_FILE_NAME, JAR_FILE_EXTENSION);
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
                    return agentJar;
                } finally {
                    inputStream.close();
                }
            }

            /**
             * {@inheritDoc}
             */
            public File resolve() throws IOException {
                try {
                    File agentJar = trySelfResolve();
                    return agentJar == null
                            ? createJarFile()
                            : agentJar;
                } catch (Exception ignored) {
                    return createJarFile();
                }
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

            /**
             * {@inheritDoc}
             */
            public File resolve() {
                return agent;
            }
        }
    }

    /**
     * An attachment evaluator is responsible for deciding if an agent can be attached from the current process.
     */
    protected interface AttachmentTypeEvaluator {

        /**
         * Checks if the current VM requires external attachment for the supplied process id.
         *
         * @param processId The process id of the process to which to attach.
         * @return {@code true} if the current VM requires external attachment for the supplied process.
         */
        boolean requiresExternalAttachment(String processId);

        /**
         * An installation action for creating an attachment type evaluator.
         */
        enum InstallationAction implements PrivilegedAction<AttachmentTypeEvaluator> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * The OpenJDK's property for specifying the legality of self-attachment.
             */
            private static final String JDK_ALLOW_SELF_ATTACH = "jdk.attach.allowAttachSelf";

            /**
             * {@inheritDoc}
             */
            @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
            public AttachmentTypeEvaluator run() {
                try {
                    if (Boolean.getBoolean(JDK_ALLOW_SELF_ATTACH)) {
                        return Disabled.INSTANCE;
                    } else {
                        return new ForJava9CapableVm(Class.forName("java.lang.ProcessHandle").getMethod("current"),
                                Class.forName("java.lang.ProcessHandle").getMethod("pid"));
                    }
                } catch (Exception ignored) {
                    return Disabled.INSTANCE;
                }
            }
        }

        /**
         * An attachment type evaluator that never requires external attachment.
         */
        enum Disabled implements AttachmentTypeEvaluator {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public boolean requiresExternalAttachment(String processId) {
                return false;
            }
        }

        /**
         * An attachment type evaluator that checks a process id against the current process id.
         */
        class ForJava9CapableVm implements AttachmentTypeEvaluator {

            /**
             * The {@code java.lang.ProcessHandle#current()} method.
             */
            private final Method current;

            /**
             * The {@code java.lang.ProcessHandle#pid()} method.
             */
            private final Method pid;

            /**
             * Creates a new attachment type evaluator.
             *
             * @param current The {@code java.lang.ProcessHandle#current()} method.
             * @param pid     The {@code java.lang.ProcessHandle#pid()} method.
             */
            protected ForJava9CapableVm(Method current, Method pid) {
                this.current = current;
                this.pid = pid;
            }

            /**
             * {@inheritDoc}
             */
            public boolean requiresExternalAttachment(String processId) {
                try {
                    return pid.invoke(current.invoke(STATIC_MEMBER)).toString().equals(processId);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access Java 9 process API", exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Error when accessing Java 9 process API", exception.getCause());
                }
            }
        }
    }
}
