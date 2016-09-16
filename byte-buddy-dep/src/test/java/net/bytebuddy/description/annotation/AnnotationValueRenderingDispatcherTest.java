package net.bytebuddy.description.annotation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AnnotationValueRenderingDispatcherTest {

    @Test
    public void testBoolean() throws Exception {
        assertThat(AnnotationValue.RenderingDispatcher.LEGACY_VM.toSourceString(true), is("true"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString(true), is("true"));
    }

    @Test
    public void testByte() throws Exception {
        assertThat(AnnotationValue.RenderingDispatcher.LEGACY_VM.toSourceString((byte) 42), is("42"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString((byte) 42), is("42"));
    }

    @Test
    public void testShort() throws Exception {
        assertThat(AnnotationValue.RenderingDispatcher.LEGACY_VM.toSourceString((short) 42), is("42"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString((short) 42), is("42"));
    }

    @Test
    public void testCharacter() throws Exception {
        assertThat(AnnotationValue.RenderingDispatcher.LEGACY_VM.toSourceString('*'), is("*"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString('*'), is("'*'"));
        assertThat(AnnotationValue.RenderingDispatcher.LEGACY_VM.toSourceString('\''), is("'"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString('\''), is("'\\''"));
    }

    @Test
    public void testInteger() throws Exception {
        assertThat(AnnotationValue.RenderingDispatcher.LEGACY_VM.toSourceString(42), is("42"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString(42), is("42"));
    }

    @Test
    public void testLong() throws Exception {
        assertThat(AnnotationValue.RenderingDispatcher.LEGACY_VM.toSourceString(42L), is("42"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString(42L), is("42"));
        assertThat(AnnotationValue.RenderingDispatcher.LEGACY_VM.toSourceString(1L + Integer.MAX_VALUE), is(Long.toString(Integer.MAX_VALUE + 1L)));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString(1L + Integer.MAX_VALUE), is(Long.toString(Integer.MAX_VALUE + 1L) + "L"));
    }

    @Test
    public void testFloat() throws Exception {
        assertThat(AnnotationValue.RenderingDispatcher.LEGACY_VM.toSourceString(42f), is("42.0"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString(42f), is("42.0f"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString(Float.POSITIVE_INFINITY), is("1.0f/0.0f"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString(Float.NEGATIVE_INFINITY), is("-1.0f/0.0f"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString(0f / 0f), is("0.0f/0.0f"));
    }

    @Test
    public void testDouble() throws Exception {
        assertThat(AnnotationValue.RenderingDispatcher.LEGACY_VM.toSourceString(42d), is("42.0"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString(42d), is("42.0"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString(Double.POSITIVE_INFINITY), is("1.0/0.0"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString(Double.NEGATIVE_INFINITY), is("-1.0/0.0"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString(0d / 0d), is("0.0/0.0"));
    }

    @Test
    public void testString() throws Exception {
        assertThat(AnnotationValue.RenderingDispatcher.LEGACY_VM.toSourceString("foo"), is("foo"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString("foo"), is("\"foo\""));
        assertThat(AnnotationValue.RenderingDispatcher.LEGACY_VM.toSourceString("\"foo\""), is("\"foo\""));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString("\"foo\""), is("\"\\\"foo\\\"\""));
    }

    @Test
    public void testTypeDescription() throws Exception {
        assertThat(AnnotationValue.RenderingDispatcher.LEGACY_VM.toSourceString(TypeDescription.OBJECT), is("class java.lang.Object"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString(TypeDescription.OBJECT), is("java.lang.Object.class"));
    }

    @Test
    public void testArray() throws Exception {
        assertThat(AnnotationValue.RenderingDispatcher.LEGACY_VM.toSourceString(Arrays.asList("foo", "bar")), is("[foo, bar]"));
        assertThat(AnnotationValue.RenderingDispatcher.JAVA_9_CAPABLE_VM.toSourceString(Arrays.asList("foo", "bar")), is("{foo, bar}"));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AnnotationValue.RenderingDispatcher.class).apply();
    }
}
