package net.bytebuddy.description.method;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class MethodDescriptionLatentTest extends AbstractMethodDescriptionTest {

    @Override
    protected MethodDescription.InDefinedShape describe(Method method) {
        return new MethodDescription.Latent(new TypeDescription.ForLoadedType(method.getDeclaringClass()),
                new MethodDescription.ForLoadedMethod(method).asToken(ElementMatchers.is(method.getDeclaringClass())));
    }

    @Override
    protected MethodDescription.InDefinedShape describe(Constructor<?> constructor) {
        return new MethodDescription.Latent(new TypeDescription.ForLoadedType(constructor.getDeclaringClass()),
                new MethodDescription.ForLoadedConstructor(constructor).asToken(ElementMatchers.is(constructor.getDeclaringClass())));
    }

    @Test
    public void testTypeInitializer() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        MethodDescription typeInitializer = new MethodDescription.Latent.TypeInitializer(typeDescription);
        assertThat(typeInitializer.getDeclaringType(), is((GenericTypeDescription) typeDescription));
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
