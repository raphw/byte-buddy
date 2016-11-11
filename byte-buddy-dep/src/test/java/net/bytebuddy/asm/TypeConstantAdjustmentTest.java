package net.bytebuddy.asm;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

public class TypeConstantAdjustmentTest {

    private static final int FOOBAR = 42, IGNORED = -1;

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassVisitor classVisitor;

    @Mock
    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        when(classVisitor.visitMethod(anyInt(), any(String.class), any(String.class), any(String.class), any(String[].class)))
                .thenReturn(methodVisitor);
    }

    @Test
    public void testWriterFlags() throws Exception {
        assertThat(TypeConstantAdjustment.INSTANCE.mergeWriter(FOOBAR), is(FOOBAR));
    }

    @Test
    public void testReaderFlags() throws Exception {
        assertThat(TypeConstantAdjustment.INSTANCE.mergeReader(FOOBAR), is(FOOBAR));
    }

    @Test
    public void testInstrumentationModernClassFile() throws Exception {
        ClassVisitor classVisitor = TypeConstantAdjustment.INSTANCE.wrap(mock(TypeDescription.class),
                this.classVisitor,
                mock(Implementation.Context.class),
                mock(TypePool.class),
                IGNORED,
                IGNORED);
        classVisitor.visit(ClassFileVersion.JAVA_V5.getMinorMajorVersion(), FOOBAR, FOO, BAR, QUX, new String[]{BAZ});
        assertThat(classVisitor.visitMethod(FOOBAR, FOO, BAR, QUX, new String[]{BAZ}), is(methodVisitor));
        verify(this.classVisitor).visit(ClassFileVersion.JAVA_V5.getMinorMajorVersion(), FOOBAR, FOO, BAR, QUX, new String[]{BAZ});
        verify(this.classVisitor).visitMethod(FOOBAR, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(this.classVisitor);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testInstrumentationLegacyClassFileObjectType() throws Exception {
        ClassVisitor classVisitor = TypeConstantAdjustment.INSTANCE.wrap(mock(TypeDescription.class),
                this.classVisitor,
                mock(Implementation.Context.class),
                mock(TypePool.class),
                IGNORED,
                IGNORED);
        classVisitor.visit(ClassFileVersion.JAVA_V4.getMinorMajorVersion(), FOOBAR, FOO, BAR, QUX, new String[]{BAZ});
        MethodVisitor methodVisitor = classVisitor.visitMethod(FOOBAR, FOO, BAR, QUX, new String[]{BAZ});
        assertThat(methodVisitor, not(this.methodVisitor));
        methodVisitor.visitLdcInsn(Type.getType(Object.class));
        verify(this.classVisitor).visit(ClassFileVersion.JAVA_V4.getMinorMajorVersion(), FOOBAR, FOO, BAR, QUX, new String[]{BAZ});
        verify(this.classVisitor).visitMethod(FOOBAR, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(this.classVisitor);
        verify(this.methodVisitor).visitLdcInsn(Type.getType(Object.class).getClassName());
        verify(this.methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC,
                Type.getType(Class.class).getInternalName(),
                "forName",
                Type.getType(Class.class.getDeclaredMethod("forName", String.class)).getDescriptor(),
                false);
        verifyNoMoreInteractions(this.methodVisitor);
    }

    @Test
    public void testInstrumentationLegacyClassFileArrayType() throws Exception {
        ClassVisitor classVisitor = TypeConstantAdjustment.INSTANCE.wrap(mock(TypeDescription.class),
                this.classVisitor,
                mock(Implementation.Context.class),
                mock(TypePool.class),
                IGNORED,
                IGNORED);
        classVisitor.visit(ClassFileVersion.JAVA_V4.getMinorMajorVersion(), FOOBAR, FOO, BAR, QUX, new String[]{BAZ});
        MethodVisitor methodVisitor = classVisitor.visitMethod(FOOBAR, FOO, BAR, QUX, new String[]{BAZ});
        assertThat(methodVisitor, not(this.methodVisitor));
        methodVisitor.visitLdcInsn(Type.getType(Object[].class));
        verify(this.classVisitor).visit(ClassFileVersion.JAVA_V4.getMinorMajorVersion(), FOOBAR, FOO, BAR, QUX, new String[]{BAZ});
        verify(this.classVisitor).visitMethod(FOOBAR, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(this.classVisitor);
        verify(this.methodVisitor).visitLdcInsn(Type.getType(Object[].class).getInternalName().replace('/', '.'));
        verify(this.methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC,
                Type.getType(Class.class).getInternalName(),
                "forName",
                Type.getType(Class.class.getDeclaredMethod("forName", String.class)).getDescriptor(),
                false);
        verifyNoMoreInteractions(this.methodVisitor);
    }

    @Test
    public void testInstrumentationLegacyClassOtherType() throws Exception {
        ClassVisitor classVisitor = TypeConstantAdjustment.INSTANCE.wrap(mock(TypeDescription.class),
                this.classVisitor,
                mock(Implementation.Context.class),
                mock(TypePool.class),
                IGNORED,
                IGNORED);
        classVisitor.visit(ClassFileVersion.JAVA_V4.getMinorMajorVersion(), FOOBAR, FOO, BAR, QUX, new String[]{BAZ});
        MethodVisitor methodVisitor = classVisitor.visitMethod(FOOBAR, FOO, BAR, QUX, new String[]{BAZ});
        assertThat(methodVisitor, not(this.methodVisitor));
        methodVisitor.visitLdcInsn(FOO);
        verify(this.classVisitor).visit(ClassFileVersion.JAVA_V4.getMinorMajorVersion(), FOOBAR, FOO, BAR, QUX, new String[]{BAZ});
        verify(this.classVisitor).visitMethod(FOOBAR, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(this.classVisitor);
        verify(this.methodVisitor).visitLdcInsn(FOO);
        verifyNoMoreInteractions(this.methodVisitor);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeConstantAdjustment.class).apply();
        ObjectPropertyAssertion.of(TypeConstantAdjustment.TypeConstantDissolvingClassVisitor.class).applyBasic();
        ObjectPropertyAssertion.of(TypeConstantAdjustment.TypeConstantDissolvingClassVisitor.TypeConstantDissolvingMethodVisitor.class).applyBasic();
    }
}
