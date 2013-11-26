package com.blogspot.mydailyjava.bytebuddy.score;

import com.blogspot.mydailyjava.bytebuddy.method.score.AnnotationMethodScorer;
import com.blogspot.mydailyjava.bytebuddy.method.score.MethodScorer;
import com.blogspot.mydailyjava.bytebuddy.method.score.NoScoreException;
import com.blogspot.mydailyjava.bytebuddy.method.stack.AllArgumentsReference;
import com.blogspot.mydailyjava.bytebuddy.method.stack.ClassIdReference;
import com.blogspot.mydailyjava.bytebuddy.method.stack.InstanceIdReference;
import com.blogspot.mydailyjava.bytebuddy.method.stack.ThisReference;
import com.blogspot.mydailyjava.bytebuddy.sample.method.statics.*;
import com.blogspot.mydailyjava.bytebuddy.sample.method.statics.annotated.P0_void_AllArguments;
import com.blogspot.mydailyjava.bytebuddy.sample.method.statics.annotated.P0_void_ClassId;
import com.blogspot.mydailyjava.bytebuddy.sample.method.statics.annotated.P0_void_InstanceId;
import com.blogspot.mydailyjava.bytebuddy.sample.method.statics.annotated.P0_void_ThisReference;
import com.blogspot.mydailyjava.bytebuddy.type.ClassLoadingCompatibilityEvaluator;
import com.blogspot.mydailyjava.bytebuddy.type.TypeCompatibilityEvaluator;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class AnnotationMethodScorerTest {

    private static final double DELTA = .000001d;
    private static final String METHOD_NAME = "invoke";

    private TypeCompatibilityEvaluator typeCompatibilityEvaluator;
    private MethodScorer methodScorer;

    @Before
    public void setUp() throws Exception {
        typeCompatibilityEvaluator = new ClassLoadingCompatibilityEvaluator(getClass().getClassLoader());
        String sourceType = Type.getDescriptor(Object.class);
        String sourceMethodDesc = Type.getMethodDescriptor(Type.VOID_TYPE);
        methodScorer = new AnnotationMethodScorer(sourceType, METHOD_NAME, sourceMethodDesc, new String[0], typeCompatibilityEvaluator);
    }

    @Test
    public void test_P0_void() throws Exception {
        Method targetMethod = P0_void.class.getDeclaredMethod(METHOD_NAME);
        MethodScorer.MatchedMethod matchedMethod = methodScorer.evaluate(targetMethod);
        assertEquals(1d, matchedMethod.getScore(), DELTA);
        assertEquals(0, matchedMethod.getValues().size());
    }

    @Test(expected = NoScoreException.class)
    public void test_P0_Object() throws Exception {
        Method targetMethod = P0_Object.class.getDeclaredMethod(METHOD_NAME);
        methodScorer.evaluate(targetMethod);
    }

    @Test(expected = NoScoreException.class)
    public void test_P0_Array() throws Exception {
        Method targetMethod = P0_Array.class.getDeclaredMethod(METHOD_NAME);
        methodScorer.evaluate(targetMethod);
    }

    @Test(expected = NoScoreException.class)
    public void test_P0_int() throws Exception {
        Method targetMethod = P0_int.class.getDeclaredMethod(METHOD_NAME);
        methodScorer.evaluate(targetMethod);
    }

    @Test(expected = NoScoreException.class)
    public void test_P0_float() throws Exception {
        Method targetMethod = P0_float.class.getDeclaredMethod(METHOD_NAME);
        methodScorer.evaluate(targetMethod);
    }

    @Test(expected = NoScoreException.class)
    public void test_P0_double() throws Exception {
        Method targetMethod = P0_double.class.getDeclaredMethod(METHOD_NAME);
        methodScorer.evaluate(targetMethod);
    }

    @Test
    public void test_P0_void_InstanceId() throws Exception {
        Method targetMethod = P0_void_InstanceId.class.getDeclaredMethod(METHOD_NAME, UUID.class);
        MethodScorer.MatchedMethod matchedMethod = methodScorer.evaluate(targetMethod);
        assertEquals(3d, matchedMethod.getScore(), DELTA);
        assertEquals(1, matchedMethod.getValues().size());
        assertEquals(InstanceIdReference.class, matchedMethod.getValues().get(0).getClass());
    }

    @Test
    public void test_P0_void_ClassId() throws Exception {
        Method targetMethod = P0_void_ClassId.class.getDeclaredMethod(METHOD_NAME, UUID.class);
        MethodScorer.MatchedMethod matchedMethod = methodScorer.evaluate(targetMethod);
        assertEquals(3d, matchedMethod.getScore(), DELTA);
        assertEquals(1, matchedMethod.getValues().size());
        assertEquals(ClassIdReference.class, matchedMethod.getValues().get(0).getClass());
    }

    @Test
    public void test_P0_void_AllArguments() throws Exception {
        Method targetMethod = P0_void_AllArguments.class.getDeclaredMethod(METHOD_NAME, Object[].class);
        MethodScorer.MatchedMethod matchedMethod = methodScorer.evaluate(targetMethod);
        assertEquals(3d, matchedMethod.getScore(), DELTA);
        assertEquals(1, matchedMethod.getValues().size());
        assertEquals(AllArgumentsReference.class, matchedMethod.getValues().get(0).getClass());
    }

    @Test
    public void test_P0_void_ThisReference() throws Exception {
        Method targetMethod = P0_void_ThisReference.class.getDeclaredMethod(METHOD_NAME, Object.class);
        MethodScorer.MatchedMethod matchedMethod = methodScorer.evaluate(targetMethod);
        assertEquals(3d, matchedMethod.getScore(), DELTA);
        assertEquals(1, matchedMethod.getValues().size());
        assertEquals(ThisReference.class, matchedMethod.getValues().get(0).getClass());
    }
}
