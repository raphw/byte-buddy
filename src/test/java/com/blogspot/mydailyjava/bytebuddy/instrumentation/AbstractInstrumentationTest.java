package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.NamingStrategy;
import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import com.blogspot.mydailyjava.bytebuddy.dynamic.ClassLoadingStrategy;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.FieldRegistry;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.LoadedSuperclassDynamicTypeBuilder;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public abstract class AbstractInstrumentationTest {

    private static final String SUFFIX = "foo";

    protected static class CallTraceable {

        protected static class MethodCall {

            public final String name;
            public final Object arguments[];

            public MethodCall(String name, Object... arguments) {
                this.name = name;
                this.arguments = arguments;
            }
        }

        protected final List<MethodCall> methodCalls;

        public CallTraceable() {
            methodCalls = new ArrayList<MethodCall>();
        }

        public void register(String name, Object... arguments) {
            methodCalls.add(new MethodCall(name, arguments));
        }

        public void assertOnlyCall(String name, Object... arguments) {
            assertThat(methodCalls.size(), is(1));
            assertThat(methodCalls.get(0).name, is(name));
            assertThat(methodCalls.get(0).arguments, equalTo(arguments));
        }

        public void assertZeroCalls() {
            assertThat(methodCalls.size(), is(0));
        }
    }

    protected <T> DynamicType.Loaded<T> instrument(Class<T> target, Instrumentation instrumentation) {
        assertThat(target.isInterface(), is(false));
        return new LoadedSuperclassDynamicTypeBuilder<T>(
                ClassFormatVersion.forCurrentJavaVersion(),
                new NamingStrategy.SuffixingRandom(SUFFIX),
                target,
                Collections.<Class<?>>emptyList(),
                Opcodes.ACC_PUBLIC,
                TypeAttributeAppender.NoOp.INSTANCE,
                not(isDeclaredBy(target)).or(isSynthetic()),
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
