import java.lang.classfile.*;
import java.nio.charset.StandardCharsets;

public class SomeCustomAttribute extends CustomAttribute<SomeCustomAttribute> {

    final String value;

    public SomeCustomAttribute(String value) {
        super(new SomeCustomAttributeMapper());
        this.value = value;
    }
}

class SomeCustomAttributeMapper implements AttributeMapper<SomeCustomAttribute> {
    @Override
    public String name() {
        return "StringAttribute";
    }

    // ...

    @Override
    public SomeCustomAttribute readAttribute(AttributedElement enclosing, ClassReader cf, int pos) {
        int length = cf.readInt(pos - 4);
        return new SomeCustomAttribute(new String(cf.readBytes(pos, length), StandardCharsets.UTF_8));
    }

    @Override
    public void writeAttribute(BufWriter buf, SomeCustomAttribute attr) {
        buf.writeIndex(buf.constantPool().utf8Entry("StringAttribute"));
        byte[] bytes = attr.value.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    @Override
    public AttributeStability stability() {
        return AttributeStability.UNKNOWN;
    }
}