package net.bytebuddy.pool;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.GenericSignatureFormatError;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class TypePoolDefaultSignatureDepthTest {

    private static final String FOO = "foo";

    private static final String TYPE_NAME = "net.bytebuddy.test.Sample";

    private static final String INTERNAL_NAME = "net/bytebuddy/test/Sample";

    private static final String OBJECT_DESCRIPTOR = "Ljava/lang/Object;";

    private static final int EXCESSIVE_ARRAY_DEPTH = 40000;

    private static final int EXCESSIVE_PARAMETERIZED_DEPTH = 12000;

    private static String arraySignature(int depth) {
        StringBuilder signature = new StringBuilder();
        for (int index = 0; index < depth - 1; index++) {
            signature.append('[');
        }
        return signature.append("Ljava/util/List<").append(OBJECT_DESCRIPTOR).append(">;").toString();
    }

    private static String parameterizedSignature(int depth) {
        return parameterizedSignature(depth, "java/util/List");
    }

    private static String parameterizedSignature(int depth, String internalName) {
        StringBuilder signature = new StringBuilder();
        for (int index = 0; index < depth; index++) {
            signature.append('L').append(internalName).append('<');
        }
        signature.append(OBJECT_DESCRIPTOR);
        for (int index = 0; index < depth; index++) {
            signature.append(">;");
        }
        return signature.toString();
    }

    private static String excessiveParameterizedSignature() {
        return parameterizedSignature(EXCESSIVE_PARAMETERIZED_DEPTH, "A");
    }

    private static TypeDescription describe(byte[] classFile) {
        return TypePool.Default.of(new ClassFileLocator.Compound(
                ClassFileLocator.Simple.of(TYPE_NAME, classFile),
                ClassFileLocator.ForClassLoader.ofSystemLoader())).describe(TYPE_NAME).resolve();
    }

    private static TypeDescription ofField(String signature) {
        ClassWriter classWriter = new ClassWriter(0);
        classWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, INTERNAL_NAME, null, "java/lang/Object", null);
        classWriter.visitField(Opcodes.ACC_PUBLIC, FOO, OBJECT_DESCRIPTOR, signature, null).visitEnd();
        classWriter.visitEnd();
        return describe(classWriter.toByteArray());
    }

    private static TypeDescription ofMethod(String signature) {
        ClassWriter classWriter = new ClassWriter(0);
        classWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, INTERNAL_NAME, null, "java/lang/Object", null);
        classWriter.visitMethod(Opcodes.ACC_PUBLIC, FOO, "()" + OBJECT_DESCRIPTOR, "()" + signature, null).visitEnd();
        classWriter.visitEnd();
        return describe(classWriter.toByteArray());
    }

    private static TypeDescription ofType(String signature) {
        ClassWriter classWriter = new ClassWriter(0);
        classWriter.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC,
                INTERNAL_NAME,
                OBJECT_DESCRIPTOR + signature,
                "java/lang/Object",
                new String[]{"java/util/List"});
        classWriter.visitEnd();
        return describe(classWriter.toByteArray());
    }

    private static void assertMalformed(TypeDescription.Generic typeDescription) {
        assertThat(typeDescription.asErasure(), is(TypeDescription.ForLoadedType.of(Object.class)));
        try {
            typeDescription.getSort();
            fail("Expected a signature that exceeds the maximum nesting depth to be rejected");
        } catch (GenericSignatureFormatError ignored) {
            /* expected */
        }
    }

    @Test
    public void testFieldArraySignatureAtMaximumDepth() throws Exception {
        FieldDescription fieldDescription = ofField(arraySignature(TypePool.Default.SIGNATURE_DEPTH)).getDeclaredFields().getOnly();
        assertThat(fieldDescription.getType().getSort(), is(TypeDefinition.Sort.GENERIC_ARRAY));
    }

    @Test
    public void testFieldArraySignatureAboveMaximumDepth() throws Exception {
        FieldDescription fieldDescription = ofField(arraySignature(TypePool.Default.SIGNATURE_DEPTH + 1)).getDeclaredFields().getOnly();
        assertMalformed(fieldDescription.getType());
    }

    @Test
    public void testFieldArraySignatureOfExcessiveDepth() throws Exception {
        FieldDescription fieldDescription = ofField(arraySignature(EXCESSIVE_ARRAY_DEPTH)).getDeclaredFields().getOnly();
        assertMalformed(fieldDescription.getType());
    }

    @Test(expected = GenericSignatureFormatError.class)
    public void testFieldParameterizedSignatureAtMaximumDepth() throws Exception {
        FieldDescription fieldDescription = ofField(parameterizedSignature(TypePool.Default.SIGNATURE_DEPTH)).getDeclaredFields().getOnly();
        fieldDescription.getType().getSort();
    }

    @Test
    public void testFieldParameterizedSignatureAboveMaximumDepth() throws Exception {
        FieldDescription fieldDescription = ofField(parameterizedSignature(TypePool.Default.SIGNATURE_DEPTH + 1)).getDeclaredFields().getOnly();
        assertMalformed(fieldDescription.getType());
    }

    @Test
    public void testFieldParameterizedSignatureOfExcessiveDepth() throws Exception {
        FieldDescription fieldDescription = ofField(excessiveParameterizedSignature()).getDeclaredFields().getOnly();
        assertMalformed(fieldDescription.getType());
    }

    @Test
    public void testMethodSignatureOfExcessiveDepth() throws Exception {
        MethodDescription methodDescription = ofMethod(excessiveParameterizedSignature()).getDeclaredMethods().filter(named(FOO)).getOnly();
        assertMalformed(methodDescription.getReturnType());
    }

    @Test
    public void testTypeSignatureOfExcessiveDepth() throws Exception {
        TypeDescription typeDescription = ofType(excessiveParameterizedSignature());
        assertThat(typeDescription.getInterfaces().getOnly().asErasure(), is(TypeDescription.ForLoadedType.of(List.class)));
    }
}
