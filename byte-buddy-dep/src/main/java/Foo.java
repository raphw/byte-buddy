import codes.rafael.asmjdkbridge.JdkClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.classfile.*;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LineNumber;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Foo {

    static class MagicUtil {

        public static boolean isInterface(String s) {
            return false;
        }

        public static String superClass(String s) {
            return "abc";
        }

        public static void field(CodeBuilder codeBuilder, FieldInstruction instruction) {

        }

        public static void method(CodeBuilder codeBuilder, InvokeInstruction instruction) {

        }
    }

    void apply(ClassVisitor classVisitor, ClassModel classModel) {
        classVisitor.visit(classModel.minorVersion() << 16 | classModel.majorVersion(),
                classModel.flags().flagsMask()
                        | (classModel.findAttribute(Attributes.deprecated()).isPresent() ? Opcodes.ACC_DEPRECATED : 0)
                        | (classModel.findAttribute(Attributes.synthetic()).isPresent() ? Opcodes.ACC_SYNTHETIC : 0)
                        | (classModel.findAttribute(Attributes.record()).isPresent() ? Opcodes.ACC_RECORD : 0),
                classModel.thisClass().asInternalName(),
                classModel.findAttribute(Attributes.signature()).map(signature -> signature.signature().stringValue()).orElse(null),
                classModel.superclass().map(ClassEntry::asInternalName).orElse(null),
                classModel.interfaces().stream().map(ClassEntry::asInternalName).toArray(String[]::new));
    }

    public static void main(String[] args) throws Exception {

        JdkClassReader classReader = new JdkClassReader()

        ClassVisitor classVisitor = null;
        ClassModel classModel = null;

        ClassFile classFile = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(classDesc -> {
            if (MagicUtil.isInterface(classDesc.displayName())) {
                return ClassHierarchyResolver.ClassHierarchyInfo.ofInterface();
            } else {
                return ClassHierarchyResolver.ClassHierarchyInfo.ofClass(
                        ClassDesc.of(MagicUtil.superClass(classDesc.displayName())));
            }
        }));
        ClassFile classFile1 = ClassFile.of(ClassFile.ConstantPoolSharingOption.NEW_POOL);
        ClassModel classModel = classFile1.parse(Path.of("Foo.class"));
        Utf8Entry name = classModel.thisClass().name();
        String thisClass = name.toString();

        //  STATELESS, CP_REFS, LABELS, UNSTABLE

        classFile.build(
                classModel.thisClass(),
                ConstantPoolBuilder.of(classModel),
                classBuilder -> {
                    classModel.forEach(classElemenet -> {
                        classBuilder.with(classElemenet);
                    });
                });

        ClassFile.of(ClassFile.AttributeMapperOption.of(name -> {
            return switch (name.stringValue()) {
                case "StringAttribute" -> new SomeCustomAttributeMapper();
                default -> null;
            };
        }));

        classFile.transformClass(classModel, (classBuilder, classElement) -> {
            switch (classElement) {
                case MethodModel methodModel -> classBuilder.transformMethod(
                        methodModel,
                        (methodBuilder, methodElement) -> {
                            switch (methodElement) {
                                case CodeModel codeModel -> methodBuilder.transformCode(
                                        codeModel,
                                        (codeBuilder, codeElement) -> {
                                            switch (codeElement) {
                                                case FieldInstruction instruction ->
                                                        MagicUtil.field(codeBuilder, instruction);
                                                case InvokeInstruction instruction ->
                                                        MagicUtil.method(codeBuilder, instruction);
                                                case LineNumber line -> {
                                                }
                                                default -> codeBuilder.with(codeElement);
                                            }
                                            ;
                                        });
                                default -> methodBuilder.with(methodElement);
                            }
                        });
                default -> classBuilder.with(classElement);
            }
        });
        classFile.bu

        classModel.methods().forEach(methodModel -> {
            String methodName = methodModel.methodName().stringValue();
            methodModel.code().ifPresent(codeModel -> {
                codeModel.forEach(codeElement -> {
                    switch (codeElement) {
                        case InvokeInstruction invoke -> invoke.owner();
                        case FieldInstruction invoke -> invoke.owner();
                        default -> throw new RuntimeException();
                    }
                });
            });
        });
        byte[] bytes = classFile.build(ClassDesc.of("Foo"), classBuilder -> {
            classBuilder.withFlags(AccessFlag.PUBLIC);
            classBuilder.withSuperclass(ClassDesc.of("Bar"));
            classBuilder.withMethod("add",
                    MethodTypeDesc.of(
                            int.class.describeConstable().orElseThrow(),
                            int.class.describeConstable().orElseThrow()),
                    AccessFlag.STATIC.mask(),
                    methodBuilder -> {
                        methodBuilder.withCode(codeBuilder -> {
                            codeBuilder.lineNumber(1);
                            int result = codeBuilder.allocateLocal(TypeKind.INT);
                            codeBuilder.iload(1);
                            codeBuilder.ldc(42);
                            codeBuilder.iadd();
                            codeBuilder.istore(result);
                            codeBuilder.lineNumber(2);
                            codeBuilder.iload(result);
                            codeBuilder.ireturn();
                        });
                    });
        });
        Files.write(Paths.get("sample.class"), bytes);
    }

    static int add(int value) {
        int result = value + 42; // 1
        return result;
    }
}
