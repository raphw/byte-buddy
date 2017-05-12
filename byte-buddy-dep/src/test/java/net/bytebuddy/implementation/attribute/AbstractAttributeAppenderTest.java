package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.mockito.Mockito.*;

public abstract class AbstractAttributeAppenderTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    protected TypeDescription instrumentedType, typeErasure;

    @Mock
    protected AnnotationValueFilter annotationValueFilter;

    @Mock
    protected TypeDescription.Generic.OfNonGenericType simpleAnnotatedType;

    @Mock
    protected TypeDescription.Generic.OfTypeVariable annotatedTypeVariable;

    @Mock
    protected TypeDescription.Generic.OfNonGenericType annotatedTypeVariableBound;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        when(annotatedTypeVariable.accept(any(TypeDescription.Generic.Visitor.class))).thenCallRealMethod();
        when(annotatedTypeVariable.getUpperBounds()).thenReturn(new TypeList.Generic.Explicit(annotatedTypeVariableBound));
        when(annotatedTypeVariable.asGenericType()).thenReturn(annotatedTypeVariable);
        when(annotatedTypeVariableBound.accept(any(TypeDescription.Generic.Visitor.class))).thenCallRealMethod();
        when(annotatedTypeVariableBound.getSort()).thenReturn(TypeDefinition.Sort.VARIABLE);
        when(annotatedTypeVariableBound.asGenericType()).thenReturn(annotatedTypeVariableBound);
        when(simpleAnnotatedType.accept(any(TypeDescription.Generic.Visitor.class))).thenCallRealMethod();
        when(simpleAnnotatedType.asGenericType()).thenReturn(simpleAnnotatedType);
        when(simpleAnnotatedType.asErasure()).thenReturn(typeErasure);
        when(annotatedTypeVariable.asErasure()).thenReturn(typeErasure);
        when(annotatedTypeVariableBound.asErasure()).thenReturn(typeErasure);
    }

    @Retention(RetentionPolicy.SOURCE)
    protected @interface Qux {

        class Instance implements Qux {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Qux.class;
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Baz {

        class Instance implements Baz {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Baz.class;
            }
        }
    }

    @Retention(RetentionPolicy.CLASS)
    protected @interface QuxBaz {

        class Instance implements QuxBaz {

            @Override
            public Class<? extends Annotation> annotationType() {
                return QuxBaz.class;
            }
        }
    }
}
