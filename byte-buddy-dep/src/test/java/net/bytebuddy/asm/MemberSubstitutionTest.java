package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.pool.TypePool;
import org.junit.Test;

import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class MemberSubstitutionTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz", RUN = "run";

    @Test
    public void testFieldReadStub() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(FOO)).stub().on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), nullValue(Object.class));
    }

    @Test
    public void testStaticFieldReadStub() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(StaticFieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(FOO)).stub().on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), nullValue(Object.class));
    }

    @Test
    public void testFieldReadWithFieldSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(FOO)).replaceWith(FieldAccessSample.class.getDeclaredField(QUX)).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(instance), is((Object) QUX));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) QUX));
        assertThat(type.getDeclaredField(QUX).get(instance), is((Object) QUX));
    }

    @Test
    public void testStaticFieldReadWithFieldSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(StaticFieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(FOO)).replaceWith(StaticFieldAccessSample.class.getDeclaredField(QUX)).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(instance), is((Object) QUX));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) QUX));
        assertThat(type.getDeclaredField(QUX).get(instance), is((Object) QUX));
    }

    @Test
    public void testFieldReadWithMethodSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(FOO)).replaceWith(FieldAccessSample.class.getDeclaredMethod(BAZ)).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(BAZ).get(instance), is((Object) BAZ));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAZ));
        assertThat(type.getDeclaredField(BAZ).get(instance), is((Object) BAZ));
    }

    @Test
    public void testStaticFieldReadWithMethodSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(StaticFieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(FOO)).replaceWith(StaticFieldAccessSample.class.getDeclaredMethod(BAZ)).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
        assertThat(type.getDeclaredField(BAZ).get(null), is((Object) BAZ));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAZ));
        assertThat(type.getDeclaredField(BAZ).get(null), is((Object) BAZ));
    }

    @Test
    public void testFieldWriteStub() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(BAR)).stub().on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
    }

    @Test
    public void testStaticFieldWriteStub() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(StaticFieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(BAR)).stub().on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
    }

    @Test
    public void testFieldWriteWithFieldSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(BAR)).replaceWith(FieldAccessSample.class.getDeclaredField(QUX)).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(instance), is((Object) QUX));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(instance), is((Object) FOO));
    }

    @Test
    public void testStaticFieldWriteWithFieldSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(StaticFieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(BAR)).replaceWith(StaticFieldAccessSample.class.getDeclaredField(QUX)).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(instance), is((Object) QUX));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(instance), is((Object) FOO));
    }

    @Test
    public void testFieldWriteWithMethodSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(BAR)).replaceWith(FieldAccessSample.class.getDeclaredMethod(BAZ, String.class)).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(BAZ).get(instance), is((Object) BAZ));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(BAZ).get(instance), is((Object) FOO));
    }

    @Test
    public void testStaticFieldWriteWithMethodSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(StaticFieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(BAR)).replaceWith(StaticFieldAccessSample.class.getDeclaredMethod(BAZ, String.class)).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
        assertThat(type.getDeclaredField(BAZ).get(null), is((Object) BAZ));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
        assertThat(type.getDeclaredField(BAZ).get(null), is((Object) FOO));
    }

    @Test
    public void testFieldReadStubWithMatchedConstraint() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(FOO)).onRead().stub().on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), nullValue(Object.class));
    }

    @Test
    public void testFieldReadStubWithNonMatchedConstraint() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(FOO)).onWrite().stub().on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) FOO));
    }

    @Test
    public void testFieldWriteStubWithMatchedConstraint() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(BAR)).onWrite().stub().on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
    }

    @Test
    public void testFieldWriteStubWithNonMatchedConstraint() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(BAR)).onRead().stub().on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) FOO));
    }

    @Test
    public void testMethodReadStub() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(MethodInvokeSample.class)
                .visit(MemberSubstitution.strict().method(named(FOO)).stub().on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), nullValue(Object.class));
    }

    @Test
    public void testStaticMethodReadStub() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(StaticMethodInvokeSample.class)
                .visit(MemberSubstitution.strict().method(named(FOO)).stub().on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), nullValue(Object.class));
    }

    @Test
    public void testMethodReadWithFieldSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(MethodInvokeSample.class)
                .visit(MemberSubstitution.strict().method(named(FOO)).replaceWith(MethodInvokeSample.class.getDeclaredField(QUX)).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(instance), is((Object) QUX));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) QUX));
        assertThat(type.getDeclaredField(QUX).get(instance), is((Object) QUX));
    }

    @Test
    public void testStaticMethodReadWithFieldSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(StaticMethodInvokeSample.class)
                .visit(MemberSubstitution.strict().method(named(FOO)).replaceWith(StaticMethodInvokeSample.class.getDeclaredField(QUX)).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(instance), is((Object) QUX));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) QUX));
        assertThat(type.getDeclaredField(QUX).get(instance), is((Object) QUX));
    }

    @Test
    public void testMethodReadWithMethodSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(MethodInvokeSample.class)
                .visit(MemberSubstitution.strict().method(named(FOO)).replaceWith(MethodInvokeSample.class.getDeclaredMethod(BAZ)).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(BAZ).get(instance), is((Object) BAZ));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAZ));
        assertThat(type.getDeclaredField(BAZ).get(instance), is((Object) BAZ));
    }

    @Test
    public void testStaticMethodReadWithMethodSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(StaticMethodInvokeSample.class)
                .visit(MemberSubstitution.strict().method(named(FOO)).replaceWith(StaticMethodInvokeSample.class.getDeclaredMethod(BAZ)).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
        assertThat(type.getDeclaredField(BAZ).get(null), is((Object) BAZ));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAZ));
        assertThat(type.getDeclaredField(BAZ).get(null), is((Object) BAZ));
    }

    @Test
    public void testMethodWriteStub() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(MethodInvokeSample.class)
                .visit(MemberSubstitution.strict().method(named(BAR)).stub().on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
    }

    @Test
    public void testStaticMethodWriteStub() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(StaticMethodInvokeSample.class)
                .visit(MemberSubstitution.strict().method(named(BAR)).stub().on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
    }

    @Test
    public void testMethodWriteWithFieldSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(MethodInvokeSample.class)
                .visit(MemberSubstitution.strict().method(named(BAR)).replaceWith(MethodInvokeSample.class.getDeclaredField(QUX)).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(instance), is((Object) QUX));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(instance), is((Object) FOO));
    }

    @Test
    public void testStaticMethodWriteWithFieldSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(StaticMethodInvokeSample.class)
                .visit(MemberSubstitution.strict().method(named(BAR)).replaceWith(StaticMethodInvokeSample.class.getDeclaredField(QUX)).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(instance), is((Object) QUX));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
        assertThat(type.getDeclaredField(QUX).get(instance), is((Object) FOO));
    }

    @Test
    public void testMethodWriteWithMethodSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(MethodInvokeSample.class)
                .visit(MemberSubstitution.strict().method(named(BAR)).replaceWith(MethodInvokeSample.class.getDeclaredMethod(BAZ, String.class)).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(BAZ).get(instance), is((Object) BAZ));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(instance), is((Object) BAR));
        assertThat(type.getDeclaredField(BAZ).get(instance), is((Object) FOO));
    }

    @Test
    public void testStaticMethodWriteWithMethodSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(StaticMethodInvokeSample.class)
                .visit(MemberSubstitution.strict().method(named(BAR)).replaceWith(StaticMethodInvokeSample.class.getDeclaredMethod(BAZ, String.class)).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
        assertThat(type.getDeclaredField(BAZ).get(null), is((Object) BAZ));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredField(BAR).get(null), is((Object) BAR));
        assertThat(type.getDeclaredField(BAZ).get(null), is((Object) FOO));
    }

    @Test
    public void testConstructorSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(ConstructorSubstitutionSample.class)
                .visit(MemberSubstitution.strict().constructor(isDeclaredBy(Object.class)).stub().on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
    }

    @Test
    public void testVirtualMethodSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(VirtualMethodSubstitutionSample.class)
                .visit(MemberSubstitution.strict().method(named(FOO)).stub().on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
    }

    @Test
    public void testVirtualMethodVirtualCallSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(VirtualMethodCallSubstitutionSample.Extension.class)
                .visit(MemberSubstitution.strict().method(named(FOO)).onVirtualCall().stub().on(named(RUN)))
                .make()
                .load(new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassFileLocator.ForClassLoader.readToNames(VirtualMethodCallSubstitutionSample.class)),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), is((Object) 1));
    }

    @Test
    public void testVirtualMethodSuperCallSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(VirtualMethodCallSubstitutionSample.Extension.class)
                .visit(MemberSubstitution.strict().method(named(FOO)).onSuperCall().stub().on(named(RUN)))
                .make()
                .load(new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassFileLocator.ForClassLoader.readToNames(VirtualMethodCallSubstitutionSample.class)),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), is((Object) 2));
    }

    @Test
    public void testVirtualMethodSuperCallSubstitutionExternal() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(VirtualMethodCallSubstitutionExternalSample.class)
                .visit(MemberSubstitution.strict().method(named(FOO)).replaceWith(Callable.class.getDeclaredMethod("call")).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(BAR, Callable.class).invoke(instance, new Callable<String>() {
            public String call() {
                return FOO;
            }
        }), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldNotAccessible() throws Exception {
        new ByteBuddy()
                .redefine(StaticFieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(FOO)).replaceWith(ValidationTarget.class.getDeclaredField(FOO)).on(named(RUN)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldReadNotAssignable() throws Exception {
        new ByteBuddy()
                .redefine(StaticFieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(FOO)).replaceWith(ValidationTarget.class.getDeclaredField(BAR)).on(named(RUN)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldWriteNotAssignable() throws Exception {
        new ByteBuddy()
                .redefine(StaticFieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(BAR)).replaceWith(ValidationTarget.class.getDeclaredField(BAR)).on(named(RUN)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldReadNotCompatible() throws Exception {
        new ByteBuddy()
                .redefine(StaticFieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(FOO)).replaceWith(ValidationTarget.class.getDeclaredField(QUX)).on(named(RUN)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldWriteNotCompatible() throws Exception {
        new ByteBuddy()
                .redefine(StaticFieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(BAR)).replaceWith(ValidationTarget.class.getDeclaredField(QUX)).on(named(RUN)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodNotAccessible() throws Exception {
        new ByteBuddy()
                .redefine(StaticFieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(BAR)).replaceWith(ValidationTarget.class.getDeclaredMethod(FOO)).on(named(RUN)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodArgumentsNotAssignable() throws Exception {
        new ByteBuddy()
                .redefine(StaticFieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(FOO)).replaceWith(ValidationTarget.class.getDeclaredMethod(BAR, Void.class)).on(named(RUN)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodReturnNotAssignable() throws Exception {
        new ByteBuddy()
                .redefine(StaticFieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(BAR)).replaceWith(ValidationTarget.class.getDeclaredMethod(BAR)).on(named(RUN)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodNotCompatible() throws Exception {
        new ByteBuddy()
                .redefine(StaticFieldAccessSample.class)
                .visit(MemberSubstitution.strict().field(named(BAR)).replaceWith(ValidationTarget.class.getDeclaredMethod(QUX)).on(named(RUN)))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorReplacement() throws Exception {
        MemberSubstitution.strict().field(any()).replaceWith(new MethodDescription.ForLoadedConstructor(Object.class.getDeclaredConstructor()));
    }

    @Test(expected = IllegalStateException.class)
    public void testOptionalField() throws Exception {
        new ByteBuddy()
                .redefine(OptionalTarget.class)
                .visit(MemberSubstitution.strict().field(named(BAR)).stub().on(named(RUN)))
                .make(TypePool.Empty.INSTANCE);
    }

    @Test(expected = IllegalStateException.class)
    public void testOptionalMethod() throws Exception {
        new ByteBuddy()
                .redefine(OptionalTarget.class)
                .visit(MemberSubstitution.strict().method(named(BAR)).stub().on(named(RUN)))
                .make(TypePool.Empty.INSTANCE);
    }

    @Test
    public void testOptionalFieldRelaxed() throws Exception {
        assertThat(new ByteBuddy()
                .redefine(OptionalTarget.class)
                .visit(MemberSubstitution.relaxed().field(named(BAR)).stub().on(named(RUN)))
                .make(TypePool.Empty.INSTANCE), notNullValue(DynamicType.class));
    }

    @Test
    public void testOptionalMethodRelaxed() throws Exception {
        assertThat(new ByteBuddy()
                .redefine(OptionalTarget.class)
                .visit(MemberSubstitution.relaxed().method(named(BAR)).stub().on(named(RUN)))
                .make(TypePool.Empty.INSTANCE), notNullValue(DynamicType.class));
    }

    public static class FieldAccessSample {

        public String foo = FOO, bar = BAR, qux = QUX, baz = BAZ;

        public void run() {
            bar = foo;
        }

        @SuppressWarnings("unused")
        private String baz() {
            return baz;
        }

        @SuppressWarnings("unused")
        private void baz(String baz) {
            this.baz = baz;
        }
    }

    public static class StaticFieldAccessSample {

        public static String foo = FOO, bar = BAR, qux = QUX, baz = BAZ;

        public void run() {
            bar = foo;
        }

        @SuppressWarnings("unused")
        private static String baz() {
            return baz;
        }

        @SuppressWarnings("unused")
        private static void baz(String baz) {
            StaticFieldAccessSample.baz = baz;
        }
    }

    public static class MethodInvokeSample {

        public String foo = FOO, bar = BAR, qux = QUX, baz = BAZ;

        public void run() {
            bar(foo());
        }

        @SuppressWarnings("unused")
        private String foo() {
            return foo;
        }

        @SuppressWarnings("unused")
        private void bar(String bar) {
            this.bar = bar;
        }

        @SuppressWarnings("unused")
        private String baz() {
            return baz;
        }

        @SuppressWarnings("unused")
        private void baz(String baz) {
            this.baz = baz;
        }
    }

    public static class StaticMethodInvokeSample {

        public static String foo = FOO, bar = BAR, qux = QUX, baz = BAZ;

        public void run() {
            bar(foo());
        }

        @SuppressWarnings("unused")
        private static String foo() {
            return foo;
        }

        @SuppressWarnings("unused")
        private static void bar(String bar) {
            StaticMethodInvokeSample.bar = bar;
        }

        @SuppressWarnings("unused")
        private static String baz() {
            return baz;
        }

        @SuppressWarnings("unused")
        private static void baz(String baz) {
            StaticMethodInvokeSample.baz = baz;
        }
    }

    public static class ConstructorSubstitutionSample {

        public Object run() {
            return new Object();
        }
    }

    public static class VirtualMethodSubstitutionSample {

        public Object run() {
            return foo();
        }

        public Object foo() {
            return FOO;
        }
    }

    public static class VirtualMethodCallSubstitutionSample {

        public int foo() {
            return 1;
        }

        public static class Extension extends VirtualMethodCallSubstitutionSample {

            public int foo() {
                return 2;
            }

            public int run() {
                return foo() + super.foo();
            }
        }
    }

    public static class VirtualMethodCallSubstitutionExternalSample {

        public Object bar(Callable<String> argument) {
            return foo(argument);
        }

        private static Object foo(Callable<String> argument) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class ValidationTarget {

        private static String foo;

        public static Void bar;

        public String qux;

        private static String foo() {
            return null;
        }

        public static Void bar() {
            return null;
        }

        public static void bar(Void bar) {
            /* empty */
        }

        public String qux() {
            return null;
        }
    }

    public static class OptionalTarget {

        public static void run() {
            ValidationTarget.bar = null;
            ValidationTarget.bar();
        }
    }

}