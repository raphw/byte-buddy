package com.blogspot.mydailyjava.bytebuddy.utility;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodSignatureToken {

    public static Map<MethodSignatureToken, MethodDescription> describe(List<? extends MethodDescription> methodDescriptions) {
        Map<MethodSignatureToken, MethodDescription> descriptions = new HashMap<MethodSignatureToken, MethodDescription>(methodDescriptions.size());
        for(MethodDescription methodDescription : methodDescriptions) {
            descriptions.put(new MethodSignatureToken(methodDescription), methodDescription);
        }
        return descriptions;
    }

    private final MethodDescription methodDescription;

    public MethodSignatureToken(MethodDescription methodDescription) {
        this.methodDescription = methodDescription;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        MethodSignatureToken that = (MethodSignatureToken) other;
        return methodDescription.getInternalName().equals(that.methodDescription.getInternalName())
                && methodDescription.getReturnType().equals(that.methodDescription.getReturnType())
                && methodDescription.getParameterTypes().equals(that.methodDescription.getParameterTypes());
    }

    @Override
    public int hashCode() {
        int result = methodDescription.getInternalName().hashCode();
        result = 31 * result + methodDescription.getReturnType().hashCode();
        result = 31 * result + methodDescription.getParameterTypes().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SignatureToken{methodDescription=" + methodDescription + '}';
    }
}
