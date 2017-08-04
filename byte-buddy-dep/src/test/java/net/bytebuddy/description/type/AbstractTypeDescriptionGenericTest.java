package net.bytebuddy.description.type;

import net.bytebuddy.description.TypeVariableSource;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public abstract class AbstractTypeDescriptionGenericTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    private static final String T = "T", S = "S", U = "U", V = "V";

    private static final String TYPE_ANNOTATION = "net.bytebuddy.test.precompiled.TypeAnnotation";

    private static final String OTHER_TYPE_ANNOTATION = "net.bytebuddy.test.precompiled.OtherTypeAnnotation";

    private static final String TYPE_ANNOTATION_SAMPLES = "net.bytebuddy.test.precompiled.TypeAnnotationSamples";

    private static final String TYPE_ANNOTATION_OTHER_SAMPLES = "net.bytebuddy.test.precompiled.TypeAnnotationOtherSamples";

    protected abstract TypeDescription.Generic describeType(Field field);

    protected abstract TypeDescription.Generic describeReturnType(Method method);

    protected abstract TypeDescription.Generic describeParameterType(Method method, int index);

    protected abstract TypeDescription.Generic describeExceptionType(Method method, int index);

    protected abstract TypeDescription.Generic describeSuperClass(Class<?> type);

    protected abstract TypeDescription.Generic describeInterfaceType(Class<?> type, int index);

    @Test
    public void testNonGenericTypeOwnerType() throws Exception {
        assertThat(describeType(NonGeneric.class.getDeclaredField(FOO)).getOwnerType(), nullValue(TypeDescription.Generic.class));
        assertThat(describeType(NonGeneric.class.getDeclaredField(BAR)).getOwnerType(), is(TypeDefinition.Sort.describe(NonGeneric.class)));
    }

    @Test(expected = IllegalStateException.class)
    public void testNonGenericTypeNoTypeArguments() throws Exception {
        describeType(NonGeneric.class.getDeclaredField(FOO)).getTypeArguments();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonGenericTypeNoBindLocation() throws Exception {
        describeType(NonGeneric.class.getDeclaredField(FOO)).findBindingOf(mock(TypeDescription.Generic.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testNonGenericTypeNoUpperBounds() throws Exception {
        describeType(NonGeneric.class.getDeclaredField(FOO)).getUpperBounds();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonGenericTypeNoLowerBounds() throws Exception {
        describeType(NonGeneric.class.getDeclaredField(FOO)).getLowerBounds();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonGenericTypeNoSymbol() throws Exception {
        describeType(NonGeneric.class.getDeclaredField(FOO)).getSymbol();
    }

    @Test
    public void testSimpleParameterizedType() throws Exception {
        TypeDescription.Generic typeDescription = describeType(SimpleParameterizedType.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getActualName(), is(SimpleParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.getTypeName(), is(SimpleParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.toString(), is(SimpleParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.hashCode(),
                is(TypeDefinition.Sort.describe(SimpleParameterizedType.class.getDeclaredField(FOO).getGenericType()).hashCode()));
        assertThat(typeDescription, is(TypeDefinition.Sort.describe(SimpleParameterizedType.class.getDeclaredField(FOO).getGenericType())));
        assertThat(typeDescription, CoreMatchers.not(TypeDefinition.Sort.describe(SimpleGenericArrayType.class.getDeclaredField(FOO).getType())));
        assertThat(typeDescription, CoreMatchers.not(new Object()));
        assertThat(typeDescription.equals(null), is(false));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        assertThat(typeDescription.getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getTypeArguments().getOnly().asErasure().represents(String.class), is(true));
        assertThat(typeDescription.getTypeName(), is(SimpleParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.getOwnerType(), nullValue(TypeDescription.Generic.class));
    }

    @Test
    public void testParameterizedTypeIterator() throws Exception {
        TypeDescription.Generic typeDescription = describeType(SimpleParameterizedType.class.getDeclaredField(FOO));
        Iterator<TypeDefinition> iterator = typeDescription.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((TypeDefinition) typeDescription));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testParameterizedTypeNoComponentType() throws Exception {
        describeType(SimpleParameterizedType.class.getDeclaredField(FOO)).getComponentType();
    }

    @Test(expected = IllegalStateException.class)
    public void testParameterizedTypeNoVariableSource() throws Exception {
        describeType(SimpleParameterizedType.class.getDeclaredField(FOO)).getTypeVariableSource();
    }

    @Test(expected = IllegalStateException.class)
    public void testParameterizedTypeNoSymbol() throws Exception {
        describeType(SimpleParameterizedType.class.getDeclaredField(FOO)).getSymbol();
    }

    @Test(expected = IllegalStateException.class)
    public void testParameterizedTypeNoUpperBounds() throws Exception {
        describeType(SimpleParameterizedType.class.getDeclaredField(FOO)).getUpperBounds();
    }

    @Test(expected = IllegalStateException.class)
    public void testParameterizedTypeNoLowerBounds() throws Exception {
        describeType(SimpleParameterizedType.class.getDeclaredField(FOO)).getLowerBounds();
    }

    @Test
    public void testUpperBoundWildcardParameterizedType() throws Exception {
        TypeDescription.Generic typeDescription = describeType(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getActualName(), is(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.getTypeName(), is(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.toString(), is(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.hashCode(),
                is(TypeDefinition.Sort.describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType()).hashCode()));
        assertThat(typeDescription,
                is(TypeDefinition.Sort.describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType())));
        assertThat(typeDescription,
                CoreMatchers.not(TypeDefinition.Sort.describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO).getType())));
        assertThat(typeDescription, CoreMatchers.not(new Object()));
        assertThat(typeDescription.equals(null), is(false));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        assertThat(typeDescription.getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.WILDCARD));
        assertThat(typeDescription.getTypeArguments().getOnly().getUpperBounds().size(), is(1));
        assertThat(typeDescription.getTypeArguments().getOnly().getUpperBounds().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getTypeArguments().getOnly().getUpperBounds().getOnly().asErasure().represents(String.class), is(true));
        assertThat(typeDescription.getTypeArguments().getOnly().getLowerBounds().size(), is(0));
        assertThat(typeDescription.getTypeName(), is(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoComponentType() throws Exception {
        describeType(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getComponentType();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoOwnerType() throws Exception {
        describeType(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getOwnerType();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoVariableSource() throws Exception {
        describeType(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getTypeVariableSource();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoSymbol() throws Exception {
        describeType(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getSymbol();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoErasure() throws Exception {
        describeType(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().asErasure();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoStackSize() throws Exception {
        describeType(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getStackSize();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoSuperClass() throws Exception {
        describeType(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getSuperClass();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoInterfaces() throws Exception {
        describeType(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getInterfaces();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoFields() throws Exception {
        describeType(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getDeclaredFields();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoMethods() throws Exception {
        describeType(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getDeclaredMethods();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoIterator() throws Exception {
        describeType(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().iterator();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardTypeNoTypeArguments() throws Exception {
        describeType(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getTypeArguments();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardTypeNoBindLocation() throws Exception {
        describeType(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().findBindingOf(mock(TypeDescription.Generic.class));
    }

    @Test
    public void testLowerBoundWildcardParameterizedType() throws Exception {
        TypeDescription.Generic typeDescription = describeType(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getActualName(), is(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.getTypeName(), is(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.toString(), is(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.hashCode(),
                is(TypeDefinition.Sort.describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType()).hashCode()));
        assertThat(typeDescription,
                is(TypeDefinition.Sort.describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType())));
        assertThat(typeDescription,
                CoreMatchers.not(TypeDefinition.Sort.describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO).getType())));
        assertThat(typeDescription, CoreMatchers.not(new Object()));
        assertThat(typeDescription.equals(null), is(false));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        assertThat(typeDescription.getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.WILDCARD));
        assertThat(typeDescription.getTypeArguments().getOnly().getUpperBounds().size(), is(1));
        assertThat(typeDescription.getTypeArguments().getOnly().getUpperBounds().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getTypeArguments().getOnly().getLowerBounds().size(), is(1));
        assertThat(typeDescription.getTypeArguments().getOnly().getLowerBounds().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getTypeArguments().getOnly().getLowerBounds().getOnly().asErasure().represents(String.class), is(true));
        assertThat(typeDescription.getTypeName(), is(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoComponentType() throws Exception {
        describeType(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getComponentType();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoOwnerType() throws Exception {
        describeType(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getOwnerType();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoVariableSource() throws Exception {
        describeType(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getTypeVariableSource();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoSymbol() throws Exception {
        describeType(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getSymbol();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoErasure() throws Exception {
        describeType(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().asErasure();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoStackSize() throws Exception {
        describeType(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getStackSize();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoSuperClass() throws Exception {
        describeType(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getSuperClass();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoInterfaces() throws Exception {
        describeType(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getInterfaces();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoFields() throws Exception {
        describeType(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getDeclaredFields();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoMethods() throws Exception {
        describeType(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getDeclaredMethods();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoIterator() throws Exception {
        describeType(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().iterator();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardTypeNoTypeArguments() throws Exception {
        describeType(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getTypeArguments();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardTypeNoBindLocation() throws Exception {
        describeType(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().findBindingOf(mock(TypeDescription.Generic.class));
    }

    @Test
    public void testUnboundWildcardParameterizedType() throws Exception {
        TypeDescription.Generic typeDescription = describeType(UnboundWildcardParameterizedType.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getActualName(), is(UnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.getTypeName(), is(UnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.toString(), is(UnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.hashCode(),
                is(TypeDefinition.Sort.describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType()).hashCode()));
        assertThat(typeDescription,
                is(TypeDefinition.Sort.describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType())));
        assertThat(typeDescription,
                CoreMatchers.not(TypeDefinition.Sort.describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO).getType())));
        assertThat(typeDescription, CoreMatchers.not(new Object()));
        assertThat(typeDescription.equals(null), is(false));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        assertThat(typeDescription.getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.WILDCARD));
        assertThat(typeDescription.getTypeArguments().getOnly().getUpperBounds().size(), is(1));
        assertThat(typeDescription.getTypeArguments().getOnly().getUpperBounds().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getTypeArguments().getOnly().getUpperBounds().getOnly().asErasure().represents(Object.class), is(true));
        assertThat(typeDescription.getTypeArguments().getOnly().getLowerBounds().size(), is(0));
        assertThat(typeDescription.getTypeName(), is(UnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardParameterizedTypeNoComponentType() throws Exception {
        describeType(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getComponentType();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardParameterizedTypeNoOwnerType() throws Exception {
        describeType(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getOwnerType();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardParameterizedTypeNoVariableSource() throws Exception {
        describeType(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getTypeVariableSource();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardParameterizedTypeNoSymbol() throws Exception {
        describeType(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getSymbol();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardParameterizedTypeNoErasure() throws Exception {
        describeType(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().asErasure();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardParameterizedTypeNoStackSize() throws Exception {
        describeType(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getStackSize();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardParameterizedTypeNoSuperClass() throws Exception {
        describeType(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getSuperClass();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardParameterizedTypeNoInterfaces() throws Exception {
        describeType(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getInterfaces();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundBoundWildcardParameterizedTypeNoFields() throws Exception {
        describeType(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getDeclaredFields();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundBoundWildcardParameterizedTypeNoMethods() throws Exception {
        describeType(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getDeclaredMethods();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundBoundWildcardParameterizedTypeNoIterator() throws Exception {
        describeType(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().iterator();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardTypeNoTypeArguments() throws Exception {
        describeType(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getTypeArguments();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardTypeNoBindLocation() throws Exception {
        describeType(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().findBindingOf(mock(TypeDescription.Generic.class));
    }

    @Test
    public void testExplicitlyUnboundWildcardParameterizedType() throws Exception {
        TypeDescription.Generic typeDescription = describeType(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getActualName(),
                is(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.getTypeName(),
                is(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.toString(),
                is(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.hashCode(),
                is(TypeDefinition.Sort.describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType()).hashCode()));
        assertThat(typeDescription,
                is(TypeDefinition.Sort.describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType())));
        assertThat(typeDescription,
                CoreMatchers.not(TypeDefinition.Sort.describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO).getType())));
        assertThat(typeDescription, CoreMatchers.not(new Object()));
        assertThat(typeDescription.equals(null), is(false));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        assertThat(typeDescription.getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.WILDCARD));
        assertThat(typeDescription.getTypeArguments().getOnly().getUpperBounds().size(), is(1));
        assertThat(typeDescription.getTypeArguments().getOnly().getUpperBounds().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getTypeArguments().getOnly().getUpperBounds().getOnly().asErasure().represents(Object.class), is(true));
        assertThat(typeDescription.getTypeArguments().getOnly().getLowerBounds().size(), is(0));
        assertThat(typeDescription.getTypeName(), is(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardParameterizedTypeNoComponentType() throws Exception {
        describeType(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getComponentType();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardParameterizedTypeNoOwnerType() throws Exception {
        describeType(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getOwnerType();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardParameterizedTypeNoVariableSource() throws Exception {
        describeType(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getTypeVariableSource();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardParameterizedTypeNoSymbol() throws Exception {
        describeType(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getSymbol();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardParameterizedTypeNoErasure() throws Exception {
        describeType(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().asErasure();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardParameterizedTypeNoStackSize() throws Exception {
        describeType(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getStackSize();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardParameterizedTypeNoSuperClass() throws Exception {
        describeType(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getSuperClass();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardParameterizedTypeNoInterfaces() throws Exception {
        describeType(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getInterfaces();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundBoundWildcardParameterizedTypeNoFields() throws Exception {
        describeType(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getDeclaredFields();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundBoundWildcardParameterizedTypeNoMethods() throws Exception {
        describeType(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getDeclaredMethods();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundBoundWildcardParameterizedTypeNoIterator() throws Exception {
        describeType(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().iterator();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardTypeNoTypeArguments() throws Exception {
        describeType(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().getTypeArguments();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardTypeNoBindLocation() throws Exception {
        describeType(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getTypeArguments().getOnly().findBindingOf(mock(TypeDescription.Generic.class));
    }

    @Test
    public void testNestedParameterizedType() throws Exception {
        TypeDescription.Generic typeDescription = describeType(NestedParameterizedType.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        assertThat(typeDescription.getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getTypeArguments().getOnly().getTypeArguments().size(), is(1));
        assertThat(typeDescription.getTypeArguments().getOnly().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getTypeArguments().getOnly().getTypeArguments().getOnly().asErasure().represents(Foo.class), is(true));
    }

    @Test
    public void testGenericArrayType() throws Exception {
        TypeDescription.Generic typeDescription = describeType(SimpleGenericArrayType.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.GENERIC_ARRAY));
        assertThat(typeDescription.getStackSize(), is(StackSize.SINGLE));
        assertThat(typeDescription.getDeclaredFields().size(), is(0));
        assertThat(typeDescription.getDeclaredMethods().size(), is(0));
        assertThat(typeDescription.getSuperClass(), is(TypeDescription.Generic.OBJECT));
        assertThat(typeDescription.getInterfaces(), is(TypeDescription.ARRAY_INTERFACES));
        assertThat(typeDescription.getActualName(), is(SimpleGenericArrayType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.getTypeName(), is(SimpleGenericArrayType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.toString(), is(SimpleGenericArrayType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.hashCode(),
                is(TypeDefinition.Sort.describe(SimpleGenericArrayType.class.getDeclaredField(FOO).getGenericType()).hashCode()));
        assertThat(typeDescription, is(TypeDefinition.Sort.describe(SimpleGenericArrayType.class.getDeclaredField(FOO).getGenericType())));
        assertThat(typeDescription, CoreMatchers.not(TypeDefinition.Sort.describe(SimpleGenericArrayType.class.getDeclaredField(FOO).getType())));
        assertThat(typeDescription, CoreMatchers.not(new Object()));
        assertThat(typeDescription.equals(null), is(false));
        assertThat(typeDescription.getComponentType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getComponentType().getTypeArguments().size(), is(1));
        assertThat(typeDescription.getComponentType().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getComponentType().getTypeArguments().getOnly().asErasure().represents(String.class), is(true));
        assertThat(typeDescription.getTypeName(), is(SimpleGenericArrayType.class.getDeclaredField(FOO).getGenericType().toString()));
    }

    @Test
    public void testGenericArrayTypeIterator() throws Exception {
        TypeDescription.Generic typeDescription = describeType(SimpleGenericArrayType.class.getDeclaredField(FOO));
        Iterator<TypeDefinition> iterator = typeDescription.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((TypeDefinition) typeDescription));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((TypeDefinition) TypeDescription.OBJECT));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayTypeNoVariableSource() throws Exception {
        describeType(SimpleGenericArrayType.class.getDeclaredField(FOO)).getTypeVariableSource();
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayTypeNoSymbol() throws Exception {
        describeType(SimpleGenericArrayType.class.getDeclaredField(FOO)).getSymbol();
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayTypeNoUpperBounds() throws Exception {
        describeType(SimpleGenericArrayType.class.getDeclaredField(FOO)).getUpperBounds();
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayTypeNoLowerBounds() throws Exception {
        describeType(SimpleGenericArrayType.class.getDeclaredField(FOO)).getLowerBounds();
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayTypeNoTypeArguments() throws Exception {
        describeType(SimpleGenericArrayType.class.getDeclaredField(FOO)).getTypeArguments();
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayTypeNoBindLocation() throws Exception {
        describeType(SimpleGenericArrayType.class.getDeclaredField(FOO)).findBindingOf(mock(TypeDescription.Generic.class));
    }

    @Test
    public void testGenericArrayOfGenericComponentType() throws Exception {
        TypeDescription.Generic typeDescription = describeType(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.GENERIC_ARRAY));
        assertThat(typeDescription.getStackSize(), is(StackSize.SINGLE));
        assertThat(typeDescription.getDeclaredFields().size(), is(0));
        assertThat(typeDescription.getDeclaredMethods().size(), is(0));
        assertThat(typeDescription.getOwnerType(), nullValue(TypeDescription.Generic.class));
        assertThat(typeDescription.getSuperClass(), is(TypeDescription.Generic.OBJECT));
        assertThat(typeDescription.getInterfaces(), is(TypeDescription.ARRAY_INTERFACES));
        assertThat(typeDescription.getActualName(), is(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.getTypeName(), is(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.toString(), is(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.hashCode(),
                is(TypeDefinition.Sort.describe(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO).getGenericType()).hashCode()));
        assertThat(typeDescription, is(TypeDefinition.Sort.describe(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO).getGenericType())));
        assertThat(typeDescription, CoreMatchers.not(TypeDefinition.Sort.describe(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO).getType())));
        assertThat(typeDescription, CoreMatchers.not(new Object()));
        assertThat(typeDescription.equals(null), is(false));
        assertThat(typeDescription.getComponentType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getComponentType().getTypeArguments().size(), is(1));
        assertThat(typeDescription.getComponentType().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(typeDescription.getComponentType().getTypeArguments().getOnly().asErasure().represents(String.class), is(true));
        assertThat(typeDescription.getTypeName(), is(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO).getGenericType().toString()));
    }

    @Test
    public void testGenericArrayOfGenericComponentTypeIterator() throws Exception {
        TypeDescription.Generic typeDescription = describeType(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO));
        Iterator<TypeDefinition> iterator = typeDescription.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((TypeDefinition) typeDescription));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((TypeDefinition) TypeDescription.OBJECT));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayOfGenericComponentTypeNoVariableSource() throws Exception {
        describeType(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO)).getTypeVariableSource();
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayOfGenericComponentTypeNoSymbol() throws Exception {
        describeType(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO)).getSymbol();
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayOfGenericComponentTypeNoUpperBounds() throws Exception {
        describeType(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO)).getUpperBounds();
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayOfGenericComponentTypeNoLowerBounds() throws Exception {
        describeType(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO)).getLowerBounds();
    }

    @Test
    public void testTypeVariableType() throws Exception {
        TypeDescription.Generic typeDescription = describeType(SimpleTypeVariableType.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(typeDescription.getActualName(), is(SimpleTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.getTypeName(), is(SimpleTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.toString(), is(SimpleTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.hashCode(),
                is(TypeDefinition.Sort.describe(SimpleTypeVariableType.class.getDeclaredField(FOO).getGenericType()).hashCode()));
        assertThat(typeDescription, is(TypeDefinition.Sort.describe(SimpleTypeVariableType.class.getDeclaredField(FOO).getGenericType())));
        assertThat(typeDescription,
                CoreMatchers.not(TypeDefinition.Sort.describe(SimpleTypeVariableType.class.getDeclaredField(FOO).getType())));
        assertThat(typeDescription, CoreMatchers.not(new Object()));
        assertThat(typeDescription.equals(null), is(false));
        assertThat(typeDescription.getSymbol(), is(T));
        assertThat(typeDescription.getUpperBounds().size(), is(1));
        assertThat(typeDescription.getUpperBounds().getOnly(), is(TypeDescription.Generic.OBJECT));
        assertThat(typeDescription.getUpperBounds().getOnly().getStackSize(), is(StackSize.SINGLE));
        assertThat(typeDescription.getTypeName(), is(SimpleTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        MatcherAssert.assertThat(typeDescription.getTypeVariableSource(), is((TypeVariableSource) new TypeDescription.ForLoadedType(SimpleTypeVariableType.class)));
        assertThat(typeDescription.getTypeVariableSource().getTypeVariables().size(), is(1));
        assertThat(typeDescription.getTypeVariableSource().getTypeVariables().getOnly(), is(typeDescription));
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableNoLowerBounds() throws Exception {
        describeType(SimpleTypeVariableType.class.getDeclaredField(FOO)).getLowerBounds();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableNoComponentType() throws Exception {
        describeType(SimpleTypeVariableType.class.getDeclaredField(FOO)).getComponentType();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableNoOwnerType() throws Exception {
        describeType(SimpleTypeVariableType.class.getDeclaredField(FOO)).getOwnerType();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableTypeNoSuperClass() throws Exception {
        describeType(SimpleTypeVariableType.class.getDeclaredField(FOO)).getSuperClass();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableTypeNoInterfaceTypes() throws Exception {
        describeType(SimpleTypeVariableType.class.getDeclaredField(FOO)).getInterfaces();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableTypeNoFields() throws Exception {
        describeType(SimpleTypeVariableType.class.getDeclaredField(FOO)).getDeclaredFields();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableTypeNoMethods() throws Exception {
        describeType(SimpleTypeVariableType.class.getDeclaredField(FOO)).getDeclaredMethods();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableTypeNoIterator() throws Exception {
        describeType(SimpleTypeVariableType.class.getDeclaredField(FOO)).iterator();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableNoTypeArguments() throws Exception {
        describeType(SimpleTypeVariableType.class.getDeclaredField(FOO)).getTypeArguments();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableNoBindLocation() throws Exception {
        describeType(SimpleTypeVariableType.class.getDeclaredField(FOO)).findBindingOf(mock(TypeDescription.Generic.class));
    }

    @Test
    public void testSingleUpperBoundTypeVariableType() throws Exception {
        TypeDescription.Generic typeDescription = describeType(SingleUpperBoundTypeVariableType.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(typeDescription.getSymbol(), is(T));
        assertThat(typeDescription.getUpperBounds().size(), is(1));
        assertThat(typeDescription.getUpperBounds().getOnly(), is((TypeDefinition) new TypeDescription.ForLoadedType(String.class)));
        assertThat(typeDescription.getUpperBounds().getOnly().getStackSize(), is(StackSize.SINGLE));
        assertThat(typeDescription.getTypeName(), is(SingleUpperBoundTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.getTypeVariableSource(), is((TypeVariableSource) new TypeDescription.ForLoadedType(SingleUpperBoundTypeVariableType.class)));
        assertThat(typeDescription.getTypeVariableSource().getTypeVariables().size(), is(1));
        assertThat(typeDescription.getTypeVariableSource().getTypeVariables().getOnly(), is(typeDescription));
    }

    @Test
    public void testMultipleUpperBoundTypeVariableType() throws Exception {
        TypeDescription.Generic typeDescription = describeType(MultipleUpperBoundTypeVariableType.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(typeDescription.getSymbol(), is(T));
        assertThat(typeDescription.getStackSize(), is(StackSize.SINGLE));
        assertThat(typeDescription.getUpperBounds().size(), is(3));
        assertThat(typeDescription.getUpperBounds().get(0), is((TypeDefinition) new TypeDescription.ForLoadedType(String.class)));
        assertThat(typeDescription.getUpperBounds().get(1), is((TypeDefinition) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(typeDescription.getUpperBounds().get(2), is((TypeDefinition) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(typeDescription.getTypeName(), is(MultipleUpperBoundTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.getTypeVariableSource(), is((TypeVariableSource) new TypeDescription.ForLoadedType(MultipleUpperBoundTypeVariableType.class)));
        assertThat(typeDescription.getTypeVariableSource().getTypeVariables().size(), is(1));
        assertThat(typeDescription.getTypeVariableSource().getTypeVariables().getOnly(), is(typeDescription));
    }

    @Test
    public void testInterfaceOnlyMultipleUpperBoundTypeVariableType() throws Exception {
        TypeDescription.Generic typeDescription = describeType(InterfaceOnlyMultipleUpperBoundTypeVariableType.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(typeDescription.getSymbol(), is(T));
        assertThat(typeDescription.getStackSize(), is(StackSize.SINGLE));
        assertThat(typeDescription.getUpperBounds().size(), is(2));
        assertThat(typeDescription.getUpperBounds().get(0), is((TypeDefinition) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(typeDescription.getUpperBounds().get(1), is((TypeDefinition) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(typeDescription.getTypeName(), is(InterfaceOnlyMultipleUpperBoundTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.getTypeVariableSource(), is((TypeVariableSource) new TypeDescription.ForLoadedType(InterfaceOnlyMultipleUpperBoundTypeVariableType.class)));
        assertThat(typeDescription.getTypeVariableSource().getTypeVariables().size(), is(1));
        assertThat(typeDescription.getTypeVariableSource().getTypeVariables().getOnly(), is(typeDescription));
    }

    @Test
    public void testShadowedTypeVariableType() throws Exception {
        TypeDescription.Generic typeDescription = describeReturnType(ShadowingTypeVariableType.class.getDeclaredMethod(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(typeDescription.getSymbol(), is(T));
        assertThat(typeDescription.getStackSize(), is(StackSize.SINGLE));
        assertThat(typeDescription.getUpperBounds().size(), is(1));
        assertThat(typeDescription.getUpperBounds().getOnly(), is(TypeDescription.Generic.OBJECT));
        assertThat(typeDescription.getTypeName(), is(ShadowingTypeVariableType.class.getDeclaredMethod(FOO).getGenericReturnType().toString()));
        assertThat(typeDescription.getTypeVariableSource(), is((TypeVariableSource) new MethodDescription.ForLoadedMethod(ShadowingTypeVariableType.class.getDeclaredMethod(FOO))));
        assertThat(typeDescription.getTypeVariableSource().getTypeVariables().size(), is(1));
        assertThat(typeDescription.getTypeVariableSource().getTypeVariables().getOnly(), is(typeDescription));
    }

    @Test
    public void testNestedTypeVariableType() throws Exception {
        TypeDescription.Generic typeDescription = describeType(NestedTypeVariableType.class.getDeclaredField(FOO));
        assertThat(typeDescription.getTypeName(), is(NestedTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getStackSize(), is(StackSize.SINGLE));
        assertThat(typeDescription.getTypeArguments().size(), is(0));
        Type ownerType = ((ParameterizedType) NestedTypeVariableType.class.getDeclaredField(FOO).getGenericType()).getOwnerType();
        assertThat(typeDescription.getOwnerType(), is(TypeDefinition.Sort.describe(ownerType)));
        assertThat(typeDescription.getOwnerType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getOwnerType().getTypeArguments().size(), is(1));
        assertThat(typeDescription.getOwnerType().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(typeDescription.getOwnerType().getTypeArguments().getOnly().getSymbol(), is(T));
    }

    @Test
    public void testNestedSpecifiedTypeVariableType() throws Exception {
        TypeDescription.Generic typeDescription = describeType(NestedSpecifiedTypeVariableType.class.getDeclaredField(FOO));
        assertThat(typeDescription.getTypeName(), is(NestedSpecifiedTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getStackSize(), is(StackSize.SINGLE));
        assertThat(typeDescription.getTypeArguments().size(), is(0));
        Type ownerType = ((ParameterizedType) NestedSpecifiedTypeVariableType.class.getDeclaredField(FOO).getGenericType()).getOwnerType();
        assertThat(typeDescription.getOwnerType(), is(TypeDefinition.Sort.describe(ownerType)));
        assertThat(typeDescription.getOwnerType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getOwnerType().getTypeArguments().size(), is(1));
        assertThat(typeDescription.getOwnerType().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getOwnerType().getTypeArguments().getOnly(), is((TypeDefinition) new TypeDescription.ForLoadedType(String.class)));
    }

    @Test
    public void testNestedStaticTypeVariableType() throws Exception {
        TypeDescription.Generic typeDescription = describeType(NestedStaticTypeVariableType.class.getDeclaredField(FOO));
        assertThat(typeDescription.getTypeName(), is(NestedStaticTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getStackSize(), is(StackSize.SINGLE));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        assertThat(typeDescription.getTypeArguments().getOnly(), is((TypeDefinition) new TypeDescription.ForLoadedType(String.class)));
        Type ownerType = ((ParameterizedType) NestedStaticTypeVariableType.class.getDeclaredField(FOO).getGenericType()).getOwnerType();
        assertThat(typeDescription.getOwnerType(), is(TypeDefinition.Sort.describe(ownerType)));
        assertThat(typeDescription.getOwnerType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
    }

    @Test
    public void testNestedInnerType() throws Exception {
        TypeDescription.Generic foo = describeReturnType(NestedInnerType.InnerType.class.getDeclaredMethod(FOO));
        assertThat(foo.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(foo.getSymbol(), is(T));
        assertThat(foo.getUpperBounds().size(), is(1));
        assertThat(foo.getUpperBounds().getOnly(), is(TypeDescription.Generic.OBJECT));
        assertThat(foo.getTypeVariableSource(), is((TypeVariableSource) new TypeDescription.ForLoadedType(NestedInnerType.class)));
        TypeDescription.Generic bar = describeReturnType(NestedInnerType.InnerType.class.getDeclaredMethod(BAR));
        assertThat(bar.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(bar.getSymbol(), is(S));
        assertThat(bar.getUpperBounds().size(), is(1));
        assertThat(bar.getUpperBounds().getOnly(), is(foo));
        assertThat(bar.getTypeVariableSource(), is((TypeVariableSource) new TypeDescription.ForLoadedType(NestedInnerType.InnerType.class)));
        TypeDescription.Generic qux = describeReturnType(NestedInnerType.InnerType.class.getDeclaredMethod(QUX));
        assertThat(qux.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(qux.getSymbol(), is(U));
        assertThat(qux.getUpperBounds().size(), is(1));
        assertThat(qux.getUpperBounds().getOnly(), is(bar));
        MethodDescription quxMethod = new MethodDescription.ForLoadedMethod(NestedInnerType.InnerType.class.getDeclaredMethod(QUX));
        assertThat(qux.getTypeVariableSource(), is((TypeVariableSource) quxMethod));
    }

    @Test
    public void testNestedInnerMethod() throws Exception {
        Class<?> innerType = new NestedInnerMethod().foo();
        TypeDescription.Generic foo = describeReturnType(innerType.getDeclaredMethod(FOO));
        assertThat(foo.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(foo.getSymbol(), is(T));
        assertThat(foo.getUpperBounds().size(), is(1));
        assertThat(foo.getUpperBounds().getOnly(), is(TypeDescription.Generic.OBJECT));
        assertThat(foo.getTypeVariableSource(), is((TypeVariableSource) new TypeDescription.ForLoadedType(NestedInnerMethod.class)));
        TypeDescription.Generic bar = describeReturnType(innerType.getDeclaredMethod(BAR));
        assertThat(bar.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(bar.getSymbol(), is(S));
        assertThat(bar.getUpperBounds().size(), is(1));
        assertThat(bar.getUpperBounds().getOnly(), is(foo));
        assertThat(bar.getTypeVariableSource(), is((TypeVariableSource) new MethodDescription.ForLoadedMethod(NestedInnerMethod.class.getDeclaredMethod(FOO))));
        TypeDescription.Generic qux = describeReturnType(innerType.getDeclaredMethod(QUX));
        assertThat(qux.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(qux.getSymbol(), is(U));
        assertThat(qux.getUpperBounds().size(), is(1));
        assertThat(qux.getUpperBounds().getOnly(), is(bar));
        assertThat(qux.getTypeVariableSource(), is((TypeVariableSource) new TypeDescription.ForLoadedType(innerType)));
        TypeDescription.Generic baz = describeReturnType(innerType.getDeclaredMethod(BAZ));
        assertThat(baz.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(baz.getSymbol(), is(V));
        assertThat(baz.getUpperBounds().size(), is(1));
        assertThat(baz.getUpperBounds().getOnly(), is(qux));
        assertThat(baz.getTypeVariableSource(), is((TypeVariableSource) new MethodDescription.ForLoadedMethod(innerType.getDeclaredMethod(BAZ))));

    }

    @Test
    public void testRecursiveTypeVariable() throws Exception {
        TypeDescription.Generic typeDescription = describeType(RecursiveTypeVariable.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(typeDescription.getSymbol(), is(T));
        assertThat(typeDescription.getUpperBounds().size(), is(1));
        TypeDescription.Generic upperBound = typeDescription.getUpperBounds().getOnly();
        assertThat(upperBound.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(upperBound.asErasure(), is(typeDescription.asErasure()));
        assertThat(upperBound.getTypeArguments().size(), is(1));
        assertThat(upperBound.getTypeArguments().getOnly(), is(typeDescription));
    }

    @Test
    public void testBackwardsReferenceTypeVariable() throws Exception {
        TypeDescription.Generic foo = describeType(BackwardsReferenceTypeVariable.class.getDeclaredField(FOO));
        assertThat(foo.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(foo.getSymbol(), is(S));
        assertThat(foo.getUpperBounds().size(), is(1));
        TypeDescription backwardsReference = new TypeDescription.ForLoadedType(BackwardsReferenceTypeVariable.class);
        assertThat(foo.getUpperBounds().getOnly(), is(backwardsReference.getTypeVariables().filter(named(T)).getOnly()));
        TypeDescription.Generic bar = describeType(BackwardsReferenceTypeVariable.class.getDeclaredField(BAR));
        assertThat(bar.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(bar.getSymbol(), is(T));
        assertThat(bar.getUpperBounds().size(), is(1));
        assertThat(bar.getUpperBounds().getOnly(), is(TypeDescription.Generic.OBJECT));
    }

    @Test
    public void testParameterizedTypeSuperClassResolution() throws Exception {
        TypeDescription.Generic typeDescription = describeType(TypeResolution.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        TypeDescription.Generic superClass = typeDescription.getSuperClass();
        assertThat(superClass.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(superClass.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.Base.class)));
        assertThat(superClass.getTypeArguments().size(), is(2));
        assertThat(superClass.getTypeArguments().get(0), is((TypeDefinition) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(superClass.getTypeArguments().get(1), is((TypeDefinition) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(superClass.getDeclaredFields().size(), is(1));
        assertThat(superClass.getDeclaredFields().getOnly().getDeclaringType(), is(superClass));
        TypeDescription.Generic fieldType = superClass.getDeclaredFields().getOnly().getType();
        assertThat(fieldType.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(fieldType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(fieldType.getTypeArguments().size(), is(2));
        assertThat(fieldType.getTypeArguments().get(0), is((TypeDefinition) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(fieldType.getTypeArguments().get(1), is((TypeDefinition) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(superClass.getDeclaredMethods().filter(isConstructor()).size(), is(1));
        assertThat(superClass.getDeclaredMethods().filter(isMethod()).size(), is(1));
        assertThat(superClass.getDeclaredMethods().filter(isMethod()).getOnly().getDeclaringType(), is((superClass)));
        assertThat(superClass.getDeclaredMethods().filter(isConstructor()).getOnly().getDeclaringType(), is((superClass)));
        TypeDescription.Generic methodReturnType = superClass.getDeclaredMethods().filter(isMethod()).getOnly().getReturnType();
        assertThat(methodReturnType.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(methodReturnType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(methodReturnType.getTypeArguments().size(), is(2));
        assertThat(methodReturnType.getTypeArguments().get(0), is((TypeDefinition) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodReturnType.getTypeArguments().get(1), is((TypeDefinition) new TypeDescription.ForLoadedType(Bar.class)));
        TypeDescription.Generic methodParameterType = superClass.getDeclaredMethods().filter(isMethod()).getOnly().getParameters().asTypeList().getOnly();
        assertThat(methodParameterType.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(methodParameterType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(methodParameterType.getTypeArguments().size(), is(2));
        assertThat(methodParameterType.getTypeArguments().get(0), is((TypeDefinition) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodParameterType.getTypeArguments().get(1), is((TypeDefinition) new TypeDescription.ForLoadedType(Bar.class)));
    }

    @Test
    public void testParameterizedTypeFindBoundValue() throws Exception {
        TypeDescription.Generic typeDescription = describeType(TypeResolution.class.getDeclaredField(FOO));
        assertThat(typeDescription.findBindingOf(typeDescription.asErasure().getTypeVariables().getOnly()),
                is(typeDescription.getTypeArguments().getOnly()));
        assertThat(typeDescription.findBindingOf(typeDescription.getOwnerType().asErasure().getTypeVariables().getOnly()),
                is(typeDescription.getOwnerType().getTypeArguments().getOnly()));
        assertThat(typeDescription.findBindingOf(mock(TypeDescription.Generic.class)),
                nullValue(TypeDescription.Generic.class));
    }

    @Test
    public void testParameterizedTypeInterfaceResolution() throws Exception {
        TypeDescription.Generic typeDescription = describeType(TypeResolution.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        assertThat(typeDescription.getInterfaces().size(), is(1));
        TypeDescription.Generic interfaceType = typeDescription.getInterfaces().getOnly();
        assertThat(interfaceType.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(interfaceType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.BaseInterface.class)));
        assertThat(interfaceType.getTypeArguments().size(), is(2));
        assertThat(interfaceType.getTypeArguments().get(0), is((TypeDefinition) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(interfaceType.getTypeArguments().get(1), is((TypeDefinition) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(interfaceType.getDeclaredFields().size(), is(0));
        assertThat(interfaceType.getDeclaredMethods().filter(isConstructor()).size(), is(0));
        assertThat(interfaceType.getDeclaredMethods().filter(isMethod()).size(), is(1));
        assertThat(interfaceType.getDeclaredMethods().filter(isMethod()).getOnly().getDeclaringType(), is((interfaceType)));
        TypeDescription.Generic methodReturnType = interfaceType.getDeclaredMethods().filter(isMethod()).getOnly().getReturnType();
        assertThat(methodReturnType.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(methodReturnType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(methodReturnType.getTypeArguments().size(), is(2));
        assertThat(methodReturnType.getTypeArguments().get(0), is((TypeDefinition) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodReturnType.getTypeArguments().get(1), is((TypeDefinition) new TypeDescription.ForLoadedType(Bar.class)));
        TypeDescription.Generic methodParameterType = interfaceType.getDeclaredMethods().filter(isMethod()).getOnly().getParameters().asTypeList().getOnly();
        assertThat(methodParameterType.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(methodParameterType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(methodParameterType.getTypeArguments().size(), is(2));
        assertThat(methodParameterType.getTypeArguments().get(0), is((TypeDefinition) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodParameterType.getTypeArguments().get(1), is((TypeDefinition) new TypeDescription.ForLoadedType(Bar.class)));
    }

    @Test
    public void testParameterizedTypeRawSuperClassResolution() throws Exception {
        TypeDescription.Generic typeDescription = describeType(TypeResolution.class.getDeclaredField(BAR));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        TypeDescription.Generic superClass = typeDescription.getSuperClass();
        assertThat(superClass.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(superClass.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.Base.class)));
        assertThat(superClass.getDeclaredFields().size(), is(1));
        assertThat(superClass.getDeclaredFields().getOnly().getDeclaringType().getDeclaredFields().getOnly().getType(),
                is(superClass.getDeclaredFields().getOnly().getType()));
        TypeDescription.Generic fieldType = superClass.getDeclaredFields().getOnly().getType();
        assertThat(fieldType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(fieldType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        TypeDescription.Generic methodReturnType = superClass.getDeclaredMethods().filter(isMethod()).getOnly().getReturnType();
        assertThat(methodReturnType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(methodReturnType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        TypeDescription.Generic methodParameterType = superClass.getDeclaredMethods().filter(isMethod()).getOnly().getParameters().asTypeList().getOnly();
        assertThat(methodParameterType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(methodParameterType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(superClass.getDeclaredMethods().filter(isMethod()).getOnly().getDeclaringType().getDeclaredMethods().filter(isMethod()).getOnly().getReturnType(),
                is(superClass.getDeclaredMethods().filter(isMethod()).getOnly().getReturnType()));
        assertThat(superClass.getDeclaredMethods().filter(isMethod()).getOnly().getDeclaringType().getDeclaredMethods().filter(isMethod()).getOnly().getParameters().getOnly().getType(),
                is(superClass.getDeclaredMethods().filter(isMethod()).getOnly().getParameters().getOnly().getType()));
    }

    @Test
    public void testParameterizedTypeRawInterfaceTypeResolution() throws Exception {
        TypeDescription.Generic typeDescription = describeType(TypeResolution.class.getDeclaredField(BAR));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        TypeDescription.Generic interfaceType = typeDescription.getInterfaces().getOnly();
        assertThat(interfaceType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(interfaceType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.BaseInterface.class)));
        assertThat(interfaceType.getDeclaredFields().size(), is(0));
        TypeDescription.Generic methodReturnType = interfaceType.getDeclaredMethods().filter(isMethod()).getOnly().getReturnType();
        assertThat(methodReturnType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(methodReturnType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        TypeDescription.Generic methodParameterType = interfaceType.getDeclaredMethods().filter(isMethod()).getOnly().getParameters().asTypeList().getOnly();
        assertThat(methodParameterType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(methodParameterType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(interfaceType.getDeclaredMethods().getOnly().getDeclaringType().getDeclaredMethods().getOnly().getReturnType(),
                is(interfaceType.getDeclaredMethods().getOnly().getReturnType()));
        assertThat(interfaceType.getDeclaredMethods().getOnly().getDeclaringType().getDeclaredMethods().getOnly().getParameters().getOnly().getType(),
                is(interfaceType.getDeclaredMethods().getOnly().getParameters().getOnly().getType()));
    }

    @Test
    public void testParameterizedTypePartiallyRawSuperClassResolution() throws Exception {
        TypeDescription.Generic typeDescription = describeType(TypeResolution.class.getDeclaredField(QUX));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        TypeDescription.Generic superClass = typeDescription.getSuperClass();
        assertThat(superClass.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(superClass.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.Intermediate.class)));
        TypeDescription.Generic superSuperClass = superClass.getSuperClass();
        assertThat(superSuperClass.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(superSuperClass.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.Base.class)));
    }

    @Test
    public void testParameterizedTypePartiallyRawInterfaceTypeResolution() throws Exception {
        TypeDescription.Generic typeDescription = describeType(TypeResolution.class.getDeclaredField(QUX));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        TypeDescription.Generic superClass = typeDescription.getSuperClass();
        assertThat(superClass.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(superClass.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.Intermediate.class)));
        TypeDescription.Generic superInterfaceType = superClass.getInterfaces().getOnly();
        assertThat(superInterfaceType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(superInterfaceType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.BaseInterface.class)));
    }

    @Test
    public void testParameterizedTypeNestedPartiallyRawSuperClassResolution() throws Exception {
        TypeDescription.Generic typeDescription = describeType(TypeResolution.class.getDeclaredField(BAZ));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        TypeDescription.Generic superClass = typeDescription.getSuperClass();
        assertThat(superClass.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(superClass.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.NestedIntermediate.class)));
        TypeDescription.Generic superSuperClass = superClass.getSuperClass();
        assertThat(superSuperClass.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(superSuperClass.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.Base.class)));
    }

    @Test
    public void testParameterizedTypeNestedPartiallyRawInterfaceTypeResolution() throws Exception {
        TypeDescription.Generic typeDescription = describeType(TypeResolution.class.getDeclaredField(BAZ));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        TypeDescription.Generic superClass = typeDescription.getSuperClass();
        assertThat(superClass.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(superClass.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.NestedIntermediate.class)));
        TypeDescription.Generic superInterfaceType = superClass.getInterfaces().getOnly();
        assertThat(superInterfaceType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(superInterfaceType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.BaseInterface.class)));
    }

    @Test
    public void testShadowedTypeSuperClassResolution() throws Exception {
        TypeDescription.Generic typeDescription = describeType(TypeResolution.class.getDeclaredField(FOO + BAR));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getTypeArguments().size(), is(2));
        TypeDescription.Generic superClass = typeDescription.getSuperClass();
        assertThat(superClass.getTypeArguments().size(), is(2));
        assertThat(superClass.getTypeArguments().get(0).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(superClass.getTypeArguments().get(0), is((TypeDefinition) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(superClass.getTypeArguments().get(1).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(superClass.getTypeArguments().get(1), is((TypeDefinition) new TypeDescription.ForLoadedType(Foo.class)));
    }

    @Test
    public void testShadowedTypeInterfaceTypeResolution() throws Exception {
        TypeDescription.Generic typeDescription = describeType(TypeResolution.class.getDeclaredField(FOO + BAR));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getTypeArguments().size(), is(2));
        TypeDescription.Generic interfaceType = typeDescription.getInterfaces().getOnly();
        assertThat(interfaceType.getTypeArguments().size(), is(2));
        assertThat(interfaceType.getTypeArguments().get(0).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(interfaceType.getTypeArguments().get(0), is((TypeDefinition) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(interfaceType.getTypeArguments().get(1).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(interfaceType.getTypeArguments().get(1), is((TypeDefinition) new TypeDescription.ForLoadedType(Foo.class)));
    }

    @Test
    public void testMethodTypeVariableIsRetained() throws Exception {
        TypeDescription.Generic typeDescription = describeType(MemberVariable.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getTypeArguments().size(), is(2));
        assertThat(typeDescription.getTypeArguments().get(0).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getTypeArguments().get(0).asErasure().represents(Number.class), is(true));
        assertThat(typeDescription.getTypeArguments().get(1).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getTypeArguments().get(1).asErasure().represents(Integer.class), is(true));
        MethodDescription methodDescription = typeDescription.getDeclaredMethods().filter(named(FOO)).getOnly();
        assertThat(methodDescription.getReturnType().getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(methodDescription.getReturnType().getSymbol(), is("S"));
        assertThat(methodDescription.getReturnType().getTypeVariableSource(), is((TypeVariableSource) methodDescription.asDefined()));
    }

    @Test
    public void testShadowedMethodTypeVariableIsRetained() throws Exception {
        TypeDescription.Generic typeDescription = describeType(MemberVariable.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getTypeArguments().size(), is(2));
        assertThat(typeDescription.getTypeArguments().get(0).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getTypeArguments().get(0).asErasure().represents(Number.class), is(true));
        assertThat(typeDescription.getTypeArguments().get(1).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getTypeArguments().get(1).asErasure().represents(Integer.class), is(true));
        MethodDescription methodDescription = typeDescription.getDeclaredMethods().filter(named(BAR)).getOnly();
        assertThat(methodDescription.getReturnType().getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(methodDescription.getReturnType().getSymbol(), is("T"));
        assertThat(methodDescription.getReturnType().getTypeVariableSource(), is((TypeVariableSource) methodDescription.asDefined()));
    }

    @Test
    public void testMethodTypeVariableWithExtensionIsRetained() throws Exception {
        TypeDescription.Generic typeDescription = describeType(MemberVariable.class.getDeclaredField(FOO));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getTypeArguments().size(), is(2));
        assertThat(typeDescription.getTypeArguments().get(0).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getTypeArguments().get(0).asErasure().represents(Number.class), is(true));
        assertThat(typeDescription.getTypeArguments().get(1).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getTypeArguments().get(1).asErasure().represents(Integer.class), is(true));
        MethodDescription methodDescription = typeDescription.getDeclaredMethods().filter(named(QUX)).getOnly();
        assertThat(methodDescription.getReturnType().getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(methodDescription.getReturnType().getSymbol(), is("S"));
        assertThat(methodDescription.getReturnType().getTypeVariableSource(), is((TypeVariableSource) methodDescription.asDefined()));
        assertThat(methodDescription.getReturnType().getUpperBounds().size(), is(1));
        assertThat(methodDescription.getReturnType().getUpperBounds().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(methodDescription.getReturnType().getUpperBounds().getOnly().asErasure().represents(Number.class), is(true));
    }

    @Test
    public void testMethodTypeVariableErasedBound() throws Exception {
        TypeDescription.Generic typeDescription = describeType(MemberVariable.class.getDeclaredField(BAR)).getSuperClass();
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        MethodDescription methodDescription = typeDescription.getDeclaredMethods().filter(named(FOO)).getOnly();
        assertThat(methodDescription.getReturnType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(methodDescription.getReturnType().asErasure(), is(TypeDescription.OBJECT));
    }

    @Test
    public void testMethodTypeVariableWithExtensionErasedBound() throws Exception {
        TypeDescription.Generic typeDescription = describeType(MemberVariable.class.getDeclaredField(BAR)).getSuperClass();
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        MethodDescription methodDescription = typeDescription.getDeclaredMethods().filter(named(QUX)).getOnly();
        assertThat(methodDescription.getReturnType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(methodDescription.getReturnType().asErasure(), is(TypeDescription.OBJECT));
    }

    @Test
    public void testGenericFieldHashCode() throws Exception {
        TypeDescription.Generic typeDescription = describeType(MemberVariable.class.getDeclaredField(FOO));
        assertThat(typeDescription.getDeclaredFields().filter(named(FOO)).getOnly().hashCode(),
                CoreMatchers.not(new FieldDescription.ForLoadedField(MemberVariable.class.getDeclaredField(FOO)).hashCode()));
        assertThat(typeDescription.getDeclaredFields().filter(named(FOO)).getOnly().asDefined().hashCode(),
                is(new FieldDescription.ForLoadedField(MemberVariable.class.getDeclaredField(FOO)).hashCode()));
    }

    @Test
    public void testGenericFieldEquality() throws Exception {
        TypeDescription.Generic typeDescription = describeType(MemberVariable.class.getDeclaredField(FOO));
        assertThat(typeDescription.getDeclaredFields().filter(named(FOO)).getOnly(),
                CoreMatchers.not((FieldDescription) new FieldDescription.ForLoadedField(MemberVariable.class.getDeclaredField(FOO))));
        assertThat(typeDescription.getDeclaredFields().filter(named(FOO)).getOnly().asDefined(),
                is((FieldDescription) new FieldDescription.ForLoadedField(MemberVariable.class.getDeclaredField(FOO))));
    }

    @Test
    public void testGenericMethodHashCode() throws Exception {
        TypeDescription.Generic typeDescription = describeType(MemberVariable.class.getDeclaredField(FOO));
        assertThat(typeDescription.getDeclaredMethods().filter(named(FOO)).getOnly().hashCode(),
                CoreMatchers.not(new MethodDescription.ForLoadedMethod(MemberVariable.class.getDeclaredMethod(FOO)).hashCode()));
        assertThat(typeDescription.getDeclaredMethods().filter(named(FOO)).getOnly().asDefined().hashCode(),
                is(new MethodDescription.ForLoadedMethod(MemberVariable.class.getDeclaredMethod(FOO)).hashCode()));
    }

    @Test
    public void testGenericMethodEquality() throws Exception {
        TypeDescription.Generic typeDescription = describeType(MemberVariable.class.getDeclaredField(FOO));
        assertThat(typeDescription.getDeclaredMethods().filter(named(FOO)).getOnly(),
                CoreMatchers.not((MethodDescription) new MethodDescription.ForLoadedMethod(MemberVariable.class.getDeclaredMethod(FOO))));
        assertThat(typeDescription.getDeclaredMethods().filter(named(FOO)).getOnly().asDefined(),
                is((MethodDescription) new MethodDescription.ForLoadedMethod(MemberVariable.class.getDeclaredMethod(FOO))));
    }

    @Test
    public void testGenericParameterHashCode() throws Exception {
        TypeDescription.Generic typeDescription = describeType(MemberVariable.class.getDeclaredField(FOO));
        assertThat(typeDescription.getDeclaredMethods().filter(named(BAZ)).getOnly().getParameters().getOnly().hashCode(), CoreMatchers.not(
                new MethodDescription.ForLoadedMethod(MemberVariable.class.getDeclaredMethod(BAZ, Object.class)).getParameters().getOnly().hashCode()));
        assertThat(typeDescription.getDeclaredMethods().filter(named(BAZ)).getOnly().getParameters().getOnly().asDefined().hashCode(), is(
                new MethodDescription.ForLoadedMethod(MemberVariable.class.getDeclaredMethod(BAZ, Object.class)).getParameters().getOnly().hashCode()));
    }

    @Test
    public void testGenericParameterEquality() throws Exception {
        TypeDescription.Generic typeDescription = describeType(MemberVariable.class.getDeclaredField(FOO));
        assertThat(typeDescription.getDeclaredMethods().filter(named(BAZ)).getOnly().getParameters().getOnly(), CoreMatchers.not((ParameterDescription)
                new MethodDescription.ForLoadedMethod(MemberVariable.class.getDeclaredMethod(BAZ, Object.class)).getParameters().getOnly()));
        assertThat(typeDescription.getDeclaredMethods().filter(named(BAZ)).getOnly().getParameters().getOnly().asDefined(), is((ParameterDescription)
                new MethodDescription.ForLoadedMethod(MemberVariable.class.getDeclaredMethod(BAZ, Object.class)).getParameters().getOnly()));
    }

    @Test
    public void testGenericTypeInconsistency() throws Exception {
        TypeDescription.Generic typeDescription = describeType(GenericDisintegrator.make());
        assertThat(typeDescription.getInterfaces().size(), is(2));
        assertThat(typeDescription.getInterfaces().get(0).getSort(), is(TypeDescription.Generic.Sort.NON_GENERIC));
        assertThat(typeDescription.getInterfaces().get(0).asErasure().represents(Callable.class), is(true));
        assertThat(typeDescription.getInterfaces().get(1).getSort(), is(TypeDescription.Generic.Sort.NON_GENERIC));
        assertThat(typeDescription.getInterfaces().get(1).represents(Serializable.class), is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(named(FOO)).getOnly().getParameters().size(), is(2));
        assertThat(typeDescription.getDeclaredMethods().filter(named(FOO)).getOnly().getParameters().get(0).getType().getSort(),
                is(TypeDescription.Generic.Sort.NON_GENERIC));
        assertThat(typeDescription.getDeclaredMethods().filter(named(FOO)).getOnly().getParameters().get(0).getType().asErasure().represents(Exception.class),
                is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(named(FOO)).getOnly().getParameters().get(1).getType().getSort(),
                is(TypeDescription.Generic.Sort.NON_GENERIC));
        assertThat(typeDescription.getDeclaredMethods().filter(named(FOO)).getOnly().getParameters().get(1).getType().represents(Void.class),
                is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(named(FOO)).getOnly().getExceptionTypes().size(), is(2));
        assertThat(typeDescription.getDeclaredMethods().filter(named(FOO)).getOnly().getExceptionTypes().get(0).getSort(),
                is(TypeDescription.Generic.Sort.NON_GENERIC));
        assertThat(typeDescription.getDeclaredMethods().filter(named(FOO)).getOnly().getExceptionTypes().get(0).asErasure().represents(Exception.class),
                is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(named(FOO)).getOnly().getExceptionTypes().get(1).getSort(),
                is(TypeDescription.Generic.Sort.NON_GENERIC));
        assertThat(typeDescription.getDeclaredMethods().filter(named(FOO)).getOnly().getExceptionTypes().get(1).represents(RuntimeException.class),
                is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(isConstructor()).getOnly().getParameters().size(), is(2));
        assertThat(typeDescription.getDeclaredMethods().filter(isConstructor()).getOnly().getParameters().get(0).getType().getSort(),
                is(TypeDescription.Generic.Sort.NON_GENERIC));
        assertThat(typeDescription.getDeclaredMethods().filter(isConstructor()).getOnly().getParameters().get(0).getType().asErasure().represents(Exception.class),
                is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(isConstructor()).getOnly().getParameters().get(1).getType().getSort(),
                is(TypeDescription.Generic.Sort.NON_GENERIC));
        assertThat(typeDescription.getDeclaredMethods().filter(isConstructor()).getOnly().getParameters().get(1).getType().represents(Void.class),
                is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(isConstructor()).getOnly().getExceptionTypes().size(), is(2));
        assertThat(typeDescription.getDeclaredMethods().filter(isConstructor()).getOnly().getExceptionTypes().get(0).getSort(),
                is(TypeDescription.Generic.Sort.NON_GENERIC));
        assertThat(typeDescription.getDeclaredMethods().filter(isConstructor()).getOnly().getExceptionTypes().get(0).asErasure().represents(Exception.class),
                is(true));
        assertThat(typeDescription.getDeclaredMethods().filter(isConstructor()).getOnly().getExceptionTypes().get(1).getSort(),
                is(TypeDescription.Generic.Sort.NON_GENERIC));
        assertThat(typeDescription.getDeclaredMethods().filter(isConstructor()).getOnly().getExceptionTypes().get(1).represents(RuntimeException.class),
                is(true));
    }

    @Test
    public void testRepresents() throws Exception {
        assertThat(describeType(SimpleParameterizedType.class.getDeclaredField(FOO))
                .represents(SimpleParameterizedType.class.getDeclaredField(FOO).getGenericType()), is(true));
        assertThat(describeType(SimpleParameterizedType.class.getDeclaredField(FOO))
                .represents(List.class), is(false));
    }

    @Test
    public void testRawType() throws Exception {
        TypeDescription.Generic type = describeType(RawType.class.getDeclaredField(FOO)).getSuperClass().getSuperClass();
        FieldDescription fieldDescription = type.getDeclaredFields().filter(named(BAR)).getOnly();
        assertThat(fieldDescription.getType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(fieldDescription.getType().asErasure(), is(TypeDescription.OBJECT));
    }

    @Test
    public void testIntermediateRawType() throws Exception {
        TypeDescription.Generic type = describeType(IntermediateRaw.class.getDeclaredField(FOO)).getSuperClass().getSuperClass().getSuperClass();
        FieldDescription fieldDescription = type.getDeclaredFields().filter(named(BAR)).getOnly();
        assertThat(fieldDescription.getType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(fieldDescription.getType().asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Integer.class)));
    }

    @Test
    public void testMixedTypeVariables() throws Exception {
        MethodDescription methodDescription = describeInterfaceType(MixedTypeVariables.Inner.class, 0).getDeclaredMethods().getOnly();
        assertThat(methodDescription.getParameters().getOnly().getType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(methodDescription.getParameters().getOnly().getType().asRawType().represents(MixedTypeVariables.SampleType.class), is(true));
        assertThat(methodDescription.getParameters().getOnly().getType().getTypeArguments().get(0), is(methodDescription.getTypeVariables().getOnly()));
        assertThat(methodDescription.getParameters().getOnly().getType().getTypeArguments().get(1).represents(Void.class), is(true));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeAnnotationsFieldType() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        Class<?> samples = Class.forName(TYPE_ANNOTATION_SAMPLES);
        TypeDescription.Generic fieldType = describeType(samples.getDeclaredField(FOO));
        assertThat(fieldType.getSort(), is(TypeDefinition.Sort.GENERIC_ARRAY));
        assertThat(fieldType.getDeclaredAnnotations().size(), is(1));
        assertThat(fieldType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(fieldType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(24));
        assertThat(fieldType.getComponentType().getSort(), is(TypeDefinition.Sort.GENERIC_ARRAY));
        assertThat(fieldType.getComponentType().getDeclaredAnnotations().size(), is(1));
        assertThat(fieldType.getComponentType().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(fieldType.getComponentType().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(25));
        assertThat(fieldType.getComponentType().getComponentType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(fieldType.getComponentType().getComponentType().getDeclaredAnnotations().size(), is(1));
        assertThat(fieldType.getComponentType().getComponentType().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(fieldType.getComponentType().getComponentType().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(22));
        assertThat(fieldType.getComponentType().getComponentType().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.WILDCARD));
        assertThat(fieldType.getComponentType().getComponentType().getTypeArguments().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(fieldType.getComponentType().getComponentType().getTypeArguments().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(fieldType.getComponentType().getComponentType().getTypeArguments().getOnly().getDeclaredAnnotations()
                .ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(23));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeAnnotationsMethodReturnType() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        Class<?> samples = Class.forName(TYPE_ANNOTATION_SAMPLES);
        TypeDescription.Generic returnType = describeReturnType(samples.getDeclaredMethod(FOO, Exception[][].class));
        assertThat(returnType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(returnType.getDeclaredAnnotations().size(), is(1));
        assertThat(returnType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(returnType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(28));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeAnnotationsMethodParameterType() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        Class<?> samples = Class.forName(TYPE_ANNOTATION_SAMPLES);
        TypeDescription.Generic parameterType = describeParameterType(samples.getDeclaredMethod(FOO, Exception[][].class), 0);
        assertThat(parameterType.getSort(), is(TypeDefinition.Sort.GENERIC_ARRAY));
        assertThat(parameterType.getDeclaredAnnotations().size(), is(1));
        assertThat(parameterType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(parameterType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(30));
        assertThat(parameterType.getComponentType().getSort(), is(TypeDefinition.Sort.GENERIC_ARRAY));
        assertThat(parameterType.getComponentType().getDeclaredAnnotations().size(), is(1));
        assertThat(parameterType.getComponentType().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(parameterType.getComponentType().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(31));
        assertThat(parameterType.getComponentType().getComponentType().getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(parameterType.getComponentType().getComponentType().getDeclaredAnnotations().size(), is(1));
        assertThat(parameterType.getComponentType().getComponentType().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(parameterType.getComponentType().getComponentType().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(29));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeAnnotationsSuperType() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        Class<?> samples = Class.forName(TYPE_ANNOTATION_SAMPLES);
        TypeDescription.Generic superClass = describeSuperClass(samples);
        assertThat(superClass.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(superClass.getDeclaredAnnotations().size(), is(1));
        assertThat(superClass.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(superClass.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(18));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeAnnotationsInterfaceType() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        Class<?> samples = Class.forName(TYPE_ANNOTATION_SAMPLES);
        TypeDescription.Generic firstInterfaceType = describeInterfaceType(samples, 0);
        assertThat(firstInterfaceType.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(firstInterfaceType.getDeclaredAnnotations().size(), is(1));
        assertThat(firstInterfaceType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(firstInterfaceType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(19));
        assertThat(firstInterfaceType.getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(firstInterfaceType.getTypeArguments().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(firstInterfaceType.getTypeArguments().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(firstInterfaceType.getTypeArguments().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(20));
        TypeDescription.Generic secondInterfaceType = describeInterfaceType(samples, 1);
        assertThat(secondInterfaceType.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(secondInterfaceType.getDeclaredAnnotations().size(), is(0));
        assertThat(secondInterfaceType.getTypeArguments().get(0).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(secondInterfaceType.getTypeArguments().get(0).getDeclaredAnnotations().size(), is(1));
        assertThat(secondInterfaceType.getTypeArguments().get(0).getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(secondInterfaceType.getTypeArguments().get(0).getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(21));
        assertThat(secondInterfaceType.getTypeArguments().get(1).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(secondInterfaceType.getTypeArguments().get(1).getDeclaredAnnotations().size(), is(0));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeAnnotationExceptionType() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        Class<?> samples = Class.forName(TYPE_ANNOTATION_SAMPLES);
        TypeDescription.Generic firstExceptionType = describeExceptionType(samples.getDeclaredMethod(FOO, Exception[][].class), 0);
        assertThat(firstExceptionType.getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(firstExceptionType.getDeclaredAnnotations().size(), is(1));
        assertThat(firstExceptionType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(firstExceptionType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(32));
        TypeDescription.Generic secondExceptionType = describeExceptionType(samples.getDeclaredMethod(FOO, Exception[][].class), 1);
        assertThat(secondExceptionType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(secondExceptionType.getDeclaredAnnotations().size(), is(1));
        assertThat(secondExceptionType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(secondExceptionType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(33));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeAnnotationOnNonGenericField() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        Class<?> samples = Class.forName(TYPE_ANNOTATION_OTHER_SAMPLES);
        TypeDescription.Generic fieldType = describeType(samples.getDeclaredField(FOO));
        assertThat(fieldType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(fieldType.getDeclaredAnnotations().size(), is(1));
        assertThat(fieldType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(fieldType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(0));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeAnnotationOnNonGenericReturnType() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        Class<?> samples = Class.forName(TYPE_ANNOTATION_OTHER_SAMPLES);
        TypeDescription.Generic returnType = describeReturnType(samples.getDeclaredMethod(FOO, Void.class));
        assertThat(returnType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(returnType.getDeclaredAnnotations().size(), is(1));
        assertThat(returnType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(returnType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(9));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeAnnotationOnNonGenericParameterType() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        Class<?> samples = Class.forName(TYPE_ANNOTATION_OTHER_SAMPLES);
        TypeDescription.Generic parameterType = describeParameterType(samples.getDeclaredMethod(FOO, Void.class), 0);
        assertThat(parameterType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(parameterType.getDeclaredAnnotations().size(), is(1));
        assertThat(parameterType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(parameterType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(10));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeAnnotationOnNonGenericExceptionType() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        Class<?> samples = Class.forName(TYPE_ANNOTATION_OTHER_SAMPLES);
        TypeDescription.Generic exceptionType = describeExceptionType(samples.getDeclaredMethod(FOO, Void.class), 0);
        assertThat(exceptionType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(exceptionType.getDeclaredAnnotations().size(), is(1));
        assertThat(exceptionType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(exceptionType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(11));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeAnnotationOnNonGenericArrayType() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        Class<?> samples = Class.forName(TYPE_ANNOTATION_SAMPLES);
        TypeDescription.Generic returnType = describeReturnType(samples.getDeclaredMethod(BAR, Void[][].class));
        assertThat(returnType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(returnType.getDeclaredAnnotations().size(), is(1));
        assertThat(returnType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(returnType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(35));
        assertThat(returnType.getComponentType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(returnType.getComponentType().getDeclaredAnnotations().size(), is(1));
        assertThat(returnType.getComponentType().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(returnType.getComponentType().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(36));
        assertThat(returnType.getComponentType().getComponentType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(returnType.getComponentType().getComponentType().getDeclaredAnnotations().size(), is(1));
        assertThat(returnType.getComponentType().getComponentType().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(returnType.getComponentType().getComponentType().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(34));
        TypeDescription.Generic parameterType = describeParameterType(samples.getDeclaredMethod(BAR, Void[][].class), 0);
        assertThat(parameterType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(parameterType.getDeclaredAnnotations().size(), is(1));
        assertThat(parameterType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(parameterType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(38));
        assertThat(parameterType.getComponentType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(parameterType.getComponentType().getDeclaredAnnotations().size(), is(1));
        assertThat(parameterType.getComponentType().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(parameterType.getComponentType().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(39));
        assertThat(parameterType.getComponentType().getComponentType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(parameterType.getComponentType().getComponentType().getDeclaredAnnotations().size(), is(1));
        assertThat(parameterType.getComponentType().getComponentType().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(parameterType.getComponentType().getComponentType().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(37));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeAnnotationOnNonGenericArrayTypeWithGenericSignature() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        Class<?> samples = Class.forName(TYPE_ANNOTATION_SAMPLES);
        TypeDescription.Generic returnType = describeReturnType(samples.getDeclaredMethod(QUX, Void[][].class));
        assertThat(returnType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(returnType.getDeclaredAnnotations().size(), is(1));
        assertThat(returnType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(returnType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(41));
        assertThat(returnType.getComponentType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(returnType.getComponentType().getDeclaredAnnotations().size(), is(1));
        assertThat(returnType.getComponentType().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(returnType.getComponentType().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(42));
        assertThat(returnType.getComponentType().getComponentType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(returnType.getComponentType().getComponentType().getDeclaredAnnotations().size(), is(1));
        assertThat(returnType.getComponentType().getComponentType().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(returnType.getComponentType().getComponentType().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(40));
        TypeDescription.Generic parameterType = describeParameterType(samples.getDeclaredMethod(QUX, Void[][].class), 0);
        assertThat(parameterType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(parameterType.getDeclaredAnnotations().size(), is(1));
        assertThat(parameterType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(parameterType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(44));
        assertThat(parameterType.getComponentType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(parameterType.getComponentType().getDeclaredAnnotations().size(), is(1));
        assertThat(parameterType.getComponentType().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(parameterType.getComponentType().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(45));
        assertThat(parameterType.getComponentType().getComponentType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(parameterType.getComponentType().getComponentType().getDeclaredAnnotations().size(), is(1));
        assertThat(parameterType.getComponentType().getComponentType().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(parameterType.getComponentType().getComponentType().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(43));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeAnnotationOwnerType() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        Class<?> samples = Class.forName(TYPE_ANNOTATION_OTHER_SAMPLES);
        TypeDescription.Generic fieldType = describeType(samples.getDeclaredField(BAR));
        assertThat(fieldType.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(fieldType.getDeclaredAnnotations().size(), is(1));
        assertThat(fieldType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(fieldType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(3));
        assertThat(fieldType.getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(fieldType.getTypeArguments().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(fieldType.getTypeArguments().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(fieldType.getTypeArguments().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(4));
        assertThat(fieldType.getOwnerType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(fieldType.getOwnerType().getDeclaredAnnotations().size(), is(1));
        assertThat(fieldType.getOwnerType().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(fieldType.getOwnerType().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(1));
        assertThat(fieldType.getOwnerType().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(fieldType.getOwnerType().getTypeArguments().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(fieldType.getOwnerType().getTypeArguments().getOnly().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(fieldType.getOwnerType().getTypeArguments().getOnly().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(2));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeAnnotationTwoAnnotations() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        Class<? extends Annotation> otherTypeAnnotation = (Class<? extends Annotation>) Class.forName(OTHER_TYPE_ANNOTATION);
        MethodDescription.InDefinedShape otherValue = new TypeDescription.ForLoadedType(otherTypeAnnotation).getDeclaredMethods().getOnly();
        Class<?> samples = Class.forName(TYPE_ANNOTATION_OTHER_SAMPLES);
        TypeDescription.Generic fieldType = describeType(samples.getDeclaredField(QUX));
        assertThat(fieldType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(fieldType.getDeclaredAnnotations().size(), is(2));
        assertThat(fieldType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(fieldType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(5));
        assertThat(fieldType.getDeclaredAnnotations().isAnnotationPresent(otherTypeAnnotation), is(true));
        assertThat(fieldType.getDeclaredAnnotations().ofType(otherTypeAnnotation).getValue(otherValue).resolve(Integer.class), is(6));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeAnnotationNonGenericInnerType() throws Exception {
        Class<? extends Annotation> typeAnnotation = (Class<? extends Annotation>) Class.forName(TYPE_ANNOTATION);
        MethodDescription.InDefinedShape value = new TypeDescription.ForLoadedType(typeAnnotation).getDeclaredMethods().getOnly();
        Class<?> samples = Class.forName(TYPE_ANNOTATION_OTHER_SAMPLES);
        TypeDescription.Generic fieldType = describeType(samples.getDeclaredField(BAZ));
        assertThat(fieldType.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(fieldType.getDeclaredAnnotations().size(), is(1));
        assertThat(fieldType.getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(fieldType.getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(8));
        assertThat(fieldType.getOwnerType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(fieldType.getOwnerType().getDeclaredAnnotations().size(), is(1));
        assertThat(fieldType.getOwnerType().getDeclaredAnnotations().isAnnotationPresent(typeAnnotation), is(true));
        assertThat(fieldType.getOwnerType().getDeclaredAnnotations().ofType(typeAnnotation).getValue(value).resolve(Integer.class), is(7));
    }

    @SuppressWarnings("unused")
    public interface Foo {
        /* empty */
    }

    @SuppressWarnings("unused")
    public interface Bar {
        /* empty */
    }

    @SuppressWarnings("unused")
    public interface Qux<T, U> {
        /* empty */
    }

    @SuppressWarnings("unused")
    public static class NonGeneric {

        Object foo;

        Inner bar;

        class Inner {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class SimpleParameterizedType {

        List<String> foo;
    }

    @SuppressWarnings("unused")
    public static class UpperBoundWildcardParameterizedType {

        List<? extends String> foo;
    }

    @SuppressWarnings("unused")
    public static class LowerBoundWildcardParameterizedType {

        List<? super String> foo;
    }

    @SuppressWarnings("unused")
    public static class UnboundWildcardParameterizedType {

        List<?> foo;
    }

    @SuppressWarnings("all")
    public static class ExplicitlyUnboundWildcardParameterizedType {

        List<? extends Object> foo;
    }

    @SuppressWarnings("unused")
    public static class NestedParameterizedType {

        List<List<Foo>> foo;
    }

    @SuppressWarnings("unused")
    public static class SimpleGenericArrayType {

        List<String>[] foo;
    }

    @SuppressWarnings("unused")
    public static class GenericArrayOfGenericComponentType<T extends String> {

        List<T>[] foo;
    }

    @SuppressWarnings("unused")
    public static class SimpleTypeVariableType<T> {

        T foo;
    }

    @SuppressWarnings("unused")
    public static class SingleUpperBoundTypeVariableType<T extends String> {

        T foo;
    }

    @SuppressWarnings("unused")
    public static class MultipleUpperBoundTypeVariableType<T extends String & Foo & Bar> {

        T foo;
    }

    @SuppressWarnings("unused")
    public static class InterfaceOnlyMultipleUpperBoundTypeVariableType<T extends Foo & Bar> {

        T foo;
    }

    @SuppressWarnings("unused")
    public static class ShadowingTypeVariableType<T> {

        @SuppressWarnings("all")
        <T> T foo() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static class NestedTypeVariableType<T> {

        Placeholder foo;

        class Placeholder {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class NestedSpecifiedTypeVariableType<T> {

        NestedSpecifiedTypeVariableType<String>.Placeholder foo;

        class Placeholder {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class NestedStaticTypeVariableType<T> {

        NestedStaticTypeVariableType.Placeholder<String> foo;

        static class Placeholder<S> {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class NestedInnerType<T> {

        class InnerType<S extends T> {

            <U extends S> T foo() {
                return null;
            }

            <U extends S> S bar() {
                return null;
            }

            <U extends S> U qux() {
                return null;
            }
        }
    }

    @SuppressWarnings("unused")
    public static class NestedInnerMethod<T> {

        <S extends T> Class<?> foo() {
            class InnerType<U extends S> {

                <V extends U> T foo() {
                    return null;
                }

                <V extends U> S bar() {
                    return null;
                }

                <V extends U> U qux() {
                    return null;
                }

                <V extends U> V baz() {
                    return null;
                }
            }
            return InnerType.class;
        }
    }

    @SuppressWarnings("unused")
    public static class RecursiveTypeVariable<T extends RecursiveTypeVariable<T>> {

        T foo;
    }

    @SuppressWarnings("unused")
    public static class BackwardsReferenceTypeVariable<T, S extends T> {

        S foo;

        T bar;
    }

    @SuppressWarnings("unused")
    public static class TypeResolution<T> {

        private TypeResolution<Foo>.Inner<Bar> foo;

        private TypeResolution<Foo>.Raw<Bar> bar;

        private TypeResolution<Foo>.PartiallyRaw<Bar> qux;

        private TypeResolution<Foo>.NestedPartiallyRaw<Bar> baz;

        private TypeResolution<Foo>.Shadowed<Bar, Foo> foobar;

        public interface BaseInterface<V, W> {

            Qux<V, W> qux(Qux<V, W> qux);
        }

        public static class Intermediate<V, W> extends Base<List<V>, List<? extends W>> implements BaseInterface<List<V>, List<? extends W>> {
            /* empty */
        }

        public static class NestedIntermediate<V, W> extends Base<List<List<V>>, List<String>> implements BaseInterface<List<List<V>>, List<String>> {
            /* empty */
        }

        public static class Base<V, W> {

            Qux<V, W> qux;

            public Qux<V, W> qux(Qux<V, W> qux) {
                return null;
            }
        }

        public class Inner<S> extends Base<T, S> implements BaseInterface<T, S> {
            /* empty */
        }

        @SuppressWarnings("unchecked")
        public class Raw<S> extends Base implements BaseInterface {
            /* empty */
        }

        @SuppressWarnings("unchecked")
        public class PartiallyRaw<S> extends Intermediate {
            /* empty */
        }

        @SuppressWarnings("unchecked")
        public class NestedPartiallyRaw<S> extends NestedIntermediate {
            /* empty */
        }

        @SuppressWarnings("all")
        public class Shadowed<T, S> extends Base<T, S> implements BaseInterface<T, S> {
            /* empty */
        }
    }

    public static class RawType<T> {

        Extension foo;

        T bar;

        public static class Intermediate<T extends Number> extends RawType<T> {
            /* empty */
        }

        public static class Extension extends Intermediate {
            /* empty */
        }
    }

    public static class IntermediateRaw<T> {

        Extension foo;

        T bar;

        public static class NonGenericIntermediate extends IntermediateRaw<Integer> {
            /* empty */
        }

        public static class GenericIntermediate<T> extends NonGenericIntermediate {
            /* empty */
        }

        public static class Extension extends GenericIntermediate {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class MemberVariable<U, T extends U> {

        public MemberVariable<Number, Integer> foo;

        public Raw bar;

        public <S> S foo() {
            return null;
        }

        @SuppressWarnings("all")
        public <T> T bar() {
            return null;
        }

        @SuppressWarnings("all")
        public <S extends U> S qux() {
            return null;
        }

        public U baz(U u) {
            return u;
        }

        @SuppressWarnings("unchecked")
        public static class Raw extends MemberVariable {
            /* empty */
        }
    }

    interface MixedTypeVariables<T> {

        interface Inner extends MixedTypeVariables<Void> {
            /* empty */
        }

        <S> void qux(SampleType<S, T> arg);

        interface SampleType<U, V> {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public abstract static class InconsistentGenerics<T extends Exception> implements Callable<T> {

        InconsistentGenerics(T t) throws T {
            /* empty */
        }

        abstract void foo(T t) throws T;

        private InconsistentGenerics<T> foo;
    }

    public static class GenericDisintegrator extends ClassVisitor {

        public static Field make() throws IOException, ClassNotFoundException, NoSuchFieldException {
            ClassReader classReader = new ClassReader(InconsistentGenerics.class.getName());
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
            classReader.accept(new GenericDisintegrator(classWriter), 0);
            return new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                    Collections.singletonMap(InconsistentGenerics.class.getName(), classWriter.toByteArray()),
                    ByteArrayClassLoader.PersistenceHandler.MANIFEST)
                    .loadClass(InconsistentGenerics.class.getName()).getDeclaredField(FOO);
        }

        public GenericDisintegrator(ClassVisitor classVisitor) {
            super(Opcodes.ASM6, classVisitor);
        }

        @Override
        public void visit(int version, int modifiers, String name, String signature, String superName, String[] interfaces) {
            super.visit(version,
                    modifiers,
                    name,
                    signature,
                    superName,
                    new String[]{Callable.class.getName().replace('.', '/'), Serializable.class.getName().replace('.', '/')});
        }

        @Override
        public void visitOuterClass(String owner, String name, String desc) {
            /* do nothing */
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            /* do nothing */
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return super.visitMethod(access,
                    name,
                    "(L" + Exception.class.getName().replace('.', '/') + ";L" + Void.class.getName().replace('.', '/') + ";)V",
                    signature,
                    new String[]{Exception.class.getName().replace('.', '/'), RuntimeException.class.getName().replace('.', '/')});
        }
    }
}
