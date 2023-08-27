package net.bytebuddy.build;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import static org.junit.Assert.assertTrue;

public final class DoesNotCrashWhenTypeMissingDepsTest {
    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Test
    public void does_not_crash_when_type_is_missing_deps() throws IOException {

        File jar = tempDir.newFile("testInput.jar");

        try (
            FileOutputStream fileOutputStream = new FileOutputStream(jar);
            JarOutputStream jarBuilder = new JarOutputStream(fileOutputStream, new Manifest())
        ) {

            addDirs(jarBuilder, DoesNotCrashWhenTypeMissingDepsTest.class);

            // Add type with a required dependency, but don't add its dependency
            // so building/instrumenting the type fails.
            addClass(jarBuilder, TypeWithDep.class);
            addClass(jarBuilder, TypeToInstrument.class);
        }


        String containerTypeName = DoesNotCrashWhenTypeMissingDepsTest.class.getTypeName();

        Plugin plugin = new Plugin() {
            @Override
            public DynamicType.Builder<?> apply(
                DynamicType.Builder<?> builder,
                TypeDescription typeDescription,
                ClassFileLocator classFileLocator
            ) {
                return builder.visit(new AsmVisitorWrapper.ForDeclaredFields().field(target -> true));
            }

            @Override
            public boolean matches(TypeDescription target) {
                return (
                    (containerTypeName + "$" + TypeToInstrument.class.getSimpleName()).equals(target.getTypeName()) ||
                    (containerTypeName + "$" + TypeWithDep.class.getSimpleName()).equals(target.getTypeName())
                );
            }

            @Override
            public void close() {}
        };

        assertTrue(
            new Plugin.Engine.Default()
                .withoutErrorHandlers()
                .apply(
                    jar,
                    tempDir.newFile("testOutput.jar"),
                    Arrays.asList(() -> plugin)
                )
                .getFailed()
                .containsKey(
                    TypeDescription.ForLoadedType.of(TypeWithDep.class)));

    }

    public static final class TypeDep {}

    public static final class TypeWithDep {
        private static final TypeDep DEP = new TypeDep();
    }

    public static final class TypeToInstrument {}

    private static void addDirs(JarOutputStream jarBuilder, Class<?> aClass) throws IOException {
        StringBuilder pathBuilder = new StringBuilder();
        String filePath = getFilePath(aClass);
        String[] pathSegments = filePath.split("[" + File.separator + "]");

        for (int i = 0; i < pathSegments.length - 1; i++) {
            pathBuilder.append(pathSegments[i]);
            pathBuilder.append("/");
            jarBuilder.putNextEntry(new ZipEntry(pathBuilder.toString()));
            jarBuilder.closeEntry();
        }
    }

    private static void addClass(JarOutputStream jarBuilder, Class<?> aClass) throws IOException {
        String filePath = getFilePath(aClass);
        jarBuilder.putNextEntry(new ZipEntry(filePath));

        try (InputStream inputStream =
            DoesNotCrashWhenTypeMissingDepsTest.class.getClassLoader().getResourceAsStream(filePath)) {

            byte[] bytes = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(bytes, 0, bytes.length)) != -1) {
                jarBuilder.write(bytes, 0, bytesRead);
            }
        }

        jarBuilder.closeEntry();
    }

    private static String getFilePath(Class<?> aClass) {
        return aClass.getTypeName().replace(".", "/") + ".class";
    }
}
