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

import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.build.CachedReturnPlugin;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;

import java.io.*;
import java.security.PrivilegedAction;

/**
 * A dispatcher to interact with the file system. If NIO2 is available, the API is used. Otherwise, byte streams are used.
 */
public abstract class FileSystem {

    /**
     * Returns the {@link FileSystem} instance to use.
     *
     * @return The {@link FileSystem} instance to use.
     */
    @CachedReturnPlugin.Enhance("INSTANCE")
    public static FileSystem getInstance() {
        try {
            Class.forName("java.nio.file.Files", false, ClassLoadingStrategy.BOOTSTRAP_LOADER);
            return new ForNio2CapableVm();
        } catch (ClassNotFoundException ignored) {
            return new ForLegacyVm();
        }
    }

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
     * Links a file as a hard-link. If linking is not supported, a copy is made.
     *
     * @param source The source file.
     * @param target The target file.
     * @throws IOException If an I/O exception occurs.
     */
    public void link(File source, File target) throws IOException {
        copy(source, target);
    }

    /**
     * Moves a file.
     *
     * @param source The source file.
     * @param target The target file.
     * @throws IOException If an I/O exception occurs.
     */
    public abstract void move(File source, File target) throws IOException;

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
         * A dispatcher to resolve a {@link File} to a {@code java.nio.file.Path}.
         */
        private static final Dispatcher DISPATCHER = doPrivileged(JavaDispatcher.of(Dispatcher.class));

        /**
         * A dispatcher to resolve a dispatcher for {@code java.nio.file.Files}.
         */
        private static final Files FILES = doPrivileged(JavaDispatcher.of(Files.class));

        /**
         * A dispatcher to interact with {@code java.nio.file.StandardCopyOption}.
         */
        private static final StandardCopyOption STANDARD_COPY_OPTION = doPrivileged(JavaDispatcher.of(StandardCopyOption.class));

        @Override
        public void copy(File source, File target) throws IOException {
            Object[] option = STANDARD_COPY_OPTION.toArray(1);
            option[0] = STANDARD_COPY_OPTION.valueOf("REPLACE_EXISTING");
            FILES.copy(DISPATCHER.toPath(source), DISPATCHER.toPath(target), option);
        }

        @Override
        public void link(File source, File target) throws IOException {
            FILES.createLink(FILES.deleteIfExists(DISPATCHER.toPath(target)), DISPATCHER.toPath(source));
        }

        @Override
        public void move(File source, File target) throws IOException {
            Object[] option = STANDARD_COPY_OPTION.toArray(1);
            option[0] = STANDARD_COPY_OPTION.valueOf("REPLACE_EXISTING");
            FILES.move(DISPATCHER.toPath(source), DISPATCHER.toPath(target), option);
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
         * A dispatcher to access the {@code java.nio.file.Files} API.
         */
        @JavaDispatcher.Proxied("java.nio.file.Files")
        protected interface Files {

            /**
             * Copies a file.
             *
             * @param source The source {@code java.nio.file.Path}.
             * @param target The target {@code java.nio.file.Path}.
             * @param option An array of copy options.
             * @return The copied file.
             * @throws IOException If an I/O exception occurs.
             */
            @JavaDispatcher.IsStatic
            Object copy(@JavaDispatcher.Proxied("java.nio.file.Path") Object source,
                        @JavaDispatcher.Proxied("java.nio.file.Path") Object target,
                        @JavaDispatcher.Proxied("java.nio.file.CopyOption") Object[] option) throws IOException;

            /**
             * Links a file.
             *
             * @param source The source {@code java.nio.file.Path}.
             * @param target The target {@code java.nio.file.Path}.
             * @return The copied file.
             * @throws IOException If an I/O exception occurs.
             */
            @JavaDispatcher.IsStatic
            Object createLink(@JavaDispatcher.Proxied("java.nio.file.Path") Object source,
                              @JavaDispatcher.Proxied("java.nio.file.Path") Object target) throws IOException;

            /**
             * Moves a file.
             *
             * @param source The source {@code java.nio.file.Path}.
             * @param target The target {@code java.nio.file.Path}.
             * @param option An array of copy options.
             * @return The moved file.
             * @throws IOException If an I/O exception occurs.
             */
            @JavaDispatcher.IsStatic
            Object move(@JavaDispatcher.Proxied("java.nio.file.Path") Object source,
                        @JavaDispatcher.Proxied("java.nio.file.Path") Object target,
                        @JavaDispatcher.Proxied("java.nio.file.CopyOption") Object[] option) throws IOException;

            /**
             * Deletes a file if it exists.
             *
             * @param file The {@code java.nio.file.Path} to delete if it exists.
             * @return The supplied file.
             * @throws IOException If an I/O exception occurs.
             */
            @JavaDispatcher.IsStatic
            Object deleteIfExists(@JavaDispatcher.Proxied("java.nio.file.Path") Object file) throws IOException;
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
