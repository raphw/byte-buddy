/*
 * Copyright 2014 - 2020 Rafael Winterhalter
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
package net.bytebuddy.build.gradle.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

/**
 * A transformer that removes classes in the {@code net.bytebuddy.build.gradle.api} package and substitutes them
 * with the type that they represent according to their {@code net.bytebuddy.build.gradle.api.GradleType}
 * type name.
 */
public class GradleTypeTransformer {

    /**
     * The API package path.
     */
    private static final String API = "net/bytebuddy/build/gradle/api";

    /**
     * Transforms the supplied jar file.
     *
     * @param jar The jar file to transform.
     * @throws IOException If an I/O exception occurs.
     */
    public void transform(File jar) throws IOException {
        Map<String, String> names = GradlePackageVisitor.toGradleTypeNames(jar);
        File temporary = File.createTempFile("gradle-byte-buddy-plugin", ".jar");
        JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jar));
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(temporary));
            try {
                JarEntry entry;
                while ((entry = jarInputStream.getNextJarEntry()) != null) {
                    if (!entry.getName().startsWith(API)) {
                        if (entry.getName().endsWith(".class")) {
                            ClassReader reader = new ClassReader(jarInputStream);
                            ClassWriter writer = new ClassWriter(0);
                            reader.accept(new ClassRemapper(writer, new SimpleRemapper(names)), 0);
                            jarOutputStream.putNextEntry(new JarEntry(entry.getName()));
                            jarOutputStream.write(writer.toByteArray());
                        } else {
                            jarOutputStream.putNextEntry(entry);
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = jarInputStream.read(buffer)) != -1) {
                                jarOutputStream.write(buffer, 0, length);
                            }
                        }
                        jarOutputStream.closeEntry();
                    }
                    jarInputStream.closeEntry();
                }
            } finally {
                jarOutputStream.close();
            }
        } finally {
            jarInputStream.close();
        }
        if (!jar.delete()) {
            throw new IllegalStateException("Could not delete " + jar);
        } else if (!temporary.renameTo(jar)) {
            throw new IllegalStateException("Could not rename " + temporary + " to " + jar);
        }
    }

    /**
     * A visitor that locates the {@code net.bytebuddy.build.gradle.api.GradleType} annotation to extract
     * a placeholder type's actual name.
     */
    protected static class GradlePackageVisitor extends ClassVisitor {

        /**
         * The names being discovered.
         */
        private final Map<String, String> names;

        /**
         * The source internal name or {@code null} if not discovered.
         */
        private String source;

        /**
         * The target internal name or {@code null} if not discovered.
         */
        private String target;

        /**
         * Creates a new Gradle package visitor.
         *
         * @param names The names being discovered.
         */
        protected GradlePackageVisitor(Map<String, String> names) {
            super(Opcodes.ASM8);
            this.names = names;
        }

        /**
         * Extracts a mapping of internal names to actual internal names of the Gradle API.
         *
         * @param jar The jar file to scan.
         * @return A mapping of internal names to actual internal names of the Gradle API.
         * @throws IOException If an I/O exception occurs.
         */
        protected static Map<String, String> toGradleTypeNames(File jar) throws IOException {
            Map<String, String> names = new HashMap<>();
            JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jar));
            try {
                JarEntry entry;
                while ((entry = jarInputStream.getNextJarEntry()) != null) {
                    if (entry.getName().startsWith(API)
                            && entry.getName().endsWith(".class")
                            && !entry.getName().equals(API + "/GradleType.class")) {
                        new ClassReader(jarInputStream).accept(new GradlePackageVisitor(names), ClassReader.SKIP_CODE);
                    }
                    jarInputStream.closeEntry();
                }
            } finally {
                jarInputStream.close();
            }
            return names;
        }

        @Override
        public void visit(int version, int modifier, String name, String signature, String superClassName, String[] interfaceName) {
            source = name;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return descriptor.equals("L" + API + "/GradleType;")
                    ? new GradleTypeAnnotationVisitor()
                    : null;
        }

        @Override
        public void visitEnd() {
            if (target == null) {
                throw new IllegalStateException("Missing annotation value for " + source);
            } else if (names.put(source, target) != null) {
                throw new IllegalStateException("Unexpected duplicate for " + source);
            }
        }

        /**
         * An annotation visitor to extract the {@code net.bytebuddy.build.gradle.api.GradleType#value} property.
         */
        protected class GradleTypeAnnotationVisitor extends AnnotationVisitor {

            /**
             * Creates a new package annotation visitor.
             */
            protected GradleTypeAnnotationVisitor() {
                super(Opcodes.ASM8);
            }

            @Override
            public void visit(String name, Object value) {
                if (!name.equals("value") || !(value instanceof String)) {
                    throw new IllegalStateException("Unexpected property: " + name);
                }
                target = ((String) value).replace('.', '/');
            }
        }
    }
}
