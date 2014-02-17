package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.scaffold;

import com.blogspot.mydailyjava.bytebuddy.*;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.AbstractInstrumentedTypeTest;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.util.Arrays;

import static org.mockito.Mockito.*;

public class SubclassLoadedTypeInstrumentationTest extends AbstractInstrumentedTypeTest {

    @Override
    protected InstrumentedType makeInstrumentedType(String name,
                                                    Class<?> superType,
                                                    Class<?>[] interfaces,
                                                    Visibility visibility,
                                                    TypeManifestation typeManifestation,
                                                    SyntheticState syntheticState) {
        NamingStrategy namingStrategy = mock(NamingStrategy.class);
        when(namingStrategy.getName(any(NamingStrategy.UnnamedType.class))).thenReturn(name);
        SubclassLoadedTypeInstrumentation instrumentedType = new SubclassLoadedTypeInstrumentation(new ClassVersion(Opcodes.V1_6),
                Object.class,
                Arrays.<Class<?>>asList(Serializable.class),
                Visibility.PUBLIC,
                TypeManifestation.CONCRETE,
                SyntheticState.NON_SYNTHETIC,
                namingStrategy);
        verify(namingStrategy).getName(instrumentedType);
        verifyNoMoreInteractions(namingStrategy);
        return instrumentedType;
    }
}
