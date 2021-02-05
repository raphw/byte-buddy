package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.matcher.LatentMatcher;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A latent method matcher that identifies methods to instrument when redefining or rebasing a type. 一种潜在的方法匹配器，在重新定义或重定类型时标识要插桩方法
 */
@HashCodeAndEqualsPlugin.Enhance
public class InliningImplementationMatcher implements LatentMatcher<MethodDescription> {

    /**
     * A method matcher that matches any ignored method.
     */
    private final LatentMatcher<? super MethodDescription> ignoredMethods;

    /**
     * A method matcher that matches any predefined method.
     */
    private final ElementMatcher<? super MethodDescription> predefinedMethodSignatures;

    /**
     * Creates a new inline implementation matcher.
     *
     * @param ignoredMethods             A method matcher that matches any ignored method.
     * @param predefinedMethodSignatures A method matcher that matches any predefined method.
     */
    protected InliningImplementationMatcher(LatentMatcher<? super MethodDescription> ignoredMethods,
                                            ElementMatcher<? super MethodDescription> predefinedMethodSignatures) {
        this.ignoredMethods = ignoredMethods;
        this.predefinedMethodSignatures = predefinedMethodSignatures;
    }

    /**
     * Creates a matcher where only overridable or declared methods are matched unless those are ignored. Methods that
     * are declared by the target type are only matched if they are not ignored. Declared methods that are not found on the
     * target type are always matched. 创建一个匹配器，其中只匹配可重写或声明的方法，除非这些方法被忽略。由目标类型声明的方法只有在不被忽略时才匹配。在目标类型上找不到的声明方法总是匹配的
     *
     * @param ignoredMethods A method matcher that matches any ignored method.
     * @param originalType   The original type of the instrumentation before adding any user methods.
     * @return A latent method matcher that identifies any method to instrument for a rebasement or redefinition.
     */
    protected static LatentMatcher<MethodDescription> of(LatentMatcher<? super MethodDescription> ignoredMethods, TypeDescription originalType) {
        ElementMatcher.Junction<MethodDescription> predefinedMethodSignatures = none();
        for (MethodDescription methodDescription : originalType.getDeclaredMethods()) {
            ElementMatcher.Junction<MethodDescription> signature = methodDescription.isConstructor()
                    ? isConstructor()
                    : ElementMatchers.<MethodDescription>named(methodDescription.getName());
            signature = signature.and(returns(methodDescription.getReturnType().asErasure()));
            signature = signature.and(takesArguments(methodDescription.getParameters().asTypeList().asErasures()));
            predefinedMethodSignatures = predefinedMethodSignatures.or(signature);
        }
        return new InliningImplementationMatcher(ignoredMethods, predefinedMethodSignatures);
    }

    @Override
    public ElementMatcher<? super MethodDescription> resolve(TypeDescription typeDescription) {
        return (ElementMatcher<? super MethodDescription>) not(ignoredMethods.resolve(typeDescription))
                .and(isVirtual().and(not(isFinal())).or(isDeclaredBy(typeDescription)))
                .or(isDeclaredBy(typeDescription).and(not(predefinedMethodSignatures)));
    }
}
