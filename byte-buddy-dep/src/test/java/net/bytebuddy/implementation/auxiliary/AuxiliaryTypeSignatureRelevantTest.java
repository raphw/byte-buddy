package net.bytebuddy.implementation.auxiliary;

import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AuxiliaryTypeSignatureRelevantTest {

    @Test
    public void testClassRetention() throws Exception {
        assertThat(AuxiliaryType.SignatureRelevant.class.getAnnotation(Retention.class).value(), is(RetentionPolicy.CLASS));
    }

    @Test
    public void testTypeTarget() throws Exception {
        assertThat(AuxiliaryType.SignatureRelevant.class.getAnnotation(Target.class).value(), is(new ElementType[]{ElementType.TYPE}));
    }

    @Test
    public void testModifiers() throws Exception {
        assertThat(Modifier.isPublic(AuxiliaryType.SignatureRelevant.class.getModifiers()), is(true));
    }
}
