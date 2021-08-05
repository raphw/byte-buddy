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
package net.bytebuddy.utility;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;

/**
 * A dispatcher to interact with the file system. If NIO2 is available, the API is used. Otherwise, byte streams are used.
 */
public abstract class FileSystem {

    /**
     * The file system accessor to use.
     */
    public static final FileSystem INSTANCE = doPrivileged(CreationAction.INSTANCE);

    /**
     * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
     *
     * @param action The action to execute from a privileged context.
     * @param <T>    The type of the action's resolved value.
     * @return The action's resolved value.
     */
    @AccessControllerPlugin.Enhance
    private static <T> T doPrivileged(PrivilegedAction<T> action) {
        return action.run();
    }

    /**
     * Copies a file.
     *
     * @param source The source file.
     * @param target The target file.
     * @throws IOException If an I/O exception occurs.
     */
    public abstract void copy(File source, File target) throws IOException;

    /**
     * Moves a file.
     *
     * @param source The source file.
     * @param target The target file.
     * @throws IOException If an I/O exception occurs.
     */
    public abstract void move(File source, File target) throws IOException;

    /**
     * An action to create a dispatcher for a {@link FileSystem}.
     */
    protected enum CreationAction implements PrivilegedAction<FileSystem> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger fallback")
        public FileSystem run() {
            try {
                Class<?> files = Class.forName("java.nio.file.Files"),
                        path = Class.forName("java.nio.file.Path"),
                        copyOption = Class.forName("[Ljava.nio.file.CopyOption;");
                return new ForNio2CapableVm(files.getMethod("copy", path, path, copyOption), files.getMethod("move", path, path, copyOption));
            } catch (Exception ignored) {
                return new ForLegacyVm();
            }
        }
    }

    /**
     * A file system representation for a VM that does not support NIO2.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class ForLegacyVm extends FileSystem {

        @Override
        public void copy(File source, File target) throws IOException {
            InputStream inputStream = new FileInputStream(source);
            try {
                OutputStream outputStream = new FileOutputStream(target);
                try {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, length);
                    }
                } finally {
                    outputStream.close();
                }
            } finally {
                inputStream.close();
            }
        }

        @Override
        public void move(File source, File target) throws IOException {
            InputStream inputStream = new FileInputStream(source);
            try {
                OutputStream outputStream = new FileOutputStream(target);
                try {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, length);
                    }
                } finally {
                    outputStream.close();
                }
            } finally {
                inputStream.close();
            }
            if (!source.delete()) {
                source.deleteOnExit();
            }
        }
    }

    /**
     * A file system representation for a VM that does support NIO2.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class ForNio2CapableVm extends FileSystem {

        /**
         * Indicates a static method invocation.
         */
        private static final Object STATIC_MEMBER = null;

        /**
         * A dispatcher to resolve a {@link File} to a {@code java.nio.file.Path}.
         */
        private static final Dispatcher DISPATCHER = doPrivileged(JavaDispatcher.of(Dispatcher.class));

        /**
         * A dispatcher to interact with {@code java.nio.file.StandardCopyOption}.
         */
        private static final StandardCopyOption STANDARD_COPY_OPTION = doPrivileged(JavaDispatcher.of(StandardCopyOption.class));

        /**
         * The {@code java.nio.file.Files#copy} method.
         */
        private final Method copy;

        /**
         * The {@code java.nio.file.Files#move} method.
         */
        private final Method move;

        /**
         * Creates a new NIO2-capable file system dispatcher.
         *
         * @param copy The {@code java.nio.file.Files#copy} method.
         * @param move The {@code java.nio.file.Files#move} method.
         */
        protected ForNio2CapableVm(Method copy, Method move) {
            this.copy = copy;
            this.move = move;
        }

        @Override
        public void copy(File source, File target) throws IOException {
            Object[] option = STANDARD_COPY_OPTION.toArray(1);
            option[0] = STANDARD_COPY_OPTION.valueOf("REPLACE_EXISTING");
            try {
                copy.invoke(STATIC_MEMBER, DISPATCHER.toPath(source), DISPATCHER.toPath(target), option);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getTargetException();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else {
                    throw new IllegalStateException(cause);
                }
            }
        }

        @Override
        public void move(File source, File target) throws IOException {
            Object[] option = STANDARD_COPY_OPTION.toArray(1);
            option[0] = STANDARD_COPY_OPTION.valueOf("REPLACE_EXISTING");
            try {
                move.invoke(STATIC_MEMBER, DISPATCHER.toPath(source), DISPATCHER.toPath(target), option);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getTargetException();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else {
                    throw new IllegalStateException(cause);
                }
            }

        }

        /**
         * A dispatcher to resolve a {@link File} to a {@code java.nio.file.Path}.
         */
        @JavaDispatcher.Proxied("java.io.File")
        protected interface Dispatcher {

            /**
             * Resolves a {@link File} to a {@code java.nio.file.Path}.
             *
             * @param value The file to convert.
             * @return The transformed {@code java.nio.file.Path}.
             * @throws IOException If an I/O exception occurs.
             */
            Object toPath(File value) throws IOException;
        }

        /**
         * A dispatcher to interact with {@code java.nio.file.StandardCopyOption}.
         */
        @JavaDispatcher.Proxied("java.nio.file.StandardCopyOption")
        protected interface StandardCopyOption {

            /**
             * Creates an array of type {@code java.nio.file.StandardCopyOption}.
             *
             * @param size The array's size.
             * @return An array of type {@code java.nio.file.StandardCopyOption}.
             */
            @JavaDispatcher.Container
            Object[] toArray(int size);

            /**
             * Resolve an enumeration for {@code java.nio.file.StandardCopyOption}.
             *
             * @param name The enumeration name.
             * @return The enumeration value.
             */
            @JavaDispatcher.IsStatic
            Object valueOf(String name);
        }
    }
}
