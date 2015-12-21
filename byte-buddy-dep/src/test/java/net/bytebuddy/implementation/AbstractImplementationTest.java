package net.bytebuddy.implementation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.FieldRegistry;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.SubclassDynamicTypeBuilder;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.ElementMatcher;
import org.hamcrest.CoreMatchers;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractImplementationTest {

    private static final String SUFFIX = "foo";

    protected <T> DynamicType.Loaded<T> implement(Class<T> target, Implementation implementation) {
        return implement(target, implementation, target.getClassLoader(), isDeclaredBy(target));
    }

    protected <T> DynamicType.Loaded<T> implement(Class<T> target,
                                                  Implementation implementation,
                                                  ClassLoader classLoader,
                                                  ElementMatcher<? super MethodDescription> targetMethods,
                                                  Class<?>... interfaces) {
        assertThat(target.isInterface(), CoreMatchers.is(false));
        for (Class<?> anInterface : interfaces) {
            assertThat(anInterface.isInterface(), CoreMatchers.is(true));
        }
        return new SubclassDynamicTypeBuilder<T>(
                ClassFileVersion.forCurrentJavaVersion(),
                new NamingStrategy.SuffixingRandom(SUFFIX),
                new AuxiliaryType.NamingStrategy.SuffixingRandom(SUFFIX),
                Implementation.Context.Default.Factory.INSTANCE,
                new GenericTypeList.ForLoadedTypes(Arrays.asList(interfaces)),
                Opcodes.ACC_PUBLIC,
                TypeAttributeAppender.NoOp.INSTANCE,
                isSynthetic(),
                new ClassVisitorWrapper.Compound(),
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
                MethodGraph.Compiler.DEFAULT,
                FieldAttributeAppender.NoOp.INSTANCE,
                MethodAttributeAppender.NoOp.INSTANCE,
                new GenericTypeDescription.ForNonGenericType.OfLoadedType(target),
                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
                .invokable(targetMethods).intercept(implementation)
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.WRAPPER);
    }
}
