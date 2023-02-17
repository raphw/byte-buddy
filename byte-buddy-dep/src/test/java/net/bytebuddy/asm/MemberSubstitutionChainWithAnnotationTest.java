package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class MemberSubstitutionChainWithAnnotationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz", RUN = "run";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    public void testMemberSubstitutionArgumentToElement() throws Exception {
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
    public void testMemberSubstitutionArgumentToMethod() throws Exception {
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
    public void testMemberSubstitutionArgumentOptional() throws Exception {
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
    public void testMemberSubstitutionArgumentNone() throws Exception {
        new ByteBuddy()
                .redefine(ArgumentSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(ArgumentSample.class.getMethod("none", String.class)))
                        .on(named(RUN)))
                .make();
    }

    @Test
    public void testMemberSubstitutionThisReferenceToElement() throws Exception {
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
    public void testMemberSubstitutionThisReferenceToMethod() throws Exception {
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
    public void testMemberSubstitutionThisReferenceOptional() throws Exception {
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
    public void testMemberSubstitutionThisReferenceNone() throws Exception {
        new ByteBuddy()
                .redefine(ThisReferenceSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(BAZ))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(ThisReferenceSample.class.getMethod("none", Object.class)))
                        .on(named(RUN)))
                .make();
    }

    @Test
    public void testMemberSubstitutionAllArgumentsToElement() throws Exception {
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
    public void testMemberSubstitutionAllArgumentsToMethod() throws Exception {
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
    public void testMemberSubstitutionAllArgumentsSelf() throws Exception {
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
    public void testMemberSubstitutionAllArgumentsEmpty() throws Exception {
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
    public void testMemberSubstitutionAllArgumentsIllegal() throws Exception {
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
    public void testMemberSubstitutionSelfCallHandle() throws Exception {
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
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) (FOO + BAR)));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, target = SelfCallHandleSample.class)
    public void testMemberSubstitutionSelfCallHandleHierarchy() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(SelfCallHandleSample.class)
                .visit(MemberSubstitution.strict()
                        .field(named(FOO))
                        .replaceWithChain(MemberSubstitution.Substitution.Chain.Step.ForDelegation.to(SelfCallHandleSample.class.getMethod("handle", Object.class, Object.class)))
                        .on(named(RUN)))
                .make()
                .load(new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, Collections.singletonMap(SelfCallHandleSubclass.class.getName(),
                        ClassFileLocator.ForClassLoader.read(SelfCallHandleSubclass.class))), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getClassLoader().loadClass(SelfCallHandleSubclass.class.getName()).getDeclaredConstructor(String.class).newInstance(FOO);
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) (FOO + BAR)));
    }

    public static class ArgumentSample {

        public String foo = FOO, bar = BAR;

        public static String qux = QUX;

        @SuppressWarnings("unused")
        public void run(String value) {
            foo = value;
        }

        public static void element(@MemberSubstitution.Substitution.Chain.Step.ForDelegation.Argument(0) String value) {
            qux = value;
        }

        public static void method(@MemberSubstitution.Substitution.Chain.Step.ForDelegation.Argument(value = 0, source = MemberSubstitution.Substitution.Chain.Step.ForDelegation.Source.ENCLOSING_METHOD) String value) {
            qux = value;
        }

        public static void optional(@MemberSubstitution.Substitution.Chain.Step.ForDelegation.Argument(value = 1, optional = true) String value) {
            qux = value;
        }

        public static void none(@MemberSubstitution.Substitution.Chain.Step.ForDelegation.Argument(value = 1) String value) {
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

        public static void element(@MemberSubstitution.Substitution.Chain.Step.ForDelegation.This Object value) {
            qux = value;
        }

        public static void method(@MemberSubstitution.Substitution.Chain.Step.ForDelegation.This(source = MemberSubstitution.Substitution.Chain.Step.ForDelegation.Source.ENCLOSING_METHOD) Object value) {
            qux = value;
        }

        public static void optional(@MemberSubstitution.Substitution.Chain.Step.ForDelegation.This(optional = true) Object value) {
            qux = value;
        }

        public static void none(@MemberSubstitution.Substitution.Chain.Step.ForDelegation.This Object value) {
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

        public static void element(@MemberSubstitution.Substitution.Chain.Step.ForDelegation.AllArguments String[] value) {
            if (value.length != 1) {
                throw new AssertionError();
            }
            qux = value[0];
        }

        public static String empty(@MemberSubstitution.Substitution.Chain.Step.ForDelegation.AllArguments(nullIfEmpty = true) String[] value) {
            if (value != null) {
                throw new AssertionError();
            }
            return null;
        }

        public static void method(@MemberSubstitution.Substitution.Chain.Step.ForDelegation.AllArguments(source = MemberSubstitution.Substitution.Chain.Step.ForDelegation.Source.ENCLOSING_METHOD) String[] value) {
            if (value.length != 1) {
                throw new AssertionError();
            }
            qux = value[0];
        }

        public static void self(@MemberSubstitution.Substitution.Chain.Step.ForDelegation.AllArguments(includeSelf = true) Object[] value) {
            if (value.length != 2) {
                throw new AssertionError();
            }
            qux = (String) value[1];
            baz = value[0];
        }

        public static void illegal(@MemberSubstitution.Substitution.Chain.Step.ForDelegation.AllArguments Void ignored) {
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
                @MemberSubstitution.Substitution.Chain.Step.ForDelegation.SelfCallHandle Object bound,
                @MemberSubstitution.Substitution.Chain.Step.ForDelegation.SelfCallHandle(bound = false) Object unbound) throws Throwable {
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
    }
}