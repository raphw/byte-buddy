package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class AnnotationDrivenBinderTest {

    private static @interface FirstMockAnnotation {
    }

    private static @interface SecondMockAnnotation {
    }

    private AnnotationDrivenBinder.ArgumentBinder<?> firstArgumentBinder, secondArgumentBinder;
    private AnnotationDrivenBinder.DefaultProvider<?> defaultProvider;
    private Assigner assigner;

    private TypeDescription typeDescription;
    private MethodDescription source, target;

    @Before
    public void setUp() throws Exception {
        firstArgumentBinder = mock(AnnotationDrivenBinder.ArgumentBinder.class);
        secondArgumentBinder = mock(AnnotationDrivenBinder.ArgumentBinder.class);
        defaultProvider = mock(AnnotationDrivenBinder.DefaultProvider.class);
        assigner = mock(Assigner.class);
        typeDescription = mock(TypeDescription.class);
        source = mock(MethodDescription.class);
        target = mock(MethodDescription.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConflictingBinderBinding() throws Exception {
        doReturn(FirstMockAnnotation.class).when(firstArgumentBinder).getHandledType();
        doReturn(FirstMockAnnotation.class).when(secondArgumentBinder).getHandledType();
        new AnnotationDrivenBinder(
                Arrays.<AnnotationDrivenBinder.ArgumentBinder<?>>asList(firstArgumentBinder, secondArgumentBinder),
                defaultProvider,
                assigner);
    }
}
