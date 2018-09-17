package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.ArgumentTypeResolver;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class TargetMethodAnnotationDrivenBinderTest {

    private static final String FOO = "foo", BAR = "bar", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TargetMethodAnnotationDrivenBinder.ParameterBinder<?> firstParameterBinder, secondParameterBinder;

    @Mock
    private TargetMethodAnnotationDrivenBinder.TerminationHandler terminationHandler;

    @Mock
    private Assigner assigner;

    private Assigner.Typing typing = Assigner.Typing.STATIC;

    @Mock
    private StackManipulation assignmentBinding, methodInvocation, termination;

    @Mock
    private TargetMethodAnnotationDrivenBinder.MethodInvoker methodInvoker;

    @Mock
    private Implementation.Target implementationTarget;

    @Mock
    private MethodDescription sourceMethod, targetMethod;

    @Mock
    private TypeDescription.Generic sourceTypeDescription, targetTypeDescription;

    @Mock
    private TypeDescription instrumentedType;

    @Mock
    private AnnotationDescription.ForLoadedAnnotation<FirstPseudoAnnotation> firstPseudoAnnotation;

    @Mock
    private AnnotationDescription.ForLoadedAnnotation<SecondPseudoAnnotation> secondPseudoAnnotation;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private ParameterDescription firstParameter, secondParameter;

    @SuppressWarnings("unchecked")
    private static MethodDelegationBinder.ParameterBinding<?> prepareArgumentBinder(TargetMethodAnnotationDrivenBinder.ParameterBinder<?> parameterBinder,
                                                                                    Class<? extends Annotation> annotationType,
                                                                                    Object identificationToken) {
        doReturn(annotationType).when(parameterBinder).getHandledType();
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = mock(MethodDelegationBinder.ParameterBinding.class);
        when(parameterBinding.isValid()).thenReturn(true);
        when(parameterBinding.apply(any(MethodVisitor.class), any(Implementation.Context.class))).thenReturn(new StackManipulation.Size(0, 0));
        when(parameterBinding.getIdentificationToken()).thenReturn(identificationToken);
        when(((TargetMethodAnnotationDrivenBinder.ParameterBinder) parameterBinder).bind(any(AnnotationDescription.Loadable.class),
                any(MethodDescription.class),
                any(ParameterDescription.class),
                any(Implementation.Target.class),
                any(Assigner.class),
                any(Assigner.Typing.class)))
                .thenReturn(parameterBinding);
        return parameterBinding;
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(assignmentBinding.apply(any(MethodVisitor.class), any(Implementation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(assigner.assign(any(TypeDescription.Generic.class), any(TypeDescription.Generic.class), any(Assigner.Typing.class)))
                .thenReturn(assignmentBinding);
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(methodInvocation);
        when(methodInvocation.apply(any(MethodVisitor.class), any(Implementation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(assignmentBinding.apply(any(MethodVisitor.class), any(Implementation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        TypeDescription declaringType = mock(TypeDescription.class);
        when(declaringType.getInternalName()).thenReturn(FOO);
        when(declaringType.isInterface()).thenReturn(false);
        when(targetMethod.getInternalName()).thenReturn(BAR);
        when(targetMethod.isStatic()).thenReturn(true);
        when(targetMethod.getDeclaringType()).thenReturn(declaringType);
        when(targetMethod.getDescriptor()).thenReturn(BAZ);
        when(firstParameter.getDeclaringMethod()).thenReturn(targetMethod);
        when(firstParameter.getIndex()).thenReturn(0);
        when(secondParameter.getDeclaringMethod()).thenReturn(targetMethod);
        when(secondParameter.getIndex()).thenReturn(1);
        when(targetMethod.getParameters())
                .thenReturn((ParameterList) new ParameterList.Explicit<ParameterDescription>(firstParameter, secondParameter));
        when(firstPseudoAnnotation.getAnnotationType())
                .thenReturn(TypeDescription.ForLoadedType.of(FirstPseudoAnnotation.class));
        when(firstPseudoAnnotation.prepare(FirstPseudoAnnotation.class)).thenReturn(firstPseudoAnnotation);
        when(secondPseudoAnnotation.getAnnotationType())
                .thenReturn(TypeDescription.ForLoadedType.of(SecondPseudoAnnotation.class));
        when(secondPseudoAnnotation.prepare(SecondPseudoAnnotation.class)).thenReturn(secondPseudoAnnotation);
        when(sourceTypeDescription.getStackSize()).thenReturn(StackSize.ZERO);
        when(targetTypeDescription.getStackSize()).thenReturn(StackSize.ZERO);
        when(sourceMethod.getReturnType()).thenReturn(sourceTypeDescription);
        when(targetMethod.getReturnType()).thenReturn(targetTypeDescription);
        when(terminationHandler.resolve(assigner, typing, sourceMethod, targetMethod)).thenReturn(termination);
        when(termination.apply(any(MethodVisitor.class), any(Implementation.Context.class))).thenReturn(new StackManipulation.Size(0, 0));
        when(implementationTarget.getInstrumentedType()).thenReturn(instrumentedType);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(implementationContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConflictingBinderBinding() throws Exception {
        doReturn(FirstPseudoAnnotation.class).when(firstParameterBinder).getHandledType();
        doReturn(FirstPseudoAnnotation.class).when(secondParameterBinder).getHandledType();
        TargetMethodAnnotationDrivenBinder.of(Arrays.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>asList(firstParameterBinder, secondParameterBinder));
    }

    @Test
    public void testIgnoreForBindingAnnotation() throws Exception {
        when(targetMethod.isAccessibleTo(instrumentedType)).thenReturn(true);
        AnnotationDescription ignoreForBinding = mock(AnnotationDescription.class);
        when(ignoreForBinding.getAnnotationType()).thenReturn(TypeDescription.ForLoadedType.of(IgnoreForBinding.class));
        when(targetMethod.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(Collections.singletonList(ignoreForBinding)));
        when(termination.isValid()).thenReturn(true);
        MethodDelegationBinder methodDelegationBinder = TargetMethodAnnotationDrivenBinder.of(Collections.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>emptyList());
        assertThat(methodDelegationBinder.compile(targetMethod).bind(implementationTarget,
                sourceMethod,
                terminationHandler,
                methodInvoker,
                assigner).isValid(), is(false));
        verifyZeroInteractions(assigner);
        verifyZeroInteractions(implementationTarget);
        verifyZeroInteractions(sourceMethod);
    }

    @Test
    public void testNonAccessible() throws Exception {
        when(targetMethod.isAccessibleTo(instrumentedType)).thenReturn(false);
        when(assignmentBinding.isValid()).thenReturn(true);
        when(methodInvocation.isValid()).thenReturn(true);
        when(termination.isValid()).thenReturn(true);
        when(targetMethod.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(firstParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(secondParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        MethodDelegationBinder methodDelegationBinder = TargetMethodAnnotationDrivenBinder.of(Collections.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>emptyList());
        assertThat(methodDelegationBinder.compile(targetMethod).bind(implementationTarget,
                sourceMethod,
                terminationHandler,
                methodInvoker,
                assigner).isValid(), is(false));
        verifyZeroInteractions(terminationHandler);
        verifyZeroInteractions(assigner);
        verifyZeroInteractions(methodInvoker);
    }

    @Test
    public void testTerminationBinderMismatch() throws Exception {
        when(targetMethod.isAccessibleTo(instrumentedType)).thenReturn(true);
        when(assignmentBinding.isValid()).thenReturn(false);
        when(methodInvocation.isValid()).thenReturn(true);
        when(termination.isValid()).thenReturn(false);
        when(targetMethod.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(firstParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(secondParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        MethodDelegationBinder methodDelegationBinder = TargetMethodAnnotationDrivenBinder.of(Collections.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>emptyList());
        assertThat(methodDelegationBinder.compile(targetMethod).bind(implementationTarget,
                sourceMethod,
                terminationHandler,
                methodInvoker,
                assigner).isValid(), is(false));
        verify(terminationHandler).resolve(assigner, typing, sourceMethod, targetMethod);
        verifyNoMoreInteractions(terminationHandler);
        verifyZeroInteractions(assigner);
        verifyZeroInteractions(methodInvoker);
    }

    @Test
    public void testDoNotBindOnIllegalMethodInvocation() throws Exception {
        when(targetMethod.isAccessibleTo(instrumentedType)).thenReturn(true);
        when(assignmentBinding.isValid()).thenReturn(true);
        when(methodInvocation.isValid()).thenReturn(false);
        when(termination.isValid()).thenReturn(true);
        when(targetMethod.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(firstParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(Collections.singletonList(firstPseudoAnnotation)));
        when(secondParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(Collections.singletonList(secondPseudoAnnotation)));
        MethodDelegationBinder.ParameterBinding<?> firstBinding = prepareArgumentBinder(
                firstParameterBinder,
                FirstPseudoAnnotation.class,
                new Key(FOO));
        MethodDelegationBinder.ParameterBinding<?> secondBinding = prepareArgumentBinder(
                secondParameterBinder,
                SecondPseudoAnnotation.class,
                new Key(BAR));
        MethodDelegationBinder methodDelegationBinder = TargetMethodAnnotationDrivenBinder.of(Arrays.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>asList(firstParameterBinder, secondParameterBinder));
        MethodDelegationBinder.MethodBinding methodBinding = methodDelegationBinder.compile(targetMethod).bind(implementationTarget,
                sourceMethod,
                terminationHandler,
                methodInvoker,
                assigner);
        assertThat(methodBinding.isValid(), is(false));
        verify(firstBinding).isValid();
        verify(secondBinding).isValid();
        verify(termination).isValid();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindingByDefault() throws Exception {
        when(targetMethod.isAccessibleTo(instrumentedType)).thenReturn(true);
        when(assignmentBinding.isValid()).thenReturn(true);
        when(methodInvocation.isValid()).thenReturn(true);
        when(termination.isValid()).thenReturn(true);
        when(targetMethod.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(firstParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(secondParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(firstParameter.getType()).thenReturn(TypeDescription.Generic.OBJECT);
        when(secondParameter.getType()).thenReturn(TypeDescription.Generic.OBJECT);
        when(sourceMethod.getParameters()).thenReturn(new ParameterList.Explicit(firstParameter, secondParameter));
        MethodDelegationBinder methodDelegationBinder = TargetMethodAnnotationDrivenBinder.of(Collections.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>emptyList());
        MethodDelegationBinder.MethodBinding methodBinding = methodDelegationBinder.compile(targetMethod).bind(implementationTarget,
                sourceMethod,
                terminationHandler,
                methodInvoker,
                assigner);
        assertThat(methodBinding.isValid(), is(true));
        assertThat(methodBinding.getTarget(), is(targetMethod));
        assertThat(methodBinding.getTargetParameterIndex(new ArgumentTypeResolver.ParameterIndexToken(0)), is(0));
        assertThat(methodBinding.getTargetParameterIndex(new ArgumentTypeResolver.ParameterIndexToken(1)), is(1));
        StackManipulation.Size size = methodBinding.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(2));
        assertThat(size.getMaximalSize(), is(2));
        verify(firstParameter, atLeast(1)).getDeclaredAnnotations();
        verify(secondParameter, atLeast(1)).getDeclaredAnnotations();
        verify(targetMethod, atLeast(1)).getDeclaredAnnotations();
        verify(terminationHandler).resolve(assigner, typing, sourceMethod, targetMethod);
        verifyNoMoreInteractions(terminationHandler);
        verify(methodInvoker).invoke(targetMethod);
        verifyNoMoreInteractions(methodInvoker);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindingByParameterAnnotations() throws Exception {
        when(targetMethod.isAccessibleTo(instrumentedType)).thenReturn(true);
        when(assignmentBinding.isValid()).thenReturn(true);
        when(methodInvocation.isValid()).thenReturn(true);
        when(termination.isValid()).thenReturn(true);
        when(targetMethod.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(firstParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(Collections.singletonList(secondPseudoAnnotation)));
        when(secondParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(Collections.singletonList(firstPseudoAnnotation)));
        MethodDelegationBinder.ParameterBinding<?> firstBinding = prepareArgumentBinder(
                firstParameterBinder,
                FirstPseudoAnnotation.class,
                new Key(FOO));
        MethodDelegationBinder.ParameterBinding<?> secondBinding = prepareArgumentBinder(
                secondParameterBinder,
                SecondPseudoAnnotation.class,
                new Key(BAR));
        MethodDelegationBinder methodDelegationBinder = TargetMethodAnnotationDrivenBinder.of(Arrays.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>asList(firstParameterBinder, secondParameterBinder));
        MethodDelegationBinder.MethodBinding methodBinding = methodDelegationBinder.compile(targetMethod).bind(implementationTarget,
                sourceMethod,
                terminationHandler,
                methodInvoker,
                assigner);
        assertThat(methodBinding.isValid(), is(true));
        assertThat(methodBinding.getTarget(), is(targetMethod));
        assertThat(methodBinding.getTargetParameterIndex(new Key(FOO)), is(1));
        assertThat(methodBinding.getTargetParameterIndex(new Key(BAR)), is(0));
        StackManipulation.Size size = methodBinding.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
        verify(targetMethod, atLeast(1)).getDeclaredAnnotations();
        verify(firstParameter, atLeast(1)).getDeclaredAnnotations();
        verify(secondParameter, atLeast(1)).getDeclaredAnnotations();
        verifyNoMoreInteractions(assigner);
        verify(terminationHandler).resolve(assigner, typing, sourceMethod, targetMethod);
        verifyNoMoreInteractions(terminationHandler);
        verify(methodInvoker).invoke(targetMethod);
        verifyNoMoreInteractions(methodInvoker);
        verify(firstParameterBinder, atLeast(1)).getHandledType();
        verify((TargetMethodAnnotationDrivenBinder.ParameterBinder) firstParameterBinder).bind(firstPseudoAnnotation,
                sourceMethod,
                secondParameter,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        verifyNoMoreInteractions(firstParameterBinder);
        verify(secondParameterBinder, atLeast(1)).getHandledType();
        verify((TargetMethodAnnotationDrivenBinder.ParameterBinder) secondParameterBinder).bind(secondPseudoAnnotation,
                sourceMethod,
                firstParameter,
                implementationTarget,
                assigner,
                Assigner.Typing.STATIC);
        verifyNoMoreInteractions(secondParameterBinder);
        verify(firstBinding, atLeast(1)).isValid();
        verify(firstBinding).getIdentificationToken();
        verify(secondBinding, atLeast(1)).isValid();
        verify(secondBinding).getIdentificationToken();
    }

    @Test
    public void testAnnotation() throws Exception {
        Argument argument = new TargetMethodAnnotationDrivenBinder.DelegationProcessor.Handler.Unbound.DefaultArgument(0);
        Argument sample = (Argument) Sample.class.getDeclaredMethod(FOO, Object.class).getParameterAnnotations()[0][0];
        assertThat(argument.toString(), is(sample.toString()));
        assertThat(argument.hashCode(), is(sample.hashCode()));
        assertThat(argument, is(sample));
        assertThat(argument, is(argument));
        assertThat(argument, not(equalTo(null)));
        assertThat(argument, not(new Object()));
    }

    private interface Sample {

        void foo(@Argument(0) Object foo);
    }

    private @interface FirstPseudoAnnotation {

    }

    private @interface SecondPseudoAnnotation {

    }

    private static class Key {

        private final String value;

        private Key(String value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && value.equals(((Key) other).value);
        }
    }
}
