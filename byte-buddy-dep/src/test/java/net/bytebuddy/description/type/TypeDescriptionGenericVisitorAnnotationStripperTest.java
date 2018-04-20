package net.bytebuddy.description.type;

import net.bytebuddy.description.annotation.AnnotationDescription;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeDescriptionGenericVisitorAnnotationStripperTest {

    private static final String FOO = "foo";

    private AnnotationDescription annotationDescription;

    @Before
    public void setUp() throws Exception {
        annotationDescription = AnnotationDescription.Builder.ofType(Foo.class).build();
    }

    @Test
    public void testWildcardLowerBound() throws Exception {
        TypeDescription.Generic typeDescription = TypeDescription.Generic.Builder.rawType(Object.class)
                .annotate(annotationDescription)
                .asWildcardLowerBound(annotationDescription);
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onWildcard(typeDescription), is(typeDescription));
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onWildcard(typeDescription).getDeclaredAnnotations().size(), is(0));
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onWildcard(typeDescription).getLowerBounds()
                .getOnly().getDeclaredAnnotations().size(), is(0));
    }

    @Test
    public void testWildcardUpperBound() throws Exception {
        TypeDescription.Generic typeDescription = TypeDescription.Generic.Builder.rawType(Object.class)
                .annotate(annotationDescription)
                .asWildcardLowerBound(annotationDescription);
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onWildcard(typeDescription), is(typeDescription));
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onWildcard(typeDescription).getDeclaredAnnotations().size(), is(0));
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onWildcard(typeDescription).getUpperBounds()
                .getOnly().getDeclaredAnnotations().size(), is(0));
    }

    @Test
    public void testGenericArray() throws Exception {
        TypeDescription.Generic typeDescription = TypeDescription.Generic.Builder.rawType(Object.class)
                .annotate(annotationDescription)
                .asArray()
                .annotate(annotationDescription)
                .build();
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onGenericArray(typeDescription), is(typeDescription));
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onGenericArray(typeDescription).getDeclaredAnnotations().size(), is(0));
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onGenericArray(typeDescription).getComponentType()
                .getDeclaredAnnotations().size(), is(0));
    }

    @Test
    public void testNonGenericArray() throws Exception {
        TypeDescription.Generic typeDescription = TypeDescription.Generic.Builder.rawType(Object.class)
                .annotate(annotationDescription)
                .asArray()
                .annotate(annotationDescription)
                .build();
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onNonGenericType(typeDescription), is(typeDescription));
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onNonGenericType(typeDescription).getDeclaredAnnotations().size(), is(0));
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onNonGenericType(typeDescription).getComponentType()
                .getDeclaredAnnotations().size(), is(0));
    }

    @Test
    public void testNonGeneric() throws Exception {
        TypeDescription.Generic typeDescription = TypeDescription.Generic.Builder.rawType(Object.class)
                .annotate(annotationDescription)
                .build();
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onNonGenericType(typeDescription), is(typeDescription));
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onNonGenericType(typeDescription).getDeclaredAnnotations().size(), is(0));
    }

    @Test
    public void testTypeVariable() throws Exception {
        TypeDescription.Generic typeDescription = TypeDescription.Generic.Builder.typeVariable(FOO).annotate(annotationDescription).build();
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onTypeVariable(typeDescription).getSymbol(), is(FOO));
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onTypeVariable(typeDescription).getDeclaredAnnotations().size(), is(0));
    }

    @Test
    public void testParameterized() throws Exception {
        TypeDescription.Generic typeDescription = TypeDescription.Generic.Builder.parameterizedType(TypeDescription.ForLoadedType.of(Collection.class),
                TypeDescription.Generic.Builder.rawType(Object.class).annotate(annotationDescription).build()).annotate(annotationDescription).build();
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onParameterizedType(typeDescription), is(typeDescription));
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onParameterizedType(typeDescription).getDeclaredAnnotations().size(), is(0));
        assertThat(TypeDescription.Generic.Visitor.AnnotationStripper.INSTANCE.onParameterizedType(typeDescription).getTypeArguments()
                .getOnly().getDeclaredAnnotations().size(), is(0));
    }

    private @interface Foo {
        /* empty */
    }
}
