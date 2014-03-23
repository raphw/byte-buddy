package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.NamingStrategy;
import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import com.blogspot.mydailyjava.bytebuddy.dynamic.ClassLoadingStrategy;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.FieldRegistry;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.SubclassDynamicTypeBuilder;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.Opcodes;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public abstract class AbstractInstrumentationTest {

    private static final String SUFFIX = "foo";

    protected <T> DynamicType.Loaded<T> instrument(Class<T> target, Instrumentation instrumentation) {
        assertThat(target.isInterface(), is(false));
        return new SubclassDynamicTypeBuilder<T>(
                ClassFormatVersion.forCurrentJavaVersion(),
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
                FieldAttributeAppender.NoOp.INSTANCE,
                MethodAttributeAppender.NoOp.INSTANCE,
                ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
                .method(isDeclaredBy(target)).intercept(instrumentation)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
    }
}
