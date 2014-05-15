package net.bytebuddy.instrumentation;

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
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.Opcodes;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public abstract class AbstractInstrumentationTest {

    private static final String SUFFIX = "foo";

    protected <T> DynamicType.Loaded<T> instrument(Class<T> target, Instrumentation instrumentation) {
        assertThat(target.isInterface(), is(false));
        return new SubclassDynamicTypeBuilder<T>(
                ClassFileVersion.forCurrentJavaVersion(),
                new NamingStrategy.SuffixingRandom(SUFFIX),
                new TypeDescription.ForLoadedType(target),
                new TypeList.Empty(),
                Opcodes.ACC_PUBLIC,
                TypeAttributeAppender.NoOp.INSTANCE,
                not(isDeclaredBy(target)).or(isSynthetic()),
                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
                new ClassVisitorWrapper.Chain(),
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
                MethodLookupEngine.Default.Factory.INSTANCE,
                FieldAttributeAppender.NoOp.INSTANCE,
                MethodAttributeAppender.NoOp.INSTANCE,
                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
                .method(isDeclaredBy(target)).intercept(instrumentation)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
    }
}
