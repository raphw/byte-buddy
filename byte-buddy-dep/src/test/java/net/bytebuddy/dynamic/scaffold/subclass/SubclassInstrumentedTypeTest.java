package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.dynamic.scaffold.AbstractInstrumentedTypeTest;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;

public class SubclassInstrumentedTypeTest extends AbstractInstrumentedTypeTest {

    private static final String FOO = "foo", BAR = "bar", FOOBAR = FOO + "." + BAR;

    @Override
    protected InstrumentedType makePlainInstrumentedType() {
        return new SubclassInstrumentedType(
                ClassFileVersion.forCurrentJavaVersion(),
                new TypeDescription.ForLoadedType(Object.class),
                new GenericTypeList.ForLoadedType(Serializable.class),
                Opcodes.ACC_PUBLIC,
                new NamingStrategy.Fixed(FOOBAR));
    }
}
