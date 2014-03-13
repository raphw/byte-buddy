package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.mockito.Mock;

public class InstrumentationContextDefaultTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Instrumentation.Context.Default.AuxiliaryTypeNamingStrategy auxiliaryTypeNamingStrategy;
    @Mock
    private AuxiliaryType.MethodAccessorFactory methodAccessorFactory;
    @Mock
    private AuxiliaryType firstAuxiliary, secondAuxiliary;
    @Mock
    private DynamicType firstDynamic, secondDynamic;
    @Mock
    private MethodDescription firstMethod, firstProxyMethod, secondMethod, secondProxyMethod;
    @Mock
    private ClassFormatVersion classFormatVersion;

    private Instrumentation.Context.Default defaultContext;

    // TODO: Restore

//    @Before
//    @SuppressWarnings("unchecked")
//    public void setUp() throws Exception {
//        defaultContext = new Instrumentation.Context.Default(classFormatVersion, auxiliaryTypeNamingStrategy, methodAccessorFactory);
//        when(firstAuxiliary.make(any(String.class), any(ClassFormatVersion.class), any(AuxiliaryType.MethodAccessorFactory.class)))
//                .thenReturn((DynamicType) firstDynamic);
//        when(secondAuxiliary.make(any(String.class), any(ClassFormatVersion.class), any(AuxiliaryType.MethodAccessorFactory.class)))
//                .thenReturn((DynamicType) secondDynamic);
//        when(firstDynamic.getDescription()).thenReturn(FOO);
//        when(secondDynamic.getDescription()).thenReturn(BAR);
//        when(auxiliaryTypeNamingStrategy.name(any(AuxiliaryType.class))).thenReturn(QUX, BAZ);
//        when(methodAccessorFactory.requireAccessorMethodFor(firstMethod)).thenReturn(firstProxyMethod);
//        when(methodAccessorFactory.requireAccessorMethodFor(secondMethod)).thenReturn(secondProxyMethod);
//    }
//
//    @Test
//    @SuppressWarnings("unchecked")
//    public void testSingleRegistration() throws Exception {
//        assertThat(defaultContext.register(firstAuxiliary), is(FOO));
//        assertThat(defaultContext.register(secondAuxiliary), is(BAR));
//        assertThat(defaultContext.getRegisteredAuxiliaryTypes().size(), is(2));
//        assertThat(defaultContext.getRegisteredAuxiliaryTypes(), hasItems(firstDynamic, secondDynamic));
//        verify(firstAuxiliary).make(QUX, classFormatVersion, defaultContext);
//        verify(secondAuxiliary).make(BAZ, classFormatVersion, defaultContext);
//        verifyNoMoreInteractions(firstAuxiliary);
//        verifyNoMoreInteractions(secondAuxiliary);
//        verify(auxiliaryTypeNamingStrategy).name(firstAuxiliary);
//        verify(auxiliaryTypeNamingStrategy).name(secondAuxiliary);
//        verifyNoMoreInteractions(auxiliaryTypeNamingStrategy);
//        verifyZeroInteractions(methodAccessorFactory);
//        verifyZeroInteractions(classFormatVersion);
//    }
//
//    @Test
//    public void testDoubleRegistration() throws Exception {
//        assertThat(defaultContext.register(firstAuxiliary), is(FOO));
//        assertThat(defaultContext.register(firstAuxiliary), is(FOO));
//        assertThat(defaultContext.getRegisteredAuxiliaryTypes().size(), is(1));
//        assertThat(defaultContext.getRegisteredAuxiliaryTypes(), hasItem(firstDynamic));
//        verify(firstAuxiliary).make(QUX, classFormatVersion, defaultContext);
//        verifyNoMoreInteractions(firstAuxiliary);
//        verify(auxiliaryTypeNamingStrategy).name(firstAuxiliary);
//        verifyNoMoreInteractions(auxiliaryTypeNamingStrategy);
//        verifyZeroInteractions(methodAccessorFactory);
//        verifyZeroInteractions(classFormatVersion);
//    }
//
//    @Test
//    public void testMethodProxyFactory() throws Exception {
//        assertThat(defaultContext.requireAccessorMethodFor(firstMethod), is(firstProxyMethod));
//        assertThat(defaultContext.requireAccessorMethodFor(secondMethod), is(secondProxyMethod));
//        assertThat(defaultContext.requireAccessorMethodFor(firstMethod), is(firstProxyMethod));
//        verify(methodAccessorFactory).requireAccessorMethodFor(firstMethod);
//        verify(methodAccessorFactory).requireAccessorMethodFor(secondMethod);
//        verifyNoMoreInteractions(methodAccessorFactory);
//        verifyZeroInteractions(auxiliaryTypeNamingStrategy);
//        verifyZeroInteractions(classFormatVersion);
//    }
}
