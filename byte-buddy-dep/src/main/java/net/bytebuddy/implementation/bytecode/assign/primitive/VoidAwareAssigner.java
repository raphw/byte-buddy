package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.DefaultValue;

/**
 * This assigner is able to handle the {@code void} type. This means: 该赋值器可以处理 {@code void} 类型，这也就意味着
 * <ol>
 * <li>If a {@code void} type is assigned to the {@code void} it will consider this a trivial operation.</li> 如果  {@code void} 赋值给  {@code void}，这就意味着这是个无关紧要的操作
 * <li>If a {@code void} type is assigned to a non-{@code void} type, it will pop the top value from the stack.</li> 如果一个{@code void}类型被分配给一个非{@code void}类型，它将从堆栈中弹出顶部值
 * <li>If a non-{@code void} type is assigned to a {@code void} type, it will load the target type's default value
 * only if this was configured at the assigner's construction.</li> 如果将非{@code void}类型分配给{@code void}类型，则仅当在赋值器的构造中配置了该类型时，它才会加载目标类型的默认值
 * <li>If two non-{@code void} types are subject of the assignment, it will delegate the assignment to its chained
 * assigner.</li> 如果两个非{@code void}类型是赋值的主题，它将把赋值委托给它的链式赋值器
 * </ol>
 */
@HashCodeAndEqualsPlugin.Enhance
public class VoidAwareAssigner implements Assigner {

    /**
     * An assigner that is capable of handling assignments that do not involve {@code void} types. 能够处理不涉及{@code void}类型的赋值的赋值器
     */
    private final Assigner chained;

    /**
     * Creates a new assigner that is capable of handling void types.
     *
     * @param chained A chained assigner which will be queried by this assigner to handle assignments that
     *                do not involve a {@code void} type. 一个链式赋值器，它将被这个赋值器查询以处理不涉及{@code void}类型的赋值
     */
    public VoidAwareAssigner(Assigner chained) {
        this.chained = chained;
    }

    @Override
    public StackManipulation assign(TypeDescription.Generic source, TypeDescription.Generic target, Typing typing) { // 这里做了一次转换，这里的source 其实是代理的，target 是被代理的
        if (source.represents(void.class) && target.represents(void.class)) {
            return StackManipulation.Trivial.INSTANCE;
        } else if (source.represents(void.class) /* && target != void.class */) {
            return typing.isDynamic()
                    ? DefaultValue.of(target)
                    : StackManipulation.Illegal.INSTANCE;
        } else if (/* source != void.class && */ target.represents(void.class)) {
            return Removal.of(source);
        } else /* source != void.class && target != void.class */ {
            return chained.assign(source, target, typing);
        }
    }
}
