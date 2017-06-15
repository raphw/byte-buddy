package net.bytebuddy;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.JavaVersionRule;

public class ClassLoadedDuringRetransformationTest {

  public static final String BARADVICE_SUFFIX = "+baradvice";
  public static final String FOOADVICE_SUFFIX = "+fooadvice";

  @Rule
  public MethodRule agentAttachmentRule = new AgentAttachmentRule();

  @Rule
  public MethodRule javaVersionRule = new JavaVersionRule();

  private ClassLoader classLoader;

  @Before
  public void setUp() throws Exception {
    classLoader = new ByteArrayClassLoader.ChildFirst(
        getClass().getClassLoader(),
        ClassFileExtraction.of(Foo.class, Bar.class),
        ByteArrayClassLoader.PersistenceHandler.MANIFEST);
  }

  @Test
  @AgentAttachmentRule.Enforce(retransformsClasses = true)
  public void testAdvice_withoutLoadedClasses() throws Exception {
    assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
    ClassFileTransformer classFileTransformer = installInstrumentation();
    try {
      verifyAdvices();
    } finally {
      ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
    }
  }

  @Test
  @AgentAttachmentRule.Enforce(retransformsClasses = true)
  public void testAdvice_withOneLoadedClass() throws Exception {
    assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
    classLoader.loadClass(Foo.class.getName());
    ClassFileTransformer classFileTransformer = installInstrumentation();
    try {
      verifyAdvices();
    } finally {
      ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
    }
  }

  @Test
  @AgentAttachmentRule.Enforce(retransformsClasses = true)
  public void testAdvice_withTwoLoadedClasses() throws Exception {
    assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
    classLoader.loadClass(Foo.class.getName());
    classLoader.loadClass(Bar.class.getName());
    ClassFileTransformer classFileTransformer = installInstrumentation();
    try {
      verifyAdvices();
    } finally {
      ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
    }
  }

  private void verifyAdvices() throws ClassNotFoundException, InstantiationException, IllegalAccessException,
      InvocationTargetException, NoSuchMethodException {
    Object obj = createInstance(classLoader, Foo.class.getName());
    Object bar = invokeMethod(classLoader, Foo.class.getName(), "createBar", obj);
    assertThat(bar.toString(), is((Object) ("x" + FOOADVICE_SUFFIX + BARADVICE_SUFFIX)));
  }

  private ClassFileTransformer installInstrumentation() {
    return new AgentBuilder.Default()
        .disableClassFormatChanges()
        .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
        .with(AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE)
        .ignore(none())
        .type(named(Foo.class.getName()), ElementMatchers.is(classLoader))
        .transform(new AgentBuilder.Transformer.ForAdvice()
            .with(AgentBuilder.LocationStrategy.ForClassLoader.STRONG)
            .include(FooAdvice.class.getClassLoader())
            .with(Assigner.DEFAULT)
            .withExceptionHandler(Removal.SINGLE)
            .advice(named("createBar"), FooAdvice.class.getName()))
        .asDecorator()
        .type(ElementMatchers.named(Bar.class.getName()), ElementMatchers.is(classLoader))
        .transform(new AgentBuilder.Transformer.ForAdvice()
            .with(AgentBuilder.LocationStrategy.ForClassLoader.STRONG)
            .include(BarAdvice.class.getClassLoader())
            .with(Assigner.DEFAULT)
            .withExceptionHandler(Removal.SINGLE)
            .advice(named("toString"), BarAdvice.class.getName()))
        .asDecorator()
        .installOnByteBuddyAgent();
  }

  public static Object invokeMethod(ClassLoader classLoader, String className, String methodName, Object instance)
      throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Class<?> type = classLoader.loadClass(className);
    return type.getDeclaredMethod(methodName).invoke(instance);
  }

  public static Object createInstance(ClassLoader classLoader, String className)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException,
      java.lang.reflect.InvocationTargetException, NoSuchMethodException {
    Class<?> type = classLoader.loadClass(className);
    return type.getDeclaredConstructor().newInstance();
  }

  public static class Foo {

    public Bar createBar() throws Exception {
      return (Bar) createInstance(this.getClass().getClassLoader(), Bar.class.getName());
    }

  }

  public static class Bar {

    private String x = "x";

    public void append(String x) {
      this.x += x;
    }

    @Override
    public String toString() {
      return x;
    }
  }

  private static class FooAdvice {

    @Advice.OnMethodExit
    private static void exit(@Advice.Return Bar value) {
      value.append(FOOADVICE_SUFFIX);
    }
  }

  private static class BarAdvice {

    @Advice.OnMethodExit
    private static void exit(@Advice.Return(readOnly = false) String value) {
      value += BARADVICE_SUFFIX;
    }
  }
}
