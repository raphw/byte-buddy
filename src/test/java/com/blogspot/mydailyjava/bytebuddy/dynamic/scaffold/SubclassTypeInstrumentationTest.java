package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold;

import com.blogspot.mydailyjava.bytebuddy.modifier.SyntheticState;
import com.blogspot.mydailyjava.bytebuddy.modifier.TypeManifestation;
import com.blogspot.mydailyjava.bytebuddy.modifier.Visibility;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.AbstractInstrumentedTypeTest;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;

public class SubclassTypeInstrumentationTest extends AbstractInstrumentedTypeTest {

    @Override
    protected InstrumentedType makeInstrumentedType(String name,
                                                    Class<?> superType,
                                                    Class<?>[] interfaces,
                                                    Visibility visibility,
                                                    TypeManifestation typeManifestation,
                                                    SyntheticState syntheticState) {
//        NamingStrategy namingStrategy = mock(NamingStrategy.class);
//        when(namingStrategy.getName(any(NamingStrategy.UnnamedType.class))).thenReturn(internalName);
//        SubclassTypeInstrumentation instrumentedType = new SubclassTypeInstrumentation(new ClassVersion(Opcodes.V1_6),
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
