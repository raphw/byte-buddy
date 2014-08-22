package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.FieldRegistry;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.util.Arrays;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.none;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FlatDynamicTypeBuilderTest {

    private static final ClassLoader BOOTSTRAP_CLASS_LOADER = null;

    private static final String FOO = "foo";

    public static class Foo {

        private final String foo;

        public Foo() {
            foo = FOO;
        }

        public String foo() {
            return FOO;
        }
    }

    @Test
    public void testPlainRebasing() throws Exception {
        Class<?> foo = new FlatDynamicTypeBuilder<Foo>(ClassFileVersion.forCurrentJavaVersion(),
                new NamingStrategy.Fixed(FOO),
                new TypeDescription.ForLoadedType(Foo.class),
                new TypeList.ForLoadedType(Arrays.<Class<?>>asList(Serializable.class)),
                Opcodes.ACC_PUBLIC,
                TypeAttributeAppender.NoOp.INSTANCE,
                none(),
                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
                new ClassVisitorWrapper.Chain(),
                new FieldRegistry.Default(),
                new MethodRegistry.Default(),
                MethodLookupEngine.Default.Factory.INSTANCE,
                FieldAttributeAppender.NoOp.INSTANCE,
                MethodAttributeAppender.NoOp.INSTANCE,
                ClassFileLocator.Default.CLASS_PATH,
                FlatDynamicTypeBuilder.TargetHandler.ForRebaseInstrumentation.INSTANCE)
//                .classVisitor(new DebuggingWrapper(System.out, new Textifier()))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(foo.getName(), is(FOO));
//        assertThat(foo.getModifiers(), is(Opcodes.ACC_PUBLIC));
        foo.newInstance();
    }
}
