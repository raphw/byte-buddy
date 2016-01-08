package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.stubbing.OngoingStubbing;
import org.objectweb.asm.MethodVisitor;

import java.lang.annotation.Annotation;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TargetMethodAnnotationDrivenBinderTest {

    private static final String FOO = "foo", BAR = "bar", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TargetMethodAnnotationDrivenBinder.ParameterBinder<?> firstParameterBinder, secondParameterBinder;

    @Mock
    private TargetMethodAnnotationDrivenBinder.DefaultsProvider defaultsProvider;

    @Mock
    private TargetMethodAnnotationDrivenBinder.TerminationHandler terminationHandler;

    @Mock
    private Assigner assigner;

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
                                                                                    Object identificationToken,
                                                                                    boolean bindingResult) {
        doReturn(annotationType).when(parameterBinder).getHandledType();
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = mock(MethodDelegationBinder.ParameterBinding.class);
        when(parameterBinding.isValid()).thenReturn(bindingResult);
        when(parameterBinding.apply(any(MethodVisitor.class), any(Implementation.Context.class))).thenReturn(new StackManipulation.Size(0, 0));
        when(parameterBinding.getIdentificationToken()).thenReturn(identificationToken);
        when(((TargetMethodAnnotationDrivenBinder.ParameterBinder) parameterBinder).bind(any(AnnotationDescription.Loadable.class),
                any(MethodDescription.class),
                any(ParameterDescription.class),
                any(Implementation.Target.class),
                any(Assigner.class)))
                .thenReturn(parameterBinding);
        return parameterBinding;
    }

    @SuppressWarnings({"unchecked", "unused"})
    private static Iterator<AnnotationDescription> prepareDefaultProvider(TargetMethodAnnotationDrivenBinder.DefaultsProvider defaultsProvider,
                                                                          List<? extends AnnotationDescription> defaultIteratorValues) {
        Iterator<AnnotationDescription> annotationIterator = mock(Iterator.class);
        when(defaultsProvider.makeIterator(any(Implementation.Target.class), any(MethodDescription.class), any(MethodDescription.class)))
                .thenReturn((Iterator) annotationIterator);
        OngoingStubbing<Boolean> iteratorConditionStubbing = when(annotationIterator.hasNext());
        for (AnnotationDescription defaultIteratorValue : defaultIteratorValues) {
            iteratorConditionStubbing = iteratorConditionStubbing.thenReturn(true);
        }
        iteratorConditionStubbing.thenReturn(false);
        OngoingStubbing<AnnotationDescription> iteratorValueStubbing = when(annotationIterator.next());
        for (AnnotationDescription defaultIteratorValue : defaultIteratorValues) {
            iteratorValueStubbing = iteratorValueStubbing.thenReturn(defaultIteratorValue);
        }
        iteratorValueStubbing.thenThrow(NoSuchElementException.class);
        return annotationIterator;
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
                .thenReturn(new TypeDescription.ForLoadedType(FirstPseudoAnnotation.class));
        when(firstPseudoAnnotation.prepare(FirstPseudoAnnotation.class)).thenReturn(firstPseudoAnnotation);
        when(secondPseudoAnnotation.getAnnotationType())
                .thenReturn(new TypeDescription.ForLoadedType(SecondPseudoAnnotation.class));
        when(secondPseudoAnnotation.prepare(SecondPseudoAnnotation.class)).thenReturn(secondPseudoAnnotation);
        when(sourceTypeDescription.getStackSize()).thenReturn(StackSize.ZERO);
        when(targetTypeDescription.getStackSize()).thenReturn(StackSize.ZERO);
        when(sourceMethod.getReturnType()).thenReturn(sourceTypeDescription);
        when(targetMethod.getReturnType()).thenReturn(targetTypeDescription);
        when(terminationHandler.resolve(assigner, sourceMethod, targetMethod)).thenReturn(termination);
        when(termination.apply(any(MethodVisitor.class), any(Implementation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(implementationContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConflictingBinderBinding() throws Exception {
        doReturn(FirstPseudoAnnotation.class).when(firstParameterBinder).getHandledType();
        doReturn(FirstPseudoAnnotation.class).when(secondParameterBinder).getHandledType();
        new TargetMethodAnnotationDrivenBinder(
                Arrays.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>asList(firstParameterBinder, secondParameterBinder),
                defaultsProvider,
                terminationHandler,
                assigner,
                methodInvoker);
    }

    @Test
    public void testIgnoreForBindingAnnotation() throws Exception {
        AnnotationDescription ignoreForBinding = mock(AnnotationDescription.class);
        when(ignoreForBinding.getAnnotationType()).thenReturn(new TypeDescription.ForLoadedType(IgnoreForBinding.class));
        when(targetMethod.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(Collections.singletonList(ignoreForBinding)));
        when(termination.isValid()).thenReturn(true);
        MethodDelegationBinder methodDelegationBinder = new TargetMethodAnnotationDrivenBinder(
                Collections.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>emptyList(),
                defaultsProvider,
                terminationHandler,
                assigner,
                methodInvoker);
        assertThat(methodDelegationBinder.bind(implementationTarget, sourceMethod, targetMethod).isValid(), is(false));
        verifyZeroInteractions(assigner);
        verifyZeroInteractions(implementationTarget);
        verifyZeroInteractions(defaultsProvider);
        verifyZeroInteractions(sourceMethod);
    }

    @Test
    public void testTerminationBinderMismatch() throws Exception {
        when(assignmentBinding.isValid()).thenReturn(false);
        when(methodInvocation.isValid()).thenReturn(true);
        when(termination.isValid()).thenReturn(false);
        when(targetMethod.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        MethodDelegationBinder methodDelegationBinder = new TargetMethodAnnotationDrivenBinder(
                Collections.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>emptyList(),
                defaultsProvider,
                terminationHandler,
                assigner,
                methodInvoker);
        assertThat(methodDelegationBinder.bind(implementationTarget, sourceMethod, targetMethod).isValid(), is(false));
        verify(terminationHandler).resolve(assigner, sourceMethod, targetMethod);
        verifyNoMoreInteractions(terminationHandler);
        verifyZeroInteractions(assigner);
        verifyZeroInteractions(methodInvoker);
        verifyZeroInteractions(defaultsProvider);
    }

    @Test
    public void testDoNotBindOnIllegalMethodInvocation() throws Exception {
        when(assignmentBinding.isValid()).thenReturn(true);
        when(methodInvocation.isValid()).thenReturn(false);
        when(termination.isValid()).thenReturn(true);
        when(targetMethod.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(firstParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(Collections.singletonList(firstPseudoAnnotation)));
        when(secondParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(Collections.singletonList(secondPseudoAnnotation)));
        MethodDelegationBinder.ParameterBinding<?> firstBinding = prepareArgumentBinder(
                firstParameterBinder,
                FirstPseudoAnnotation.class,
                new Key(FOO),
                true);
        MethodDelegationBinder.ParameterBinding<?> secondBinding = prepareArgumentBinder(
                secondParameterBinder,
                SecondPseudoAnnotation.class,
                new Key(BAR),
                true);
        Iterator<AnnotationDescription> defaultsIterator = prepareDefaultProvider(defaultsProvider, Collections.<AnnotationDescription>emptyList());
        MethodDelegationBinder methodDelegationBinder = new TargetMethodAnnotationDrivenBinder(
                Arrays.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>asList(firstParameterBinder, secondParameterBinder),
                defaultsProvider,
                terminationHandler,
                assigner,
                methodInvoker);
        MethodDelegationBinder.MethodBinding methodBinding = methodDelegationBinder.bind(implementationTarget, sourceMethod, targetMethod);
        assertThat(methodBinding.isValid(), is(false));
        verify(firstBinding).isValid();
        verify(secondBinding).isValid();
        verify(termination).isValid();
        verifyZeroInteractions(defaultsIterator);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindingByDefaults() throws Exception {
        when(assignmentBinding.isValid()).thenReturn(true);
        when(methodInvocation.isValid()).thenReturn(true);
        when(termination.isValid()).thenReturn(true);
        when(targetMethod.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(firstParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(secondParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        MethodDelegationBinder.ParameterBinding<?> firstBinding = prepareArgumentBinder(
                firstParameterBinder,
                FirstPseudoAnnotation.class,
                new Key(FOO),
                true);
        MethodDelegationBinder.ParameterBinding<?> secondBinding = prepareArgumentBinder(
                secondParameterBinder,
                SecondPseudoAnnotation.class,
                new Key(BAR),
                true);
        Iterator<AnnotationDescription> defaultsIterator = prepareDefaultProvider(defaultsProvider,
                Arrays.asList(secondPseudoAnnotation, firstPseudoAnnotation));
        MethodDelegationBinder methodDelegationBinder = new TargetMethodAnnotationDrivenBinder(
                Arrays.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>asList(firstParameterBinder, secondParameterBinder),
                defaultsProvider,
                terminationHandler,
                assigner,
                methodInvoker);
        MethodDelegationBinder.MethodBinding methodBinding = methodDelegationBinder.bind(implementationTarget, sourceMethod, targetMethod);
        assertThat(methodBinding.isValid(), is(true));
        assertThat(methodBinding.getTarget(), is(targetMethod));
        assertThat(methodBinding.getTargetParameterIndex(new Key(FOO)), is(1));
        assertThat(methodBinding.getTargetParameterIndex(new Key(BAR)), is(0));
        StackManipulation.Size size = methodBinding.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
        verify(firstParameter, atLeast(1)).getDeclaredAnnotations();
        verify(secondParameter, atLeast(1)).getDeclaredAnnotations();
        verify(targetMethod, atLeast(1)).getDeclaredAnnotations();
        verifyZeroInteractions(assigner);
        verify(terminationHandler).resolve(assigner, sourceMethod, targetMethod);
        verifyNoMoreInteractions(terminationHandler);
        verify(methodInvoker).invoke(targetMethod);
        verifyNoMoreInteractions(methodInvoker);
        verify(firstParameterBinder, atLeast(1)).getHandledType();
        verify((TargetMethodAnnotationDrivenBinder.ParameterBinder) firstParameterBinder).bind(firstPseudoAnnotation,
                sourceMethod,
                secondParameter,
                implementationTarget,
                assigner);
        verifyNoMoreInteractions(firstParameterBinder);
        verify(secondParameterBinder, atLeast(1)).getHandledType();
        verify((TargetMethodAnnotationDrivenBinder.ParameterBinder) secondParameterBinder).bind(secondPseudoAnnotation,
                sourceMethod,
                firstParameter,
                implementationTarget,
                assigner);
        verifyNoMoreInteractions(secondParameterBinder);
        verify(defaultsIterator, times(2)).hasNext();
        verify(defaultsIterator, times(2)).next();
        verifyNoMoreInteractions(defaultsIterator);
        verify(firstBinding, atLeast(1)).isValid();
        verify(firstBinding).getIdentificationToken();
        verify(secondBinding, atLeast(1)).isValid();
        verify(secondBinding).getIdentificationToken();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInsufficientDefaults() throws Exception {
        when(assignmentBinding.isValid()).thenReturn(true);
        when(methodInvocation.isValid()).thenReturn(true);
        when(termination.isValid()).thenReturn(true);
        when(targetMethod.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(firstParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(secondParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        MethodDelegationBinder.ParameterBinding<?> firstBinding = prepareArgumentBinder(
                firstParameterBinder,
                FirstPseudoAnnotation.class,
                new Key(FOO),
                true);
        MethodDelegationBinder.ParameterBinding<?> secondBinding = prepareArgumentBinder(
                secondParameterBinder,
                SecondPseudoAnnotation.class,
                new Key(BAR),
                true);
        Iterator<AnnotationDescription> defaultsIterator = prepareDefaultProvider(defaultsProvider,
                Collections.singletonList(firstPseudoAnnotation));
        MethodDelegationBinder methodDelegationBinder = new TargetMethodAnnotationDrivenBinder(
                Arrays.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>asList(firstParameterBinder, secondParameterBinder),
                defaultsProvider,
                terminationHandler,
                assigner,
                methodInvoker);
        assertThat(methodDelegationBinder.bind(implementationTarget, sourceMethod, targetMethod).isValid(), is(false));
        verify(firstParameterBinder, atLeast(1)).getHandledType();
        verify((TargetMethodAnnotationDrivenBinder.ParameterBinder) firstParameterBinder).bind(firstPseudoAnnotation,
                sourceMethod,
                firstParameter,
                implementationTarget,
                assigner);
        verifyNoMoreInteractions(firstParameterBinder);
        verify(secondParameterBinder, atLeast(1)).getHandledType();
        verifyNoMoreInteractions(secondParameterBinder);
        verify(defaultsIterator, times(2)).hasNext();
        verify(defaultsIterator, times(1)).next();
        verifyNoMoreInteractions(defaultsIterator);
        verifyZeroInteractions(assigner);
        verify(terminationHandler).resolve(assigner, sourceMethod, targetMethod);
        verifyNoMoreInteractions(terminationHandler);
        verifyZeroInteractions(methodInvoker);
        verify(firstBinding, atLeast(1)).isValid();
        verify(firstBinding).getIdentificationToken();
        verifyNoMoreInteractions(firstBinding);
        verifyZeroInteractions(secondBinding);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindingByParameterAnnotations() throws Exception {
        when(assignmentBinding.isValid()).thenReturn(true);
        when(methodInvocation.isValid()).thenReturn(true);
        when(termination.isValid()).thenReturn(true);
        when(targetMethod.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(firstParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(Collections.singletonList(secondPseudoAnnotation)));
        when(secondParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(Collections.singletonList(firstPseudoAnnotation)));
        MethodDelegationBinder.ParameterBinding<?> firstBinding = prepareArgumentBinder(
                firstParameterBinder,
                FirstPseudoAnnotation.class,
                new Key(FOO),
                true);
        MethodDelegationBinder.ParameterBinding<?> secondBinding = prepareArgumentBinder(
                secondParameterBinder,
                SecondPseudoAnnotation.class,
                new Key(BAR),
                true);
        Iterator<AnnotationDescription> defaultsIterator = prepareDefaultProvider(defaultsProvider, Collections.<AnnotationDescription>emptyList());
        MethodDelegationBinder methodDelegationBinder = new TargetMethodAnnotationDrivenBinder(
                Arrays.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>asList(firstParameterBinder, secondParameterBinder),
                defaultsProvider,
                terminationHandler,
                assigner,
                methodInvoker);
        MethodDelegationBinder.MethodBinding methodBinding = methodDelegationBinder.bind(implementationTarget, sourceMethod, targetMethod);
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
        verify(terminationHandler).resolve(assigner, sourceMethod, targetMethod);
        verifyNoMoreInteractions(terminationHandler);
        verify(methodInvoker).invoke(targetMethod);
        verifyNoMoreInteractions(methodInvoker);
        verify(firstParameterBinder, atLeast(1)).getHandledType();
        verify((TargetMethodAnnotationDrivenBinder.ParameterBinder) firstParameterBinder).bind(firstPseudoAnnotation,
                sourceMethod,
                secondParameter,
                implementationTarget,
                assigner);
        verifyNoMoreInteractions(firstParameterBinder);
        verify(secondParameterBinder, atLeast(1)).getHandledType();
        verify((TargetMethodAnnotationDrivenBinder.ParameterBinder) secondParameterBinder).bind(secondPseudoAnnotation,
                sourceMethod,
                firstParameter,
                implementationTarget,
                assigner);
        verifyNoMoreInteractions(secondParameterBinder);
        verifyZeroInteractions(defaultsIterator);
        verify(firstBinding, atLeast(1)).isValid();
        verify(firstBinding).getIdentificationToken();
        verify(secondBinding, atLeast(1)).isValid();
        verify(secondBinding).getIdentificationToken();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindingByParameterAnnotationsAndDefaults() throws Exception {
        when(assignmentBinding.isValid()).thenReturn(true);
        when(methodInvocation.isValid()).thenReturn(true);
        when(termination.isValid()).thenReturn(true);
        when(targetMethod.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(firstParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(secondParameter.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(Collections.singletonList(firstPseudoAnnotation)));
        MethodDelegationBinder.ParameterBinding<?> firstBinding = prepareArgumentBinder(
                firstParameterBinder,
                FirstPseudoAnnotation.class,
                new Key(FOO),
                true);
        MethodDelegationBinder.ParameterBinding<?> secondBinding = prepareArgumentBinder(
                secondParameterBinder,
                SecondPseudoAnnotation.class,
                new Key(BAR),
                true);
        Iterator<AnnotationDescription> defaultsIterator = prepareDefaultProvider(defaultsProvider,
                Collections.singletonList(secondPseudoAnnotation));
        MethodDelegationBinder methodDelegationBinder = new TargetMethodAnnotationDrivenBinder(
                Arrays.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>asList(firstParameterBinder, secondParameterBinder),
                defaultsProvider,
                terminationHandler,
                assigner,
                methodInvoker);
        MethodDelegationBinder.MethodBinding methodBinding = methodDelegationBinder.bind(implementationTarget, sourceMethod, targetMethod);
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
        verifyZeroInteractions(assigner);
        verify(terminationHandler).resolve(assigner, sourceMethod, targetMethod);
        verifyNoMoreInteractions(terminationHandler);
        verify(methodInvoker).invoke(targetMethod);
        verifyNoMoreInteractions(methodInvoker);
        verify(firstParameterBinder, atLeast(1)).getHandledType();
        verify((TargetMethodAnnotationDrivenBinder.ParameterBinder) firstParameterBinder).bind(firstPseudoAnnotation,
                sourceMethod,
                secondParameter,
                implementationTarget,
                assigner);
        verifyNoMoreInteractions(firstParameterBinder);
        verify(secondParameterBinder, atLeast(1)).getHandledType();
        verify((TargetMethodAnnotationDrivenBinder.ParameterBinder) secondParameterBinder).bind(secondPseudoAnnotation,
                sourceMethod,
                firstParameter,
                implementationTarget,
                assigner);
        verifyNoMoreInteractions(secondParameterBinder);
        verify(defaultsIterator).hasNext();
        verify(defaultsIterator).next();
        verifyNoMoreInteractions(defaultsIterator);
        verify(firstBinding, atLeast(1)).isValid();
        verify(firstBinding).getIdentificationToken();
        verify(secondBinding, atLeast(1)).isValid();
        verify(secondBinding).getIdentificationToken();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TargetMethodAnnotationDrivenBinder.class).refine(new ObjectPropertyAssertion.Refinement<TargetMethodAnnotationDrivenBinder.ParameterBinder>() {
            @Override
            public void apply(TargetMethodAnnotationDrivenBinder.ParameterBinder mock) {
                when(mock.getHandledType()).thenReturn(Annotation.class);
            }
        }).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                when(mock.getStackSize()).thenReturn(StackSize.ZERO);
            }
        }).create(new ObjectPropertyAssertion.Creator<List<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>>() {
            @Override
            public List<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> create() {
                TargetMethodAnnotationDrivenBinder.ParameterBinder<?> parameterBinder = mock(TargetMethodAnnotationDrivenBinder.ParameterBinder.class);
                doReturn(Annotation.class).when(parameterBinder).getHandledType();
                return Collections.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>singletonList(parameterBinder);
            }
        }).apply();
        ObjectPropertyAssertion.of(TargetMethodAnnotationDrivenBinder.DefaultsProvider.Empty.class).apply();
        ObjectPropertyAssertion.of(TargetMethodAnnotationDrivenBinder.DefaultsProvider.Empty.EmptyIterator.class).apply();
        ObjectPropertyAssertion.of(TargetMethodAnnotationDrivenBinder.TerminationHandler.Dropping.class).apply();
        ObjectPropertyAssertion.of(TargetMethodAnnotationDrivenBinder.TerminationHandler.Returning.class).apply();
        ObjectPropertyAssertion.of(TargetMethodAnnotationDrivenBinder.DelegationProcessor.class).apply();
        ObjectPropertyAssertion.of(TargetMethodAnnotationDrivenBinder.DelegationProcessor.Handler.Bound.class).apply();
        ObjectPropertyAssertion.of(TargetMethodAnnotationDrivenBinder.DelegationProcessor.Handler.Unbound.class).apply();
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
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && value.equals(((Key) other).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }
}
