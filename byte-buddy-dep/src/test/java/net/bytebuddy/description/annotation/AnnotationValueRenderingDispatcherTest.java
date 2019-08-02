package net.bytebuddy.description.annotation;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import java.lang.annotation.Annotation;
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
    public void testComponentTag() throws Exception {
        assertThat(AnnotationValue.RenderingDispatcher.CURRENT.toComponentTag(TypeDescription.ForLoadedType.of(boolean.class)), is((int) 'Z'));
        assertThat(AnnotationValue.RenderingDispatcher.CURRENT.toComponentTag(TypeDescription.ForLoadedType.of(byte.class)), is((int) 'B'));
        assertThat(AnnotationValue.RenderingDispatcher.CURRENT.toComponentTag(TypeDescription.ForLoadedType.of(short.class)), is((int) 'S'));
        assertThat(AnnotationValue.RenderingDispatcher.CURRENT.toComponentTag(TypeDescription.ForLoadedType.of(char.class)), is((int) 'C'));
        assertThat(AnnotationValue.RenderingDispatcher.CURRENT.toComponentTag(TypeDescription.ForLoadedType.of(int.class)), is((int) 'I'));
        assertThat(AnnotationValue.RenderingDispatcher.CURRENT.toComponentTag(TypeDescription.ForLoadedType.of(long.class)), is((int) 'J'));
        assertThat(AnnotationValue.RenderingDispatcher.CURRENT.toComponentTag(TypeDescription.ForLoadedType.of(float.class)), is((int) 'F'));
        assertThat(AnnotationValue.RenderingDispatcher.CURRENT.toComponentTag(TypeDescription.ForLoadedType.of(double.class)), is((int) 'D'));
        assertThat(AnnotationValue.RenderingDispatcher.CURRENT.toComponentTag(TypeDescription.ForLoadedType.of(String.class)), is((int) 's'));
        assertThat(AnnotationValue.RenderingDispatcher.CURRENT.toComponentTag(TypeDescription.ForLoadedType.of(SampleEnumeration.class)), is((int) 'e'));
        assertThat(AnnotationValue.RenderingDispatcher.CURRENT.toComponentTag(TypeDescription.ForLoadedType.of(SampleAnnotation.class)), is((int) '@'));
        assertThat(AnnotationValue.RenderingDispatcher.CURRENT.toComponentTag(TypeDescription.ForLoadedType.of(Object[].class)), is((int) '['));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalComponentTag() throws Exception {
        AnnotationValue.RenderingDispatcher.CURRENT.toComponentTag(TypeDescription.ForLoadedType.of(void.class));
    }

    private enum SampleEnumeration {
        SAMPLE
    }

    private @interface SampleAnnotation {
        /* empty */
    }
}
