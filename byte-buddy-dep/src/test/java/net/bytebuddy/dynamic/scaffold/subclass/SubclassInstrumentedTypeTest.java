package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.instrumentation.type.AbstractInstrumentedTypeTest;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.util.Collections;

public class SubclassInstrumentedTypeTest extends AbstractInstrumentedTypeTest {

    private static final String FOO = "foo", BAR = "bar", FOOBAR = FOO + "." + BAR;

    @Override
    protected InstrumentedType makePlainInstrumentedType() {
        return new SubclassInstrumentedType(
                ClassFileVersion.forCurrentJavaVersion(),
                new TypeDescription.ForLoadedType(Object.class),
                new TypeList.ForLoadedType(Collections.<Class<?>>singletonList(Serializable.class)),
                Opcodes.ACC_PUBLIC,
                new NamingStrategy.Fixed(FOOBAR));
    }
}
