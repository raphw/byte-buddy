package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.inline.MethodRebaseResolver;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.StreamDrainer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeWriterEngineForRedefinitionTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    private static final int TYPE_MODIFIER = 42, CLASS_VERSION = 60;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription instrumentedType, superType;

    @Mock
    private ClassFileVersion classFileVersion;

    @Mock
    private ClassVisitorWrapper classVisitorWrapper;

    @Mock
    private TypeAttributeAppender typeAttributeAppender;

    @Mock
    private TypeWriter.FieldPool fieldPool;

    @Mock
    private TypeWriter.FieldPool.Entry firstFieldEntry, secondFieldEntry;

    @Mock
    private TypeWriter.MethodPool methodPool;

    @Mock
    private TypeWriter.MethodPool.Entry firstMethodEntry, secondMethodEntry, fooEntry, barEntry, constructorEntry;

    @Mock
    private MethodDescription firstMethod, secondMethod, barResolutionMethod;

    @Mock
    private FieldDescription firstField, secondField;

    @Mock
    private Instrumentation.Context.ExtractableView instrumentationContext;

    @Mock
    private ClassVisitor classVisitor;

    @Mock
    private MethodVisitor fooMethodVisitor, barMethodVisitor, quxMethodVisitor;

    @Mock
    private ClassFileLocator classFileLocator;

    @Mock
    private MethodRebaseResolver methodRebaseResolver, otherMethodRebaseResolver;

    @Mock
    private MethodAttributeAppender fooAttributeAppender, barAttributeAppender;

    @Mock
    private ByteCodeAppender fooByteCodeAppender, barByteCodeAppender;

    @Mock
    private MethodRebaseResolver.Resolution barResolution;

    @Mock
    private ClassFileLocator.Resolution resolution;

    private List<MethodDescription> invokableMethods;

    private TypeDescription targetType;

    @Before
    public void setUp() throws Exception {
        invokableMethods = Arrays.asList(firstMethod, secondMethod,
                new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod(FOO)),
                new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod(BAR)));
        targetType = new TypeDescription.ForLoadedType(Foo.class);
        FieldList declaredFields = new FieldList.Explicit(Arrays.asList(firstField, secondField));
        when(firstMethod.getUniqueSignature()).thenReturn(FOO);
        when(firstMethod.getUniqueSignature()).thenReturn(BAR);
        when(instrumentedType.getSupertype()).thenReturn(superType);
        when(superType.getInternalName()).thenReturn(BAR);
        when(instrumentedType.getDeclaredFields()).thenReturn(declaredFields);
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Empty());
        when(classFileVersion.getVersionNumber()).thenReturn(Opcodes.V1_6);
        when(classVisitorWrapper.wrap(any(ClassVisitor.class))).thenReturn(classVisitor);
        when(fieldPool.target(firstField)).thenReturn(firstFieldEntry);
        when(fieldPool.target(secondField)).thenReturn(secondFieldEntry);
        when(methodPool.target(firstMethod)).thenReturn(firstMethodEntry);
        when(methodPool.target(secondMethod)).thenReturn(secondMethodEntry);
        when(methodPool.target(new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod(FOO)))).thenReturn(fooEntry);
        when(methodPool.target(new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod(BAR)))).thenReturn(barEntry);
        when(methodPool.target(new MethodDescription.ForLoadedConstructor(Foo.class.getDeclaredConstructor()))).thenReturn(constructorEntry);
        when(instrumentedType.getInternalName()).thenReturn(FOO);
        when(instrumentedType.getGenericSignature()).thenReturn(QUX);
        when(instrumentedType.getActualModifiers(anyBoolean())).thenReturn(TYPE_MODIFIER);
        TypeList interfaceTypes = mock(TypeList.class);
        when(interfaceTypes.toInternalNames()).thenReturn(new String[]{BAZ});
        when(instrumentedType.getInterfaces()).thenReturn(interfaceTypes);
        when(classFileVersion.getVersionNumber()).thenReturn(CLASS_VERSION);
        when(classVisitor.visitMethod(any(int.class), eq(FOO), any(String.class), any(String.class), any(String[].class)))
                .thenReturn(fooMethodVisitor);
        when(classVisitor.visitMethod(any(int.class), eq(BAR), any(String.class), any(String.class), any(String[].class)))
                .thenReturn(barMethodVisitor);
        when(classVisitor.visitMethod(any(int.class), eq(QUX), any(String.class), any(String.class), any(String[].class)))
                .thenReturn(quxMethodVisitor);
        when(fooEntry.isDefineMethod()).thenReturn(true);
        when(fooEntry.getAttributeAppender()).thenReturn(fooAttributeAppender);
        when(fooEntry.getByteCodeAppender()).thenReturn(fooByteCodeAppender);
        when(fooByteCodeAppender.appendsCode()).thenReturn(true);
        when(barEntry.isDefineMethod()).thenReturn(true);
        when(barEntry.getAttributeAppender()).thenReturn(barAttributeAppender);
        when(barEntry.getByteCodeAppender()).thenReturn(barByteCodeAppender);
        when(barByteCodeAppender.appendsCode()).thenReturn(true);
        when(fooByteCodeAppender.apply(any(MethodVisitor.class), any(Instrumentation.Context.class), any(MethodDescription.class)))
                .thenReturn(new ByteCodeAppender.Size(0, 0));
        when(barByteCodeAppender.apply(any(MethodVisitor.class), any(Instrumentation.Context.class), any(MethodDescription.class)))
                .thenReturn(new ByteCodeAppender.Size(0, 0));
        when(methodRebaseResolver.resolve(new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod(BAR))))
                .thenReturn(barResolution);
        when(barResolution.getResolvedMethod()).thenReturn(barResolutionMethod);
        when(barResolutionMethod.getModifiers()).thenReturn(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC);
        when(barResolutionMethod.getInternalName()).thenReturn(BAR + FOO);
        when(barResolutionMethod.getDescriptor()).thenReturn(BAR + QUX);
        when(barResolutionMethod.getGenericSignature()).thenReturn(QUX + FOO);
        TypeList barExceptionTypes = mock(TypeList.class);
        when(barExceptionTypes.toInternalNames()).thenReturn(new String[]{BAZ + QUX});
        when(barResolutionMethod.getExceptionTypes()).thenReturn(barExceptionTypes);
        when(classFileVersion.compareTo(any(ClassFileVersion.class))).thenReturn(1);
        when(classFileLocator.locate(Foo.class.getName())).thenReturn(resolution);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotLocatedClassFileThrowsException() throws Exception {
        new TypeWriter.Engine.ForRedefinition(instrumentedType,
                targetType,
                classFileVersion,
                invokableMethods,
                classVisitorWrapper,
                typeAttributeAppender,
                fieldPool,
                methodPool,
                classFileLocator,
                methodRebaseResolver).create(instrumentationContext);
    }

    @Test
    public void testTypeCreationWithRebase() throws Exception {
        when(resolution.isResolved()).thenReturn(true);
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(Foo.class.getName().replace('.', '/') + ".class");
        try {
            when(resolution.resolve()).thenReturn(new StreamDrainer().drain(inputStream));
        } finally {
            inputStream.close();
        }
        when(barResolution.isRebased()).thenReturn(true);
        assertThat(new TypeWriter.Engine.ForRedefinition(instrumentedType,
                targetType,
                classFileVersion,
                invokableMethods,
                classVisitorWrapper,
                typeAttributeAppender,
                fieldPool,
                methodPool,
                classFileLocator,
                methodRebaseResolver).create(instrumentationContext), notNullValue());
        verify(classFileVersion).compareTo(any(ClassFileVersion.class));
        verify(classVisitor).visit(CLASS_VERSION, TYPE_MODIFIER, FOO, QUX, BAR, new String[]{BAZ});
        verify(classVisitor, atLeast(0)).visitSource(any(String.class), any(String.class));
        verify(classVisitor, atLeast(0)).visitInnerClass(any(String.class), any(String.class), any(String.class), any(int.class));
        verify(classVisitor).visitAnnotation(Type.getDescriptor(Bar.class), true);
        verify(classVisitor).visitField(Opcodes.ACC_PRIVATE, FOO, Type.getDescriptor(Void.class), null, null);
        verify(classVisitor).visitMethod(Opcodes.ACC_PUBLIC, MethodDescription.CONSTRUCTOR_INTERNAL_NAME, "()V", null, null);
        verify(classVisitor).visitMethod(Opcodes.ACC_PUBLIC, FOO, "()V", null, null);
        verify(classVisitor).visitMethod(Opcodes.ACC_PUBLIC, BAR, "()V", null, null);
        verify(classVisitor).visitMethod(Opcodes.ACC_PUBLIC, QUX, "()V", null, null);
        verify(classVisitor).visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, BAR + FOO, BAR + QUX, QUX + FOO, new String[]{BAZ + QUX});
        verify(methodPool).target(new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod(FOO)));
        verify(fooEntry).isDefineMethod();
        verify(fooAttributeAppender).apply(any(MethodVisitor.class),
                eq(new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod(FOO))));
        verify(fooByteCodeAppender, atLeast(1)).appendsCode();
        verify(fooByteCodeAppender).apply(any(MethodVisitor.class),
                eq(instrumentationContext),
                eq(new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod(FOO))));
        verify(methodPool).target(new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod(BAR)));
        verify(barEntry).isDefineMethod();
        verify(methodPool).target(firstMethod);
        verify(firstMethodEntry).apply(classVisitor, instrumentationContext, firstMethod);
        verify(methodPool).target(secondMethod);
        verify(secondMethodEntry).apply(classVisitor, instrumentationContext, secondMethod);
        verify(quxMethodVisitor).visitCode();
        verify(quxMethodVisitor).visitEnd();
        verify(classVisitor).visitEnd();
        verifyNoMoreInteractions(classVisitor);
        verify(typeAttributeAppender).apply(any(ClassVisitor.class), eq(instrumentedType));
        verifyNoMoreInteractions(typeAttributeAppender);
        verify(fieldPool).target(secondField);
        verify(secondFieldEntry).apply(classVisitor, secondField);
        verifyNoMoreInteractions(fieldPool);
        verifyZeroInteractions(firstFieldEntry);
        verifyNoMoreInteractions(secondFieldEntry);
    }

    @Test
    public void testTypeCreationWithoutRebase() throws Exception {
        when(resolution.isResolved()).thenReturn(true);
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(Foo.class.getName().replace('.', '/') + ".class");
        try {
            when(resolution.resolve()).thenReturn(new StreamDrainer().drain(inputStream));
        } finally {
            inputStream.close();
        }
        when(barResolution.isRebased()).thenReturn(false);
        assertThat(new TypeWriter.Engine.ForRedefinition(instrumentedType,
                targetType,
                classFileVersion,
                invokableMethods,
                classVisitorWrapper,
                typeAttributeAppender,
                fieldPool,
                methodPool,
                classFileLocator,
                methodRebaseResolver).create(instrumentationContext), notNullValue());
        verify(classFileVersion).compareTo(any(ClassFileVersion.class));
        verify(classVisitor).visit(CLASS_VERSION, TYPE_MODIFIER, FOO, QUX, BAR, new String[]{BAZ});
        verify(classVisitor, atLeast(0)).visitSource(any(String.class), any(String.class));
        verify(classVisitor, atLeast(0)).visitInnerClass(any(String.class), any(String.class), any(String.class), any(int.class));
        verify(classVisitor).visitAnnotation(Type.getDescriptor(Bar.class), true);
        verify(classVisitor).visitField(Opcodes.ACC_PRIVATE, FOO, Type.getDescriptor(Void.class), null, null);
        verify(classVisitor).visitMethod(Opcodes.ACC_PUBLIC, MethodDescription.CONSTRUCTOR_INTERNAL_NAME, "()V", null, null);
        verify(classVisitor).visitMethod(Opcodes.ACC_PUBLIC, FOO, "()V", null, null);
        verify(classVisitor).visitMethod(Opcodes.ACC_PUBLIC, BAR, "()V", null, null);
        verify(classVisitor).visitMethod(Opcodes.ACC_PUBLIC, QUX, "()V", null, null);
        verify(classVisitor, never()).visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, BAR + FOO, BAR + QUX, QUX + FOO, new String[]{BAZ + QUX});
        verify(methodPool).target(new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod(FOO)));
        verify(fooEntry).isDefineMethod();
        verify(fooAttributeAppender).apply(any(MethodVisitor.class),
                eq(new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod(FOO))));
        verify(fooByteCodeAppender, atLeast(1)).appendsCode();
        verify(fooByteCodeAppender).apply(any(MethodVisitor.class),
                eq(instrumentationContext),
                eq(new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod(FOO))));
        verify(methodPool).target(new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod(BAR)));
        verify(barEntry).isDefineMethod();
        verify(methodPool).target(firstMethod);
        verify(firstMethodEntry).apply(classVisitor, instrumentationContext, firstMethod);
        verify(methodPool).target(secondMethod);
        verify(secondMethodEntry).apply(classVisitor, instrumentationContext, secondMethod);
        verify(quxMethodVisitor).visitCode();
        verify(quxMethodVisitor).visitEnd();
        verify(classVisitor).visitEnd();
        verifyNoMoreInteractions(classVisitor);
        verify(typeAttributeAppender).apply(any(ClassVisitor.class), eq(instrumentedType));
        verifyNoMoreInteractions(typeAttributeAppender);
        verify(fieldPool).target(secondField);
        verify(secondFieldEntry).apply(classVisitor, secondField);
        verifyNoMoreInteractions(fieldPool);
        verifyZeroInteractions(firstFieldEntry);
        verifyNoMoreInteractions(secondFieldEntry);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeWriter.Engine.ForRedefinition.class).apply();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Bar {
        /* empty */
    }

    @Bar
    @SuppressWarnings("unused")
    public static abstract class Foo {

        private Void foo;

        public abstract void foo();

        public void bar() {
            /* empty */
        }

        public void qux() {
            /* empty */
        }
    }
}
