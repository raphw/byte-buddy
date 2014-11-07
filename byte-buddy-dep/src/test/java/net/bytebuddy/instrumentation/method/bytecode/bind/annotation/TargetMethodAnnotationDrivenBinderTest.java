package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.MockitoRule;
import net.bytebuddy.utility.ObjectPropertyAssertion;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyBoolean;
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
    private Instrumentation.Target instrumentationTarget;
    @Mock
    private MethodDescription source, target;
    @Mock
    private TypeDescription sourceTypeDescription, targetTypeDescription;
    @Mock
    private AnnotationDescription.ForLoadedAnnotation<FirstPseudoAnnotation> firstPseudoAnnotation;
    @Mock
    private AnnotationDescription.ForLoadedAnnotation<SecondPseudoAnnotation> secondPseudoAnnotation;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;
    @Mock
    private AnnotationList annotationList;

    @SuppressWarnings("unchecked")
    private static MethodDelegationBinder.ParameterBinding<?> prepareArgumentBinder(TargetMethodAnnotationDrivenBinder.ParameterBinder<?> parameterBinder,
                                                                                    Class<? extends Annotation> annotationType,
                                                                                    Object identificationToken,
                                                                                    boolean bindingResult) {
        doReturn(annotationType).when(parameterBinder).getHandledType();
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = mock(MethodDelegationBinder.ParameterBinding.class);
        when(parameterBinding.isValid()).thenReturn(bindingResult);
        when(parameterBinding.apply(any(MethodVisitor.class), any(Instrumentation.Context.class))).thenReturn(new StackManipulation.Size(0, 0));
        when(parameterBinding.getIdentificationToken()).thenReturn(identificationToken);
        when(((TargetMethodAnnotationDrivenBinder.ParameterBinder) parameterBinder).bind(any(AnnotationDescription.Loadable.class),
                anyInt(),
                any(MethodDescription.class),
                any(MethodDescription.class),
                any(Instrumentation.Target.class),
                any(Assigner.class)))
                .thenReturn(parameterBinding);
        return parameterBinding;
    }

    @SuppressWarnings({"unchecked", "unused"})
    private static Iterator<AnnotationDescription> prepareDefaultProvider(TargetMethodAnnotationDrivenBinder.DefaultsProvider defaultsProvider,
                                                                          List<? extends AnnotationDescription> defaultIteratorValues) {
        Iterator<AnnotationDescription> annotationIterator = mock(Iterator.class);
        when(defaultsProvider.makeIterator(any(Instrumentation.Target.class), any(MethodDescription.class), any(MethodDescription.class)))
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
    public void setUp() throws Exception {
        when(assignmentBinding.apply(any(MethodVisitor.class), any(Instrumentation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(assigner.assign(any(TypeDescription.class), any(TypeDescription.class), anyBoolean()))
                .thenReturn(assignmentBinding);
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(methodInvocation);
        when(methodInvocation.apply(any(MethodVisitor.class), any(Instrumentation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(assignmentBinding.apply(any(MethodVisitor.class), any(Instrumentation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        TypeDescription declaringType = mock(TypeDescription.class);
        when(declaringType.getInternalName()).thenReturn(FOO);
        when(declaringType.isInterface()).thenReturn(false);
        when(target.getDeclaringType()).thenReturn(declaringType);
        when(target.getInternalName()).thenReturn(BAR);
        when(target.getDescriptor()).thenReturn(BAZ);
        when(target.isStatic()).thenReturn(true);
        when(firstPseudoAnnotation.getAnnotationType())
                .thenReturn(new TypeDescription.ForLoadedType(FirstPseudoAnnotation.class));
        when(firstPseudoAnnotation.prepare(FirstPseudoAnnotation.class)).thenReturn(firstPseudoAnnotation);
        when(secondPseudoAnnotation.getAnnotationType())
                .thenReturn(new TypeDescription.ForLoadedType(SecondPseudoAnnotation.class));
        when(secondPseudoAnnotation.prepare(SecondPseudoAnnotation.class)).thenReturn(secondPseudoAnnotation);
        when(sourceTypeDescription.getStackSize()).thenReturn(StackSize.ZERO);
        when(targetTypeDescription.getStackSize()).thenReturn(StackSize.ZERO);
        when(source.getReturnType()).thenReturn(sourceTypeDescription);
        when(target.getReturnType()).thenReturn(targetTypeDescription);
        when(terminationHandler.resolve(assigner, source, target)).thenReturn(termination);
        when(termination.apply(any(MethodVisitor.class), any(Instrumentation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(instrumentationContext);
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
        when(target.getDeclaredAnnotations()).thenReturn(annotationList);
        when(termination.isValid()).thenReturn(true);
        when(annotationList.isAnnotationPresent(IgnoreForBinding.class)).thenReturn(true);
        MethodDelegationBinder methodDelegationBinder = new TargetMethodAnnotationDrivenBinder(
                Collections.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>emptyList(),
                defaultsProvider,
                terminationHandler,
                assigner,
                methodInvoker);
        assertThat(methodDelegationBinder.bind(instrumentationTarget, source, target).isValid(), is(false));
        verifyZeroInteractions(assigner);
        verifyZeroInteractions(instrumentationTarget);
        verifyZeroInteractions(defaultsProvider);
        verifyZeroInteractions(source);
    }

    @Test
    public void testTerminationBinderMismatch() throws Exception {
        when(assignmentBinding.isValid()).thenReturn(false);
        when(methodInvocation.isValid()).thenReturn(true);
        when(termination.isValid()).thenReturn(false);
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        MethodDelegationBinder methodDelegationBinder = new TargetMethodAnnotationDrivenBinder(
                Collections.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>emptyList(),
                defaultsProvider,
                terminationHandler,
                assigner,
                methodInvoker);
        assertThat(methodDelegationBinder.bind(instrumentationTarget, source, target).isValid(), is(false));
        verify(terminationHandler).resolve(assigner, source, target);
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
        TypeList typeList = mock(TypeList.class);
        when(typeList.size()).thenReturn(0);
        when(target.getParameterTypes()).thenReturn(typeList);
        when(target.getStackSize()).thenReturn(0);
        when(target.getParameterAnnotations()).thenReturn(AnnotationList.Empty.asList(2));
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        MethodDelegationBinder.ParameterBinding<?> firstBinding = prepareArgumentBinder(
                firstParameterBinder,
                FirstPseudoAnnotation.class,
                new Key(FOO),
                true);
        Iterator<AnnotationDescription> defaultsIterator = prepareDefaultProvider(defaultsProvider, Collections.<AnnotationDescription>emptyList());
        MethodDelegationBinder methodDelegationBinder = new TargetMethodAnnotationDrivenBinder(
                Arrays.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>asList(firstParameterBinder),
                defaultsProvider,
                terminationHandler,
                assigner,
                methodInvoker);
        MethodDelegationBinder.MethodBinding methodBinding = methodDelegationBinder.bind(instrumentationTarget, source, target);
        assertThat(methodBinding.isValid(), is(false));
        assertThat(methodBinding.getTarget(), is(target));
        verifyZeroInteractions(firstBinding);
        verifyZeroInteractions(defaultsIterator);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindingByDefaults() throws Exception {
        when(assignmentBinding.isValid()).thenReturn(true);
        when(methodInvocation.isValid()).thenReturn(true);
        when(termination.isValid()).thenReturn(true);
        TypeList typeList = mock(TypeList.class);
        when(typeList.size()).thenReturn(2);
        when(target.getParameterTypes()).thenReturn(typeList);
        when(target.getStackSize()).thenReturn(3);
        when(target.getParameterAnnotations()).thenReturn(AnnotationList.Empty.asList(2));
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
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
        MethodDelegationBinder.MethodBinding methodBinding = methodDelegationBinder.bind(instrumentationTarget, source, target);
        assertThat(methodBinding.isValid(), is(true));
        assertThat(methodBinding.getTarget(), is(target));
        assertThat(methodBinding.getTargetParameterIndex(new Key(FOO)), is(1));
        assertThat(methodBinding.getTargetParameterIndex(new Key(BAR)), is(0));
        StackManipulation.Size size = methodBinding.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(target, atLeast(1)).getDeclaredAnnotations();
        verifyZeroInteractions(assigner);
        verify(terminationHandler).resolve(assigner, source, target);
        verifyNoMoreInteractions(terminationHandler);
        verify(methodInvoker).invoke(target);
        verifyNoMoreInteractions(methodInvoker);
        verify(firstParameterBinder, atLeast(1)).getHandledType();
        verify((TargetMethodAnnotationDrivenBinder.ParameterBinder) firstParameterBinder).bind(firstPseudoAnnotation,
                1,
                source,
                target,
                instrumentationTarget,
                assigner);
        verifyNoMoreInteractions(firstParameterBinder);
        verify(secondParameterBinder, atLeast(1)).getHandledType();
        verify((TargetMethodAnnotationDrivenBinder.ParameterBinder) secondParameterBinder).bind(secondPseudoAnnotation,
                0,
                source,
                target,
                instrumentationTarget,
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
        TypeList typeList = mock(TypeList.class);
        when(typeList.size()).thenReturn(2);
        when(target.getParameterTypes()).thenReturn(typeList);
        when(target.getStackSize()).thenReturn(3);
        when(target.getParameterAnnotations()).thenReturn(AnnotationList.Empty.asList(2));
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
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
                Arrays.asList(secondPseudoAnnotation));
        MethodDelegationBinder methodDelegationBinder = new TargetMethodAnnotationDrivenBinder(
                Arrays.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>asList(firstParameterBinder, secondParameterBinder),
                defaultsProvider,
                terminationHandler,
                assigner,
                methodInvoker);
        assertThat(methodDelegationBinder.bind(instrumentationTarget, source, target).isValid(), is(false));
        verify(firstParameterBinder, atLeast(1)).getHandledType();
        verifyNoMoreInteractions(firstParameterBinder);
        verify(secondParameterBinder, atLeast(1)).getHandledType();
        verify((TargetMethodAnnotationDrivenBinder.ParameterBinder) secondParameterBinder).bind(secondPseudoAnnotation,
                0,
                source,
                target,
                instrumentationTarget,
                assigner);
        verifyNoMoreInteractions(secondParameterBinder);
        verify(defaultsIterator, times(2)).hasNext();
        verify(defaultsIterator, times(1)).next();
        verifyNoMoreInteractions(defaultsIterator);
        verifyZeroInteractions(assigner);
        verify(terminationHandler).resolve(assigner, source, target);
        verifyNoMoreInteractions(terminationHandler);
        verifyZeroInteractions(methodInvoker);
        verifyZeroInteractions(firstBinding);
        verify(secondBinding, atLeast(1)).isValid();
        verify(secondBinding).getIdentificationToken();
        verifyNoMoreInteractions(secondBinding);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindingByParameterAnnotations() throws Exception {
        when(assignmentBinding.isValid()).thenReturn(true);
        when(methodInvocation.isValid()).thenReturn(true);
        when(termination.isValid()).thenReturn(true);
        TypeList typeList = mock(TypeList.class);
        when(typeList.size()).thenReturn(2);
        when(target.getParameterTypes()).thenReturn(typeList);
        when(target.getStackSize()).thenReturn(3);
        when(target.getParameterAnnotations()).thenReturn(AnnotationList.Explicit.asList(Arrays.asList(Arrays.asList(secondPseudoAnnotation),
                Arrays.asList(firstPseudoAnnotation))));
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
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
        MethodDelegationBinder.MethodBinding methodBinding = methodDelegationBinder.bind(instrumentationTarget, source, target);
        assertThat(methodBinding.isValid(), is(true));
        assertThat(methodBinding.getTarget(), is(target));
        assertThat(methodBinding.getTargetParameterIndex(new Key(FOO)), is(1));
        assertThat(methodBinding.getTargetParameterIndex(new Key(BAR)), is(0));
        StackManipulation.Size size = methodBinding.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(target, atLeast(1)).getDeclaredAnnotations();
        verifyNoMoreInteractions(assigner);
        verify(terminationHandler).resolve(assigner, source, target);
        verifyNoMoreInteractions(terminationHandler);
        verify(methodInvoker).invoke(target);
        verifyNoMoreInteractions(methodInvoker);
        verify(firstParameterBinder, atLeast(1)).getHandledType();
        verify((TargetMethodAnnotationDrivenBinder.ParameterBinder) firstParameterBinder).bind(firstPseudoAnnotation,
                1,
                source,
                target,
                instrumentationTarget,
                assigner);
        verifyNoMoreInteractions(firstParameterBinder);
        verify(secondParameterBinder, atLeast(1)).getHandledType();
        verify((TargetMethodAnnotationDrivenBinder.ParameterBinder) secondParameterBinder).bind(secondPseudoAnnotation,
                0,
                source,
                target,
                instrumentationTarget,
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
        TypeList typeList = mock(TypeList.class);
        when(typeList.size()).thenReturn(2);
        when(target.getParameterTypes()).thenReturn(typeList);
        when(target.getStackSize()).thenReturn(3);
        when(target.getParameterAnnotations()).thenReturn(AnnotationList.Explicit.asList(Arrays.asList(Collections.<AnnotationDescription>emptyList(),
                Arrays.asList(firstPseudoAnnotation))));
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
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
        MethodDelegationBinder.MethodBinding methodBinding = methodDelegationBinder.bind(instrumentationTarget, source, target);
        assertThat(methodBinding.isValid(), is(true));
        assertThat(methodBinding.getTarget(), is(target));
        assertThat(methodBinding.getTargetParameterIndex(new Key(FOO)), is(1));
        assertThat(methodBinding.getTargetParameterIndex(new Key(BAR)), is(0));
        StackManipulation.Size size = methodBinding.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(target, atLeast(1)).getDeclaredAnnotations();
        verifyZeroInteractions(assigner);
        verify(terminationHandler).resolve(assigner, source, target);
        verifyNoMoreInteractions(terminationHandler);
        verify(methodInvoker).invoke(target);
        verifyNoMoreInteractions(methodInvoker);
        verify(firstParameterBinder, atLeast(1)).getHandledType();
        verify((TargetMethodAnnotationDrivenBinder.ParameterBinder) firstParameterBinder).bind(firstPseudoAnnotation,
                1,
                source,
                target,
                instrumentationTarget,
                assigner);
        verifyNoMoreInteractions(firstParameterBinder);
        verify(secondParameterBinder, atLeast(1)).getHandledType();
        verify((TargetMethodAnnotationDrivenBinder.ParameterBinder) secondParameterBinder).bind(secondPseudoAnnotation,
                0,
                source,
                target,
                instrumentationTarget,
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
                return Arrays.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>asList(parameterBinder);
            }
        }).apply();
    }

    private static @interface FirstPseudoAnnotation {
    }

    private static @interface SecondPseudoAnnotation {
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
