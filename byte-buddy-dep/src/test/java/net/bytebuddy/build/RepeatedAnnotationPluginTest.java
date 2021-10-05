package net.bytebuddy.build;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class RepeatedAnnotationPluginTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @JavaVersionRule.Enforce(8)
    public void testRepeatingAnnotation() throws Exception {
        Class<?> transformed = new RepeatedAnnotationPlugin().apply(new ByteBuddy().redefine(Sample.class),
                        TypeDescription.ForLoadedType.of(Sample.class),
                        ClassFileLocator.ForClassLoader.of(Sample.class.getClassLoader()))
                .make()
                .load(Sample.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> repeatable = (Class<? extends Annotation>) Class.forName("java.lang.annotation.Repeatable");
        assertThat(repeatable.getMethod("value").invoke(transformed.getAnnotation(repeatable)), is((Object) Samples.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testRequiresCompatibleTarget() {
        new RepeatedAnnotationPlugin().apply(new ByteBuddy().redefine(SampleBroken.class),
                        TypeDescription.ForLoadedType.of(SampleBroken.class),
                        ClassFileLocator.ForClassLoader.of(SampleBroken.class.getClassLoader()))
                .make();
    }

    @RepeatedAnnotationPlugin.Enhance(Samples.class)
    public @interface Sample {
        /* empty */
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Samples {

        Sample[] value();
    }

    @RepeatedAnnotationPlugin.Enhance(Annotation.class)
    public @interface SampleBroken {
        /* empty */
    }
}
