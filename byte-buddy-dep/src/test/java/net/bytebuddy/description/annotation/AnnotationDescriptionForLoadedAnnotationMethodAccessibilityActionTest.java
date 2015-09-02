package net.bytebuddy.description.annotation;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import static org.junit.Assert.*;

public class AnnotationDescriptionForLoadedAnnotationMethodAccessibilityActionTest {

    @Test
    public void testObjectProperties() throws Exception {
        final Iterator<Method> methods = Arrays.asList(Object.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(AnnotationDescription.ForLoadedAnnotation.MethodAccessibilityAction.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return methods.next();
            }
        }).apply();
    }
}
