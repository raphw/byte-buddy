package net.bytebuddy.pool;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.RandomString;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypePoolDefaultComponentTypeLocatorTest {

    private static final String FOO = "foo", BAR = "bar", BAR_DESCRIPTOR = "L" + BAR + ";", QUX = "qux";

    @Test(expected = IllegalStateException.class)
    public void testIllegal() throws Exception {
        TypePool.Default.ComponentTypeLocator.Illegal.INSTANCE.bind(FOO);
    }

    @Test
    public void testForAnnotationProperty() throws Exception {
        TypePool typePool = mock(TypePool.class);
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(typePool.describe(BAR)).thenReturn(new TypePool.Resolution.Simple(typeDescription));
        MethodDescription methodDescription = mock(MethodDescription.class);
        when(typeDescription.getDeclaredMethods()).thenReturn(new MethodList.Explicit(Arrays.asList(methodDescription)));
        when(methodDescription.getSourceCodeName()).thenReturn(FOO);
        TypeDescription returnType = mock(TypeDescription.class);
        when(methodDescription.getReturnType()).thenReturn(returnType);
        TypeDescription componentType = mock(TypeDescription.class);
        when(returnType.getComponentType()).thenReturn(componentType);
        when(componentType.getName()).thenReturn(QUX);
        assertThat(new TypePool.Default.ComponentTypeLocator.ForAnnotationProperty(typePool, BAR_DESCRIPTOR)
                .bind(FOO).lookup(), is(QUX));
    }

    @Test
    public void testForArrayType() throws Exception {
        assertThat(new TypePool.Default.ComponentTypeLocator.ForArrayType("()[" + BAR_DESCRIPTOR)
                .bind(FOO).lookup(), is(BAR));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.Default.ComponentTypeLocator.ForAnnotationProperty.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.ComponentTypeLocator.ForAnnotationProperty.Bound.class).skipSynthetic().apply();
        ObjectPropertyAssertion.of(TypePool.Default.ComponentTypeLocator.ForArrayType.class).create(new ObjectPropertyAssertion.Creator<String>() {
            @Override
            public String create() {
                return "()L" + RandomString.make() + ";";
            }
        }).apply();
        TypePool.Default.TypeExtractor typeExtractor = new TypePool.Default(mock(TypePool.CacheProvider.class), mock(TypePool.SourceLocator.class))
                .new TypeExtractor();
        ObjectPropertyAssertion.of(TypePool.Default.TypeExtractor.OnTypeCollector.class).apply(typeExtractor.new OnTypeCollector(FOO));
        ObjectPropertyAssertion.of(TypePool.Default.TypeExtractor.MethodExtractor.class).apply(typeExtractor.new MethodExtractor(0, FOO, "()" + BAR_DESCRIPTOR, null));
        ObjectPropertyAssertion.of(TypePool.Default.TypeExtractor.MethodExtractor.OnMethodCollector.class).apply(typeExtractor
                .new MethodExtractor(0, FOO, "()" + BAR_DESCRIPTOR, null).new OnMethodCollector(FOO));
        ObjectPropertyAssertion.of(TypePool.Default.TypeExtractor.MethodExtractor.OnMethodParameterCollector.class).apply(typeExtractor
                .new MethodExtractor(0, FOO, "()" + BAR_DESCRIPTOR, null).new OnMethodParameterCollector(FOO, 0));
        ObjectPropertyAssertion.of(TypePool.Default.TypeExtractor.FieldExtractor.class).apply(typeExtractor.new FieldExtractor(0, FOO, BAR_DESCRIPTOR));
        ObjectPropertyAssertion.of(TypePool.Default.TypeExtractor.FieldExtractor.OnFieldCollector.class).apply(typeExtractor
                .new FieldExtractor(0, FOO, BAR_DESCRIPTOR).new OnFieldCollector(FOO));
        ObjectPropertyAssertion.of(TypePool.Default.TypeExtractor.AnnotationExtractor.class).apply(typeExtractor
                .new AnnotationExtractor(mock(TypePool.Default.AnnotationRegistrant.class), mock(TypePool.Default.ComponentTypeLocator.class)));
        ObjectPropertyAssertion.of(TypePool.Default.TypeExtractor.AnnotationExtractor.ArrayLookup.class).apply(typeExtractor
                .new AnnotationExtractor(mock(TypePool.Default.AnnotationRegistrant.class), mock(TypePool.Default.ComponentTypeLocator.class))
                .new ArrayLookup(FOO, mock(TypePool.LazyTypeDescription.AnnotationValue.ForComplexArray.ComponentTypeReference.class)));
        ObjectPropertyAssertion.of(TypePool.Default.TypeExtractor.AnnotationExtractor.AnnotationLookup.class).apply(typeExtractor
                .new AnnotationExtractor(mock(TypePool.Default.AnnotationRegistrant.class), mock(TypePool.Default.ComponentTypeLocator.class))
                .new AnnotationLookup(FOO, BAR));
    }
}
