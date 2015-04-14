package net.bytebuddy.description.method;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class MethodDescriptionLatentTest extends AbstractMethodDescriptionTest {

    @Override
    protected MethodDescription describe(Method method) {
        return new MethodDescription.Latent(method.getName(),
                new TypeDescription.ForLoadedType(method.getDeclaringClass()),
                new TypeDescription.ForLoadedType(method.getReturnType()),
                new TypeList.ForLoadedType(method.getParameterTypes()),
                method.getModifiers(),
                new TypeList.ForLoadedType(method.getExceptionTypes()));
    }

    @Override
    protected MethodDescription describe(Constructor<?> constructor) {
        return new MethodDescription.Latent(MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                new TypeDescription.ForLoadedType(constructor.getDeclaringClass()),
                new TypeDescription.ForLoadedType(void.class),
                new TypeList.ForLoadedType(constructor.getParameterTypes()),
                constructor.getModifiers(),
                new TypeList.ForLoadedType(constructor.getExceptionTypes()));
    }

    @Test
    @Override
    public void testAnnotations() throws Exception {
        assertThat(describe(Object.class.getDeclaredMethod("toString")).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.Empty()));
        assertThat(describe(Object.class.getDeclaredConstructor()).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.Empty()));
    }

    @Test
    @Override
    public void testParameterAnnotations() throws Exception {
        assertThat(describe(Object.class.getDeclaredMethod("equals", Object.class)).getParameters().get(0).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.Empty()));
    }

    @Test
    public void testTypeInitializer() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        MethodDescription typeInitializer = MethodDescription.Latent.typeInitializerOf(typeDescription);
        assertThat(typeInitializer.getDeclaringType(), is(typeDescription));
        assertThat(typeInitializer.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(typeInitializer.getParameters(), is((ParameterList) new ParameterList.Empty()));
        assertThat(typeInitializer.getExceptionTypes(), is((TypeList) new TypeList.Empty()));
        assertThat(typeInitializer.getDeclaredAnnotations(), is((AnnotationList) new AnnotationList.Empty()));
        assertThat(typeInitializer.getModifiers(), is(MethodDescription.TYPE_INITIALIZER_MODIFIER));
    }

    @Override
    public void testParameterNameAndModifiers() throws Exception {
        assertThat(describe(Object.class.getDeclaredMethod("equals", Object.class)).getParameters().getOnly().getName(), is("arg0"));
    }

    @Override
    protected boolean canReadDebugInformation() {
        return false;
    }
}
