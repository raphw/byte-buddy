package net.bytebuddy.agent;

import java.lang.instrument.Instrumentation;

/**
 * An installer class which defined the hook-in methods that are required by the Java agent specification.
 */
public class Installer {

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
     * <p>
     * Returns the instrumentation that was loaded by the Byte Buddy agent. When a security manager is active,
     * the {@link RuntimePermission} for {@code getInstrumentation} is required by the caller.
     * </p>
     * <p>
     * <b>Important</b>: This method must only be invoked via the {@link ClassLoader#getSystemClassLoader()} where any
     * Java agent is loaded. It is possible that two versions of this class exist for different class loaders.
     * </p>
     *
     * @return The instrumentation instance of the Byte Buddy agent.
     */
    public static Instrumentation getInstrumentation() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new RuntimePermission("getInstrumentation"));
        }
        Instrumentation instrumentation = Installer.instrumentation;
        if (instrumentation == null) {
            throw new IllegalStateException("The Byte Buddy agent is not loaded or this method is not called via the system class loader");
        }
        return instrumentation;
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
    @SuppressWarnings("unused")
    public static void agentmain(String agentArguments, Instrumentation instrumentation) {
        Installer.instrumentation = instrumentation;
    }
}
