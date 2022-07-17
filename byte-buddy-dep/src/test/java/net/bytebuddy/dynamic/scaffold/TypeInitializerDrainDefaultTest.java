package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.ClassVisitor;

import static org.mockito.Mockito.*;

public class TypeInitializerDrainDefaultTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription instrumentedType;

    @Mock
    private TypeWriter.MethodPool methodPool;

    @Mock
    private AnnotationValueFilter.Factory annotationValueFilterFactory;

    @Mock
    private ClassVisitor classVisitor;

    @Mock
    private TypeInitializer typeInitializer;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private TypeWriter.MethodPool.Record record, transformed;

    @Test
    public void testDrain() throws Exception {
        when(methodPool.target(new MethodDescription.Latent.TypeInitializer(instrumentedType))).thenReturn(record);
        when(typeInitializer.wrap(record)).thenReturn(transformed);
        new TypeInitializer.Drain.Default(instrumentedType, methodPool, annotationValueFilterFactory).apply(classVisitor, typeInitializer, implementationContext);
        verify(transformed).apply(classVisitor, implementationContext, annotationValueFilterFactory);
        verifyNoMoreInteractions(transformed);
    }
}
