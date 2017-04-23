package net.bytebuddy.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class Attacher {

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

    private static final String ATTACHER_FILE_NAME = "byteBuddyAttacher";

    private static final String CLASS_FILE_EXTENSION = ".class";

    private static final String JAR_FILE_EXTENSION = ".jar";

    public static void main(String[] args) {
        try {
            install(Class.forName(args[0]), args[1], new File(args[2]), args[3]);
        } catch (Exception ignored) {
            System.exit(1);
        }
    }

    protected static void installExternal(String virtualMachineType,
                                          List<File> jars,
                                          String processId,
                                          File agent,
                                          String argument)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException, InterruptedException {
        InputStream inputStream = Attacher.class.getResourceAsStream('/' + Attacher.class.getName().replace('.', '/') + CLASS_FILE_EXTENSION);
        if (inputStream == null) {
            throw new IllegalStateException("Cannot locate class file for Byte Buddy installation process");
        }
        File attachmentJar;
        try {
            attachmentJar = File.createTempFile(ATTACHER_FILE_NAME, JAR_FILE_EXTENSION);
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(attachmentJar));
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
        StringBuilder classPath = new StringBuilder(attachmentJar.getAbsolutePath());
        for (File jar : jars) {
            classPath.append(File.pathSeparatorChar).append(jar.getAbsolutePath());
        }
        if (new ProcessBuilder()
                .command(System.getProperty("java.home")
                        + " -cp " + classPath.toString()
                        + " " + Attacher.class.getName()
                        + " " + virtualMachineType + " " + processId + " " + agent.getAbsolutePath() + " " + argument.replace(" ", "\\ "))
                .start()
                .waitFor() != 0) {
            throw new IllegalStateException("Could not self-attach to current VM using external process");
        }
    }

    protected static void install(Class<?> virtualMachineType,
                                  String processId,
                                  File agent,
                                  String argument) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object virtualMachineInstance = virtualMachineType
                .getMethod(ATTACH_METHOD_NAME, String.class)
                .invoke(STATIC_MEMBER, processId);
        try {
            virtualMachineType
                    .getMethod(LOAD_AGENT_METHOD_NAME, String.class, String.class)
                    .invoke(virtualMachineInstance, agent.getAbsoluteFile(), argument);
        } finally {
            virtualMachineType
                    .getMethod(DETACH_METHOD_NAME)
                    .invoke(virtualMachineInstance);
        }
    }

    private Attacher() {
        throw new UnsupportedOperationException();
    }
}
