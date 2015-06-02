package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.*;

public abstract class AbstractInstrumentedTypeTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    protected abstract InstrumentedType makePlainInstrumentedType();

    @Test
    public void testWithField() throws Exception {
        TypeDescription fieldType = mock(TypeDescription.class);
        when(fieldType.asRawType()).thenReturn(fieldType); // REFACTOR
        when(fieldType.getName()).thenReturn(FOO);
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        instrumentedType = instrumentedType.withField(BAR, fieldType, Opcodes.ACC_PUBLIC);
        assertThat(instrumentedType.getDeclaredFields().size(), is(1));
        FieldDescription fieldDescription = instrumentedType.getDeclaredFields().get(0);
        assertThat(fieldDescription.getFieldType(), is(fieldType));
        assertThat(fieldDescription.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(fieldDescription.getName(), is(BAR));
        assertThat(fieldDescription.getDeclaringType(), sameInstance((TypeDescription) instrumentedType));
    }

    @Test
    public void testWithFieldOfInstrumentedType() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        instrumentedType = instrumentedType.withField(BAR, instrumentedType, Opcodes.ACC_PUBLIC);
        assertThat(instrumentedType.getDeclaredFields().size(), is(1));
        FieldDescription fieldDescription = instrumentedType.getDeclaredFields().get(0);
        assertThat(fieldDescription.getFieldType(), sameInstance((TypeDescription) instrumentedType));
        assertThat(fieldDescription.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(fieldDescription.getName(), is(BAR));
        assertThat(fieldDescription.getDeclaringType(), sameInstance((TypeDescription) instrumentedType));
    }

    @Test
    public void testWithFieldOfInstrumentedTypeAsArray() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        instrumentedType = instrumentedType.withField(BAR, TypeDescription.ArrayProjection.of(instrumentedType, 1), Opcodes.ACC_PUBLIC);
        assertThat(instrumentedType.getDeclaredFields().size(), is(1));
        FieldDescription fieldDescription = instrumentedType.getDeclaredFields().get(0);
        assertThat(fieldDescription.getFieldType().isArray(), is(true));
        assertThat(fieldDescription.getFieldType().getComponentType(), sameInstance((TypeDescription) instrumentedType));
        assertThat(fieldDescription.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(fieldDescription.getName(), is(BAR));
        assertThat(fieldDescription.getDeclaringType(), sameInstance((TypeDescription) instrumentedType));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithFieldDouble() throws Exception {
        TypeDescription fieldType = mock(TypeDescription.class);
        when(fieldType.asRawType()).thenReturn(fieldType); // REFACTOR
        when(fieldType.getName()).thenReturn(FOO);
        makePlainInstrumentedType()
                .withField(BAR, fieldType, Opcodes.ACC_PUBLIC)
                .withField(BAR, fieldType, Opcodes.ACC_PUBLIC);
    }

    @Test
    public void testWithMethod() throws Exception {
        TypeDescription returnType = mock(TypeDescription.class);
        when(returnType.asRawType()).thenReturn(returnType); // REFACTOR
        TypeDescription parameterType = mock(TypeDescription.class);
        when(parameterType.asRawType()).thenReturn(parameterType); // REFACTOR
        TypeDescription exceptionType = mock(TypeDescription.class);
        when(exceptionType.asRawType()).thenReturn(exceptionType); // REFACTOR
        when(returnType.getName()).thenReturn(FOO);
        when(parameterType.getName()).thenReturn(QUX);
        when(parameterType.getStackSize()).thenReturn(StackSize.ZERO);
        when(exceptionType.getName()).thenReturn(BAZ);
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        instrumentedType = instrumentedType.withMethod(BAR,
                returnType,
                Collections.singletonList(parameterType),
                Collections.singletonList(exceptionType),
                Opcodes.ACC_PUBLIC);
        assertThat(instrumentedType.getDeclaredMethods().size(), is(1));
        MethodDescription methodDescription = instrumentedType.getDeclaredMethods().get(0);
        assertThat(methodDescription.getReturnType(), is(returnType));
        assertThat(methodDescription.getParameters().size(), is(1));
        assertThat(methodDescription.getParameters().asTypeList(), is(Collections.singletonList(parameterType)));
        assertThat(methodDescription.getExceptionTypes().size(), is(1));
        assertThat(methodDescription.getExceptionTypes(), is(Collections.singletonList(exceptionType)));
        assertThat(methodDescription.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(methodDescription.getName(), is(BAR));
        assertThat(methodDescription.getDeclaringType(), sameInstance((TypeDescription) instrumentedType));
    }

    @Test
    public void testWithMethodOfInstrumentedType() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        instrumentedType = instrumentedType.withMethod(BAR,
                instrumentedType,
                Collections.singletonList(instrumentedType),
                Collections.<TypeDescription>emptyList(),
                Opcodes.ACC_PUBLIC);
        assertThat(instrumentedType.getDeclaredMethods().size(), is(1));
        MethodDescription methodDescription = instrumentedType.getDeclaredMethods().get(0);
        assertThat(methodDescription.getReturnType(), sameInstance((TypeDescription) instrumentedType));
        assertThat(methodDescription.getParameters().size(), is(1));
        assertThat(methodDescription.getParameters().asTypeList().get(0), sameInstance((TypeDescription) instrumentedType));
        assertThat(methodDescription.getExceptionTypes().size(), is(0));
        assertThat(methodDescription.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(methodDescription.getName(), is(BAR));
        assertThat(methodDescription.getDeclaringType(), sameInstance((TypeDescription) instrumentedType));
    }

    @Test
    public void testWithMethodOfInstrumentedTypeAsArray() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        instrumentedType = instrumentedType.withMethod(BAR,
                TypeDescription.ArrayProjection.of(instrumentedType, 1),
                Collections.singletonList(TypeDescription.ArrayProjection.of(instrumentedType, 1)),
                Collections.<TypeDescription>emptyList(),
                Opcodes.ACC_PUBLIC);
        assertThat(instrumentedType.getDeclaredMethods().size(), is(1));
        MethodDescription methodDescription = instrumentedType.getDeclaredMethods().get(0);
        assertThat(methodDescription.getReturnType().isArray(), is(true));
        assertThat(methodDescription.getReturnType().getComponentType(), sameInstance((TypeDescription) instrumentedType));
        assertThat(methodDescription.getParameters().size(), is(1));
        assertThat(methodDescription.getParameters().asTypeList().get(0).isArray(), is(true));
        assertThat(methodDescription.getParameters().asTypeList().get(0).getComponentType(), sameInstance((TypeDescription) instrumentedType));
        assertThat(methodDescription.getExceptionTypes().size(), is(0));
        assertThat(methodDescription.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(methodDescription.getName(), is(BAR));
        assertThat(methodDescription.getDeclaringType(), sameInstance((TypeDescription) instrumentedType));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithMethodDouble() throws Exception {
        TypeDescription returnType = mock(TypeDescription.class);
        when(returnType.asRawType()).thenReturn(returnType); // REFACTOR
        when(returnType.getName()).thenReturn(FOO);
        makePlainInstrumentedType()
                .withMethod(BAR, returnType, Collections.<TypeDescription>emptyList(), Collections.<TypeDescription>emptyList(), Opcodes.ACC_PUBLIC)
                .withMethod(BAR, returnType, Collections.<TypeDescription>emptyList(), Collections.<TypeDescription>emptyList(), Opcodes.ACC_PUBLIC);
    }

    @Test
    public void testWithLoadedTypeInitializerInitial() throws Exception {
        LoadedTypeInitializer loadedTypeInitializer = makePlainInstrumentedType().getLoadedTypeInitializer();
        assertThat(loadedTypeInitializer.isAlive(), is(false));
    }

    @Test
    public void testWithLoadedTypeInitializerSingle() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        LoadedTypeInitializer loadedTypeInitializer = mock(LoadedTypeInitializer.class);
        instrumentedType = instrumentedType.withInitializer(loadedTypeInitializer);
        assertThat(instrumentedType.getLoadedTypeInitializer(),
                is((LoadedTypeInitializer) new LoadedTypeInitializer.Compound(LoadedTypeInitializer.NoOp.INSTANCE, loadedTypeInitializer)));
    }

    @Test
    public void testWithLoadedTypeInitializerDouble() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        LoadedTypeInitializer first = mock(LoadedTypeInitializer.class), second = mock(LoadedTypeInitializer.class);
        instrumentedType = instrumentedType.withInitializer(first).withInitializer(second);
        assertThat(instrumentedType.getLoadedTypeInitializer(),
                is((LoadedTypeInitializer) new LoadedTypeInitializer.Compound(new LoadedTypeInitializer
                        .Compound(LoadedTypeInitializer.NoOp.INSTANCE, first), second)));
    }

    @Test
    public void testWithTypeInitializerInitial() throws Exception {
        InstrumentedType.TypeInitializer typeInitializer = makePlainInstrumentedType().getTypeInitializer();
        assertThat(typeInitializer.isDefined(), is(false));
    }

    @Test
    public void testWithTypeInitializerSingle() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        ByteCodeAppender byteCodeAppender = mock(ByteCodeAppender.class);
        instrumentedType = instrumentedType.withInitializer(byteCodeAppender);
        InstrumentedType.TypeInitializer typeInitializer = instrumentedType.getTypeInitializer();
        assertThat(typeInitializer.isDefined(), is(true));
        MethodDescription methodDescription = mock(MethodDescription.class);
        typeInitializer.apply(methodVisitor, implementationContext, methodDescription);
        verify(byteCodeAppender).apply(methodVisitor, implementationContext, methodDescription);
    }

    @Test
    public void testWithTypeInitializerDouble() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        ByteCodeAppender first = mock(ByteCodeAppender.class), second = mock(ByteCodeAppender.class);
        MethodDescription methodDescription = mock(MethodDescription.class);
        when(first.apply(methodVisitor, implementationContext, methodDescription)).thenReturn(new ByteCodeAppender.Size(0, 0));
        when(second.apply(methodVisitor, implementationContext, methodDescription)).thenReturn(new ByteCodeAppender.Size(0, 0));
        instrumentedType = instrumentedType.withInitializer(first).withInitializer(second);
        InstrumentedType.TypeInitializer typeInitializer = instrumentedType.getTypeInitializer();
        assertThat(typeInitializer.isDefined(), is(true));
        typeInitializer.apply(methodVisitor, implementationContext, methodDescription);
        verify(first).apply(methodVisitor, implementationContext, methodDescription);
        verify(second).apply(methodVisitor, implementationContext, methodDescription);
    }

    @Test
    public void testGetStackSize() throws Exception {
        assertThat(makePlainInstrumentedType().getStackSize(), is(StackSize.SINGLE));
    }

    @Test
    public void testHashCode() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.hashCode(), is(instrumentedType.getInternalName().hashCode()));
    }

    @Test
    public void testEquals() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        TypeDescription other = mock(TypeDescription.class);
        when(other.getInternalName()).thenReturn(instrumentedType.getInternalName());
        when(other.getSort()).thenReturn(GenericTypeDescription.Sort.RAW);
        when(other.asRawType()).thenReturn(other);
        assertThat(instrumentedType, equalTo(other));
        verify(other, atLeast(1)).getInternalName();
    }

    @Test
    public void testIsAssignableFrom() {
        assertThat(makePlainInstrumentedType().isAssignableFrom(Object.class), is(false));
        assertThat(makePlainInstrumentedType().isAssignableFrom(Serializable.class), is(false));
        assertThat(makePlainInstrumentedType().isAssignableFrom(Integer.class), is(false));
        TypeDescription objectTypeDescription = new TypeDescription.ForLoadedType(Object.class);
        assertThat(makePlainInstrumentedType().isAssignableFrom(objectTypeDescription), is(false));
        TypeDescription serializableTypeDescription = new TypeDescription.ForLoadedType(Serializable.class);
        assertThat(makePlainInstrumentedType().isAssignableFrom(serializableTypeDescription), is(false));
        TypeDescription integerTypeDescription = new TypeDescription.ForLoadedType(Integer.class);
        assertThat(makePlainInstrumentedType().isAssignableFrom(integerTypeDescription), is(false));
    }

    @Test
    public void testIsAssignableTo() {
        assertThat(makePlainInstrumentedType().isAssignableTo(Object.class), is(true));
        assertThat(makePlainInstrumentedType().isAssignableTo(Serializable.class), is(true));
        assertThat(makePlainInstrumentedType().isAssignableTo(Integer.class), is(false));
        TypeDescription objectTypeDescription = new TypeDescription.ForLoadedType(Object.class);
        assertThat(makePlainInstrumentedType().isAssignableTo(objectTypeDescription), is(true));
        TypeDescription serializableTypeDescription = new TypeDescription.ForLoadedType(Serializable.class);
        assertThat(makePlainInstrumentedType().isAssignableTo(serializableTypeDescription), is(true));
        TypeDescription integerTypeDescription = new TypeDescription.ForLoadedType(Integer.class);
        assertThat(makePlainInstrumentedType().isAssignableTo(integerTypeDescription), is(false));
    }

    @Test
    public void testRepresents() {
        assertThat(makePlainInstrumentedType().represents(Object.class), is(false));
        assertThat(makePlainInstrumentedType().represents(Serializable.class), is(false));
        assertThat(makePlainInstrumentedType().represents(Integer.class), is(false));
    }

    @Test
    public void testSupertype() {
        assertThat(makePlainInstrumentedType().getSuperType(), is((TypeDescription) new TypeDescription.ForLoadedType(Object.class)));
        assertThat(makePlainInstrumentedType().getSuperType(), not(is((TypeDescription) new TypeDescription.ForLoadedType(Integer.class))));
        assertThat(makePlainInstrumentedType().getSuperType(), not(is((TypeDescription) new TypeDescription.ForLoadedType(Serializable.class))));
    }

    @Test
    public void testInterfaces() {
        TypeList interfaces = makePlainInstrumentedType().getInterfaces();
        assertThat(interfaces.size(), is(1));
        assertThat(interfaces.get(0), is(is((TypeDescription) new TypeDescription.ForLoadedType(Serializable.class))));
    }

    @Test
    public void testPackage() {
        assertThat(makePlainInstrumentedType().getPackage().getName(), is(FOO));
    }

    @Test
    public void testSimpleName() {
        assertThat(makePlainInstrumentedType().getSimpleName(), is(BAR));
    }

    @Test
    public void testEnclosingMethod() throws Exception {
        assertThat(makePlainInstrumentedType().getEnclosingMethod(), nullValue());
    }

    @Test
    public void testEnclosingType() throws Exception {
        assertThat(makePlainInstrumentedType().getEnclosingType(), nullValue());
    }

    @Test
    public void testDeclaringType() throws Exception {
        assertThat(makePlainInstrumentedType().getDeclaringType(), nullValue());
    }

    @Test
    public void testIsAnonymous() throws Exception {
        assertThat(makePlainInstrumentedType().isAnonymousClass(), is(false));
    }

    @Test
    public void testCanonicalName() throws Exception {
        TypeDescription typeDescription = makePlainInstrumentedType();
        assertThat(typeDescription.getCanonicalName(), is(typeDescription.getName()));
    }

    @Test
    public void testIsMemberClass() throws Exception {
        assertThat(makePlainInstrumentedType().isMemberClass(), is(false));
    }
}
