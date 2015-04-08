package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.matcher.LatentMethodMatcher;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class InlineInstrumentationMatcher implements LatentMethodMatcher {

    private final ElementMatcher<? super MethodDescription> ignoredMethods;

    private final ElementMatcher<MethodDescription> predefinedMethodSignatures;

    protected static LatentMethodMatcher of(ElementMatcher<? super MethodDescription> ignoredMethods, TypeDescription targetType) {
        ElementMatcher.Junction<MethodDescription> predefinedMethodSignatures = none();
        for (MethodDescription methodDescription : targetType.getDeclaredMethods()) {
            ElementMatcher.Junction<MethodDescription> signature = methodDescription.isConstructor()
                    ? isConstructor()
                    : ElementMatchers.<MethodDescription>named(methodDescription.getName());
            signature = signature.and(returns(methodDescription.getReturnType()));
            signature = signature.and(takesArguments(methodDescription.getParameters().asTypeList()));
            predefinedMethodSignatures = predefinedMethodSignatures.or(signature);
        }
        return new InlineInstrumentationMatcher(ignoredMethods, predefinedMethodSignatures);
    }

    protected InlineInstrumentationMatcher(ElementMatcher<? super MethodDescription> ignoredMethods,
                                           ElementMatcher<MethodDescription> predefinedMethodSignatures) {
        this.ignoredMethods = ignoredMethods;
        this.predefinedMethodSignatures = predefinedMethodSignatures;
    }

    @Override
    public ElementMatcher<? super MethodDescription> manifest(TypeDescription typeDescription) {
        return not(ignoredMethods).and(isOverridable().or(isDeclaredBy(typeDescription))
                .or(isDeclaredBy(typeDescription).and(not(predefinedMethodSignatures))));
    }
}
