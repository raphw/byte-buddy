package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.IllegalAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.LegalTrivialAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.MethodDelegationBinder;
import com.blogspot.mydailyjava.bytebuddy.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

public class AnnotationDrivenBinderTest {

    private static final String FOO = "foo", BAR = "bar", BAZ = "baz";

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

    private static @interface FirstPseudoAnnotation {
    }

    private static @interface SecondPseudoAnnotation {
    }

    private AnnotationDrivenBinder.ArgumentBinder<?> firstArgumentBinder, secondArgumentBinder;
    private AnnotationDrivenBinder.DefaultProvider<?> defaultProvider;
    private Assigner assigner;

    private TypeDescription typeDescription;
    private MethodDescription source, target;

    private FirstPseudoAnnotation firstPseudoAnnotation;
    private SecondPseudoAnnotation secondPseudoAnnotation;

    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        firstArgumentBinder = mock(AnnotationDrivenBinder.ArgumentBinder.class);
        secondArgumentBinder = mock(AnnotationDrivenBinder.ArgumentBinder.class);
        defaultProvider = mock(AnnotationDrivenBinder.DefaultProvider.class);
        assigner = mock(Assigner.class);
        typeDescription = mock(TypeDescription.class);
        source = mock(MethodDescription.class);
        target = mock(MethodDescription.class);
        when(target.getDeclaringClassInternalName()).thenReturn(FOO);
        when(target.getInternalName()).thenReturn(BAR);
        when(target.getDescriptor()).thenReturn(BAZ);
        when(target.isStatic()).thenReturn(true);
        when(target.isInterfaceMethod()).thenReturn(false);
        firstPseudoAnnotation = mock(FirstPseudoAnnotation.class);
        doReturn(FirstPseudoAnnotation.class).when(firstPseudoAnnotation).annotationType();
        secondPseudoAnnotation = mock(SecondPseudoAnnotation.class);
        doReturn(SecondPseudoAnnotation.class).when(secondPseudoAnnotation).annotationType();
        methodVisitor = mock(MethodVisitor.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConflictingBinderBinding() throws Exception {
        doReturn(FirstPseudoAnnotation.class).when(firstArgumentBinder).getHandledType();
        doReturn(FirstPseudoAnnotation.class).when(secondArgumentBinder).getHandledType();
        new AnnotationDrivenBinder(
                Arrays.<AnnotationDrivenBinder.ArgumentBinder<?>>asList(firstArgumentBinder, secondArgumentBinder),
                defaultProvider,
                assigner);
    }

    @Test
    public void testReturnTypeMismatchNoRuntimeType() throws Exception {
        when(assigner.assign(any(Class.class), any(Class.class), anyBoolean())).thenReturn(IllegalAssignment.INSTANCE);
        doReturn(Object.class).when(source).getReturnType();
        doReturn(Void.class).when(target).getReturnType();
        when(target.getAnnotations()).thenReturn(new Annotation[0]);
        MethodDelegationBinder methodDelegationBinder = new AnnotationDrivenBinder(
                Collections.<AnnotationDrivenBinder.ArgumentBinder<?>>emptyList(),
                defaultProvider,
                assigner);
        assertThat(methodDelegationBinder.bind(typeDescription, source, target).isValid(), is(false));
        verify(assigner).assign(Void.class, Object.class, false);
        verifyNoMoreInteractions(assigner);
        verify(source, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).getReturnType();
        verifyZeroInteractions(defaultProvider);
    }

    @Test
    public void testReturnTypeMismatchRuntimeType() throws Exception {
        when(assigner.assign(any(Class.class), any(Class.class), anyBoolean())).thenReturn(IllegalAssignment.INSTANCE);
        doReturn(Object.class).when(source).getReturnType();
        doReturn(Void.class).when(target).getReturnType();
        RuntimeType runtimeType = mock(RuntimeType.class);
        doReturn(RuntimeType.class).when(runtimeType).annotationType();
        when(target.getAnnotations()).thenReturn(new Annotation[]{runtimeType});
        MethodDelegationBinder methodDelegationBinder = new AnnotationDrivenBinder(
                Collections.<AnnotationDrivenBinder.ArgumentBinder<?>>emptyList(),
                defaultProvider,
                assigner);
        assertThat(methodDelegationBinder.bind(typeDescription, source, target).isValid(), is(false));
        verify(assigner).assign(Void.class, Object.class, true);
        verifyNoMoreInteractions(assigner);
        verify(source, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).getReturnType();
        verifyZeroInteractions(defaultProvider);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindingByDefaults() throws Exception {
        when(assigner.assign(any(Class.class), any(Class.class), anyBoolean())).thenReturn(LegalTrivialAssignment.INSTANCE);
        doReturn(Object.class).when(source).getReturnType();
        doReturn(Void.class).when(target).getReturnType();
        when(target.getParameterTypes()).thenReturn(new Class<?>[]{int.class, long.class});
        when(target.getParameterAnnotations()).thenReturn(new Annotation[2][0]);
        when(target.getAnnotations()).thenReturn(new Annotation[0]);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> firstBinding = prepareArgumentBinder(
                firstArgumentBinder,
                FirstPseudoAnnotation.class,
                new Key(FOO),
                true);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> secondBinding = prepareArgumentBinder(
                secondArgumentBinder,
                SecondPseudoAnnotation.class,
                new Key(BAR),
                true);
        Iterator<Annotation> defaultsIterator = prepareDefaultProvider(defaultProvider, Arrays.asList(secondPseudoAnnotation, firstPseudoAnnotation));
        MethodDelegationBinder methodDelegationBinder = new AnnotationDrivenBinder(
                Arrays.<AnnotationDrivenBinder.ArgumentBinder<?>>asList(firstArgumentBinder, secondArgumentBinder),
                defaultProvider,
                assigner);
        MethodDelegationBinder.Binding binding = methodDelegationBinder.bind(typeDescription, source, target);
        assertThat(binding.isValid(), is(true));
        assertThat(binding.getTarget(), is(target));
        assertThat(binding.getTargetParameterIndex(new Key(FOO)), is(1));
        assertThat(binding.getTargetParameterIndex(new Key(BAR)), is(0));
        Assignment.Size size = binding.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-2));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC, FOO, BAR, BAZ);
        verifyNoMoreInteractions(methodVisitor);
        verify(source, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(target, atLeast(1)).getAnnotations();
        verify(assigner).assign(Void.class, Object.class, false);
        verifyNoMoreInteractions(assigner);
        verify(firstArgumentBinder, atLeast(1)).getHandledType();
        verify((AnnotationDrivenBinder.ArgumentBinder) firstArgumentBinder).bind(firstPseudoAnnotation, 1, source, target, typeDescription, assigner);
        verifyNoMoreInteractions(firstArgumentBinder);
        verify(secondArgumentBinder, atLeast(1)).getHandledType();
        verify((AnnotationDrivenBinder.ArgumentBinder) secondArgumentBinder).bind(secondPseudoAnnotation, 0, source, target, typeDescription, assigner);
        verifyNoMoreInteractions(secondArgumentBinder);
        verify(defaultsIterator, times(2)).hasNext();
        verify(defaultsIterator, times(2)).next();
        verifyNoMoreInteractions(defaultsIterator);
        verify(firstBinding, atLeast(1)).isValid();
        verify(firstBinding).getAssignment();
        verify(firstBinding).getIdentificationToken();
        verify(secondBinding, atLeast(1)).isValid();
        verify(secondBinding).getAssignment();
        verify(secondBinding).getIdentificationToken();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInsufficientDefaults() throws Exception {
        when(assigner.assign(any(Class.class), any(Class.class), anyBoolean())).thenReturn(LegalTrivialAssignment.INSTANCE);
        doReturn(Object.class).when(source).getReturnType();
        doReturn(Void.class).when(target).getReturnType();
        when(target.getParameterTypes()).thenReturn(new Class<?>[]{int.class, long.class});
        when(target.getParameterAnnotations()).thenReturn(new Annotation[2][0]);
        when(target.getAnnotations()).thenReturn(new Annotation[0]);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> firstBinding = prepareArgumentBinder(
                firstArgumentBinder,
                FirstPseudoAnnotation.class,
                new Key(FOO),
                true);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> secondBinding = prepareArgumentBinder(
                secondArgumentBinder,
                SecondPseudoAnnotation.class,
                new Key(BAR),
                true);
        Iterator<Annotation> defaultsIterator = prepareDefaultProvider(defaultProvider, Arrays.asList(secondPseudoAnnotation));
        MethodDelegationBinder methodDelegationBinder = new AnnotationDrivenBinder(
                Arrays.<AnnotationDrivenBinder.ArgumentBinder<?>>asList(firstArgumentBinder, secondArgumentBinder),
                defaultProvider,
                assigner);
        assertThat(methodDelegationBinder.bind(typeDescription, source, target).isValid(), is(false));
        verify(firstArgumentBinder, atLeast(1)).getHandledType();
        verifyNoMoreInteractions(firstArgumentBinder);
        verify(secondArgumentBinder, atLeast(1)).getHandledType();
        verify((AnnotationDrivenBinder.ArgumentBinder) secondArgumentBinder).bind(secondPseudoAnnotation, 0, source, target, typeDescription, assigner);
        verifyNoMoreInteractions(secondArgumentBinder);
        verify(defaultsIterator, times(2)).hasNext();
        verify(defaultsIterator, times(1)).next();
        verifyNoMoreInteractions(defaultsIterator);
        verify(assigner).assign(Void.class, Object.class, false);
        verifyNoMoreInteractions(assigner);
        verifyZeroInteractions(firstBinding);
        verify(secondBinding, atLeast(1)).isValid();
        verify(secondBinding).getIdentificationToken();
        verify(secondBinding).getAssignment();
        verifyNoMoreInteractions(secondBinding);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindingByParameterAnnotations() throws Exception {
        when(assigner.assign(any(Class.class), any(Class.class), anyBoolean())).thenReturn(LegalTrivialAssignment.INSTANCE);
        doReturn(Object.class).when(source).getReturnType();
        doReturn(Void.class).when(target).getReturnType();
        when(target.getParameterTypes()).thenReturn(new Class<?>[]{int.class, long.class});
        when(target.getParameterAnnotations()).thenReturn(new Annotation[][]{{secondPseudoAnnotation}, {firstPseudoAnnotation}});
        when(target.getAnnotations()).thenReturn(new Annotation[0]);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> firstBinding = prepareArgumentBinder(
                firstArgumentBinder,
                FirstPseudoAnnotation.class,
                new Key(FOO),
                true);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> secondBinding = prepareArgumentBinder(
                secondArgumentBinder,
                SecondPseudoAnnotation.class,
                new Key(BAR),
                true);
        Iterator<Annotation> defaultsIterator = prepareDefaultProvider(defaultProvider, Collections.<Annotation>emptyList());
        MethodDelegationBinder methodDelegationBinder = new AnnotationDrivenBinder(
                Arrays.<AnnotationDrivenBinder.ArgumentBinder<?>>asList(firstArgumentBinder, secondArgumentBinder),
                defaultProvider,
                assigner);
        MethodDelegationBinder.Binding binding = methodDelegationBinder.bind(typeDescription, source, target);
        assertThat(binding.isValid(), is(true));
        assertThat(binding.getTarget(), is(target));
        assertThat(binding.getTargetParameterIndex(new Key(FOO)), is(1));
        assertThat(binding.getTargetParameterIndex(new Key(BAR)), is(0));
        Assignment.Size size = binding.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-2));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC, FOO, BAR, BAZ);
        verifyNoMoreInteractions(methodVisitor);
        verify(source, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(target, atLeast(1)).getAnnotations();
        verify(assigner).assign(Void.class, Object.class, false);
        verifyNoMoreInteractions(assigner);
        verify(firstArgumentBinder, atLeast(1)).getHandledType();
        verify((AnnotationDrivenBinder.ArgumentBinder) firstArgumentBinder).bind(firstPseudoAnnotation, 1, source, target, typeDescription, assigner);
        verifyNoMoreInteractions(firstArgumentBinder);
        verify(secondArgumentBinder, atLeast(1)).getHandledType();
        verify((AnnotationDrivenBinder.ArgumentBinder) secondArgumentBinder).bind(secondPseudoAnnotation, 0, source, target, typeDescription, assigner);
        verifyNoMoreInteractions(secondArgumentBinder);
        verifyZeroInteractions(defaultsIterator);
        verify(firstBinding, atLeast(1)).isValid();
        verify(firstBinding).getAssignment();
        verify(firstBinding).getIdentificationToken();
        verify(secondBinding, atLeast(1)).isValid();
        verify(secondBinding).getAssignment();
        verify(secondBinding).getIdentificationToken();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindingByParameterAnnotationsAndDefaults() throws Exception {
        when(assigner.assign(any(Class.class), any(Class.class), anyBoolean())).thenReturn(LegalTrivialAssignment.INSTANCE);
        doReturn(Object.class).when(source).getReturnType();
        doReturn(Void.class).when(target).getReturnType();
        when(target.getParameterTypes()).thenReturn(new Class<?>[]{int.class, long.class});
        when(target.getParameterAnnotations()).thenReturn(new Annotation[][]{{}, {firstPseudoAnnotation}});
        when(target.getAnnotations()).thenReturn(new Annotation[0]);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> firstBinding = prepareArgumentBinder(
                firstArgumentBinder,
                FirstPseudoAnnotation.class,
                new Key(FOO),
                true);
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> secondBinding = prepareArgumentBinder(
                secondArgumentBinder,
                SecondPseudoAnnotation.class,
                new Key(BAR),
                true);
        Iterator<Annotation> defaultsIterator = prepareDefaultProvider(defaultProvider, Collections.singletonList(secondPseudoAnnotation));
        MethodDelegationBinder methodDelegationBinder = new AnnotationDrivenBinder(
                Arrays.<AnnotationDrivenBinder.ArgumentBinder<?>>asList(firstArgumentBinder, secondArgumentBinder),
                defaultProvider,
                assigner);
        MethodDelegationBinder.Binding binding = methodDelegationBinder.bind(typeDescription, source, target);
        assertThat(binding.isValid(), is(true));
        assertThat(binding.getTarget(), is(target));
        assertThat(binding.getTargetParameterIndex(new Key(FOO)), is(1));
        assertThat(binding.getTargetParameterIndex(new Key(BAR)), is(0));
        Assignment.Size size = binding.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(-2));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC, FOO, BAR, BAZ);
        verifyNoMoreInteractions(methodVisitor);
        verify(source, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).getReturnType();
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(target, atLeast(1)).getAnnotations();
        verify(assigner).assign(Void.class, Object.class, false);
        verifyNoMoreInteractions(assigner);
        verify(firstArgumentBinder, atLeast(1)).getHandledType();
        verify((AnnotationDrivenBinder.ArgumentBinder) firstArgumentBinder).bind(firstPseudoAnnotation, 1, source, target, typeDescription, assigner);
        verifyNoMoreInteractions(firstArgumentBinder);
        verify(secondArgumentBinder, atLeast(1)).getHandledType();
        verify((AnnotationDrivenBinder.ArgumentBinder) secondArgumentBinder).bind(secondPseudoAnnotation, 0, source, target, typeDescription, assigner);
        verifyNoMoreInteractions(secondArgumentBinder);
        verify(defaultsIterator).hasNext();
        verify(defaultsIterator).next();
        verifyNoMoreInteractions(defaultsIterator);
        verify(firstBinding, atLeast(1)).isValid();
        verify(firstBinding).getAssignment();
        verify(firstBinding).getIdentificationToken();
        verify(secondBinding, atLeast(1)).isValid();
        verify(secondBinding).getAssignment();
        verify(secondBinding).getIdentificationToken();
    }

    @SuppressWarnings("unchecked")
    private static AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> prepareArgumentBinder(AnnotationDrivenBinder.ArgumentBinder<?> argumentBinder,
                                                                                                    Class<? extends Annotation> annotationType,
                                                                                                    Object identificationToken,
                                                                                                    boolean bindingResult) {
        doReturn(annotationType).when(argumentBinder).getHandledType();
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding = mock(AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding.class);
        when(identifiedBinding.isValid()).thenReturn(bindingResult);
        when(identifiedBinding.getIdentificationToken()).thenReturn(identificationToken);
        when(identifiedBinding.getAssignment()).thenReturn(bindingResult ? LegalTrivialAssignment.INSTANCE : IllegalAssignment.INSTANCE);
        when(((AnnotationDrivenBinder.ArgumentBinder) argumentBinder).bind(any(Annotation.class),
                anyInt(),
                any(MethodDescription.class),
                any(MethodDescription.class),
                any(TypeDescription.class),
                any(Assigner.class)))
                .thenReturn(identifiedBinding);
        return identifiedBinding;
    }

    @SuppressWarnings({"unchecked", "unused"})
    private static Iterator<Annotation> prepareDefaultProvider(AnnotationDrivenBinder.DefaultProvider<?> defaultProvider,
                                                               List<? extends Annotation> defaultIteratorValues) {
        Iterator<Annotation> annotationIterator = mock(Iterator.class);
        when(defaultProvider.makeIterator(any(TypeDescription.class), any(MethodDescription.class), any(MethodDescription.class)))
                .thenReturn((Iterator) annotationIterator);
        OngoingStubbing<Boolean> iteratorConditionStubbing = when(annotationIterator.hasNext());
        for (Annotation defaultIteratorValue : defaultIteratorValues) {
            iteratorConditionStubbing = iteratorConditionStubbing.thenReturn(true);
        }
        iteratorConditionStubbing.thenReturn(false);
        OngoingStubbing<Annotation> iteratorValueStubbing = when(annotationIterator.next());
        for (Annotation defaultIteratorValue : defaultIteratorValues) {
            iteratorValueStubbing = iteratorValueStubbing.thenReturn(defaultIteratorValue);
        }
        iteratorValueStubbing.thenThrow(NoSuchElementException.class);
        return annotationIterator;
    }
}
