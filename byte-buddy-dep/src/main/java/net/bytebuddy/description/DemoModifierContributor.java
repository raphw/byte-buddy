package net.bytebuddy.description;

import net.bytebuddy.description.modifier.ModifierContributor;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.List;

public class DemoModifierContributor implements ModifierContributor {

    public static String demo = "hello;";

    @Override
    public int getMask() {
        // 仅有前四位有效的mask 即 0000000000001010
        int MASK = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC;
        return MASK;
    }

    @Override
    public int getRange() {
        // 屏蔽调 public
        return Opcodes.ACC_PUBLIC;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    public static void main(String[] args) throws Exception {
        // 打印demo 的modifier
        int modifier = DemoModifierContributor.class.getDeclaredField("demo").getModifiers();
        System.out.println("origin modifier of demo : "+ Integer.toBinaryString(modifier));
        // mask
        DemoModifierContributor demo = new DemoModifierContributor();
        System.out.println("mask  : "+ Integer.toBinaryString(demo.getMask()));
        // range
        System.out.println("range : "+ Integer.toBinaryString(demo.getRange()));
        // resolver 用来获取 有效的 modifier
        List<DemoModifierContributor> list = Collections.singletonList(new DemoModifierContributor());
        DemoModifierContributor.Resolver resolver = DemoModifierContributor.Resolver.of(list);

        // 获取 (modifiers & ~modifierContributor.getRange()) | modifierContributor.getMask();
        // (1001 & ~0001)|1100  --> (1001 & 1110)|1010 --> 1000|1010 --> 1010
        System.out.println( Integer.toBinaryString(resolver.resolve(modifier)));

    }
}
