import groovy.transform.CompileStatic
import groovy.transform.Internal
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream

/**
 * This task is responsible for patching the Gradle API jars
 * so that we lower the class version to the minimal supported
 * version by ByteBuddy.
 *
 * This is a hack because they theoretically may depend on higher
 * versions.
 */
@CompileStatic
class DowngradeGradleApi extends DefaultTask {
    private static final int BUFFER = 4096

    @Input
    int classVersion

    @InputFiles
    FileCollection gradleApi

    @Internal
    File outputDir

    @OutputFiles
    FileCollection getDowngradedJars() {
        project.fileTree(outputDir)
    }

    @TaskAction
    void downgrade() {
        outputDir.deleteDir()
        outputDir.mkdir()
        gradleApi.files.each {
            transform(it)
        }
    }

    void transform(File jar) {
        def trn = new File(outputDir, jar.name)
        if (jar.name.startsWith("gradle-")) {
            trn.withOutputStream { out ->
                new JarOutputStream(out).withCloseable { jarOut ->
                    new JarInputStream(jar.newInputStream()).withCloseable { jarIn ->
                        JarEntry je
                        while ((je = jarIn.getNextJarEntry()) != null) {
                            def outEntry = new JarEntry(
                                    je.name
                            )
                            jarOut.putNextEntry(outEntry)
                            if (je.name.endsWith(".class")) {
                                ClassReader reader = new ClassReader(jarIn)
                                ClassWriter writer = new ClassWriter(0)
                                reader.accept(new VersionChanger(classVersion, writer), 0)
                                jarOut.write(writer.toByteArray())
                            } else {
                                byte[] data = new byte[BUFFER]
                                int count
                                while ((count = jarIn.read(data, 0, BUFFER)) != -1) {
                                    jarOut.write(data, 0, count);
                                }
                            }
                            jarIn.closeEntry()
                            jarOut.closeEntry()
                        }
                    }
                }
            }
        } else {
            trn.bytes = jar.bytes
        }
    }

    private final static class VersionChanger extends ClassVisitor implements Opcodes {
        private final int classVersion
        private boolean inInterface

        VersionChanger(int classVersion, ClassVisitor classWriter) {
            super(Opcodes.ASM7, classWriter)
            this.classVersion = classVersion
        }

        @Override
        void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(computeTargetVersion(version), access, name, signature, superName, interfaces)
            inInterface = (access & ACC_INTERFACE) == ACC_INTERFACE
        }

        @Override
        FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (shouldIgnore(descriptor) || shouldIgnore(signature)) {
                return
            }
            return super.visitField(access, name, descriptor, signature, value)
        }

        @Override
        MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (access == 4161 || inInterface && access == ACC_PUBLIC) {
                // default method, drop
                return
            }
            if (shouldIgnore(descriptor) || shouldIgnore(signature)) {
                return
            }
            if ((access & ACC_ABSTRACT) == ACC_ABSTRACT) {
                int vargs = access & ACC_VARARGS
                return new MethodVisitor(ASM7, super.visitMethod(inInterface ? ACC_PUBLIC | ACC_ABSTRACT | vargs : access, name, descriptor, signature, exceptions)) {
                    @Override
                    void visitCode() {
                    }

                }

            }
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }

        private static boolean shouldIgnore(String descriptor) {
            descriptor?.contains("java/time")
        }

        private int computeTargetVersion(int version) {
            return version > classVersion ? classVersion : version
        }
    }
}
