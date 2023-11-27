package net.bytebuddy.agent;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodDelegation;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.bytebuddy.dynamic.loading.ClassInjector.UsingInstrumentation.Target.BOOTSTRAP;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.agent.SystemClassInterceptor.ENV_VAR_NAME;
import static net.bytebuddy.agent.SystemClassInterceptor.ENV_VAR_VALUE;
import static org.junit.Assert.assertEquals;

public class SystemClassTest {

    @Test
    public void canRedefineSystemClass() throws Exception {
        final Instrumentation instrumentation = ByteBuddyAgent.install();

        injectBootstrapClasses(instrumentation, SystemClassInterceptor.class);

        final Class<?> targetClass = java.lang.System.class;
        final Method targetMethod = targetClass.getMethod("getenv", String.class);

        final AgentBuilder.Transformer transformer =
                (b, typeDescription, classLoader, javaModule, protectionDomain) ->
                    b.method(is(targetMethod))
                            .intercept(MethodDelegation.to(SystemClassInterceptor.class));

        final ByteBuddy byteBuddy = new ByteBuddy().with(Implementation.Context.Disabled.Factory.INSTANCE);

        final AgentBuilder agentBuilder = new AgentBuilder.Default()
                .with(byteBuddy)
                .ignore(none())
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .type(named(targetClass.getName()))
                .transform(transformer);

        agentBuilder.installOn(instrumentation);

        assertEquals(ENV_VAR_VALUE, System.getenv(ENV_VAR_NAME));
    }

    private static void injectBootstrapClasses(Instrumentation instrumentation, Class<?>... classes) throws IOException {
        File temp = Files.createTempDirectory("tmp").toFile();
        temp.deleteOnExit();

        Map<TypeDescription.ForLoadedType, byte[]> types = Stream.of(classes)
                .collect(Collectors.toMap(TypeDescription.ForLoadedType::new, ClassFileLocator.ForClassLoader::read));

        ClassInjector.UsingInstrumentation.of(temp, BOOTSTRAP, instrumentation)
                .inject(types);
    }
}