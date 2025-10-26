package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.InjectionClassLoader;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.ClassConstant;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.packaging.AdviceTestHelper;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static junit.framework.TestCase.fail;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdviceTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    private static final String ENTER = "enter", EXIT = "exit", INSIDE = "inside", THROWABLE = "throwable";

    private static final int VALUE = 42, IGNORED = 1;

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    public void testEmptyAdviceEntryAndExit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntryAndExitWithEntrySuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceWithEntrySuppression.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntryAndExitWithExitSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceWithEntrySuppression.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntryAndExitWithSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceWithSuppression.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntryAndExitWithExceptionHandling() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceWithExceptionHandling.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntryAndExitWithExceptionHandlingAndEntrySuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceWithExceptionHandlingAndEntrySuppression.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntryAndExitWithExceptionHandlingAndExitSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceWithExceptionHandlingAndExitSuppression.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntryAndExitWithExceptionHandlingAndSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceWithExceptionHandlingAndSuppression.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntry() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceEntry.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceEntryWithSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceEntryWithSuppression.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceExit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceExit.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceExitAndSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceExitAndSuppression.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceExitWithExceptionHandling() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceExitWithExceptionHandling.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testEmptyAdviceExitWithExceptionHandlingAndSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyMethod.class)
                .visit(Advice.to(EmptyAdviceExitWithExceptionHandlingAndSuppression.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testTrivialDelegation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(EmptyDelegationAdvice.class)
                .visit(Advice.to(EmptyDelegationAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testTrivialAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testTrivialAdviceWithDelegation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TrivialAdviceDelegation.class)
                .visit(Advice.to(TrivialAdviceDelegation.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = TrivialAdviceDelegation.class)
    public void testTrivialAdviceWithDelegationBootstrapped() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.v7.AdviceBootstrap");
        Class<?> type = new ByteBuddy()
                .redefine(TrivialAdviceDelegation.class)
                .visit(Advice.withCustomMapping().bootstrap(bootstrap.getMethod("bootstrap",
                        JavaType.METHOD_HANDLES_LOOKUP.load(),
                        String.class,
                        JavaType.METHOD_TYPE.load(),
                        String.class,
                        int.class,
                        Class.class,
                        String.class,
                        JavaType.METHOD_HANDLE.load())).to(TrivialAdviceDelegation.class).on(named(FOO)))
                .make()
                .load(bootstrap.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = TypedAdviceDelegation.class)
    public void testErasedAdviceWithDelegationBootstrapped() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.v7.AdviceBootstrapErased");
        Class<?> type = new ByteBuddy()
                .redefine(TypedAdviceDelegation.class)
                .visit(Advice.withCustomMapping().bootstrap(bootstrap.getMethod("bootstrap",
                        JavaType.METHOD_HANDLES_LOOKUP.load(),
                        String.class,
                        JavaType.METHOD_TYPE.load(),
                        String.class,
                        String.class), new Advice.BootstrapArgumentResolver.Factory() {
                    public Advice.BootstrapArgumentResolver resolve(final MethodDescription.InDefinedShape adviceMethod, boolean exit) {
                        return new Advice.BootstrapArgumentResolver() {
                            public List<JavaConstant> resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                                return Arrays.asList(
                                        JavaConstant.Simple.ofLoaded(adviceMethod.getDeclaringType().getName()),
                                        JavaConstant.Simple.ofLoaded(adviceMethod.getDescriptor()));
                            }
                        };
                    }
                }, TypeDescription.Generic.Visitor.Generalizing.INSTANCE).to(TypedAdviceDelegation.class).on(named(FOO)))
                .make()
                .load(bootstrap.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, String.class).invoke(type.getDeclaredConstructor().newInstance(), FOO), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testTrivialAdviceWithSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceWithSuppression.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testTrivialAdviceDistributedEnterOnly() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class, EmptyAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testTrivialAdviceDistributedExitOnly() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(EmptyAdvice.class, TrivialAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 0));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testTrivialAdviceWithDelegationEnterOnly() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TrivialAdviceDelegation.class)
                .visit(Advice.to(TrivialAdviceDelegation.class, EmptyAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testTrivialAdviceWithDelegationExitOnly() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TrivialAdviceDelegation.class)
                .visit(Advice.to(EmptyAdvice.class, TrivialAdviceDelegation.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 0));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testTrivialAdviceMultipleMethods() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(isMethod()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
        assertThat(type.getDeclaredMethod(BAZ).invoke(null), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 2));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 2));
    }

    @Test
    public void testTrivialAdviceMultipleMethodsWithSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceWithSuppression.class).on(isMethod()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
        assertThat(type.getDeclaredMethod(BAZ).invoke(null), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 2));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 2));
    }

    @Test
    public void testTrivialAdviceNested() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO)))
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 2));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 2));
    }

    @Test
    public void testTrivialAdviceNestedWithSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceWithSuppression.class).on(named(FOO)))
                .visit(Advice.to(TrivialAdviceWithSuppression.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 2));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 2));
    }

    @Test
    public void testTrivialAdviceWithHandler() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO + BAZ)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO + BAZ).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testTrivialAdviceWithHandlerAndSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceWithSuppression.class).on(named(FOO + BAZ)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO + BAZ).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceOnConstructor() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceSkipException.class).on(isConstructor()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor().newInstance(), notNullValue(Object.class));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceOnConstructorExitAdviceWithSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceSkipExceptionWithSuppression.class).on(isConstructor()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor().newInstance(), notNullValue(Object.class));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testFrameAdviceSimpleShift() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(FrameShiftAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testFrameAdviceSimpleShiftExpanded() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(FrameShiftAdvice.class).on(named(FOO)).readerFlags(ClassReader.EXPAND_FRAMES))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceOnConstructorWithSuppressionNotLegal() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(isConstructor()))
                .make();
    }

    @Test
    public void testAdviceWithImplicitArgument() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ArgumentAdvice.class).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.getDeclaredConstructor().newInstance(), BAR), is((Object) BAR));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithImplicitArgumentDelegation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ArgumentAdviceDelegationImplicit.class)
                .visit(Advice.to(ArgumentAdviceDelegationImplicit.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, String.class).invoke(type.getDeclaredConstructor().newInstance(), BAR), is((Object) BAR));
    }

    @Test
    public void testAdviceWithExplicitArgument() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ArgumentAdviceExplicit.class).on(named(QUX)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(QUX, String.class, String.class).invoke(type.getDeclaredConstructor().newInstance(), FOO, BAR), is((Object) (FOO + BAR)));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithIncrement() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(IncrementSample.class)
                .visit(Advice.to(IncrementAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, int.class).invoke(type.getDeclaredConstructor().newInstance(), 0), is((Object) 3));
    }

    @Test
    public void testAdviceWithThisReference() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ThisReferenceAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithOptionalThisReferenceNonOptional() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(OptionalThisReferenceAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithOptionalThisReferenceOptional() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(OptionalThisReferenceAdvice.class).on(named(BAZ)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAZ).invoke(null), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithReturnValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ReturnValueAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 0));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithExceptionHandler() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ExceptionHandlerAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithExceptionHandlerNested() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ExceptionHandlerAdvice.class).on(named(FOO)))
                .visit(Advice.to(ExceptionHandlerAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 2));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 2));
    }

    @Test
    public void testAdviceNotSkipExceptionImplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO + BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO + BAR).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceNotSkipExceptionImplicitNested() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO + BAR)))
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO + BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO + BAR).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 2));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 2));
    }

    @Test
    public void testAdviceSkipExceptionImplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceSkipException.class).on(named(FOO + BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO + BAR).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testAdviceNotSkipExceptionExplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(BAR + BAZ)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(BAR + BAZ).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(NullPointerException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceNotSkipExceptionExplicitNested() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(BAR + BAZ)))
                .visit(Advice.to(TrivialAdvice.class).on(named(BAR + BAZ)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(BAR + BAZ).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(NullPointerException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 2));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 2));
    }

    @Test
    public void testAdviceSkipExceptionExplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceSkipException.class).on(named(BAR + BAZ)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(BAR + BAZ).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(NullPointerException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testAdviceSkipExceptionDoesNotSkipNonException() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceSkipException.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testObsoleteReturnValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ObsoleteReturnValueAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testUnusedReturnValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(UnusedReturnValueAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testExceptionWhenNotThrown() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ThrowableAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) (FOO)));
        assertThat(type.getDeclaredField(THROWABLE).get(null), nullValue(Object.class));
    }

    @Test
    public void testExceptionWhenThrown() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ThrowableAdvice.class).on(named(FOO + BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO + BAR).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(THROWABLE).get(null), instanceOf(RuntimeException.class));
    }

    @Test
    public void testAdviceThrowOnEnter() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TracableSample.class)
                .visit(Advice.to(ThrowOnEnter.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(INSIDE).get(null), is((Object) 0));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testAdviceThrowOnExit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TracableSample.class)
                .visit(Advice.to(ThrowOnExit.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(INSIDE).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceThrowSuppressed() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TracableSample.class)
                .visit(Advice.to(ThrowSuppressed.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance());
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(INSIDE).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceThrowNotSuppressedOnEnter() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TracableSample.class)
                .visit(Advice.to(ThrowNotSuppressedOnEnter.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(Exception.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(INSIDE).get(null), is((Object) 0));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testAdviceThrowNotSuppressedOnExit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TracableSample.class)
                .visit(Advice.to(ThrowNotSuppressedOnExit.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(Exception.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(INSIDE).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testThisValueSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Box.class)
                .visit(Advice.to(ThisSubstitutionAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor(String.class).newInstance(FOO)), is((Object) BAR));
    }

    @Test
    public void testThisValueSubstitutionOptional() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Box.class)
                .visit(Advice.to(ThisOptionalSubstitutionAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor(String.class).newInstance(FOO)), is((Object) BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalThisValueSubstitution() throws Exception {
        new ByteBuddy()
                .redefine(Box.class)
                .visit(Advice.to(IllegalThisSubstitutionAdvice.class).on(named(FOO)))
                .make();
    }

    @Test
    public void testParameterValueSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Box.class)
                .visit(Advice.to(ParameterSubstitutionAdvice.class).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(null, FOO), is((Object) BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalParameterValueSubstitution() throws Exception {
        new ByteBuddy()
                .redefine(Box.class)
                .visit(Advice.to(IllegalParameterSubstitutionAdvice.class).on(named(BAR)))
                .make();
    }

    @Test
    public void testReturnValueSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ReturnSubstitutionAdvice.class).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.getDeclaredConstructor().newInstance(), FOO), is((Object) BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalReturnValueSubstitution() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalReturnSubstitutionAdvice.class).on(named(FOO)))
                .make();
    }

    @Test
    public void testFieldAdviceBean() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Bean.class)
                .visit(Advice.to(FieldAdviceBean.class).on(ElementMatchers.isSetter()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        type.getDeclaredMethod("setFoo", String.class).invoke(type.getDeclaredConstructor().newInstance(), BAR);
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
    }

    @Test
    public void testFieldAdviceImplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceImplicit.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testFieldAdviceExplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceExplicit.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = Bean.class)
    public void testFieldAdviceHandleBean() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Bean.class)
                .visit(Advice.to(FieldAdviceHandleBean.class).on(ElementMatchers.isSetter()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        type.getDeclaredMethod("setFoo", String.class).invoke(type.getDeclaredConstructor().newInstance(), BAR);
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = Bean.class)
    public void testFieldAdviceHandleSetterBean() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Bean.class)
                .visit(Advice.to(FieldAdviceHandleSetterBean.class).on(ElementMatchers.isGetter()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod("getFoo").invoke(type.getDeclaredConstructor().newInstance()), is((Object) QUX));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = FieldSample.class)
    public void testFieldAdviceHandleImplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceHandleImplicit.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = FieldSample.class)
    public void testFieldAdviceHandleExplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceHandleExplicit.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAllArgumentsStackSizeAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(BoxedArgumentsStackSizeAdvice.class).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.getDeclaredConstructor().newInstance(), FOO), is((Object) FOO));
    }

    @Test
    public void testAllArgumentsObjectTypeAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(BoxedArgumentsObjectTypeAdvice.class).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.getDeclaredConstructor().newInstance(), FOO), is((Object) FOO));
    }

    @Test
    public void testAllArgumentsStackSizeEmptyAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(BoxedArgumentsStackSizeAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testAllArgumentsNoArgumentNull() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(NoArguments.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testAllArgumentsNoArgumentWriteNoEffect() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(NoArgumentsWrite.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testAllArgumentsIncludeSelf() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(AllArgumentsIncludeSelf.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testOriginAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(OriginAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testOriginCustomAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(OriginCustomAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testOriginMethodStackSizeAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(OriginMethodStackSizeAdvice.class).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.getDeclaredConstructor().newInstance(), FOO), is((Object) FOO));
    }

    @Test
    public void testOriginMethodStackSizeEmptyAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(OriginMethodStackSizeAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testOriginConstructorStackSizeAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(OriginConstructorStackSizeAdvice.class).on(isConstructor()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testOriginMethodAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(OriginMethodAdvice.class).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.getDeclaredConstructor().newInstance(), FOO), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = Sample.class)
    public void testOriginMethodHandleAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(Class.forName("net.bytebuddy.test.precompiled.v7.AdviceOriginMethodHandle")).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.getDeclaredConstructor().newInstance(), FOO), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = Sample.class)
    public void testOriginMethodTypeAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(Class.forName("net.bytebuddy.test.precompiled.v7.AdviceOriginMethodType")).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.getDeclaredConstructor().newInstance(), FOO), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testOriginMethodHandlesLookupAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(Class.forName("net.bytebuddy.test.precompiled.v7.AdviceOriginMethodHandlesLookup")).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.getDeclaredConstructor().newInstance(), FOO), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test(expected = IllegalStateException.class)
    public void testOriginMethodNonAssignableAdvice() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(OriginMethodAdvice.class).on(isConstructor()))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testOriginConstructorNonAssignableAdvice() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(OriginConstructorAdvice.class).on(named(BAR)))
                .make();
    }

    @Test
    public void testOriginConstructorAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(OriginConstructorAdvice.class).on(isConstructor()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testExceptionSuppressionAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ExceptionSuppressionAdvice.class).on(named(FOO + BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO + BAR).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testExceptionTypeAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ExceptionTypeAdvice.class)
                .visit(Advice.to(ExceptionTypeAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(IllegalStateException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testExceptionNotCatchedAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ExceptionNotCatchedAdvice.class)
                .visit(Advice.to(ExceptionNotCatchedAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(Exception.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testExceptionCatchedAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ExceptionCatchedAdvice.class)
                .visit(Advice.to(ExceptionCatchedAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testExceptionCatchedWithExchangeAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ExceptionCatchedWithExchangeAdvice.class)
                .visit(Advice.to(ExceptionCatchedWithExchangeAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(IOException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testNonAssignableCasting() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(NonAssignableReturnTypeAdvice.class)
                .visit(Advice.to(NonAssignableReturnTypeAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(ClassCastException.class));
        }
    }

    @Test
    public void testTrivialAssignableCasting() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TrivialReturnTypeAdvice.class)
                .visit(Advice.to(TrivialReturnTypeAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
    }

    @Test
    public void testPrimitiveNonAssignableCasting() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(NonAssignablePrimitiveReturnTypeAdvice.class)
                .visit(Advice.to(NonAssignablePrimitiveReturnTypeAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(ClassCastException.class));
        }
    }

    @Test
    public void testUserNullValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, (Object) null).to(CustomAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testUserTypeValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, Object.class).to(CustomAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testUserEnumValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, RetentionPolicy.CLASS).to(CustomAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testUserSerializableTypeValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .bindSerialized(Custom.class, (Serializable) Collections.singletonMap(FOO, BAR))
                        .to(CustomSerializableAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testUserStackManipulation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, ClassConstant.of(TypeDescription.ForLoadedType.of(Object.class)), Object.class).to(CustomAdvice.class).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.getDeclaredConstructor().newInstance(), BAR), is((Object) BAR));
    }

    @Test
    public void testUserOffsetMapping() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, new Advice.OffsetMapping.ForStackManipulation(ClassConstant.of(TypeDescription.ForLoadedType.of(Object.class)),
                        TypeDescription.ForLoadedType.of(String.class).asGenericType(),
                        TypeDescription.ForLoadedType.of(String.class).asGenericType(),
                        Assigner.Typing.STATIC)).to(CustomAdvice.class).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.getDeclaredConstructor().newInstance(), BAR), is((Object) BAR));
    }

    @Test
    public void testLineNumberPrepend() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(LineNumberAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testLineNumberNoPrepend() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(NoLineNumberAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testLineNumberPrependDelegation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(LineNumberDelegatingAdvice.class)
                .visit(Advice.to(LineNumberDelegatingAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testLineNumberNoPrependDelegation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(NoLineNumberDelegatingAdvice.class)
                .visit(Advice.to(NoLineNumberDelegatingAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testExceptionPrinting() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ExceptionWriterSample.class)
                .visit(Advice.to(ExceptionWriterTest.class).withExceptionPrinting().on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField("exception").getBoolean(null), is(true));
    }

    @Test
    public void testExceptionRethrowing() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ExceptionWriterSample.class)
                .visit(Advice.to(ExceptionWriterTest.class).withExceptionHandler(Advice.ExceptionHandler.Default.RETHROWING).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException().getClass().getName(), is(ExceptionWriterSample.class.getName()));
        }
    }

    @Test
    public void testOptionalArgument() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(OptionalArgumentAdvice.class)
                .visit(Advice.to(OptionalArgumentAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testConstructorEnterFrame() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ConstructorEnterFrameAdvice.class).on(isConstructor()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor().newInstance(), notNullValue(Object.class));
    }

    @Test
    public void testConstructorEnterFrameLarge() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ConstructorEnterFrameLargeAdvice.class).on(isConstructor()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor().newInstance(), notNullValue(Object.class));
    }

    @Test
    public void testParameterAnnotations() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ParameterAnnotationSample.class)
                .visit(Advice.to(ParameterAnnotationSample.class).on(named(FOO)))
                .make()
                .load(ParameterAnnotationSample.SampleParameter.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, String.class).invoke(type.getDeclaredConstructor().newInstance(), FOO), is((Object) FOO));
        assertThat(type.getDeclaredMethod(FOO, String.class).getParameterAnnotations().length, is(1));
        assertThat(type.getDeclaredMethod(FOO, String.class).getParameterAnnotations()[0].length, is(1));
        assertThat(type.getDeclaredMethod(FOO, String.class).getParameterAnnotations()[0][0], instanceOf(ParameterAnnotationSample.SampleParameter.class));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = HandleSample.class)
    public void testHandle() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(HandleSample.class)
                .visit(Advice.to(HandleSample.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getMethod(FOO, String.class).invoke(instance, FOO), is((Object) FOO));
        assertThat(type.getField(FOO).get(null), is((Object) (BAR)));
    }

    @Test
    @JavaVersionRule.Enforce(value = 11, target = HandleSample.class)
    public void testDynamicConstant() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.v11.AdviceDynamicConstant");
        Class<?> type = new ByteBuddy()
                .redefine(bootstrap)
                .visit(Advice.to(bootstrap).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        Object value = type.getMethod(FOO).invoke(instance);
        assertThat(value, notNullValue(Object.class));
        assertThat(type.getMethod(FOO).invoke(instance), sameInstance(value));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = HandleSample.class)
    public void testDynamicConstantInvokedynamic() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.v7.AdviceDynamicConstant");
        Class<?> type = new ByteBuddy()
                .redefine(bootstrap)
                .visit(Advice.to(bootstrap).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        Object value = type.getMethod(FOO).invoke(instance);
        assertThat(value, notNullValue(Object.class));
        assertThat(type.getMethod(FOO).invoke(instance), sameInstance(value));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = SelfCallHandleSample.class)
    public void testSelfCallHandle() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(SelfCallHandleSample.class)
                .visit(Advice.to(SelfCallHandleSample.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getMethod(FOO, String.class).invoke(instance, FOO), is((Object) FOO));
        assertThat(type.getField(FOO).get(null), is((Object) (FOO + BAR)));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = SelfCallHandleSample.class)
    public void testSelfCallHandleSubclass() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(SelfCallHandleSample.class)
                .visit(Advice.to(SelfCallHandleSubclass.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER.opened())
                .getLoaded();
        Object instance = ((InjectionClassLoader) type.getClassLoader()).defineClass(SelfCallHandleSubclass.class.getName(), ClassFileLocator.ForClassLoader.read(SelfCallHandleSubclass.class))
                .getDeclaredConstructor()
                .newInstance();
        assertThat(type.getMethod(FOO, String.class).invoke(instance, FOO), is((Object) FOO));
        assertThat(type.getField(FOO).get(null), is((Object) (FOO + BAR)));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = SelfCallHandleStaticSample.class)
    public void testSelfCallHandleStatic() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(SelfCallHandleStaticSample.class)
                .visit(Advice.to(SelfCallHandleStaticSample.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(null, FOO), is((Object) FOO));
        assertThat(type.getField(FOO).get(null), is((Object) (FOO + BAR)));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = SelfCallHandlePrimitiveSample.class)
    public void testSelfCallHandlePrimitive() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(SelfCallHandlePrimitiveSample.class)
                .visit(Advice.to(SelfCallHandlePrimitiveSample.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getMethod(FOO, int.class).invoke(instance, 42), is((Object) 42));
        assertThat(type.getField(FOO).getInt(null), is((Object) (42 * 3)));
    }

    @Test
    public void testConstructorNoArgumentBackupAndNoFrames() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(NoBackupArguments.class)
                .visit(Advice.to(NoBackupArguments.class).on(isConstructor().and(takesArguments(0))))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor().newInstance(), notNullValue(Object.class));
    }

    @Test
    public void testConstructorNoArgumentBackupAndFrames() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(NoBackupArguments.class)
                .visit(Advice.to(NoBackupArguments.class).on(isConstructor().and(takesArguments(boolean.class))))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor(boolean.class).newInstance(false), notNullValue(Object.class));
    }

    @Test
    public void testAssigningEnterPostProcessorInline() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(PostProcessorInline.class)
                .visit(Advice.withCustomMapping().with(new Advice.PostProcessor.Factory() {
                    public Advice.PostProcessor make(List<? extends AnnotationDescription> annotations,
                                                     final TypeDescription returnType,
                                                     boolean exit) {
                        return new Advice.PostProcessor() {
                            public StackManipulation resolve(TypeDescription instrumentedType,
                                                             MethodDescription instrumentedMethod,
                                                             Assigner assigner,
                                                             Advice.ArgumentHandler argumentHandler,
                                                             Advice.StackMapFrameHandler.ForPostProcessor stackMapFrameHandler,
                                                             StackManipulation exceptionHandler) {
                                return new StackManipulation.Compound(
                                        MethodVariableAccess.of(returnType).loadFrom(argumentHandler.enter()),
                                        MethodVariableAccess.store(instrumentedMethod.getParameters().get(0))
                                );
                            }
                        };
                    }
                }).to(PostProcessorInline.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, String.class).invoke(type.getDeclaredConstructor().newInstance(), BAR), is((Object) FOO));
    }

    @Test
    public void testAssigningEnterPostProcessorDelegate() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(PostProcessorDelegate.class)
                .visit(Advice.withCustomMapping().with(new Advice.PostProcessor.Factory() {
                    public Advice.PostProcessor make(List<? extends AnnotationDescription> annotations,
                                                     final TypeDescription returnType,
                                                     boolean exit) {
                        return new Advice.PostProcessor() {
                            public StackManipulation resolve(TypeDescription instrumentedType,
                                                             MethodDescription instrumentedMethod,
                                                             Assigner assigner,
                                                             Advice.ArgumentHandler argumentHandler,
                                                             Advice.StackMapFrameHandler.ForPostProcessor stackMapFrameHandler,
                                                             StackManipulation exceptionHandler) {
                                return new StackManipulation.Compound(
                                        MethodVariableAccess.of(returnType).loadFrom(argumentHandler.enter()),
                                        MethodVariableAccess.store(instrumentedMethod.getParameters().get(0))
                                );
                            }
                        };
                    }
                }).to(PostProcessorDelegate.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, String.class).invoke(type.getDeclaredConstructor().newInstance(), BAR), is((Object) FOO));
    }

    @Test
    public void testPopsValueAfterArrayWrite() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(AllArgumentsConstructor.class)
                .visit(Advice.to(AllArgumentsConstructor.class).on(isConstructor()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor().newInstance(), notNullValue(Object.class));
    }

    @Test
    public void testDrainsStackOnReservedValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, boolean.class, Ownership.STATIC)
                .defineMethod(BAR, boolean.class, Visibility.PUBLIC)
                .intercept(new Implementation.Simple(new ByteCodeAppender() {
                    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, implementationContext.getInstrumentedType().getInternalName(), FOO, Type.getDescriptor(boolean.class));
                        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, implementationContext.getInstrumentedType().getInternalName(), FOO, Type.getDescriptor(boolean.class));
                        methodVisitor.visitInsn(Opcodes.IRETURN);
                        return new Size(2, 0);
                    }
                }))
                .visit(Advice.to(EmptyAdviceWithEnterValue.class).on(isMethod()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER)
                .getLoaded();
        assertThat(type.getMethod(BAR).invoke(type.getConstructor().newInstance()), is((Object) false));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = Sample.class)
    public void testAdviceDynamicInvocation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bindDynamic(Custom.class,
                        Class.forName("net.bytebuddy.test.precompiled.v7.DynamicSampleBootstrap").getMethod("callable",
                                JavaType.METHOD_HANDLES_LOOKUP.load(),
                                String.class,
                                JavaType.METHOD_TYPE.load(),
                                String.class),
                        FOO).to(CustomDynamicAdvice.class).on(named(FOO)))
                .make()
                .load(Sample.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce(value = 8, target = Sample.class)
    public void testAdviceDynamicLambdaInvocation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bindLambda(Custom.class,
                        Sample.class.getMethod("baz"),
                        Callable.class).to(CustomDynamicAdvice.class).on(named(FOO)))
                .make()
                .load(Sample.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testLabelPrependingMethod() throws Exception {
        Class<? extends Runnable> sample = new ByteBuddy()
                .subclass(Runnable.class)
                .defineMethod("run", void.class, Visibility.PUBLIC)
                .intercept(new Implementation.Simple(new StackManipulation.AbstractBase() {
                    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                        Label start = new Label();
                        methodVisitor.visitLabel(start);
                        methodVisitor.visitFrame(Opcodes.F_FULL, 0, new Object[0], 0, new Object[0]);
                        methodVisitor.visitInsn(Opcodes.ICONST_0);
                        methodVisitor.visitJumpInsn(Opcodes.IFNE, start);
                        methodVisitor.visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);
                        methodVisitor.visitInsn(Opcodes.RETURN);
                        return new Size(1, 1);
                    }
                }))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        Class<?> type = new ByteBuddy()
                .redefine(sample)
                .visit(Advice.to(EmptyDelegationAdvice.class).on(named("run")))
                .make()
                .load(sample.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getMethod("run").invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testWriteFieldInConstructor() throws Exception {
        Class<?> sample = new ByteBuddy()
                .redefine(ConstructorWriteField.class)
                .visit(Advice.to(ConstructorWriteField.class).on(isConstructor()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(sample.getField(FOO).get(sample.getConstructor().newInstance()), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testDroppingOfThisFrameInConstructor() throws Exception {
        new ByteBuddy()
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .defineConstructor(Visibility.PUBLIC)
                .intercept(new Implementation.Simple(
                        MethodVariableAccess.loadThis(),
                        MethodInvocation.invoke(new MethodDescription.ForLoadedConstructor(Object.class.getConstructor()))
                                .special(TypeDescription.ForLoadedType.of(Object.class)),
                        new StackManipulation.AbstractBase() {
                            @Override
                            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                                methodVisitor.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
                                return Size.ZERO;
                            }
                        },
                        MethodReturn.VOID
                ))
                .visit(Advice.to(EmptyExitAdvice.class).on(isConstructor()))
                .make();
    }


    @Test(expected = IllegalArgumentException.class)
    public void testUserSerializableTypeValueNonAssignable() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, Collections.singletonList(FOO)).to(CustomSerializableAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalUserValue() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, new Object()).to(CustomAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonAssignableStringValue() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, FOO).to(CustomPrimitiveAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonAssignableTypeValue() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, Object.class).to(CustomPrimitiveAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonAssignableTypeDescriptionValue() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, TypeDescription.ForLoadedType.of(Object.class)).to(CustomPrimitiveAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonAssignableSerializableValue() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, new ArrayList<String>()).to(CustomPrimitiveAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testInvisibleField() throws Exception {
        new ByteBuddy()
                .redefine(SampleExtension.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, AdviceTestHelper.class.getDeclaredField("object")).to(CustomAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonRelatedField() throws Exception {
        new ByteBuddy()
                .redefine(TracableSample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, Sample.class.getDeclaredField("object")).to(CustomAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonAssignableField() throws Exception {
        new ByteBuddy()
                .redefine(SampleExtension.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, SampleExtension.class.getDeclaredField(FOO)).to(CustomPrimitiveAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMethodNegativeIndex() throws Exception {
        Advice.withCustomMapping().bind(Custom.class, Sample.class.getDeclaredMethod(BAR, String.class), -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMethodOverflowIndex() throws Exception {
        Advice.withCustomMapping().bind(Custom.class, Sample.class.getDeclaredMethod(BAR, String.class), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstrcutorNegativeIndex() throws Exception {
        Advice.withCustomMapping().bind(Custom.class, Sample.class.getDeclaredConstructor(), -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorOverflowIndex() throws Exception {
        Advice.withCustomMapping().bind(Custom.class, Sample.class.getDeclaredConstructor(), 0);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonAssignableParameter() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, Sample.class.getDeclaredMethod(BAR, String.class), 0).to(CustomPrimitiveAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnrelatedMethodParameter() throws Exception {
        new ByteBuddy()
                .redefine(SampleExtension.class)
                .visit(Advice.withCustomMapping().bind(Custom.class, Sample.class.getDeclaredMethod(BAR, String.class), 0).to(CustomPrimitiveAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithThisReferenceOnConstructor() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ThisReferenceAdvice.class).on(isConstructor()))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithFieldOnConstructor() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceExplicit.class).on(isConstructor()))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithExceptionCatchOnConstructor() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(EmptyAdviceExitWithExceptionHandling.class).on(isConstructor()))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdviceWithoutAnnotations() throws Exception {
        Advice.to(Object.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testDuplicateAdvice() throws Exception {
        Advice.to(DuplicateAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testIOExceptionOnRead() throws Exception {
        ClassFileLocator classFileLocator = mock(ClassFileLocator.class);
        when(classFileLocator.locate(TrivialAdvice.class.getName())).thenThrow(new IOException());
        Advice.to(TrivialAdvice.class, classFileLocator);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonStaticAdvice() throws Exception {
        Advice.to(NonStaticAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testAmbiguousAdvice() throws Exception {
        Advice.to(AmbiguousAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testAmbiguousAdviceDelegation() throws Exception {
        Advice.to(AmbiguousAdviceDelegation.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotBindEnterToReturn() throws Exception {
        Advice.to(EnterToReturnAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonExistentArgument() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalArgumentAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableParameterImplicit() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalArgumentAdvice.class).on(named(BAR)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableParameter() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalArgumentWritableAdvice.class).on(named(BAR)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonEqualParameter() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalArgumentReadOnlyAdvice.class).on(named(BAR)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceThisReferenceNonExistent() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ThisReferenceAdvice.class).on(named(BAZ)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableThisReference() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalThisReferenceAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonEqualThisReference() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalThisReferenceWritableAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableReturnValue() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(NonAssignableReturnAdvice.class).on(named(FOO + QUX)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableReturnValueWritable() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(NonEqualReturnWritableAdvice.class).on(named(FOO + QUX)))
                .make();
    }

    @Test
    public void testAdviceAbstractMethodIsSkipped() throws Exception {
        Advice.Dispatcher.Resolved.ForMethodEnter methodEnter = mock(Advice.Dispatcher.Resolved.ForMethodEnter.class);
        Advice.Dispatcher.Resolved.ForMethodExit methodExit = mock(Advice.Dispatcher.Resolved.ForMethodExit.class);
        MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
        when(methodDescription.isAbstract()).thenReturn(true);
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        assertThat(new Advice(methodEnter, methodExit).wrap(mock(TypeDescription.class),
                methodDescription,
                methodVisitor,
                mock(Implementation.Context.class),
                mock(TypePool.class),
                IGNORED,
                IGNORED), sameInstance(methodVisitor));
    }

    @Test
    public void testAdviceNativeMethodIsSkipped() throws Exception {
        Advice.Dispatcher.Resolved.ForMethodEnter methodEnter = mock(Advice.Dispatcher.Resolved.ForMethodEnter.class);
        Advice.Dispatcher.Resolved.ForMethodExit methodExit = mock(Advice.Dispatcher.Resolved.ForMethodExit.class);
        MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
        when(methodDescription.isNative()).thenReturn(true);
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        assertThat(new Advice(methodEnter, methodExit).wrap(mock(TypeDescription.class),
                methodDescription,
                methodVisitor,
                mock(Implementation.Context.class),
                mock(TypePool.class),
                IGNORED,
                IGNORED), sameInstance(methodVisitor));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalThrowableRequest() throws Exception {
        Advice.to(IllegalThrowableRequestAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalThrowableType() throws Exception {
        Advice.to(IllegalThrowableTypeAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldIllegalExplicit() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceIllegalExplicit.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldNonExistent() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceNonExistent.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldNonAssignable() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceNonAssignable.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldWrite() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceWrite.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldHandleIllegalExplicit() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceHandleIllegalExplicit.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldHandleNonExistent() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceHandleNonExistent.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldHandleGetterNonAssignable() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceGetterHandleNonAssignable.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldHandleSetterNonAssignable() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceSetterHandleNonAssignable.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalOriginType() throws Exception {
        Advice.to(IllegalOriginType.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalOriginPattern() throws Exception {
        Advice.to(IllegalOriginPattern.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalOriginPatternEnd() throws Exception {
        Advice.to(IllegalOriginPatternEnd.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotWriteOrigin() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalOriginWriteAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateRegistration() throws Exception {
        Advice.withCustomMapping().bind(Custom.class, FOO).bind(Custom.class, FOO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotAnnotationType() throws Exception {
        Advice.withCustomMapping().bind(Annotation.class, (Serializable) null);
    }

    @Test(expected = IllegalStateException.class)
    public void testInlineAdviceCannotWriteParameter() throws Exception {
        Advice.to(IllegalArgumentWritableInlineAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testInlineAdviceCannotWriteThis() throws Exception {
        Advice.to(IllegalThisWritableInlineAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testInlineAdviceCannotWriteField() throws Exception {
        Advice.to(IllegalFieldWritableInlineAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testInlineAdviceCannotWriteThrow() throws Exception {
        Advice.to(IllegalThrowWritableInlineAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testInlineAdviceCannotWriteReturn() throws Exception {
        Advice.to(IllegalThrowWritableInlineAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testInvisibleDelegationAdvice() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(AdviceTestHelper.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonResolvedAdvice() throws Exception {
        Advice.to(TypeDescription.ForLoadedType.of(TrivialAdvice.class));
    }

    @Test
    public void testCannotInstantiateSuppressionMarker() throws Exception {
        Class<?> type = Class.forName(Advice.class.getName() + "$NoExceptionHandler");
        assertThat(Modifier.isPrivate(type.getModifiers()), is(true));
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            assertThat(Modifier.isPrivate(constructor.getModifiers()), is(true));
            constructor.setAccessible(true);
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(UnsupportedOperationException.class));
        }
    }

    @Test
    public void testCannotInstantiateSkipDefaultValueMarker() throws Exception {
        try {
            Constructor<?> constructor = Advice.OnDefaultValue.class.getDeclaredConstructor();
            assertThat(Modifier.isPrivate(constructor.getModifiers()), is(true));
            constructor.setAccessible(true);
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(UnsupportedOperationException.class));
        }
    }

    @Test
    public void testCannotInstantiateSkipNonDefaultValueMarker() throws Exception {
        try {
            Constructor<?> constructor = Advice.OnNonDefaultValue.class.getDeclaredConstructor();
            assertThat(Modifier.isPrivate(constructor.getModifiers()), is(true));
            constructor.setAccessible(true);
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(UnsupportedOperationException.class));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDistributedAdviceNoEnterAdvice() throws Exception {
        Advice.to(Object.class, EmptyAdvice.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDistributedAdviceNoExitAdvice() throws Exception {
        Advice.to(EmptyAdvice.class, Object.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalBoxedReturn() throws Exception {
        Advice.to(IllegalBoxedReturnType.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testBoxedArgumentsWriteDelegateEntry() throws Exception {
        Advice.to(BoxedArgumentsWriteDelegateEntry.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testBoxedArgumentsWriteDelegateExit() throws Exception {
        Advice.to(BoxedArgumentsWriteDelegateExit.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testBoxedArgumentsCannotWrite() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(BoxedArgumentsCannotWrite.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testNoArgumentsCannotWrite() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(NoArgumentsCannotWrite.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testSelfCallHandleNoConstructor() throws Exception {
        new ByteBuddy()
                .redefine(SelfCallHandleSample.class)
                .visit(Advice.to(SelfCallHandleSample.class).on(isConstructor()))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testSelfCallHandleNotAssignable() throws Exception {
        Advice.to(SelfCallHandleIllegalSample.class);
    }

    @SuppressWarnings("unused")
    public static class Sample {

        private Object object;

        public static int enter, exit;

        public static Throwable throwable;

        public String foo() {
            return FOO;
        }

        public String foobar() {
            throw new RuntimeException();
        }

        public String bar(String argument) {
            return argument;
        }

        public String qux(String first, String second) {
            return first + second;
        }

        public static String baz() {
            return FOO;
        }

        public String quxbaz() {
            String foo = FOO, bar = BAR, qux = QUX, baz = BAZ;
            return foo + bar + qux + baz;
        }

        public void fooqux() {
            /* do nothing */
        }

        public void barbaz() {
            object.getClass(); // implicit null pointer
        }

        public String foobaz() {
            try {
                throw new Exception();
            } catch (Exception ignored) {
                return FOO;
            }
        }
    }

    public static class SampleExtension extends Sample {

        public Object foo;

        public String foo() {
            return null;
        }
    }

    public static class TracableSample {

        public static int enter, exit, inside;

        public void foo() {
            inside++;
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyDelegationAdvice {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodEnter
        private static void enter() {
            /* empty */
        }

        @Advice.OnMethodExit
        private static void exit() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class TrivialAdvice {

        @Advice.OnMethodEnter
        private static void enter() {
            Sample.enter++;
        }

        @Advice.OnMethodExit(onThrowable = Exception.class)
        private static void exit() {
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class TrivialAdviceWithSuppression {

        @Advice.OnMethodEnter(suppress = Exception.class)
        private static void enter() {
            Sample.enter++;
        }

        @Advice.OnMethodExit(suppress = Exception.class)
        private static void exit() {
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class TrivialAdviceDelegation {

        public static int enter, exit;

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(inline = false)
        private static void enter() {
            enter++;
        }

        @Advice.OnMethodExit(inline = false, onThrowable = Exception.class)
        private static void exit() {
            exit++;
        }
    }


    @SuppressWarnings("unused")
    public static class TypedAdviceDelegation {

        public static int enter, exit;

        public String foo(String argument) {
            return argument;
        }

        @Advice.OnMethodEnter(inline = false)
        private static String enter(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            enter++;
            return BAR;
        }

        @Advice.OnMethodExit(inline = false, onThrowable = Exception.class)
        private static void exit(@Advice.Enter String enter) {
            if (!BAR.equals(enter)) {
                throw new AssertionError();
            }
            exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyMethod {

        public void foo() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdvice {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceDelegation {

        public void foo() {
            /* do nothing */
        }

        @Advice.OnMethodEnter(inline = false)
        @Advice.OnMethodExit(inline = false)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceWithEntrySuppression {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        @Advice.OnMethodExit
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceWithExitSuppression {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit(suppress = Throwable.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceWithSuppression {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit(suppress = Throwable.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceWithExceptionHandling {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit(onThrowable = Exception.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceWithExceptionHandlingAndEntrySuppression {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        @Advice.OnMethodExit(onThrowable = Exception.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceWithExceptionHandlingAndExitSuppression {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Exception.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceWithExceptionHandlingAndSuppression {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Exception.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceEntry {

        @Advice.OnMethodEnter
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceEntryWithSuppression {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceWithEnterValue {

        @Advice.OnMethodEnter
        private static Object enter() {
            return null;
        }

        @Advice.OnMethodExit
        private static void exit() {
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceExit {

        @Advice.OnMethodExit
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceExitAndSuppression {

        @Advice.OnMethodExit(suppress = Throwable.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceExitWithExceptionHandling {

        @Advice.OnMethodExit(onThrowable = Exception.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class EmptyAdviceExitWithExceptionHandlingAndSuppression {

        @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Exception.class)
        private static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class TrivialAdviceSkipException {

        @Advice.OnMethodEnter
        private static void enter() {
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit() {
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class TrivialAdviceSkipExceptionWithSuppression {

        @Advice.OnMethodEnter(suppress = Exception.class)
        private static void enter() {
            Sample.enter++;
        }

        @Advice.OnMethodExit(suppress = Exception.class)
        private static void exit() {
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ArgumentAdvice {

        public static int enter, exit;

        @Advice.OnMethodEnter
        private static void enter(String argument) {
            if (!argument.equals(BAR)) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(String argument) {
            if (!argument.equals(BAR)) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ArgumentAdviceExplicit {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Argument(1) String argument) {
            if (!argument.equals(BAR)) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Argument(1) String argument) {
            if (!argument.equals(BAR)) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ArgumentAdviceDelegationImplicit {

        public String foo(String value) {
            return value;
        }

        @Advice.OnMethodEnter(inline = false)
        private static void enter(String argument) {
            if (!argument.equals(BAR)) {
                throw new AssertionError();
            }
        }

        @Advice.OnMethodExit(inline = false)
        private static void exit(String argument) {
            if (!argument.equals(BAR)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IncrementSample {

        public int foo(int value) {
            return ++value;
        }
    }

    @SuppressWarnings("unused")
    public static class IncrementAdvice {

        @Advice.OnMethodEnter
        private static int enter(@Advice.Argument(value = 0, readOnly = false) int argument, @Advice.Unused int ignored) {
            if (++argument != 1) {
                throw new AssertionError();
            }
            if (++ignored != 0) {
                throw new AssertionError();
            }
            int value = 0;
            if (++value != 1) {
                throw new AssertionError();
            }
            return value;
        }

        @Advice.OnMethodExit
        private static int exit(@Advice.Return(readOnly = false) int argument, @Advice.Unused int ignored) {
            if (++argument != 3) {
                throw new AssertionError();
            }
            if (++ignored != 0) {
                throw new AssertionError();
            }
            int value = 0;
            if (++value != 1) {
                throw new AssertionError();
            }
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class ThisReferenceAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.This Sample thiz) {
            if (thiz == null) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.This Sample thiz) {
            if (thiz == null) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class OptionalThisReferenceAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.This(optional = true) Sample thiz) {
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.This(optional = true) Sample thiz) {
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ReturnValueAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Return String value) {
            if (!value.equals(FOO)) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ExceptionHandlerAdvice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        private static void enter() {
            try {
                throw new Exception();
            } catch (Exception ignored) {
                Sample.enter++;
            }
        }

        @Advice.OnMethodExit(suppress = Throwable.class)
        private static void exit() {
            try {
                throw new Exception();
            } catch (Exception ignored) {
                Sample.exit++;
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ObsoleteReturnValueAdvice {

        @Advice.OnMethodEnter
        private static int enter() {
            Sample.enter++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class UnusedReturnValueAdvice {

        @Advice.OnMethodEnter
        private static int enter() {
            Sample.enter++;
            return VALUE;
        }

        @Advice.OnMethodExit
        private static void exit() {
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ThrowableAdvice {

        @Advice.OnMethodExit(onThrowable = Exception.class)
        private static void exit(@Advice.Thrown Throwable throwable) {
            Sample.throwable = throwable;
        }
    }

    @SuppressWarnings("unused")
    public static class ThrowOnEnter {

        @Advice.OnMethodEnter
        private static void enter() {
            TracableSample.enter++;
            throw new RuntimeException();
        }

        @Advice.OnMethodExit
        private static void exit() {
            TracableSample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ThrowOnExit {

        @Advice.OnMethodEnter
        private static void enter() {
            TracableSample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit() {
            TracableSample.exit++;
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class ThrowSuppressed {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        private static void enter() {
            TracableSample.enter++;
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        private static void exit() {
            TracableSample.exit++;
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class ThrowNotSuppressedOnEnter {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        private static void enter() throws Exception {
            TracableSample.enter++;
            throw new Exception();
        }
    }

    @SuppressWarnings("unused")
    public static class ThrowNotSuppressedOnExit {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        private static void enter() {
            TracableSample.enter++;
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        private static void exit() throws Exception {
            TracableSample.exit++;
            throw new Exception();
        }
    }

    @SuppressWarnings("unused")
    public static class ThisSubstitutionAdvice {

        @Advice.OnMethodEnter
        @SuppressWarnings("all")
        private static void enter(@Advice.This(readOnly = false) Box box) {
            box = new Box(BAR);
        }
    }

    @SuppressWarnings("unused")
    public static class ThisOptionalSubstitutionAdvice {

        @Advice.OnMethodEnter
        @SuppressWarnings("all")
        private static void enter(@Advice.This(readOnly = false, optional = true) Box box) {
            box = new Box(BAR);
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThisSubstitutionAdvice {

        @Advice.OnMethodEnter
        @SuppressWarnings("all")
        private static void enter(@Advice.This Box box) {
            box = new Box(BAR);
        }
    }

    @SuppressWarnings("unused")
    public static class ParameterSubstitutionAdvice {

        @Advice.OnMethodEnter
        @SuppressWarnings("all")
        private static void enter(@Advice.Argument(value = 0, readOnly = false) String value) {
            value = BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalParameterSubstitutionAdvice {

        @Advice.OnMethodEnter
        @SuppressWarnings("all")
        private static void enter(@Advice.Argument(0) String value) {
            value = BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class ReturnSubstitutionAdvice {

        @Advice.OnMethodExit
        @SuppressWarnings("all")
        private static void exit(@Advice.Return(readOnly = false) String value) {
            value = BAR;
            if (!value.equals(BAR)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalReturnSubstitutionAdvice {

        @Advice.OnMethodExit
        @SuppressWarnings("all")
        private static void exit(@Advice.Return String value) {
            value = BAR;
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class Box {

        public final String value;

        public Box(String value) {
            this.value = value;
        }

        public String foo() {
            return value;
        }

        public static String bar(String value) {
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class FieldSample {

        public static int enter, exit;

        private String foo = FOO;

        public String foo() {
            return foo;
        }

        public static String bar() {
            return BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class Bean {

        public static int enter;

        private String foo;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceBean {

        @Advice.OnMethodExit
        private static void enter(@Advice.FieldValue String propertyValue, @Advice.Origin("#p") String propertyName) {
            Bean.enter++;
            if (!propertyValue.equals(BAR)) {
                throw new AssertionError();
            }
            if (!propertyName.equals(FOO)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceImplicit {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue("foo") String foo) {
            FieldSample.enter++;
            if (!foo.equals(FOO)) {
                throw new AssertionError();
            }
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.FieldValue("foo") String foo) {
            FieldSample.exit++;
            if (!foo.equals(FOO)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceExplicit {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue(value = "foo", declaringType = FieldSample.class) String foo) {
            FieldSample.enter++;
            if (!foo.equals(FOO)) {
                throw new AssertionError();
            }
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.FieldValue(value = "foo", declaringType = FieldSample.class) String foo) {
            FieldSample.exit++;
            if (!foo.equals(FOO)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceHandleBean {

        @Advice.OnMethodExit
        private static void enter(@Advice.FieldGetterHandle Object handle, @Advice.Origin("#p") String propertyName) throws Throwable {
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            Bean.enter++;
            if (!method.invoke(handle, Collections.emptyList()).equals(BAR)) {
                throw new AssertionError();
            }
            if (!propertyName.equals(FOO)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceHandleSetterBean {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldSetterHandle Object setter) throws Throwable {
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            Bean.enter++;
            method.invoke(setter, Collections.singletonList(QUX));
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceHandleImplicit {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldGetterHandle("foo") Object handle) throws Throwable{
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            FieldSample.enter++;
            if (!method.invoke(handle, Collections.emptyList()).equals(FOO)) {
                throw new AssertionError();
            }
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.FieldGetterHandle("foo") Object handle) throws Throwable {
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            FieldSample.exit++;
            if (!method.invoke(handle, Collections.emptyList()).equals(FOO)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceHandleExplicit {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldGetterHandle(value = "foo", declaringType = FieldSample.class) Object handle) throws Throwable {
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            FieldSample.enter++;
            if (!method.invoke(handle, Collections.emptyList()).equals(FOO)) {
                throw new AssertionError();
            }
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.FieldGetterHandle(value = "foo", declaringType = FieldSample.class) Object handle) throws Throwable {
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            FieldSample.exit++;
            if (!method.invoke(handle, Collections.emptyList()).equals(FOO)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class OriginAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Origin String origin) throws Exception {
            if (!origin.equals(Sample.class.getDeclaredMethod(FOO).toString())) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin String origin) throws Exception {
            if (!origin.equals(Sample.class.getDeclaredMethod(FOO).toString())) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class OriginCustomAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Origin("#t #m #d #r #s") String origin, @Advice.Origin Class<?> type) throws Exception {
            if (!origin.equals(Sample.class.getName() + " "
                    + FOO
                    + " ()L" + String.class.getName().replace('.', '/') + "; "
                    + String.class.getName()
                    + " ()")) {
                throw new AssertionError();
            }
            if (type != Sample.class) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin("\\#\\#\\\\#m") String origin, @Advice.Origin Class<?> type) throws Exception {
            if (!origin.equals("##\\" + FOO)) {
                throw new AssertionError();
            }
            if (type != Sample.class) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class OriginMethodStackSizeAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Origin Method origin) throws Exception {
            Object ignored = origin;
        }
    }

    @SuppressWarnings("unused")
    public static class OriginConstructorStackSizeAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Origin Constructor<?> origin) throws Exception {
            Object ignored = origin;
        }
    }

    @SuppressWarnings("unused")
    public static class OriginMethodAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Origin Method origin) throws Exception {
            if (!origin.equals(Sample.class.getDeclaredMethod(BAR, String.class))) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin Method origin) throws Exception {
            if (!origin.equals(Sample.class.getDeclaredMethod(BAR, String.class))) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class OriginConstructorAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Origin Constructor<?> origin) throws Exception {
            if (!origin.equals(Sample.class.getDeclaredConstructor())) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin Constructor<?> origin) throws Exception {
            if (!origin.equals(Sample.class.getDeclaredConstructor())) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("all")
    public static class FrameShiftAdvice {

        @Advice.OnMethodEnter
        private static String enter() {
            int v0 = 0;
            if (v0 != 0) {
                // do nothing
            }
            return BAR;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Enter String value) {
            if (!value.equals(BAR)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ExceptionTypeAdvice {

        public static int enter, exit;

        public void foo() {
            throw new IllegalStateException();
        }

        @Advice.OnMethodEnter(suppress = UnsupportedOperationException.class)
        private static void enter() {
            enter++;
            throw new UnsupportedOperationException();
        }

        @Advice.OnMethodExit(suppress = ArrayIndexOutOfBoundsException.class, onThrowable = IllegalStateException.class)
        private static void exit() {
            exit++;
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    @SuppressWarnings("unused")
    public static class ExceptionNotCatchedAdvice {

        public static int enter, exit;

        public void foo() throws Exception {
            throw new Exception();
        }

        @Advice.OnMethodEnter
        private static void enter() {
            enter++;
        }

        @Advice.OnMethodExit(onThrowable = RuntimeException.class)
        private static void exit() {
            exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ExceptionCatchedAdvice {

        public static int enter, exit;

        public void foo() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        private static void enter() {
            enter++;
        }

        @Advice.OnMethodExit(onThrowable = Exception.class)
        private static void exit() {
            exit++;
        }
    }

    @SuppressWarnings("all")
    public static class ExceptionCatchedWithExchangeAdvice {

        public static int enter, exit;

        public void foo() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        private static void enter() {
            enter++;
        }

        @Advice.OnMethodExit(onThrowable = RuntimeException.class)
        private static void exit(@Advice.Thrown(readOnly = false) Throwable throwable) {
            exit++;
            throwable = new IOException();
        }
    }

    @SuppressWarnings("all")
    public static class NonAssignableReturnTypeAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object value) {
            value = new Object();
        }
    }

    @SuppressWarnings("all")
    public static class TrivialReturnTypeAdvice {

        public Object foo() {
            return FOO;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object value) {
            value = BAR;
        }
    }

    @SuppressWarnings("all")
    public static class NonAssignablePrimitiveReturnTypeAdvice {

        public int foo() {
            return 0;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object value) {
            value = new Object();
        }
    }

    @SuppressWarnings("unused")
    public static class OptionalArgumentAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodEnter
        private static void enter(@Advice.Argument(value = 0, optional = true) String value) {
            if (value != null) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ParameterAnnotationSample {

        public String foo(@SampleParameter String value) {
            return value;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Unused Void ignored1, @Advice.Unused Void ignored2) {
            if (ignored1 != null || ignored2 != null) {
                throw new AssertionError();
            }
        }

        @Retention(RetentionPolicy.RUNTIME)
        public @interface SampleParameter {
            /* empty */
        }
    }

    public static class HandleSample {

        public static String foo;

        public static String bar() {
            return BAR;
        }

        public String foo(String value) {
            return value;
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Handle(
                type = JavaConstant.MethodHandle.HandleType.INVOKE_STATIC,
                name = "bar",
                returnType = String.class,
                parameterTypes = {}) Object bound) throws Throwable {
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            foo = method.invoke(bound, Collections.emptyList()).toString();
        }
    }

    public static class SelfCallHandleSample {

        protected static boolean checked;

        public static String foo;

        public String foo(String value) {
            return value;
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.SelfCallHandle Object bound, @Advice.SelfCallHandle(bound = false) Object unbound) throws Throwable {
            if (checked) {
                return;
            } else {
                checked = true;
            }
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            foo = method.invoke(bound, Collections.emptyList()).toString() + method.invoke(unbound, Arrays.asList(new SelfCallHandleSample(), BAR));
        }
    }

    public static class SelfCallHandleSubclass extends SelfCallHandleSample {

        private int check;

        @Override
        public String foo(String value) {
            if (check++ != 0) {
                throw new AssertionError();
            }
            return super.foo(value);
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.SelfCallHandle Object bound, @Advice.SelfCallHandle(bound = false) Object unbound) throws Throwable {
            if (SelfCallHandleSample.checked) {
                return;
            } else {
                SelfCallHandleSample.checked = true;
            }
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            foo = method.invoke(bound, Collections.emptyList()).toString() + method.invoke(unbound, Arrays.asList(new SelfCallHandleSubclass(), BAR));
        }
    }

    public static class SelfCallHandleStaticSample {

        private static boolean checked;

        public static String foo;

        public static String foo(String value) {
            return value;
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.SelfCallHandle Object bound, @Advice.SelfCallHandle(bound = false) Object unbound) throws Throwable {
            if (checked) {
                return;
            } else {
                checked = true;
            }
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            foo = method.invoke(bound, Collections.emptyList()).toString() + method.invoke(unbound, Collections.singletonList(BAR));
        }
    }

    public static class SelfCallHandlePrimitiveSample {

        private static boolean checked;

        public static int foo;

        public int foo(int value) {
            return value;
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.SelfCallHandle Object bound, @Advice.SelfCallHandle(bound = false) Object unbound) throws Throwable {
            if (checked) {
                return;
            } else {
                checked = true;
            }
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            foo = (Integer) method.invoke(bound, Collections.emptyList()) + (Integer) method.invoke(unbound, Arrays.asList(new SelfCallHandlePrimitiveSample(), 84));
        }
    }

    public static class SelfCallHandleIllegalSample {

        @Advice.OnMethodExit
        public static void exit(@Advice.SelfCallHandle String value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceIllegalExplicit {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue(value = "bar", declaringType = Void.class) String bar) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceNonExistent {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue("bar") String bar) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceNonAssignable {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue("foo") Void foo) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("all")
    public static class FieldAdviceWrite {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue("foo") String foo) {
            foo = BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceHandleIllegalExplicit {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldGetterHandle(value = "bar", declaringType = Void.class) String bar) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceHandleNonExistent {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldGetterHandle("bar") String bar) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceGetterHandleNonAssignable {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldGetterHandle("foo") Void foo) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAdviceSetterHandleNonAssignable {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldSetterHandle("foo") Void foo) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("all")
    public static class ExceptionSuppressionAdvice {

        @Advice.OnMethodExit(onThrowable = Exception.class)
        private static void exit(@Advice.Thrown(readOnly = false) Throwable throwable) {
            throwable = null;
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalArgumentAdvice {

        @Advice.OnMethodEnter
        private static void enter(Void argument) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalArgumentReadOnlyAdvice {

        @SuppressWarnings("unused")
        @Advice.OnMethodEnter
        private static void enter(@Advice.Argument(0) Void argument) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalArgumentWritableAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Argument(value = 0, readOnly = false) Object argument) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class DuplicateAdvice {

        @Advice.OnMethodEnter
        private static void enter1() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter
        private static void enter2() {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonStaticAdvice {

        @Advice.OnMethodEnter
        private void enter() {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThisReferenceAdvice {

        @Advice.OnMethodExit
        private static void enter(@Advice.This Void thiz) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThisReferenceWritableAdvice {

        @Advice.OnMethodExit
        private static void enter(@Advice.This(readOnly = false) Object thiz) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class AmbiguousAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Argument(0) @Advice.This Object thiz) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class AmbiguousAdviceDelegation {

        @Advice.OnMethodEnter(inline = false)
        private static void enter(@Advice.Argument(0) @Advice.This Object thiz) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class EnterToReturnAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Return Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonAssignableReturnAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Return Void value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonEqualReturnWritableAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Return(readOnly = false) Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThrowableRequestAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Thrown Throwable value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThrowableTypeAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Thrown Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalOriginType {

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin Void value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalOriginPattern {

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin("#x") String value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalOriginPatternEnd {

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin("#") String value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("all")
    public static class IllegalOriginWriteAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin String value) {
            value = null;
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalArgumentWritableInlineAdvice {

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Argument(value = 0, readOnly = false) Object argument) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThisWritableInlineAdvice {

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.This(readOnly = false) Object argument) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThrowWritableInlineAdvice {

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Thrown(readOnly = false) Object argument) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalReturnWritableInlineAdvice {

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Return(readOnly = false) Object argument) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalFieldWritableInlineAdvice {

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.Argument(value = 0, readOnly = false) Object argument) {
            throw new AssertionError();
        }
    }

    public static class IllegalBoxedReturnType {

        @Advice.OnMethodEnter
        private static void advice(@Advice.StubValue int value) {
            throw new AssertionError();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Custom {
        /* empty */
    }

    @SuppressWarnings("unused")
    public static class CustomAdvice {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit
        private static void advice(@Custom Object value) {
            if (value != Object.class && value != RetentionPolicy.CLASS && value != null) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class CustomPrimitiveAdvice {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit
        private static void advice(@Custom int value) {
            if (value == 0) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class CustomSerializableAdvice {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit
        private static void advice(@Custom Map<String, String> value) {
            if (value.size() != 1 && !value.get(FOO).equals(BAR)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class CustomDynamicAdvice {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit
        private static void advice(@Custom Callable<String> callable) throws Exception {
            if (!callable.call().equals(FOO)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class NonVisibleAdvice {

        @Advice.OnMethodEnter(inline = false)
        private static void enter() {
            /* empty */
        }
    }

    public static class LineNumberAdvice {

        @Advice.OnMethodEnter
        private static void enter() {
            StackTraceElement top = new Throwable().getStackTrace()[0];
            if (top.getLineNumber() < 0) {
                throw new AssertionError();
            }
        }
    }

    public static class NoLineNumberAdvice {

        @Advice.OnMethodEnter(prependLineNumber = false)
        private static void enter() {
            StackTraceElement top = new Throwable().getStackTrace()[0];
            if (top.getLineNumber() >= 0) {
                throw new AssertionError();
            }
        }
    }

    public static class LineNumberDelegatingAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(inline = false)
        private static void enter() {
            StackTraceElement top = new Throwable().getStackTrace()[1];
            if (top.getLineNumber() < 0) {
                throw new AssertionError();
            }
        }
    }

    public static class NoLineNumberDelegatingAdvice {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(inline = false, prependLineNumber = false)
        private static void enter() {
            StackTraceElement top = new Throwable().getStackTrace()[1];
            if (top.getLineNumber() >= 0) {
                throw new AssertionError();
            }
        }
    }

    public static class BoxedArgumentsStackSizeAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.AllArguments Object[] value) {
            Object ignored = value;
        }
    }

    public static class BoxedArgumentsObjectTypeAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.AllArguments Object value) {
            Object ignored = value;
        }
    }

    public static class BoxedArgumentsWriteDelegateEntry {

        @Advice.OnMethodEnter(inline = false)
        private static void enter(@Advice.AllArguments(readOnly = false) Object[] value) {
            throw new AssertionError();
        }
    }

    public static class BoxedArgumentsWriteDelegateExit {

        @Advice.OnMethodExit(inline = false)
        private static void exit(@Advice.AllArguments(readOnly = false) Object[] value) {
            throw new AssertionError();
        }
    }

    public static class BoxedArgumentsCannotWrite {

        @Advice.OnMethodEnter
        private static void enter(@Advice.AllArguments Object[] value) {
            value = new Object[0];
        }
    }

    public static class NoArguments {

        @Advice.OnMethodEnter
        private static void enter(@Advice.AllArguments(nullIfEmpty = true) Object[] value) {
            if (value != null) {
                throw new AssertionError();
            }
        }
    }

    public static class NoArgumentsWrite {

        @Advice.OnMethodEnter
        private static void enter(@Advice.AllArguments(readOnly = false, nullIfEmpty = true) Object[] value) {
            value = new Object[0];
        }
    }

    public static class NoArgumentsCannotWrite {

        @Advice.OnMethodEnter
        private static void enter(@Advice.AllArguments(nullIfEmpty = true) Object[] value) {
            value = new Object[0];
        }
    }

    public static class AllArgumentsIncludeSelf {

        @Advice.OnMethodEnter
        private static void enter(@Advice.AllArguments(includeSelf = true) Object[] value) {
            if (value.length != 1 || !(value[0] instanceof Sample)) {
                throw new AssertionError();
            }
        }
    }

    public static class ExceptionWriterSample extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public static boolean exception;

        public String foo() {
            return FOO;
        }

        @Override
        public void printStackTrace() {
            exception = true;
        }
    }

    public static class ExceptionWriterTest {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        @Advice.OnMethodExit(suppress = RuntimeException.class)
        private static void advice() {
            throw new ExceptionWriterSample();
        }
    }

    public static class ConstructorEnterFrameAdvice {

        @Advice.OnMethodEnter
        public static void advice() {
            String foo = "foo";
            if (foo.equals("xxx")) {
                throw new AssertionError();
            }
        }
    }

    public static class ConstructorEnterFrameLargeAdvice {

        @Advice.OnMethodEnter
        public static void advice() {
            String foo = "foo", bar = "bar", qux = "qux", baz = "baz";
            if (foo.equals("xxx") || bar.equals("xxx") || qux.equals("xxx") || baz.equals("xxx")) {
                throw new AssertionError();
            }
        }
    }

    public static class EnterLocalVariableNotAllowedAdvice {

        @Advice.OnMethodEnter
        public static void advice(@Advice.Local("foo") Void argument) {
            /* empty */
        }
    }

    public static class NoBackupArguments {

        public NoBackupArguments() {
            /* empty */
        }

        public NoBackupArguments(boolean value) {
            if (value) {
                throw new IllegalStateException();
            }
        }

        @Advice.OnMethodExit(backupArguments = false)
        public static void advice() {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class AllArgumentsConstructor {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit
        public static void advice(@Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args) {
            args = new Object[0];
            String ignored = "" + args;
        }
    }

    public static class PostProcessorInline {

        @Advice.OnMethodEnter
        static String enter() {
            return "foo";
        }

        public String foo(String x) {
            return x;
        }
    }

    public static class PostProcessorDelegate {

        @Advice.OnMethodEnter(inline = false)
        static String enter() {
            return "foo";
        }

        public String foo(String x) {
            return x;
        }
    }

    public static class ConstructorWriteField {

        public String foo;

        @Advice.OnMethodEnter
        static String enter(@Advice.FieldValue(value = "foo", readOnly = false) String value) {
            return value = "foo";
        }
    }

    public static class EmptyExitAdvice {

        @Advice.OnMethodExit
        static void exit() {
            /* do nothing */
        }
    }
}
