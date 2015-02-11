package net.bytebuddy.instrumentation;

import jdk.nashorn.internal.runtime.Debug;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.FieldRegistry;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.SubclassDynamicTypeBuilder;
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.test.utility.DebuggingWrapper;
import org.hamcrest.CoreMatchers;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Textifier;

import java.util.Arrays;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractInstrumentationTest {

    private static final String SUFFIX = "foo";

    protected <T> DynamicType.Loaded<T> instrument(Class<T> target,
                                                   Instrumentation instrumentation) {
        return instrument(target, instrumentation, target.getClassLoader(), isDeclaredBy(target));
    }

    protected <T> DynamicType.Loaded<T> instrument(Class<T> target,
                                                   Instrumentation instrumentation,
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
                new TypeDescription.ForLoadedType(target),
                new TypeList.ForLoadedType(Arrays.asList(interfaces)),
                Opcodes.ACC_PUBLIC,
                TypeAttributeAppender.NoOp.INSTANCE,
                isSynthetic(),
                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
                new ClassVisitorWrapper.Chain(),
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
                MethodLookupEngine.Default.Factory.INSTANCE,
                FieldAttributeAppender.NoOp.INSTANCE,
                MethodAttributeAppender.NoOp.INSTANCE,
                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
                .method(targetMethods).intercept(instrumentation)
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.WRAPPER);
    }
}
