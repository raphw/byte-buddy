/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.matcher.LatentMatcher;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A latent method matcher that identifies methods to instrument when redefining or rebasing a type.
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
     * target type are always matched.
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

    /**
     * {@inheritDoc}
     */
    public ElementMatcher<? super MethodDescription> resolve(TypeDescription typeDescription) {
        return (ElementMatcher<? super MethodDescription>) not(ignoredMethods.resolve(typeDescription))
                .and(isVirtual().and(not(isFinal())).or(isDeclaredBy(typeDescription)))
                .or(isDeclaredBy(typeDescription).and(not(predefinedMethodSignatures)));
    }
}
