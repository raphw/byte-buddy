package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;

public interface LatentMethodMatcher {

    ElementMatcher<? super MethodDescription> manifest(TypeDescription typeDescription);

    class Simple implements LatentMethodMatcher {

        private final ElementMatcher<? super MethodDescription> methodMatcher;

        public Simple(ElementMatcher<? super MethodDescription> methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public ElementMatcher<? super MethodDescription> manifest(TypeDescription typeDescription) {
            return methodMatcher;
        }
    }
}
