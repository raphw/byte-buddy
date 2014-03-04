package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute;

import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.rules.TestRule;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class AbstractAttributeAppenderTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    protected static @interface Qux {

        static class Instance implements Qux {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Qux.class;
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    protected static @interface Baz {

        static class Instance implements Baz {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Baz.class;
            }
        }
    }

    @Retention(RetentionPolicy.CLASS)
    protected static @interface QuxBaz {

        static class Instance implements QuxBaz {
            @Override
            public Class<? extends Annotation> annotationType() {
                return QuxBaz.class;
            }
        }
    }
}
