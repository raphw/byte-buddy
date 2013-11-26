package com.blogspot.mydailyjava.bytebuddy.interceptor;

import com.blogspot.mydailyjava.bytebuddy.util.InstrumentationClassLoader;
import org.junit.Before;
import org.junit.BeforeClass;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

public class AbstractInterceptorTest {

    protected static final int FLAGS = 0;

    protected static final int INT = 0;
    protected static final float FLOAT = 0f;
    protected static final double DOUBLE = 0d;
    protected static final Object[] ARRAY = new Object[0];
    protected static final Object OBJECT = new Object();

    protected static Objenesis OBJENESIS;

    @BeforeClass
    public static void setUpObjenesis() throws Exception {
        OBJENESIS = new ObjenesisStd();
    }

    protected InstrumentationClassLoader classLoader;

    @Before
    public void setUpClassLoader() throws Exception {
        classLoader = new InstrumentationClassLoader();
    }
}
