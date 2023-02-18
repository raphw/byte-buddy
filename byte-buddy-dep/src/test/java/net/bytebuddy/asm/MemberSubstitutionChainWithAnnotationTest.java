package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.InjectionClassLoader;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class MemberSubstitutionChainWithAnnotationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz", RUN = "run";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    public void testArgumentToElement() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ArgumentSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(ArgumentSample.class.getMethod("element", String.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is((Object) QUX));
        assertThat(type.getDeclaredMethod(RUN, String.class).invoke(instance, BAZ), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is((Object) BAZ));
    }

    @Test
    public void testArgumentToMethod() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ArgumentSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(ArgumentSample.class.getMethod("method", String.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is((Object) QUX));
        assertThat(type.getDeclaredMethod(RUN, String.class).invoke(instance, BAZ), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is((Object) BAZ));
    }

    @Test
    public void testArgumentOptional() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ArgumentSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(ArgumentSample.class.getMethod("optional", String.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is((Object) QUX));
        assertThat(type.getDeclaredMethod(RUN, String.class).invoke(instance, BAZ), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), nullValue(Object.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testArgumentNone() throws Exception {
        new ByteBuddy()
                .redefine(ArgumentSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(ArgumentSample.class.getMethod("none", String.class)))
                        .on(named(RUN)))
                .make();
    }

    @Test
    public void testThisReferenceToElement() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ThisReferenceSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(ThisReferenceSample.class.getMethod("element", Object.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance(), argument = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is((Object) QUX));
        assertThat(type.getDeclaredMethod(RUN, type).invoke(instance, argument), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is(argument));
    }

    @Test
    public void testThisReferenceToMethod() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ThisReferenceSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(ThisReferenceSample.class.getMethod("method", Object.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance(), argument = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is((Object) QUX));
        assertThat(type.getDeclaredMethod(RUN, type).invoke(instance, argument), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is(instance));
    }

    @Test
    public void testThisReferenceOptional() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ThisReferenceSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(BAZ))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(ThisReferenceSample.class.getMethod("optional", Object.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance(), argument = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is((Object) QUX));
        assertThat(type.getDeclaredField(BAZ).get(null), is((Object) BAZ));
        assertThat(type.getDeclaredMethod(RUN, type).invoke(instance, argument), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), nullValue(Object.class));
        assertThat(type.getDeclaredField(BAZ).get(null), is((Object) BAZ));
    }

    @Test(expected = IllegalStateException.class)
    public void testThisReferenceNone() throws Exception {
        new ByteBuddy()
                .redefine(ThisReferenceSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(BAZ))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(ThisReferenceSample.class.getMethod("none", Object.class)))
                        .on(named(RUN)))
                .make();
    }

    @Test
    public void testAllArgumentsToElement() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(AllArgumentsSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(AllArgumentsSample.class.getMethod("element", String[].class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is((Object) QUX));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is((Object) BAR));
    }

    @Test
    public void testAllArgumentsToMethod() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(AllArgumentsSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(AllArgumentsSample.class.getMethod("method", String[].class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is((Object) QUX));
        assertThat(type.getDeclaredMethod(RUN, String.class).invoke(instance, BAZ), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is((Object) BAZ));
    }

    @Test
    public void testAllArgumentsSelf() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(AllArgumentsSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(AllArgumentsSample.class.getMethod("self", Object[].class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is((Object) QUX));
        assertThat(type.getDeclaredField(BAZ).get(null), nullValue(Object.class));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is((Object) BAR));
        assertThat(type.getDeclaredField(BAZ).get(null), is(instance));
    }

    @Test
    public void testAllArgumentsEmpty() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(AllArgumentsSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(BAR))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(AllArgumentsSample.class.getMethod("empty", String[].class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is((Object) QUX));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(null), is((Object) QUX));
    }

    @Test(expected = IllegalStateException.class)
    public void testAllArgumentsIllegal() throws Exception {
        new ByteBuddy()
                .redefine(ThisReferenceSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(BAZ))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(AllArgumentsSample.class.getMethod("illegal", Void.class)))
                        .on(named(RUN)))
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = SelfCallHandleSample.class)
    public void testSelfCallHandle() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(SelfCallHandleSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(SelfCallHandleSample.class.getMethod("handle", Object.class, Object.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor(String.class).newInstance(FOO);
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), is((Object) (FOO + BAR)));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = SelfCallHandleSample.class)
    public void testSelfCallHandleHierarchy() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(SelfCallHandleSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(SelfCallHandleSubclass.class.getMethod("handle", Object.class, Object.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER.opened())
                .getLoaded();
        Object instance = ((InjectionClassLoader) type.getClassLoader()).defineClass(SelfCallHandleSubclass.class.getName(), ClassFileLocator.ForClassLoader.read(SelfCallHandleSubclass.class))
                .getDeclaredConstructor(String.class)
                .newInstance(FOO);
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), is((Object) (FOO + BAR)));
    }

    @Test
    public void testFieldValueNamedImplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldValueTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(BAR))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(FieldValueTest.class.getMethod("implicit", String.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(FieldValueTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), is((Object) FOO));
    }

    @Test
    public void testFieldValueNamedExplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldValueTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(BAR))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(FieldValueTest.class.getMethod("explicit", String.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(FieldValueTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), is((Object) FOO));
    }

    @Test
    public void testFieldValueAccessor() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldValueTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(BAR))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(FieldValueTest.class.getMethod("accessor", String.class)))
                        .on(named("getFoo")))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(FieldValueTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod("getFoo").invoke(instance), is((Object) FOO));
    }

    @Test
    public void testFieldValueProvided() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldValueTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(BAR))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.withCustomMapping()
                                .bind(Custom.class, FieldValueTest.class.getDeclaredField(FOO))
                                .to(FieldValueTest.class.getMethod("provided", String.class)))
                        .on(named("getFoo")))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(FieldValueTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod("getFoo").invoke(instance), is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = FieldGetterHandlerTest.class)
    public void testFieldGetterHandleNamedImplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldGetterHandlerTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(BAR))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(FieldGetterHandlerTest.class.getMethod("implicit", Object.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(FieldGetterHandlerTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = FieldGetterHandlerTest.class)
    public void testFieldGetterHandleNamedExplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldGetterHandlerTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(BAR))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(FieldGetterHandlerTest.class.getMethod("explicit", Object.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(FieldGetterHandlerTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = FieldGetterHandlerTest.class)
    public void testFieldGetterHandleAccessor() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldGetterHandlerTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(BAR))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(FieldGetterHandlerTest.class.getMethod("accessor", Object.class)))
                        .on(named("getFoo")))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(FieldGetterHandlerTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod("getFoo").invoke(instance), is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = FieldSetterHandlerTest.class)
    public void testFieldSetterHandleNamedImplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldSetterHandlerTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(BAR))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(FieldSetterHandlerTest.class.getMethod("implicit", Object.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(FieldSetterHandlerTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), is((Object) BAZ));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) QUX));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = FieldSetterHandlerTest.class)
    public void testFieldSetterHandleNamedExplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldSetterHandlerTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(BAR))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(FieldSetterHandlerTest.class.getMethod("explicit", Object.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(FieldSetterHandlerTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), is((Object) BAZ));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) QUX));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = FieldSetterHandlerTest.class)
    public void testFieldSetterHandleAccessor() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldSetterHandlerTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(BAR))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(FieldSetterHandlerTest.class.getMethod("accessor", Object.class)))
                        .on(named("getFoo")))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(FieldSetterHandlerTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod("getFoo").invoke(instance), is((Object) BAZ));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) QUX));
    }

    @Test
    public void testUnused() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(UnusedTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(UnusedTest.class.getMethod("unused", Object.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(UnusedTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), is((Object) BAR));
    }

    @Test
    public void testStubValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(StubValueTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(StubValueTest.class.getMethod("stubbed", Object.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(StubValueTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), is((Object) BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testStubValueIllegal() throws Exception {
        new ByteBuddy()
                .redefine(StubValueTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(StubValueTest.class.getMethod("illegal", String.class)))
                        .on(named(RUN)))
                .make();
    }

    @Test
    public void testOriginElementString() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(OriginTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(OriginTest.class.getDeclaredMethod("stringElement", String.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(OriginTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), is((Object) OriginTest.class.getDeclaredField(FOO).toString()));
    }

    @Test
    public void testOriginMethodString() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(OriginTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(OriginTest.class.getDeclaredMethod("stringMethod", String.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(OriginTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), is((Object) OriginTest.class.getDeclaredMethod(RUN).toString()));
    }

    @Test
    public void testOriginElementField() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(OriginTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(OriginTest.class.getDeclaredMethod("fieldElement", Field.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(OriginTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance).toString(), is((Object) OriginTest.class.getDeclaredField(FOO).toString()));
    }

    @Test
    public void testOriginMethodMethod() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(OriginTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(OriginTest.class.getDeclaredMethod("methodMethod", Method.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(OriginTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance).toString(), is((Object) OriginTest.class.getDeclaredMethod(RUN).toString()));
    }

    @Test
    public void testOriginElementType() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(OriginTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(OriginTest.class.getDeclaredMethod("typeElement", Class.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(OriginTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance).toString(), is((Object) OriginTest.class.toString()));
    }

    @Test
    public void testOriginMethodType() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(OriginTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(OriginTest.class.getDeclaredMethod("typeMethod", Class.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(OriginTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance).toString(), is((Object) OriginTest.class.toString()));
    }

    @Test
    @JavaVersionRule.Enforce(8) // Cannot resolve field types from method handles prior to Java 8
    public void testOriginElementMethodHandle() throws Exception {
        Class<?> origin = Class.forName("net.bytebuddy.test.precompiled.v7.MemberSubstitutionOriginMethodHandle");
        Class<?> type = new ByteBuddy()
                .redefine(origin)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(origin.getDeclaredMethod("handleElement", JavaType.METHOD_HANDLE.load())))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(origin.getName()).getDeclaredConstructor().newInstance();
        assertThat(JavaConstant.MethodHandle.ofLoaded(type.getDeclaredMethod(RUN).invoke(instance)), is(JavaConstant.MethodHandle.ofGetter(origin.getDeclaredField(FOO))));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testOriginMethodMethodHandle() throws Exception {
        Class<?> origin = Class.forName("net.bytebuddy.test.precompiled.v7.MemberSubstitutionOriginMethodHandle");
        Class<?> type = new ByteBuddy()
                .redefine(origin)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(origin.getDeclaredMethod("handleMethod", JavaType.METHOD_HANDLE.load())))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(origin.getName()).getDeclaredConstructor().newInstance();
        assertThat(JavaConstant.MethodHandle.ofLoaded(type.getDeclaredMethod(RUN).invoke(instance)), is(JavaConstant.MethodHandle.of(origin.getDeclaredMethod(RUN))));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testOriginElementMethodType() throws Exception {
        Class<?> origin = Class.forName("net.bytebuddy.test.precompiled.v7.MemberSubstitutionOriginMethodType");
        Class<?> type = new ByteBuddy()
                .redefine(origin)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(origin.getDeclaredMethod("methodTypeElement", JavaType.METHOD_TYPE.load())))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(origin.getName()).getDeclaredConstructor().newInstance();
        assertThat(JavaConstant.MethodType.ofLoaded(type.getDeclaredMethod(RUN).invoke(instance)), is(JavaConstant.MethodType.ofGetter(origin.getDeclaredField(FOO))));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testOriginMethodMethodType() throws Exception {
        Class<?> origin = Class.forName("net.bytebuddy.test.precompiled.v7.MemberSubstitutionOriginMethodType");
        Class<?> type = new ByteBuddy()
                .redefine(origin)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(origin.getDeclaredMethod("methodTypeMethod", JavaType.METHOD_TYPE.load())))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(origin.getName()).getDeclaredConstructor().newInstance();
        assertThat(JavaConstant.MethodType.ofLoaded(type.getDeclaredMethod(RUN).invoke(instance)), is(JavaConstant.MethodType.of(origin.getDeclaredMethod(RUN))));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testOriginLookup() throws Exception {
        Class<?> origin = Class.forName("net.bytebuddy.test.precompiled.v7.MemberSubstitutionOriginMethodHandlesLookup");
        Class<?> type = new ByteBuddy()
                .redefine(origin)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(origin.getDeclaredMethod("lookup", JavaType.METHOD_HANDLES_LOOKUP.load())))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(origin.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), instanceOf(JavaType.METHOD_HANDLES_LOOKUP.load()));
    }

    @Test
    public void testCurrent() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(CurrentTest.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(
                                MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(CurrentTest.class.getMethod("first", String.class)),
                                MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(CurrentTest.class.getMethod("second", String.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(CurrentTest.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), is((Object) QUX));
    }

    @Test
    public void testSerializable() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(SerializableSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.withCustomMapping()
                                .bindSerialized(Custom.class, (Serializable) Collections.singletonMap(FOO, BAR))
                                .to(SerializableSample.class.getDeclaredMethod("delegate", Map.class)))
                        .on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(SerializableSample.class.getName()).getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), is((Object) BAR));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = DynamicSample.class)
    public void testAdviceDynamicInvocation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(DynamicSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.withCustomMapping().bindDynamic(Custom.class,
                                Class.forName("net.bytebuddy.test.precompiled.v7.DynamicSampleBootstrap").getMethod("callable",
                                        JavaType.METHOD_HANDLES_LOOKUP.load(),
                                        String.class,
                                        JavaType.METHOD_TYPE.load(),
                                        String.class),
                                FOO).to(DynamicSample.class.getDeclaredMethod("delegate", Callable.class))).on(named(RUN)))
                .make()
                .load(DynamicSample.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getMethod(RUN).invoke(type.getConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce(value = 8, target = DynamicSample.class)
    public void testDynamicLambdaInvocation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(DynamicSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.withCustomMapping().bindLambda(Custom.class,
                                DynamicSample.class.getDeclaredMethod("baz"),
                                Callable.class).to(DynamicSample.class.getDeclaredMethod("delegate", Callable.class))).on(named(RUN)))
                .make()
                .load(DynamicSample.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getMethod(RUN).invoke(type.getConstructor().newInstance()), is((Object) BAZ));
    }


    @Test
    @JavaVersionRule.Enforce(value = 7, target = BootstrapSample.class)
    public void testDelegationBootstrapped() throws Exception {
        Class<?> bootstrap = Class.forName("net.bytebuddy.test.precompiled.v7.MemberSubstitutionBootstrap");
        Class<?> type = new ByteBuddy()
                .redefine(BootstrapSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.withCustomMapping().bootstrap(bootstrap.getMethod("bootstrap",
                                JavaType.METHOD_HANDLES_LOOKUP.load(),
                                String.class,
                                JavaType.METHOD_TYPE.load(),
                                String.class,
                                Class.class,
                                String.class,
                                JavaType.METHOD_HANDLE.load(),
                                Class.class,
                                String.class,
                                JavaType.METHOD_HANDLE.load())).to(BootstrapSample.class.getDeclaredMethod("delegate"))).on(named(RUN)))
                .make()
                .load(BootstrapSample.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getDeclaredMethod(RUN).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    public static class ArgumentSample {

        public String foo = FOO, bar = BAR;

        public static String qux = QUX;

        @SuppressWarnings("unused")
        public void run(String value) {
            foo = value;
        }

        public static void element(@MemberSubstitution.Argument(0) String value) {
            qux = value;
        }

        public static void method(@MemberSubstitution.Argument(value = 0, source = MemberSubstitution.Source.ENCLOSING_METHOD) String value) {
            qux = value;
        }

        public static void optional(@MemberSubstitution.Argument(value = 1, optional = true) String value) {
            qux = value;
        }

        public static void none(@MemberSubstitution.Argument(value = 1) String value) {
            qux = value;
        }
    }

    public static class ThisReferenceSample {

        public Object foo = FOO, bar = BAR;

        public static Object qux = QUX, baz = BAZ;

        @SuppressWarnings("unused")
        public void run(ThisReferenceSample sample) {
            sample.foo = sample.bar;
            baz = sample.bar;
        }

        public static void element(@MemberSubstitution.This Object value) {
            qux = value;
        }

        public static void method(@MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object value) {
            qux = value;
        }

        public static void optional(@MemberSubstitution.This(optional = true) Object value) {
            qux = value;
        }

        public static void none(@MemberSubstitution.This Object value) {
            qux = value;
        }
    }

    public static class AllArgumentsSample {

        public String foo = FOO, bar = BAR;

        public static String qux = QUX;
        public static Object baz;

        @SuppressWarnings("unused")
        public void run(String value) {
            foo = value;
        }

        public void run() {
            foo = bar;
        }

        public static void element(@MemberSubstitution.AllArguments String[] value) {
            if (value.length != 1) {
                throw new AssertionError();
            }
            qux = value[0];
        }

        public static String empty(@MemberSubstitution.AllArguments(nullIfEmpty = true) String[] value) {
            if (value != null) {
                throw new AssertionError();
            }
            return null;
        }

        public static void method(@MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) String[] value) {
            if (value.length != 1) {
                throw new AssertionError();
            }
            qux = value[0];
        }

        public static void self(@MemberSubstitution.AllArguments(includeSelf = true) Object[] value) {
            if (value.length != 2) {
                throw new AssertionError();
            }
            qux = (String) value[1];
            baz = value[0];
        }

        public static void illegal(@MemberSubstitution.AllArguments Void ignored) {
            throw new AssertionError();
        }
    }

    public static class SelfCallHandleSample {

        public final String foo;

        public SelfCallHandleSample(String foo) {
            this.foo = foo;
        }

        public String run() {
            return foo;
        }

        public static String handle(
                @MemberSubstitution.SelfCallHandle Object bound,
                @MemberSubstitution.SelfCallHandle(bound = false) Object unbound) throws Throwable {
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            return method.invoke(bound, Collections.emptyList()).toString() + method.invoke(unbound, Collections.singletonList(new SelfCallHandleSample(BAR)));
        }
    }

    public static class SelfCallHandleSubclass extends SelfCallHandleSample {

        private int check;

        public SelfCallHandleSubclass(String foo) {
            super(foo);
        }

        @Override
        public String run() {
            if (check++ != 0) {
                throw new AssertionError();
            }
            return super.run();
        }

        public static String handle(
                @MemberSubstitution.SelfCallHandle Object bound,
                @MemberSubstitution.SelfCallHandle(bound = false) Object unbound) throws Throwable {
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            return method.invoke(bound, Collections.emptyList()).toString() + method.invoke(unbound, Collections.singletonList(new SelfCallHandleSubclass(BAR)));
        }
    }

    public static class FieldValueTest {

        public String foo = FOO, bar = BAR;

        public String run() {
            return bar;
        }

        public String getFoo() {
            return bar;
        }

        public String implicit(@MemberSubstitution.FieldValue(FOO) String value) {
            return value;
        }

        public String accessor(@MemberSubstitution.FieldValue String value) {
            return value;
        }

        public String explicit(@MemberSubstitution.FieldValue(value = FOO, declaringType = FieldValueTest.class) String value) {
            return value;
        }

        public String provided(@Custom String value) {
            return value;
        }
    }

    public static class FieldGetterHandlerTest {

        public String foo = FOO, bar = BAR;

        public String run() {
            return bar;
        }

        public String getFoo() {
            return bar;
        }

        public String implicit(@MemberSubstitution.FieldGetterHandle(FOO) Object handle) throws Throwable {
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            return (String) method.invoke(handle, Collections.emptyList());
        }

        public String accessor(@MemberSubstitution.FieldGetterHandle Object handle) throws Throwable {
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            return (String) method.invoke(handle, Collections.emptyList());
        }

        public String explicit(@MemberSubstitution.FieldGetterHandle(value = FOO, declaringType = FieldGetterHandlerTest.class) Object handle) throws Throwable {
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            return (String) method.invoke(handle, Collections.emptyList());
        }
    }

    public static class FieldSetterHandlerTest {

        public String foo = FOO, bar = BAR;

        public String run() {
            return bar;
        }

        public String getFoo() {
            return bar;
        }

        public String implicit(@MemberSubstitution.FieldSetterHandle(FOO) Object handle) throws Throwable {
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            method.invoke(handle, Collections.singletonList(QUX));
            return BAZ;
        }

        public String accessor(@MemberSubstitution.FieldSetterHandle Object handle) throws Throwable {
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            method.invoke(handle, Collections.singletonList(QUX));
            return BAZ;
        }

        public String explicit(@MemberSubstitution.FieldSetterHandle(value = FOO, declaringType = FieldSetterHandlerTest.class) Object handle) throws Throwable {
            Method method = Class.forName("java.lang.invoke.MethodHandle").getMethod("invokeWithArguments", List.class);
            method.invoke(handle, Collections.singletonList(QUX));
            return BAZ;
        }
    }

    public static class UnusedTest {

        public String foo = FOO;

        public String run() {
            return foo;
        }

        public String unused(@MemberSubstitution.Unused Object value) {
            if (value != null) {
                throw new AssertionError();
            }
            return BAR;
        }
    }

    public static class StubValueTest {

        public String foo = FOO;

        public String run() {
            return foo;
        }

        public String stubbed(@MemberSubstitution.StubValue Object value) {
            if (value != null) {
                throw new AssertionError();
            }
            return BAR;
        }

        public String illegal(@MemberSubstitution.StubValue String value) {
            throw new AssertionError();
        }
    }

    public static class OriginTest {

        public Object foo;

        public Object run() {
            return foo;
        }

        private Object stringElement(@MemberSubstitution.Origin String origin) {
            return origin;
        }

        private Object stringMethod(@MemberSubstitution.Origin(source = MemberSubstitution.Source.ENCLOSING_METHOD) String origin) {
            return origin;
        }

        private Object fieldElement(@MemberSubstitution.Origin Field origin) {
            return origin;
        }

        private Object methodMethod(@MemberSubstitution.Origin(source = MemberSubstitution.Source.ENCLOSING_METHOD) Method origin) {
            return origin;
        }

        private Object typeElement(@MemberSubstitution.Origin Class<?> origin) {
            return origin;
        }

        private Object typeMethod(@MemberSubstitution.Origin(source = MemberSubstitution.Source.ENCLOSING_METHOD) Class<?> origin) {
            return origin;
        }
    }

    public static class CurrentTest {

        public String foo = FOO;

        public String run() {
            return foo;
        }

        public String first(@MemberSubstitution.Current String value) {
            if (value != null) {
                throw new AssertionError();
            }
            return BAR;
        }

        public String second(@MemberSubstitution.Current String value) {
            if (!value.equals(BAR)) {
                throw new AssertionError();
            }
            return QUX;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Custom {
        /* empty */
    }

    @SuppressWarnings("unused")
    public static class SerializableSample {

        private String foo = FOO;

        public String run() {
            return foo;
        }

        private String delegate(@Custom Map<String, String> value) {
            if (value.size() != 1) {
                throw new AssertionError();
            }
            return value.get(FOO);
        }
    }

    public static class DynamicSample {

        private Callable<?> foo;

        public Object run() throws Exception {
            return foo.call();
        }

        private Callable<?> delegate(@Custom Callable<?> callable) {
            return callable;
        }

        private static Object baz() {
            return BAZ;
        }
    }

    public static class BootstrapSample {

        private String foo;

        public String run() {
            return foo;
        }

        private static String delegate() {
            return FOO;
        }
    }
}