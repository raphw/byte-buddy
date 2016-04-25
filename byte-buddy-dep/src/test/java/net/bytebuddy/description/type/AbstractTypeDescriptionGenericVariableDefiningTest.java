package net.bytebuddy.description.type;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Test;

import java.lang.annotation.Annotation;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractTypeDescriptionGenericVariableDefiningTest extends AbstractTypeDescriptionGenericTest {

    private static final String FOO = "foo";

    private static final String T = "T", S = "S", U = "U", V = "V", W = "W", X = "X";

    private static final String TYPE_ANNOTATION = "net.bytebuddy.test.precompiled.TypeAnnotation";

    private static final String TYPE_ANNOTATION_SAMPLES = "net.bytebuddy.test.precompiled.TypeAnnotationSamples";

    private static final String RECEIVER_TYPE_SAMPLE = "net.bytebuddy.test.precompiled.ReceiverTypeSample", INNER = "Inner", NESTED = "Nested", GENERIC = "Generic";

    protected abstract TypeDescription describe(Class<?> type);

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableT() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription typeDescription = describe(Class.forName(TYPE_ANNOTATION_SAMPLES));
        TypeDescription.Generic t = typeDescription.getTypeVariables().filter(named(T)).getOnly();
        assertThat(t.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(t.getDeclaredAnnotations().size(), is(1));
        assertThat(t.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(t.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(0));
        assertThat(t.getUpperBounds().size(), is(1));
        assertThat(t.getUpperBounds().contains(TypeDescription.Generic.OBJECT), is(true));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableS() throws Exception {
        TypeDescription typeDescription = describe(Class.forName(TYPE_ANNOTATION_SAMPLES));
        TypeDescription.Generic t = typeDescription.getTypeVariables().filter(named(S)).getOnly();
        assertThat(t.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(t.getDeclaredAnnotations().size(), is(0));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableU() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription typeDescription = describe(Class.forName(TYPE_ANNOTATION_SAMPLES));
        TypeDescription.Generic u = typeDescription.getTypeVariables().filter(named(U)).getOnly();
        assertThat(u.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(u.getDeclaredAnnotations().size(), is(1));
        assertThat(u.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(u.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(2));
        assertThat(u.getUpperBounds().get(0).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(u.getUpperBounds().get(0).getDeclaredAnnotations().size(), is(0));
        assertThat(u.getUpperBounds().get(1).getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(u.getUpperBounds().get(1).getDeclaredAnnotations().size(), is(1));
        assertThat(u.getUpperBounds().get(1).getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(u.getUpperBounds().get(1).getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(3));
        assertThat(u.getUpperBounds().get(1).getTypeArguments().get(0).getSort(), is(TypeDefinition.Sort.WILDCARD));
        assertThat(u.getUpperBounds().get(1).getTypeArguments().get(0).getDeclaredAnnotations().size(), is(1));
        assertThat(u.getUpperBounds().get(1).getTypeArguments().get(0).getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(u.getUpperBounds().get(1).getTypeArguments().get(0).getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(4));
        assertThat(u.getUpperBounds().get(2).getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(u.getUpperBounds().get(2).getDeclaredAnnotations().size(), is(1));
        assertThat(u.getUpperBounds().get(2).getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(u.getUpperBounds().get(2).getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(5));
        assertThat(u.getUpperBounds().get(2).getTypeArguments().get(0).getSort(), is(TypeDefinition.Sort.WILDCARD));
        assertThat(u.getUpperBounds().get(2).getTypeArguments().get(0).getDeclaredAnnotations().size(), is(1));
        assertThat(u.getUpperBounds().get(2).getTypeArguments().get(0).getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(u.getUpperBounds().get(2).getTypeArguments().get(0).getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(6));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableV() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription typeDescription = describe(Class.forName(TYPE_ANNOTATION_SAMPLES));
        TypeDescription.Generic v = typeDescription.getTypeVariables().filter(named(V)).getOnly();
        assertThat(v.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(v.getDeclaredAnnotations().size(), is(1));
        assertThat(v.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(v.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(7));
        assertThat(v.getUpperBounds().get(0).getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(v.getUpperBounds().get(0).getDeclaredAnnotations().size(), is(0));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(0).getSort(), is(TypeDefinition.Sort.WILDCARD));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(0).getDeclaredAnnotations().size(), is(1));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(0).getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(0).getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(8));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(0).getUpperBounds().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(0).getUpperBounds().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(0).getUpperBounds().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(0).getUpperBounds().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(9));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getDeclaredAnnotations().size(), is(1));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(10));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.WILDCARD));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getTypeArguments().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getTypeArguments().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getTypeArguments().getOnly().getDeclaredAnnotations().ofType(typeAnnotation)
                .getValue(value, Integer.class), is(11));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getTypeArguments().getOnly().getLowerBounds().getOnly().getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getTypeArguments().getOnly().getLowerBounds().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getTypeArguments().getOnly().getLowerBounds().getOnly().getDeclaredAnnotations()
                .isAnnotationPresent(typeAnnotation), is(true));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getTypeArguments().getOnly().getLowerBounds().getOnly().getDeclaredAnnotations()
                .ofType(typeAnnotation).getValue(value, Integer.class), is(12));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getTypeArguments().getOnly().getLowerBounds().getOnly().getUpperBounds().get(0)
                .getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getTypeArguments().getOnly().getLowerBounds().getOnly().getUpperBounds().get(0)
                .getDeclaredAnnotations().size(), is(0));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getTypeArguments().getOnly().getLowerBounds().getOnly().getUpperBounds().get(1)
                .getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getTypeArguments().getOnly().getLowerBounds().getOnly().getUpperBounds().get(1)
                .getDeclaredAnnotations().size(), is(1));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getTypeArguments().getOnly().getLowerBounds().getOnly().getUpperBounds().get(1)
                .getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(v.getUpperBounds().get(0).getTypeArguments().get(1).getTypeArguments().getOnly().getLowerBounds().getOnly().getUpperBounds().get(1)
                .getDeclaredAnnotations().getOnly().prepare(typeAnnotation).getValue(value, Integer.class), is(3));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableW() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription typeDescription = describe(Class.forName(TYPE_ANNOTATION_SAMPLES));
        TypeDescription.Generic t = typeDescription.getTypeVariables().filter(named(W)).getOnly();
        assertThat(t.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(t.getDeclaredAnnotations().size(), is(1));
        assertThat(t.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(t.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(13));
        assertThat(t.getUpperBounds().size(), is(1));
        assertThat(t.getUpperBounds().getOnly().getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(14));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableX() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription typeDescription = describe(Class.forName(TYPE_ANNOTATION_SAMPLES));
        TypeDescription.Generic t = typeDescription.getTypeVariables().filter(named(X)).getOnly();
        assertThat(t.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(t.getDeclaredAnnotations().size(), is(1));
        assertThat(t.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(t.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(15));
        assertThat(t.getUpperBounds().size(), is(1));
        assertThat(t.getUpperBounds().getOnly().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(16));
        assertThat(t.getUpperBounds().getOnly().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.WILDCARD));
        assertThat(t.getUpperBounds().getOnly().getTypeArguments().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(t.getUpperBounds().getOnly().getTypeArguments().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(t.getUpperBounds().getOnly().getTypeArguments().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(17));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(8)
    public void testMethodVariableT() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        MethodDescription methodDescription = describe(Class.forName(TYPE_ANNOTATION_SAMPLES)).getDeclaredMethods().filter(named(FOO)).getOnly();
        TypeDescription.Generic t = methodDescription.getTypeVariables().getOnly();
        assertThat(t.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(t.getDeclaredAnnotations().size(), is(1));
        assertThat(t.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(t.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(26));
        assertThat(t.getUpperBounds().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(t.getUpperBounds().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(27));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(8)
    public void testNonGenericTypeAnnotationReceiverTypeOnMethod() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription.Generic receiverType = describe(Class.forName(RECEIVER_TYPE_SAMPLE))
                .getDeclaredMethods()
                .filter(named(FOO))
                .getOnly()
                .getReceiverType();
        assertThat(receiverType, notNullValue(TypeDescription.Generic.class));
        assertThat(receiverType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(receiverType.represents(Class.forName(RECEIVER_TYPE_SAMPLE)), is(true));
        assertThat(receiverType.getDeclaredAnnotations().size(), is(1));
        assertThat(receiverType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(receiverType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(0));
        assertThat(receiverType.getOwnerType(), nullValue(TypeDescription.Generic.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(8)
    public void testNonGenericTypeAnnotationReceiverTypeOnConstructor() throws Exception {
        TypeDescription.Generic receiverType = describe(Class.forName(RECEIVER_TYPE_SAMPLE))
                .getDeclaredMethods()
                .filter(isConstructor())
                .getOnly()
                .getReceiverType();
        assertThat(receiverType, notNullValue(TypeDescription.Generic.class));
        assertThat(receiverType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(receiverType.represents(Class.forName(RECEIVER_TYPE_SAMPLE)), is(true));
        assertThat(receiverType.getDeclaredAnnotations().size(), is(0));
        assertThat(receiverType.getOwnerType(), nullValue(TypeDescription.Generic.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(8)
    public void testNonGenericInnerTypeAnnotationReceiverTypeOnMethod() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription.Generic receiverType = describe(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + INNER))
                .getDeclaredMethods()
                .filter(named(FOO))
                .getOnly()
                .getReceiverType();
        assertThat(receiverType, notNullValue(TypeDescription.Generic.class));
        assertThat(receiverType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(receiverType.represents(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + INNER)), is(true));
        assertThat(receiverType.getDeclaredAnnotations().size(), is(1));
        assertThat(receiverType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(receiverType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(1));
        assertThat(receiverType.getOwnerType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(receiverType.getOwnerType().represents(Class.forName(RECEIVER_TYPE_SAMPLE)), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(8)
    public void testNonGenericInnerTypeAnnotationReceiverTypeOnConstructor() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription.Generic receiverType = describe(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + INNER))
                .getDeclaredMethods()
                .filter(isConstructor())
                .getOnly()
                .getReceiverType();
        assertThat(receiverType, notNullValue(TypeDescription.Generic.class));
        assertThat(receiverType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(receiverType.represents(Class.forName(RECEIVER_TYPE_SAMPLE)), is(true));
        assertThat(receiverType.getDeclaredAnnotations().size(), is(1));
        assertThat(receiverType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(receiverType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(2));
        assertThat(receiverType.getOwnerType(), nullValue(TypeDescription.Generic.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(8)
    public void testNonGenericNestedTypeAnnotationReceiverTypeOnMethod() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription.Generic receiverType = describe(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + NESTED))
                .getDeclaredMethods()
                .filter(named(FOO))
                .getOnly()
                .getReceiverType();
        assertThat(receiverType, notNullValue(TypeDescription.Generic.class));
        assertThat(receiverType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(receiverType.represents(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + NESTED)), is(true));
        assertThat(receiverType.getDeclaredAnnotations().size(), is(1));
        assertThat(receiverType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(receiverType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(3));
        assertThat(receiverType.getOwnerType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(receiverType.getOwnerType().represents(Class.forName(RECEIVER_TYPE_SAMPLE)), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(8)
    public void testNonGenericNestedTypeAnnotationReceiverTypeOnConstructor() throws Exception {
        TypeDescription.Generic receiverType = describe(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + NESTED))
                .getDeclaredMethods()
                .filter(isConstructor())
                .getOnly()
                .getReceiverType();
        assertThat(receiverType, notNullValue(TypeDescription.Generic.class));
        assertThat(receiverType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(receiverType.represents(Class.forName(RECEIVER_TYPE_SAMPLE)), is(true));
        assertThat(receiverType.getDeclaredAnnotations().size(), is(0));
        assertThat(receiverType.getOwnerType(), nullValue(TypeDescription.Generic.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(8)
    public void testGenericTypeAnnotationReceiverTypeOnMethod() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription.Generic receiverType = describe(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + GENERIC))
                .getDeclaredMethods()
                .filter(named(FOO))
                .getOnly()
                .getReceiverType();
        assertThat(receiverType, notNullValue(TypeDescription.Generic.class));
        assertThat(receiverType.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(receiverType.asErasure().represents(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + GENERIC)), is(true));
        assertThat(receiverType.getDeclaredAnnotations().size(), is(1));
        assertThat(receiverType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(receiverType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(4));
        assertThat(receiverType.getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(receiverType.getTypeArguments().getOnly().getSymbol(), is(T));
        assertThat(receiverType.getTypeArguments().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(receiverType.getTypeArguments().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(receiverType.getTypeArguments().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(5));
        assertThat(receiverType.getOwnerType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(receiverType.getOwnerType().represents(Class.forName(RECEIVER_TYPE_SAMPLE)), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(8)
    public void testGenericTypeAnnotationReceiverTypeOnConstructor() throws Exception {
        TypeDescription.Generic receiverType = describe(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + GENERIC))
                .getDeclaredMethods()
                .filter(isConstructor())
                .getOnly()
                .getReceiverType();
        assertThat(receiverType, notNullValue(TypeDescription.Generic.class));
        assertThat(receiverType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(receiverType.asErasure().represents(Class.forName(RECEIVER_TYPE_SAMPLE)), is(true));
        assertThat(receiverType.getDeclaredAnnotations().size(), is(0));
        assertThat(receiverType.getOwnerType(), nullValue(TypeDescription.Generic.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(8)
    public void testGenericInnerTypeAnnotationReceiverTypeOnMethod() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription.Generic receiverType = describe(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + GENERIC + "$" + INNER))
                .getDeclaredMethods()
                .filter(named(FOO))
                .getOnly()
                .getReceiverType();
        assertThat(receiverType, notNullValue(TypeDescription.Generic.class));
        assertThat(receiverType.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(receiverType.asErasure().represents(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + GENERIC + "$" + INNER)), is(true));
        assertThat(receiverType.getDeclaredAnnotations().size(), is(1));
        assertThat(receiverType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(receiverType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(8));
        assertThat(receiverType.getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(receiverType.getTypeArguments().getOnly().getSymbol(), is(S));
        assertThat(receiverType.getTypeArguments().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(receiverType.getTypeArguments().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(receiverType.getTypeArguments().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(9));
        assertThat(receiverType.getOwnerType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(receiverType.getOwnerType().asErasure().represents(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + GENERIC)), is(true));
        assertThat(receiverType.getOwnerType().getDeclaredAnnotations().size(), is(1));
        assertThat(receiverType.getOwnerType().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(receiverType.getOwnerType().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(6));
        assertThat(receiverType.getOwnerType().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(receiverType.getOwnerType().getTypeArguments().getOnly().getSymbol(), is(T));
        assertThat(receiverType.getOwnerType().getTypeArguments().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(receiverType.getOwnerType().getTypeArguments().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(receiverType.getOwnerType().getTypeArguments().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(7));
        assertThat(receiverType.getOwnerType().getOwnerType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(receiverType.getOwnerType().getOwnerType().represents(Class.forName(RECEIVER_TYPE_SAMPLE)), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(8)
    public void testGenericInnerTypeAnnotationReceiverTypeOnConstructor() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription.Generic receiverType = describe(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + GENERIC + "$" + INNER))
                .getDeclaredMethods()
                .filter(isConstructor())
                .getOnly()
                .getReceiverType();
        assertThat(receiverType, notNullValue(TypeDescription.Generic.class));
        assertThat(receiverType.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(receiverType.asErasure().represents(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + GENERIC)), is(true));
        assertThat(receiverType.getDeclaredAnnotations().size(), is(1));
        assertThat(receiverType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(receiverType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(10));
        assertThat(receiverType.getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(receiverType.getTypeArguments().getOnly().getSymbol(), is(T));
        assertThat(receiverType.getTypeArguments().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(receiverType.getTypeArguments().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(receiverType.getTypeArguments().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(11));
        assertThat(receiverType.getOwnerType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(receiverType.getOwnerType().represents(Class.forName(RECEIVER_TYPE_SAMPLE)), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(8)
    public void testGenericNestedTypeAnnotationReceiverTypeOnMethod() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        TypeDescription.Generic receiverType = describe(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + GENERIC + "$" + NESTED))
                .getDeclaredMethods()
                .filter(named(FOO))
                .getOnly()
                .getReceiverType();
        assertThat(receiverType, notNullValue(TypeDescription.Generic.class));
        assertThat(receiverType.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(receiverType.asErasure().represents(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + GENERIC + "$" + NESTED)), is(true));
        assertThat(receiverType.getDeclaredAnnotations().size(), is(1));
        assertThat(receiverType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(receiverType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(12));
        assertThat(receiverType.getTypeArguments().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(receiverType.getTypeArguments().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(receiverType.getTypeArguments().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value, Integer.class), is(13));
        assertThat(receiverType.getOwnerType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(receiverType.getOwnerType().represents(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + GENERIC)), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    @JavaVersionRule.Enforce(8)
    public void testGenericNestedTypeAnnotationReceiverTypeOnConstructor() throws Exception {
        TypeDescription.Generic receiverType = describe(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + GENERIC + "$" + NESTED))
                .getDeclaredMethods()
                .filter(isConstructor())
                .getOnly()
                .getReceiverType();
        assertThat(receiverType, notNullValue(TypeDescription.Generic.class));
        assertThat(receiverType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(receiverType.asErasure().represents(Class.forName(RECEIVER_TYPE_SAMPLE + "$" + GENERIC)), is(true));
        assertThat(receiverType.getDeclaredAnnotations().size(), is(0));
        assertThat(receiverType.getOwnerType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(receiverType.getOwnerType().represents(Class.forName(RECEIVER_TYPE_SAMPLE)), is(true));
    }
}
