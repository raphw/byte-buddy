package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ModifierAdjustmentTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testTypeModifier() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new ModifierAdjustment().withTypeModifiers(named(Sample.class.getName()), Visibility.PUBLIC))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC));
    }

    @Test
    public void testTypeModifierNotApplied() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new ModifierAdjustment().withTypeModifiers(named(FOO), Visibility.PUBLIC))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getModifiers(), is(Sample.class.getModifiers()));
    }

    @Test
    public void testTypeModifierUnqualified() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new ModifierAdjustment().withTypeModifiers(Visibility.PUBLIC))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC));
    }

    @Test
    public void testFieldModifier() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new ModifierAdjustment().withFieldModifiers(named(FOO), Visibility.PUBLIC))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredField(FOO).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(type.getDeclaredField(BAR).getModifiers(), is(0));
    }

    @Test
    public void testFieldModifierUnqualified() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new ModifierAdjustment().withFieldModifiers(Visibility.PUBLIC))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredField(FOO).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(type.getDeclaredField(BAR).getModifiers(), is(Opcodes.ACC_PUBLIC));
    }

    @Test
    public void testMethodModifier() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new ModifierAdjustment().withMethodModifiers(named(FOO), Visibility.PUBLIC))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(type.getDeclaredMethod(BAR).getModifiers(), is(0));
    }

    @Test
    public void testMethodModifierUnqualified() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new ModifierAdjustment().withMethodModifiers(Visibility.PUBLIC))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(type.getDeclaredMethod(BAR).getModifiers(), is(Opcodes.ACC_PUBLIC));
    }

    @Test
    public void testConstructorModifier() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new ModifierAdjustment().withConstructorModifiers(takesArgument(0, Void.class), Visibility.PUBLIC))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor(Void.class).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(type.getDeclaredConstructor().getModifiers(), is(0));
    }

    @Test
    public void testConstructorModifierUnqualified() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new ModifierAdjustment().withConstructorModifiers(Visibility.PUBLIC))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor(Void.class).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(type.getDeclaredConstructor().getModifiers(), is(Opcodes.ACC_PUBLIC));
    }

    @Test
    public void testInvokableModifier() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new ModifierAdjustment().withInvokableModifiers(named(FOO).or(takesArgument(0, Void.class)), Visibility.PUBLIC))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(type.getDeclaredMethod(BAR).getModifiers(), is(0));
        assertThat(type.getDeclaredConstructor(Void.class).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(type.getDeclaredConstructor().getModifiers(), is(0));
    }

    @Test
    public void testInvokableModifierUnqualified() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new ModifierAdjustment().withInvokableModifiers(Visibility.PUBLIC))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(type.getDeclaredMethod(BAR).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(type.getDeclaredConstructor(Void.class).getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(type.getDeclaredConstructor().getModifiers(), is(Opcodes.ACC_PUBLIC));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ModifierAdjustment.class).apply();
        ObjectPropertyAssertion.of(ModifierAdjustment.Adjustment.class).apply();
    }

    private static class Sample {

        Void foo;

        Void bar;

        Sample() {
            /* empty */
        }

        Sample(Void ignored) {
            /* empty */
        }

        void foo() {
            /* empty*/
        }

        void bar() {
            /* empty*/
        }
    }
}