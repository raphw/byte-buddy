package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold;

import com.blogspot.mydailyjava.bytebuddy.SyntheticState;
import com.blogspot.mydailyjava.bytebuddy.TypeManifestation;
import com.blogspot.mydailyjava.bytebuddy.Visibility;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.AbstractInstrumentedTypeTest;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;

public class SubclassLoadedTypeInstrumentationTest extends AbstractInstrumentedTypeTest {

    @Override
    protected InstrumentedType makeInstrumentedType(String name,
                                                    Class<?> superType,
                                                    Class<?>[] interfaces,
                                                    Visibility visibility,
                                                    TypeManifestation typeManifestation,
                                                    SyntheticState syntheticState) {
//        NamingStrategy namingStrategy = mock(NamingStrategy.class);
//        when(namingStrategy.getName(any(NamingStrategy.UnnamedType.class))).thenReturn(name);
//        SubclassLoadedTypeInstrumentation instrumentedType = new SubclassLoadedTypeInstrumentation(new ClassVersion(Opcodes.V1_6),
//                Object.class,
//                Arrays.<Class<?>>asList(Serializable.class),
//                Visibility.PUBLIC,
//                TypeManifestation.PLAIN,
//                SyntheticState.NON_SYNTHETIC,
//                namingStrategy);
//        verify(namingStrategy).getName(instrumentedType);
//        verifyNoMoreInteractions(namingStrategy);
//        return instrumentedType;
        return null;
    }
}
