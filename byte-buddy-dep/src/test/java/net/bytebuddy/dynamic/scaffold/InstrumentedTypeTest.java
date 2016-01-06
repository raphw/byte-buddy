package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.TypeVariableSource;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.TypeVariableToken;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class InstrumentedTypeTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    private static final int MODIFIERS = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    protected static InstrumentedType.WithFlexibleName makePlainInstrumentedType() {
        return new InstrumentedType.Default(FOO + "." + BAZ,
                MODIFIERS,
                TypeDescription.Generic.OBJECT,
                Collections.<TypeVariableToken>emptyList(),
                Collections.<TypeDescription.Generic>emptyList(),
                Collections.<FieldDescription.Token>emptyList(),
                Collections.<MethodDescription.Token>emptyList(),
                Collections.<AnnotationDescription>emptyList(),
                InstrumentedType.TypeInitializer.None.INSTANCE,
                LoadedTypeInitializer.NoOp.INSTANCE,
                TypeDescription.UNDEFINED,
                MethodDescription.UNDEFINED,
                TypeDescription.UNDEFINED,
                Collections.<TypeDescription>emptyList(),
                false,
                false,
                false);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWithTypeVariable() throws Exception {
        TypeDescription.Generic boundType = mock(TypeDescription.Generic.class);
        when(boundType.asGenericType()).thenReturn(boundType);
        when(boundType.accept(Mockito.any(TypeDescription.Generic.Visitor.class))).thenReturn(boundType);
        TypeDescription rawBoundType = mock(TypeDescription.class);
        when(boundType.asErasure()).thenReturn(rawBoundType);
        when(rawBoundType.getName()).thenReturn(FOO);
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getTypeVariables().size(), is(0));
        instrumentedType = instrumentedType.withTypeVariable(new TypeVariableToken(BAR, boundType));
        assertThat(instrumentedType.getTypeVariables().size(), is(1));
        TypeDescription.Generic typeVariable = instrumentedType.getTypeVariables().get(0);
        assertThat(typeVariable.getTypeName(), is(BAR));
        assertThat(typeVariable.getVariableSource(), sameInstance((TypeVariableSource) instrumentedType));
        assertThat(typeVariable.getUpperBounds(), is(Collections.singletonList(boundType)));
    }

    @Test
    public void testWithTypeVariableWithInstrumentedType() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getTypeVariables().size(), is(0));
        instrumentedType = instrumentedType.withTypeVariable(new TypeVariableToken(BAR, TargetType.GENERIC_DESCRIPTION));
        assertThat(instrumentedType.getTypeVariables().size(), is(1));
        TypeDescription.Generic typeVariable = instrumentedType.getTypeVariables().get(0);
        assertThat(typeVariable.getTypeName(), is(BAR));
        assertThat(typeVariable.getVariableSource(), sameInstance((TypeVariableSource) instrumentedType));
        assertThat(typeVariable.getUpperBounds(), is(Collections.singletonList(instrumentedType.asGenericType())));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWithField() throws Exception {
        TypeDescription.Generic fieldType = mock(TypeDescription.Generic.class);
        when(fieldType.accept(Mockito.any(TypeDescription.Generic.Visitor.class))).thenReturn(fieldType);
        TypeDescription rawFieldType = mock(TypeDescription.class);
        when(fieldType.asErasure()).thenReturn(rawFieldType);
        when(rawFieldType.getName()).thenReturn(FOO);
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        instrumentedType = instrumentedType.withField(new FieldDescription.Token(BAR, Opcodes.ACC_PUBLIC, fieldType));
        assertThat(instrumentedType.getDeclaredFields().size(), is(1));
        FieldDescription.InDefinedShape fieldDescription = instrumentedType.getDeclaredFields().get(0);
        assertThat(fieldDescription.getType(), is(fieldType));
        assertThat(fieldDescription.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(fieldDescription.getName(), is(BAR));
        assertThat(fieldDescription.getDeclaringType(), sameInstance((TypeDescription) instrumentedType));
    }

    @Test
    public void testWithFieldOfInstrumentedType() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        instrumentedType = instrumentedType.withField(new FieldDescription.Token(BAR, Opcodes.ACC_PUBLIC, TargetType.GENERIC_DESCRIPTION));
        assertThat(instrumentedType.getDeclaredFields().size(), is(1));
        FieldDescription.InDefinedShape fieldDescription = instrumentedType.getDeclaredFields().get(0);
        assertThat(fieldDescription.getType().asErasure(), sameInstance((TypeDescription) instrumentedType));
        assertThat(fieldDescription.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(fieldDescription.getName(), is(BAR));
        assertThat(fieldDescription.getDeclaringType(), sameInstance((TypeDescription) instrumentedType));
    }

    @Test
    public void testWithFieldOfInstrumentedTypeAsArray() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        instrumentedType = instrumentedType.withField(new FieldDescription.Token(BAR, Opcodes.ACC_PUBLIC,
                TypeDescription.Generic.OfGenericArray.Latent.of(TargetType.GENERIC_DESCRIPTION, 1)));
        assertThat(instrumentedType.getDeclaredFields().size(), is(1));
        FieldDescription.InDefinedShape fieldDescription = instrumentedType.getDeclaredFields().get(0);
        assertThat(fieldDescription.getType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(fieldDescription.getType().asErasure().isArray(), is(true));
        assertThat(fieldDescription.getType().asErasure().getComponentType(), sameInstance((TypeDescription) instrumentedType));
        assertThat(fieldDescription.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(fieldDescription.getName(), is(BAR));
        assertThat(fieldDescription.getDeclaringType(), sameInstance((TypeDescription) instrumentedType));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWithMethod() throws Exception {
        TypeDescription.Generic returnType = mock(TypeDescription.Generic.class);
        when(returnType.accept(Mockito.any(TypeDescription.Generic.Visitor.class))).thenReturn(returnType);
        TypeDescription rawReturnType = mock(TypeDescription.class);
        when(returnType.asErasure()).thenReturn(rawReturnType);
        when(rawReturnType.getName()).thenReturn(FOO);
        TypeDescription.Generic parameterType = mock(TypeDescription.Generic.class);
        when(parameterType.accept(Mockito.any(TypeDescription.Generic.Visitor.class))).thenReturn(parameterType);
        when(parameterType.asGenericType()).thenReturn(parameterType);
        TypeDescription rawParameterType = mock(TypeDescription.class);
        when(parameterType.asErasure()).thenReturn(rawParameterType);
        when(rawParameterType.getName()).thenReturn(QUX);
        when(rawParameterType.getStackSize()).thenReturn(StackSize.ZERO);
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        instrumentedType = instrumentedType.withMethod(new MethodDescription.Token(BAR,
                Opcodes.ACC_PUBLIC,
                returnType,
                Collections.singletonList(parameterType)));
        assertThat(instrumentedType.getDeclaredMethods().size(), is(1));
        MethodDescription.InDefinedShape methodDescription = instrumentedType.getDeclaredMethods().get(0);
        assertThat(methodDescription.getReturnType(), is(returnType));
        assertThat(methodDescription.getParameters().size(), is(1));
        assertThat(methodDescription.getParameters().asTypeList(), is(Collections.singletonList(parameterType)));
        assertThat(methodDescription.getExceptionTypes().size(), is(0));
        assertThat(methodDescription.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(methodDescription.getName(), is(BAR));
        assertThat(methodDescription.getDeclaringType(), sameInstance((TypeDescription) instrumentedType));
    }

    @Test
    public void testWithMethodOfInstrumentedType() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        instrumentedType = instrumentedType.withMethod(new MethodDescription.Token(BAR,
                Opcodes.ACC_PUBLIC,
                TargetType.GENERIC_DESCRIPTION,
                Collections.singletonList(TargetType.GENERIC_DESCRIPTION)));
        assertThat(instrumentedType.getDeclaredMethods().size(), is(1));
        MethodDescription.InDefinedShape methodDescription = instrumentedType.getDeclaredMethods().get(0);
        assertThat(methodDescription.getReturnType().asErasure(), sameInstance((TypeDescription) instrumentedType));
        assertThat(methodDescription.getParameters().size(), is(1));
        assertThat(methodDescription.getParameters().asTypeList().get(0).asErasure(), sameInstance((TypeDescription) instrumentedType));
        assertThat(methodDescription.getExceptionTypes().size(), is(0));
        assertThat(methodDescription.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(methodDescription.getName(), is(BAR));
        assertThat(methodDescription.getDeclaringType(), sameInstance((TypeDescription) instrumentedType));
    }

    @Test
    public void testWithMethodOfInstrumentedTypeAsArray() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        instrumentedType = instrumentedType.withMethod(new MethodDescription.Token(BAR,
                Opcodes.ACC_PUBLIC,
                TypeDescription.Generic.OfGenericArray.Latent.of(TargetType.GENERIC_DESCRIPTION, 1),
                Collections.singletonList(TypeDescription.Generic.OfGenericArray.Latent.of(TargetType.GENERIC_DESCRIPTION, 1))));
        assertThat(instrumentedType.getDeclaredMethods().size(), is(1));
        MethodDescription.InDefinedShape methodDescription = instrumentedType.getDeclaredMethods().get(0);
        assertThat(methodDescription.getReturnType().asErasure().isArray(), is(true));
        assertThat(methodDescription.getReturnType().getComponentType().asErasure(), sameInstance((TypeDescription) instrumentedType));
        assertThat(methodDescription.getParameters().size(), is(1));
        assertThat(methodDescription.getParameters().asTypeList().asErasures().get(0).isArray(), is(true));
        assertThat(methodDescription.getParameters().asTypeList().get(0).getComponentType().asErasure(), sameInstance((TypeDescription) instrumentedType));
        assertThat(methodDescription.getExceptionTypes().size(), is(0));
        assertThat(methodDescription.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(methodDescription.getName(), is(BAR));
        assertThat(methodDescription.getDeclaringType(), sameInstance((TypeDescription) instrumentedType));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWithInterface() throws Exception {
        TypeDescription.Generic interfaceType = mock(TypeDescription.Generic.class);
        when(interfaceType.asGenericType()).thenReturn(interfaceType);
        when(interfaceType.accept(Mockito.any(TypeDescription.Generic.Visitor.class))).thenReturn(interfaceType);
        TypeDescription rawBoundType = mock(TypeDescription.class);
        when(interfaceType.asErasure()).thenReturn(rawBoundType);
        when(rawBoundType.getName()).thenReturn(FOO);
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getInterfaces().size(), is(0));
        instrumentedType = instrumentedType.withInterfaces(new TypeList.Generic.Explicit(interfaceType));
        assertThat(instrumentedType.getInterfaces().size(), is(1));
        assertThat(instrumentedType.getInterfaces(), is(Collections.singletonList(interfaceType)));
    }

    @Test
    public void testWithInterfaceOfInstrumentedType() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getInterfaces().size(), is(0));
        instrumentedType = instrumentedType.withInterfaces(new TypeList.Generic.Explicit(TargetType.DESCRIPTION));
        assertThat(instrumentedType.getInterfaces().size(), is(1));
        assertThat(instrumentedType.getInterfaces(), is(Collections.singletonList(instrumentedType.asGenericType())));
    }

    @Test
    public void testWithAnnotation() throws Exception {
        AnnotationDescription annotationDescription = mock(AnnotationDescription.class);
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredAnnotations().size(), is(0));
        instrumentedType = instrumentedType.withAnnotations(Collections.singletonList(annotationDescription));
        assertThat(instrumentedType.getDeclaredAnnotations(), is(Collections.singletonList(annotationDescription)));
    }

    @Test
    public void testWithName() throws Exception {
        InstrumentedType.WithFlexibleName instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getName(), is(FOO + "." + BAZ));
        instrumentedType = instrumentedType.withName(BAR);
        assertThat(instrumentedType.getName(), is(BAR));
    }

    @Test
    public void testModifiers() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getModifiers(), is(MODIFIERS));
        instrumentedType = instrumentedType.withModifiers(MODIFIERS);
        assertThat(instrumentedType.getModifiers(), is(MODIFIERS));
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
        when(other.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(other.asErasure()).thenReturn(other);
        assertThat(instrumentedType, is(other));
        verify(other, atLeast(1)).getInternalName();
    }

    @Test
    public void testIsAssignableFrom() {
        assertThat(makePlainInstrumentedType().isAssignableFrom(Object.class), is(false));
        assertThat(makePlainInstrumentedType().isAssignableFrom(Serializable.class), is(false));
        assertThat(makePlainInstrumentedType().isAssignableFrom(Integer.class), is(false));
        TypeDescription objectTypeDescription = TypeDescription.OBJECT;
        assertThat(makePlainInstrumentedType().isAssignableFrom(objectTypeDescription), is(false));
        TypeDescription serializableTypeDescription = new TypeDescription.ForLoadedType(Serializable.class);
        assertThat(makePlainInstrumentedType().isAssignableFrom(serializableTypeDescription), is(false));
        TypeDescription integerTypeDescription = new TypeDescription.ForLoadedType(Integer.class);
        assertThat(makePlainInstrumentedType().isAssignableFrom(integerTypeDescription), is(false));
    }

    @Test
    public void testIsAssignableTo() {
        assertThat(makePlainInstrumentedType().isAssignableTo(Object.class), is(true));
        assertThat(makePlainInstrumentedType().isAssignableTo(makePlainInstrumentedType()), is(true));
        assertThat(makePlainInstrumentedType().isAssignableTo(Integer.class), is(false));
        assertThat(makePlainInstrumentedType().isAssignableTo(TypeDescription.OBJECT), is(true));
    }

    @Test
    public void testRepresents() {
        assertThat(makePlainInstrumentedType().represents(Object.class), is(false));
        assertThat(makePlainInstrumentedType().represents(Serializable.class), is(false));
        assertThat(makePlainInstrumentedType().represents(Integer.class), is(false));
    }

    @Test
    public void testSuperType() {
        assertThat(makePlainInstrumentedType().getSuperType(), is(TypeDescription.Generic.OBJECT));
        assertThat(makePlainInstrumentedType().getSuperType(), not((TypeDescription.Generic) new TypeDescription.Generic.OfNonGenericType.ForLoadedType(Integer.class)));
        assertThat(makePlainInstrumentedType().getSuperType(), not((TypeDescription.Generic) new TypeDescription.Generic.OfNonGenericType.ForLoadedType(Serializable.class)));
    }

    @Test
    public void testInterfaces() {
        assertThat(makePlainInstrumentedType().getInterfaces().size(), is(0));
    }

    @Test
    public void testPackage() {
        assertThat(makePlainInstrumentedType().getPackage().getName(), is(FOO));
    }

    @Test
    public void testSimpleName() {
        assertThat(makePlainInstrumentedType().getSimpleName(), is(BAZ));
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

    @Test
    public void testDeclaredTypes() throws Exception {
        assertThat(makePlainInstrumentedType().getDeclaredTypes().size(), is(0));
    }

    @Test
    public void testFieldTokenIsVisited() throws Exception {
        FieldDescription.Token token = mock(FieldDescription.Token.class);
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.withField(token), is(instrumentedType));
        verify(token).accept(TypeDescription.Generic.Visitor.Substitutor.ForDetachment.of(instrumentedType));
        verifyNoMoreInteractions(token);
    }

    @Test
    public void testMethodTokenIsVisited() throws Exception {
        MethodDescription.Token token = mock(MethodDescription.Token.class);
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.withMethod(token), is(instrumentedType));
        verify(token).accept(TypeDescription.Generic.Visitor.Substitutor.ForDetachment.of(instrumentedType));
        verifyNoMoreInteractions(token);
    }

    @Test
    public void testTypeVariableIsVisited() throws Exception {
        TypeVariableToken token = mock(TypeVariableToken.class);
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.withTypeVariable(token), is(instrumentedType));
        verify(token).accept(TypeDescription.Generic.Visitor.Substitutor.ForDetachment.of(instrumentedType));
        verifyNoMoreInteractions(token);
    }

    @Test
    public void testInterfaceTypesVisited() throws Exception {
        TypeDescription.Generic typeDescription = mock(TypeDescription.Generic.class);
        when(typeDescription.asGenericType()).thenReturn(typeDescription);
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.withInterfaces(new TypeList.Generic.Explicit(typeDescription)), is(instrumentedType));
        verify(typeDescription).accept(TypeDescription.Generic.Visitor.Substitutor.ForDetachment.of(instrumentedType));
        verify(typeDescription).asGenericType();
        verifyNoMoreInteractions(typeDescription);
    }
}
