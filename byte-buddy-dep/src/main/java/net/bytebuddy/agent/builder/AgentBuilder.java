package net.bytebuddy.agent.builder;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

public interface AgentBuilder {

    AgentBuilder with(ExceptionRegistrant exceptionRegistrant);

    AgentBuilder type(ElementMatcher<? super TypeDescription> matcher);

    ClassFileTransformer registerWith(Instrumentation instrumentation);

    ClassFileTransformer registerWithByteBuddyAgent();

    static interface ExceptionRegistrant {

        void register(Throwable throwable);
    }
}
