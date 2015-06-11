package net.bytebuddy.description.method;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class MethodDescriptionLatentTest extends AbstractMethodDescriptionTest {

    @Override
    protected MethodDescription describe(Method method) {
        List<ParameterDescription.Token> tokens = new ArrayList<ParameterDescription.Token>(method.getParameterTypes().length);
        ParameterList parameterList = ParameterList.ForLoadedExecutable.of(method);
        for (ParameterDescription parameterDescription : parameterList) {
            tokens.add(parameterList.hasExplicitMetaData()
                    ? new ParameterDescription.Token(parameterDescription.getType(),
                    parameterDescription.getDeclaredAnnotations(),
                    parameterDescription.getName(),
                    parameterDescription.getModifiers())
                    : new ParameterDescription.Token(parameterDescription.getType(), parameterDescription.getDeclaredAnnotations()));
        }
        return new MethodDescription.Latent(new TypeDescription.ForLoadedType(method.getDeclaringClass()),
                method.getName(),
                GenericTypeDescription.Sort.describe(method.getGenericReturnType()),
                tokens,
                method.getModifiers(),
                new GenericTypeList.ForLoadedType(method.getGenericExceptionTypes()),
                new AnnotationList.ForLoadedAnnotation(method.getDeclaredAnnotations()));
    }

    @Override
    protected MethodDescription describe(Constructor<?> constructor) {
        List<ParameterDescription.Token> tokens = new ArrayList<ParameterDescription.Token>(constructor.getParameterTypes().length);
        ParameterList parameterList = ParameterList.ForLoadedExecutable.of(constructor);
        for (ParameterDescription parameterDescription : parameterList) {
            tokens.add(parameterList.hasExplicitMetaData()
                    ? new ParameterDescription.Token(parameterDescription.getType(),
                    parameterDescription.getDeclaredAnnotations(),
                    parameterDescription.getName(),
                    parameterDescription.getModifiers())
                    : new ParameterDescription.Token(parameterDescription.getType(), parameterDescription.getDeclaredAnnotations()));
        }
        return new MethodDescription.Latent(new TypeDescription.ForLoadedType(constructor.getDeclaringClass()),
                MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                TypeDescription.VOID,
                tokens,
                constructor.getModifiers(),
                new GenericTypeList.ForLoadedType(constructor.getGenericExceptionTypes()),
                new AnnotationList.ForLoadedAnnotation(constructor.getDeclaredAnnotations()));
    }

    @Test
    public void testTypeInitializer() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        MethodDescription typeInitializer = MethodDescription.Latent.typeInitializerOf(typeDescription);
        assertThat(typeInitializer.getDeclaringType(), is(typeDescription));
        assertThat(typeInitializer.getReturnType(), is((GenericTypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(typeInitializer.getParameters(), is((ParameterList) new ParameterList.Empty()));
        assertThat(typeInitializer.getExceptionTypes(), is((GenericTypeList) new GenericTypeList.Empty()));
        assertThat(typeInitializer.getDeclaredAnnotations(), is((AnnotationList) new AnnotationList.Empty()));
        assertThat(typeInitializer.getModifiers(), is(MethodDescription.TYPE_INITIALIZER_MODIFIER));
    }

    @Override
    protected boolean canReadDebugInformation() {
        return false;
    }
}
