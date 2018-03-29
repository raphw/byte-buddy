package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class FieldConstantTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private FieldDescription.InDefinedShape fieldDescription, cacheField;

    @Mock
    private TypeDescription declaringType, cacheDeclaringType, cacheFieldType, instrumentedType;

    @Mock
    private TypeDescription.Generic genericCacheFieldType;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private ClassFileVersion classFileVersion;

    @Mock
    private Implementation.Context implementationContext;

    @Before
    public void setUp() throws Exception {
        when(declaringType.getInternalName()).thenReturn(FOO);
        when(fieldDescription.getInternalName()).thenReturn(BAR);
        when(fieldDescription.getDeclaringType()).thenReturn(declaringType);
        when(declaringType.getDescriptor()).thenReturn("L" + QUX + ";");
        when(implementationContext.cache(new FieldConstant(fieldDescription), new TypeDescription.ForLoadedType(Field.class)))
                .thenReturn(cacheField);
        when(cacheField.getDeclaringType()).thenReturn(cacheDeclaringType);
        when(cacheField.isStatic()).thenReturn(true);
        when(declaringType.getName()).thenReturn(BAZ);
        when(cacheDeclaringType.getInternalName()).thenReturn(BAZ);
        when(cacheField.getName()).thenReturn(FOO + BAR);
        when(cacheField.getType()).thenReturn(genericCacheFieldType);
        when(genericCacheFieldType.asErasure()).thenReturn(cacheFieldType);
        when(genericCacheFieldType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(cacheField.getInternalName()).thenReturn(FOO + BAR);
        when(cacheField.getDescriptor()).thenReturn(QUX + BAZ);
        when(implementationContext.getClassFileVersion()).thenReturn(classFileVersion);
        when(implementationContext.getInstrumentedType()).thenReturn(instrumentedType);
    }

    @Test
    public void testConstantCreationModernVisible() throws Exception {
        when(classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5)).thenReturn(true);
        when(declaringType.isVisibleTo(instrumentedType)).thenReturn(true);
        StackManipulation stackManipulation = new FieldConstant(fieldDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(2));
        verify(methodVisitor).visitLdcInsn(Type.getObjectType(QUX));
        verify(methodVisitor).visitLdcInsn(BAR);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "java/lang/Class",
                "getDeclaredField",
                "(Ljava/lang/String;)Ljava/lang/reflect/Field;",
                false);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testConstantCreationModernInvisible() throws Exception {
        when(classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5)).thenReturn(true);
        when(declaringType.isVisibleTo(instrumentedType)).thenReturn(false);
        StackManipulation stackManipulation = new FieldConstant(fieldDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(2));
        verify(methodVisitor).visitLdcInsn(BAZ);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC,
                Type.getInternalName(Class.class),
                "forName",
                Type.getMethodDescriptor(Type.getType(Class.class), Type.getType(String.class)),
                false);
        verify(methodVisitor).visitLdcInsn(BAR);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "java/lang/Class",
                "getDeclaredField",
                "(Ljava/lang/String;)Ljava/lang/reflect/Field;",
                false);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testConstantCreationLegacy() throws Exception {
        when(classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5)).thenReturn(false);
        when(declaringType.isVisibleTo(instrumentedType)).thenReturn(true);
        StackManipulation stackManipulation = new FieldConstant(fieldDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(2));
        verify(methodVisitor).visitLdcInsn(BAZ);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC,
                Type.getInternalName(Class.class),
                "forName",
                Type.getMethodDescriptor(Type.getType(Class.class), Type.getType(String.class)),
                false);
        verify(methodVisitor).visitLdcInsn(BAR);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "java/lang/Class",
                "getDeclaredField",
                "(Ljava/lang/String;)Ljava/lang/reflect/Field;",
                false);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testCached() throws Exception {
        StackManipulation stackManipulation = new FieldConstant(fieldDescription).cached();
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(implementationContext).cache(new FieldConstant(fieldDescription), new TypeDescription.ForLoadedType(Field.class));
        verifyNoMoreInteractions(implementationContext);
        verify(methodVisitor).visitFieldInsn(Opcodes.GETSTATIC, BAZ, FOO + BAR, QUX + BAZ);
        verifyNoMoreInteractions(methodVisitor);
    }
}
