package net.bytebuddy.dynamic.scaffold;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.ModifierReviewable;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.TypeVariableSource;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.*;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.Transformer;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.packaging.PackagePrivateType;
import net.bytebuddy.test.utility.MockitoRule;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class InstrumentedTypeDefaultTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz", ILLEGAL_NAME = "<>";

    private static final int ILLEGAL_MODIFIERS = -1, OTHER_MODIFIERS = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private AnnotationDescription annotationDescription;

    protected static InstrumentedType.WithFlexibleName makePlainInstrumentedType() {
        return new InstrumentedType.Default(FOO + "." + BAZ,
                ModifierReviewable.EMPTY_MASK,
                TypeDescription.Generic.OBJECT,
                Collections.<TypeVariableToken>emptyList(),
                Collections.<TypeDescription.Generic>emptyList(),
                Collections.<FieldDescription.Token>emptyList(),
                Collections.<MethodDescription.Token>emptyList(),
                Collections.<AnnotationDescription>emptyList(),
                TypeInitializer.None.INSTANCE,
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
        instrumentedType = instrumentedType.withTypeVariable(new TypeVariableToken(BAR, Collections.singletonList(boundType)));
        assertThat(instrumentedType.getTypeVariables().size(), is(1));
        TypeDescription.Generic typeVariable = instrumentedType.getTypeVariables().get(0);
        assertThat(typeVariable.getTypeName(), is(BAR));
        assertThat(typeVariable.getTypeVariableSource(), sameInstance((TypeVariableSource) instrumentedType));
        assertThat(typeVariable.getUpperBounds(), is(Collections.singletonList(boundType)));
    }

    @Test
    public void testWithTypeVariableWithInstrumentedType() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getTypeVariables().size(), is(0));
        instrumentedType = instrumentedType.withTypeVariable(new TypeVariableToken(BAR, Collections.singletonList(TargetType.DESCRIPTION.asGenericType())));
        assertThat(instrumentedType.getTypeVariables().size(), is(1));
        TypeDescription.Generic typeVariable = instrumentedType.getTypeVariables().get(0);
        assertThat(typeVariable.getTypeName(), is(BAR));
        assertThat(typeVariable.getTypeVariableSource(), sameInstance((TypeVariableSource) instrumentedType));
        assertThat(typeVariable.getUpperBounds(), is(Collections.singletonList(instrumentedType.asGenericType())));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWithTypeVariableTransformed() throws Exception {
        TypeDescription.Generic boundType = mock(TypeDescription.Generic.class);
        when(boundType.asGenericType()).thenReturn(boundType);
        when(boundType.accept(Mockito.any(TypeDescription.Generic.Visitor.class))).thenReturn(boundType);
        TypeDescription rawBoundType = mock(TypeDescription.class);
        when(boundType.asErasure()).thenReturn(rawBoundType);
        when(rawBoundType.getName()).thenReturn(FOO);
        InstrumentedType.WithFlexibleName instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getTypeVariables().size(), is(0));
        instrumentedType = instrumentedType.withTypeVariable(new TypeVariableToken(BAR, Collections.singletonList(boundType)));
        Transformer<TypeVariableToken> transformer = mock(Transformer.class);
        TypeDescription.Generic otherBoundType = mock(TypeDescription.Generic.class);
        when(otherBoundType.asGenericType()).thenReturn(otherBoundType);
        when(otherBoundType.accept(Mockito.any(TypeDescription.Generic.Visitor.class))).thenReturn(otherBoundType);
        TypeDescription rawOtherBoundType = mock(TypeDescription.class);
        when(otherBoundType.asErasure()).thenReturn(rawOtherBoundType);
        when(transformer.transform(instrumentedType, new TypeVariableToken(BAR, Collections.singletonList(boundType))))
                .thenReturn(new TypeVariableToken(QUX, Collections.singletonList(otherBoundType)));
        instrumentedType = instrumentedType.withTypeVariables(named(BAR), transformer);
        assertThat(instrumentedType.getTypeVariables().size(), is(1));
        TypeDescription.Generic typeVariable = instrumentedType.getTypeVariables().get(0);
        assertThat(typeVariable.getTypeName(), is(QUX));
        assertThat(typeVariable.getTypeVariableSource(), sameInstance((TypeVariableSource) instrumentedType));
        assertThat(typeVariable.getUpperBounds(), is(Collections.singletonList(otherBoundType)));
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
        instrumentedType = instrumentedType.withField(new FieldDescription.Token(BAR, Opcodes.ACC_PUBLIC, TargetType.DESCRIPTION.asGenericType()));
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
                new TypeDescription.Generic.OfGenericArray.Latent(TargetType.DESCRIPTION.asGenericType(), Collections.singletonList(annotationDescription))));
        assertThat(instrumentedType.getDeclaredFields().size(), is(1));
        FieldDescription.InDefinedShape fieldDescription = instrumentedType.getDeclaredFields().get(0);
        assertThat(fieldDescription.getType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(fieldDescription.getType().asErasure().isArray(), is(true));
        assertThat(fieldDescription.getType().asErasure().getComponentType(), sameInstance((TypeDescription) instrumentedType));
        assertThat(fieldDescription.getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(fieldDescription.getName(), is(BAR));
        assertThat(fieldDescription.getType().getDeclaredAnnotations().size(), is(1));
        assertThat(fieldDescription.getType().getDeclaredAnnotations().getOnly(), is(annotationDescription));
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
                TargetType.DESCRIPTION.asGenericType(),
                Collections.singletonList(TargetType.DESCRIPTION.asGenericType())));
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
                new TypeDescription.Generic.OfGenericArray.Latent(TargetType.DESCRIPTION.asGenericType(), Collections.singletonList(annotationDescription)),
                Collections.singletonList(new TypeDescription.Generic.OfGenericArray.Latent(TargetType.DESCRIPTION.asGenericType(), Collections.singletonList(annotationDescription)))));
        assertThat(instrumentedType.getDeclaredMethods().size(), is(1));
        MethodDescription.InDefinedShape methodDescription = instrumentedType.getDeclaredMethods().get(0);
        assertThat(methodDescription.getReturnType().asErasure().isArray(), is(true));
        assertThat(methodDescription.getReturnType().getComponentType().asErasure(), sameInstance((TypeDescription) instrumentedType));
        assertThat(methodDescription.getParameters().size(), is(1));
        assertThat(methodDescription.getParameters().asTypeList().asErasures().get(0).isArray(), is(true));
        assertThat(methodDescription.getParameters().asTypeList().get(0).getComponentType().asErasure(), sameInstance((TypeDescription) instrumentedType));
        assertThat(methodDescription.getExceptionTypes().size(), is(0));
        assertThat(methodDescription.getReturnType().getDeclaredAnnotations().size(), is(1));
        assertThat(methodDescription.getReturnType().getDeclaredAnnotations().getOnly(), is(annotationDescription));
        assertThat(methodDescription.getParameters().getOnly().getType().getDeclaredAnnotations().size(), is(1));
        assertThat(methodDescription.getParameters().getOnly().getType().getDeclaredAnnotations().getOnly(), is(annotationDescription));
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
        assertThat(instrumentedType.getModifiers(), is(ModifierContributor.EMPTY_MASK));
        instrumentedType = instrumentedType.withModifiers(OTHER_MODIFIERS);
        assertThat(instrumentedType.getModifiers(), is(OTHER_MODIFIERS));
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
        TypeInitializer typeInitializer = makePlainInstrumentedType().getTypeInitializer();
        assertThat(typeInitializer.isDefined(), is(false));
    }

    @Test
    public void testWithTypeInitializerSingle() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.getDeclaredFields().size(), is(0));
        ByteCodeAppender byteCodeAppender = mock(ByteCodeAppender.class);
        instrumentedType = instrumentedType.withInitializer(byteCodeAppender);
        TypeInitializer typeInitializer = instrumentedType.getTypeInitializer();
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
        TypeInitializer typeInitializer = instrumentedType.getTypeInitializer();
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
        assertThat(instrumentedType.hashCode(), is(instrumentedType.getName().hashCode()));
    }

    @Test
    public void testEquals() throws Exception {
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        TypeDescription other = mock(TypeDescription.class);
        when(other.getName()).thenReturn(instrumentedType.getName());
        when(other.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(other.asErasure()).thenReturn(other);
        assertThat(instrumentedType, is(other));
        verify(other, atLeast(1)).getName();
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
    public void testSuperClass() {
        assertThat(makePlainInstrumentedType().getSuperClass(), is(TypeDescription.Generic.OBJECT));
        assertThat(makePlainInstrumentedType().getSuperClass(), not((TypeDescription.Generic) new TypeDescription.Generic.OfNonGenericType.ForLoadedType(Integer.class)));
        assertThat(makePlainInstrumentedType().getSuperClass(), not((TypeDescription.Generic) new TypeDescription.Generic.OfNonGenericType.ForLoadedType(Serializable.class)));
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
    @SuppressWarnings("unchecked")
    public void testInterfaceTypesVisited() throws Exception {
        TypeDescription.Generic typeDescription = mock(TypeDescription.Generic.class);
        when(typeDescription.asGenericType()).thenReturn(typeDescription);
        when(typeDescription.accept(Mockito.any(TypeDescription.Generic.Visitor.class))).thenReturn(typeDescription);
        InstrumentedType instrumentedType = makePlainInstrumentedType();
        assertThat(instrumentedType.withInterfaces(new TypeList.Generic.Explicit(typeDescription)), is(instrumentedType));
        verify(typeDescription).accept(TypeDescription.Generic.Visitor.Substitutor.ForDetachment.of(instrumentedType));
        verify(typeDescription, times(2)).asGenericType();
        verifyNoMoreInteractions(typeDescription);
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeIllegalName() throws Exception {
        makePlainInstrumentedType().withName(ILLEGAL_NAME).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeIllegalEndName() throws Exception {
        makePlainInstrumentedType().withName(FOO + ILLEGAL_NAME).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeEmptyEndName() throws Exception {
        makePlainInstrumentedType().withName(NamedElement.EMPTY_NAME).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeChainedEmptyEndName() throws Exception {
        makePlainInstrumentedType().withName("." + FOO).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeIllegalKeywordName() throws Exception {
        makePlainInstrumentedType().withName(void.class.getName()).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeIllegalSubType() throws Exception {
        InstrumentedType.Default.subclass(FOO, ModifierContributor.EMPTY_MASK, TypeDefinition.Sort.describe(Serializable.class)).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeInvisibleSubType() throws Exception {
        InstrumentedType.Default.subclass(FOO, ModifierContributor.EMPTY_MASK, TypeDefinition.Sort.describe(PackagePrivateType.TYPE)).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeIllegalModifiers() throws Exception {
        InstrumentedType.Default.subclass(FOO, ILLEGAL_MODIFIERS, TypeDefinition.Sort.describe(Object.class)).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testPackageTypeIllegalModifiers() throws Exception {
        InstrumentedType.Default.subclass(FOO + "." + PackageDescription.PACKAGE_CLASS_NAME, ModifierContributor.EMPTY_MASK, TypeDefinition.Sort.describe(Object.class))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeIllegalInterfaceType() throws Exception {
        makePlainInstrumentedType().withInterfaces(new TypeList.Generic.Explicit(TypeDescription.Generic.OBJECT)).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testInvisibleInterfaceType() throws Exception {
        makePlainInstrumentedType().withInterfaces(new TypeList.Generic.Explicit(TypeDefinition.Sort.describe(PackagePrivateType.INTERFACE_TYPE))).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeDuplicateInterface() throws Exception {
        makePlainInstrumentedType().withInterfaces(new TypeList.Generic.Explicit(
                new TypeDescription.ForLoadedType(Serializable.class),
                new TypeDescription.ForLoadedType(Serializable.class)
        )).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeThrowableWithGenerics() throws Exception {
        InstrumentedType.Default.of(new TypeDescription.ForLoadedType(Exception.class))
                .withTypeVariable(new TypeVariableToken(FOO, Collections.singletonList(TypeDescription.Generic.OBJECT)))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeDuplicateTypeVariableName() throws Exception {
        makePlainInstrumentedType()
                .withTypeVariable(new TypeVariableToken(FOO, Collections.singletonList(TypeDescription.Generic.OBJECT)))
                .withTypeVariable(new TypeVariableToken(FOO, Collections.singletonList(TypeDescription.Generic.OBJECT)))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeTypeVariableIllegalName() throws Exception {
        makePlainInstrumentedType()
                .withTypeVariable(new TypeVariableToken(ILLEGAL_NAME, Collections.singletonList(TypeDescription.Generic.OBJECT)))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeTypeVariableMissingBound() throws Exception {
        makePlainInstrumentedType()
                .withTypeVariable(new TypeVariableToken(FOO, Collections.<TypeDescription.Generic>emptyList()))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeTypeVariableDuplicateBound() throws Exception {
        makePlainInstrumentedType()
                .withTypeVariable(new TypeVariableToken(FOO, Arrays.asList(TypeDescription.Sort.describe(Serializable.class), TypeDefinition.Sort.describe(Serializable.class))))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeTypeVariableIllegalBound() throws Exception {
        makePlainInstrumentedType()
                .withTypeVariable(new TypeVariableToken(FOO, Collections.singletonList(TypeDescription.Generic.VOID)))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeTypeVariableDoubleClassBound() throws Exception {
        makePlainInstrumentedType()
                .withTypeVariable(new TypeVariableToken(FOO, Arrays.asList(TypeDescription.Generic.OBJECT, TypeDefinition.Sort.describe(String.class))))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeDuplicateAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withAnnotations(Arrays.asList(
                        AnnotationDescription.Builder.ofType(SampleAnnotation.class).build(),
                        AnnotationDescription.Builder.ofType(SampleAnnotation.class).build()
                )).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeIncompatibleAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withAnnotations(Collections.singletonList(AnnotationDescription.Builder.ofType(IncompatibleAnnotation.class).build()))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testPackageIncompatibleAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withName(FOO + "." + PackageDescription.PACKAGE_CLASS_NAME)
                .withAnnotations(Collections.singletonList(AnnotationDescription.Builder.ofType(IncompatibleAnnotation.class).build()))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationTypeIncompatibleAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withModifiers(Opcodes.ACC_ANNOTATION | Opcodes.ACC_ABSTRACT)
                .withAnnotations(Collections.singletonList(AnnotationDescription.Builder.ofType(IncompatibleAnnotation.class).build()))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationTypeIncompatibleSuperClassTypeAnnotation() throws Exception {
        InstrumentedType.Default.subclass(FOO, ModifierReviewable.EMPTY_MASK, TypeDescription.Generic.Builder.rawType(Object.class)
                .build(AnnotationDescription.Builder.ofType(IncompatibleAnnotation.class).build()))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationTypeIncompatibleInterfaceTypeAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withInterfaces(new TypeList.Generic.Explicit(TypeDescription.Generic.Builder.rawType(Runnable.class)
                        .build(AnnotationDescription.Builder.ofType(IncompatibleAnnotation.class).build())))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationTypeIncompatibleTypeVariableTypeAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withTypeVariable(new TypeVariableToken(FOO,
                        Collections.singletonList(TypeDescription.Generic.OBJECT),
                        Collections.singletonList(AnnotationDescription.Builder.ofType(IncompatibleAnnotation.class).build())))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotationTypeIncompatibleTypeVariableBoundTypeAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withTypeVariable(new TypeVariableToken(FOO,
                        Collections.singletonList(TypeDescription.Generic.Builder.rawType(Object.class)
                                .build(AnnotationDescription.Builder.ofType(IncompatibleAnnotation.class).build()))))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldDuplicateName() throws Exception {
        makePlainInstrumentedType()
                .withField(new FieldDescription.Token(FOO, ModifierContributor.EMPTY_MASK, TypeDescription.Generic.OBJECT))
                .withField(new FieldDescription.Token(FOO, ModifierContributor.EMPTY_MASK, TypeDescription.Generic.OBJECT))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldIllegalName() throws Exception {
        makePlainInstrumentedType().withField(new FieldDescription.Token(ILLEGAL_NAME, ModifierContributor.EMPTY_MASK, TypeDescription.Generic.OBJECT)).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldIllegalModifiers() throws Exception {
        makePlainInstrumentedType().withField(new FieldDescription.Token(FOO, ILLEGAL_MODIFIERS, TypeDescription.Generic.OBJECT)).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldIllegalType() throws Exception {
        makePlainInstrumentedType().withField(new FieldDescription.Token(ILLEGAL_NAME, ModifierContributor.EMPTY_MASK, TypeDescription.Generic.VOID)).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldInvisibleType() throws Exception {
        makePlainInstrumentedType()
                .withField(new FieldDescription.Token(FOO, ModifierContributor.EMPTY_MASK, TypeDefinition.Sort.describe(PackagePrivateType.TYPE)))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void tesFieldDuplicateAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withField(new FieldDescription.Token(FOO, ModifierContributor.EMPTY_MASK, TypeDescription.Generic.OBJECT, Arrays.asList(
                        AnnotationDescription.Builder.ofType(SampleAnnotation.class).build(),
                        AnnotationDescription.Builder.ofType(SampleAnnotation.class).build()
                ))).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void tesFieldIncompatibleAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withField(new FieldDescription.Token(FOO, ModifierContributor.EMPTY_MASK, TypeDescription.Generic.OBJECT, Collections.singletonList(
                        AnnotationDescription.Builder.ofType(IncompatibleAnnotation.class).build()
                ))).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldIncompatibleTypeAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withField(new FieldDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        TypeDescription.Generic.Builder.rawType(Runnable.class)
                                .build(AnnotationDescription.Builder.ofType(IncompatibleAnnotation.class).build())))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodDuplicateErasure() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO, ModifierContributor.EMPTY_MASK, TypeDescription.Generic.OBJECT))
                .withMethod(new MethodDescription.Token(FOO, ModifierContributor.EMPTY_MASK, TypeDescription.Generic.OBJECT))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodTypeInitializer() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME, ModifierContributor.EMPTY_MASK, TypeDescription.Generic.VOID))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testConstructorNonVoidReturnType() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(MethodDescription.CONSTRUCTOR_INTERNAL_NAME, ModifierContributor.EMPTY_MASK, TypeDescription.Generic.OBJECT))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodInvisibleReturnType() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO, ModifierContributor.EMPTY_MASK, TypeDefinition.Sort.describe(PackagePrivateType.TYPE)))
                .validated();
    }

    @Test
    public void testMethodInvisibleReturnTypeSynthetic() throws Exception {
        assertThat(makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO, Opcodes.ACC_SYNTHETIC, TypeDefinition.Sort.describe(PackagePrivateType.TYPE)))
                .validated(), instanceOf(TypeDescription.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodIllegalName() throws Exception {
        makePlainInstrumentedType().withMethod(new MethodDescription.Token(ILLEGAL_NAME, ModifierContributor.EMPTY_MASK, TypeDescription.Generic.OBJECT)).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodIllegalModifiers() throws Exception {
        makePlainInstrumentedType().withMethod(new MethodDescription.Token(FOO, ILLEGAL_MODIFIERS, TypeDescription.Generic.OBJECT)).validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodDuplicateAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Arrays.asList(
                                AnnotationDescription.Builder.ofType(SampleAnnotation.class).build(),
                                AnnotationDescription.Builder.ofType(SampleAnnotation.class).build()
                        ), AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodIncompatibleAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.singletonList(AnnotationDescription.Builder.ofType(IncompatibleAnnotation.class).build()),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodIncompatibleReturnTypeTypeAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        TypeDescription.Generic.Builder.rawType(Runnable.class)
                                .build(AnnotationDescription.Builder.ofType(IncompatibleAnnotation.class).build())))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodIllegalTypeVariableTypeAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.singletonList(new TypeVariableToken(FOO,
                                Collections.singletonList(TypeDescription.Generic.OBJECT),
                                Collections.singletonList(AnnotationDescription.Builder.ofType(IncompatibleAnnotation.class).build()))),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodIllegalTypeVariableBoundTypeAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.singletonList(new TypeVariableToken(FOO,
                                Collections.singletonList(TypeDescription.Generic.Builder.rawType(Object.class).build(
                                AnnotationDescription.Builder.ofType(IncompatibleAnnotation.class).build())))),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodIllegalTypeVariableName() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.singletonList(new TypeVariableToken(ILLEGAL_NAME, Collections.singletonList(TypeDescription.Generic.OBJECT))),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodDuplicateTypeVariableName() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Arrays.asList(
                                new TypeVariableToken(FOO, Collections.singletonList(TypeDescription.Generic.OBJECT)),
                                new TypeVariableToken(FOO, Collections.singletonList(TypeDescription.Generic.OBJECT))
                        ),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodTypeVariableMissingBound() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.singletonList(new TypeVariableToken(FOO, Collections.<TypeDescription.Generic>emptyList())),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodTypeVariableIllegalBound() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.singletonList(new TypeVariableToken(FOO, Collections.singletonList(TypeDescription.Generic.VOID))),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodTypeVariableDuplicateBound() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.singletonList(new TypeVariableToken(FOO, Arrays.asList(TypeDefinition.Sort.describe(Serializable.class),
                                TypeDefinition.Sort.describe(Serializable.class)))),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodTypeVariableDoubleClassBound() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.singletonList(new TypeVariableToken(FOO, Arrays.asList(TypeDescription.Generic.OBJECT, TypeDefinition.Sort.describe(String.class)))),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodParameterIllegalName() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.singletonList(new ParameterDescription.Token(TypeDescription.Generic.OBJECT, ILLEGAL_NAME, 0)),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodParameterIllegalType() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.singletonList(new ParameterDescription.Token(TypeDescription.Generic.VOID)),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodParameterInvisibleType() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.singletonList(new ParameterDescription.Token(TypeDefinition.Sort.describe(PackagePrivateType.TYPE))),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test
    public void testMethodParameterInvisibleTypeSynthetic() throws Exception {
        assertThat(makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        Opcodes.ACC_SYNTHETIC,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.singletonList(new ParameterDescription.Token(TypeDefinition.Sort.describe(PackagePrivateType.TYPE))),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated(), notNullValue(TypeDescription.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodParameterDuplicateName() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Arrays.asList(
                                new ParameterDescription.Token(TypeDescription.Generic.OBJECT, FOO, 0),
                                new ParameterDescription.Token(TypeDescription.Generic.OBJECT, FOO, 0)
                        ), Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodParameterIllegalModifiers() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.singletonList(new ParameterDescription.Token(TypeDescription.Generic.OBJECT, FOO, -1)),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodParameterDuplicateAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.singletonList(new ParameterDescription.Token(TypeDescription.Generic.OBJECT, Arrays.asList(
                                AnnotationDescription.Builder.ofType(SampleAnnotation.class).build(),
                                AnnotationDescription.Builder.ofType(SampleAnnotation.class).build()
                        ))), Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodParameterIncompatibleAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.singletonList(new ParameterDescription.Token(TypeDescription.Generic.OBJECT, Collections.singletonList(
                                AnnotationDescription.Builder.ofType(IncompatibleAnnotation.class).build()
                        ))), Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodIllegalExceptionType() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.singletonList(TypeDescription.Generic.OBJECT),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodIncompatibleExceptionTypeTypeAnnotation() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.singletonList(TypeDescription.Generic.Builder.rawType(Exception.class)
                        .build(AnnotationDescription.Builder.ofType(IncompatibleAnnotation.class).build())),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodInvisibleExceptionType() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.singletonList(TypeDefinition.Sort.describe(PackagePrivateType.EXCEPTION_TYPE)),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test
    public void testMethodInvisibleExceptionSynthetic() throws Exception {
        assertThat(makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        Opcodes.ACC_SYNTHETIC,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.singletonList(TypeDefinition.Sort.describe(PackagePrivateType.EXCEPTION_TYPE)),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated(), notNullValue(TypeDescription.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodDuplicateExceptionType() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Arrays.asList(TypeDefinition.Sort.describe(Exception.class), TypeDefinition.Sort.describe(Exception.class)),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodIllegalDefaultValue() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.ForConstant.of(FOO),
                        TypeDescription.Generic.UNDEFINED))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonNullReceiverStaticMethod() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        Opcodes.ACC_STATIC,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.OBJECT))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testInconsistentReceiverNonStaticMethod() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(FOO,
                        ModifierContributor.EMPTY_MASK,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.OBJECT))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testInconsistentReceiverConstructor() throws Exception {
        makePlainInstrumentedType()
                .withMethod(new MethodDescription.Token(MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                        ModifierContributor.EMPTY_MASK,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDescription.Generic.OBJECT))
                .validated();
    }

    @Test(expected = IllegalStateException.class)
    public void testInconsistentReceiverConstructorInnerClass() throws Exception {
        InstrumentedType.Default.of(new TypeDescription.ForLoadedType(Foo.class))
                .withMethod(new MethodDescription.Token(MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                        ModifierContributor.EMPTY_MASK,
                        Collections.<TypeVariableToken>emptyList(),
                        TypeDescription.Generic.OBJECT,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.<TypeDescription.Generic>emptyList(),
                        Collections.<AnnotationDescription>emptyList(),
                        AnnotationValue.UNDEFINED,
                        TypeDefinition.Sort.describe(Foo.class)))
                .validated();
    }

    @Test
    public void testTypeVariableOutOfScopeIsErased() throws Exception {
        TypeDescription typeDescription = new InstrumentedType.Default("foo",
                Opcodes.ACC_PUBLIC,
                new TypeDescription.Generic.OfNonGenericType.ForLoadedType(AbstractOuter.ExtendedInner.class),
                Collections.<TypeVariableToken>emptyList(),
                Collections.<TypeDescription.Generic>emptyList(),
                Collections.<FieldDescription.Token>emptyList(),
                Collections.singletonList(new MethodDescription.Token("foo",
                        Opcodes.ACC_BRIDGE,
                        TypeDescription.Generic.VOID,
                        Collections.<TypeDescription.Generic>emptyList())),
                Collections.<AnnotationDescription>emptyList(),
                TypeInitializer.None.INSTANCE,
                LoadedTypeInitializer.NoOp.INSTANCE,
                TypeDescription.UNDEFINED,
                MethodDescription.UNDEFINED,
                TypeDescription.UNDEFINED,
                Collections.<TypeDescription>emptyList(),
                false,
                false,
                false);
        MethodDescription methodDescription = typeDescription.getSuperClass().getSuperClass().getDeclaredMethods().filter(named(FOO)).getOnly();
        assertThat(methodDescription.getReturnType(), is(TypeDescription.Generic.OBJECT));

    }

    public @interface SampleAnnotation {
        /* empty */
    }

    @Target({})
    public @interface IncompatibleAnnotation {
        /* empty */
    }

    private class Foo {
        /* empty */
    }

    public static abstract class AbstractOuter<T> {

        public abstract class Inner {

            public abstract T foo();
        }

        public abstract class ExtendedInner extends Inner {
            /* empty */
        }
    }
}
