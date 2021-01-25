package net.bytebuddy.description;

public class VisitorMode {
    // 拜访者
    public interface Visitor {
        public void visit(WangHost wangHost);
        public void visit(LiHost liHost);
    }

    public static class MeVisitor implements Visitor {
        @Override
        public void visit(WangHost wangHost) {
            System.out.println(" 抽根烟");
        }

        @Override
        public void visit(LiHost liHost) {
            System.out.println(" 来点酒");
        }
    }

    // 主人
    public interface Host {
        // 接待
        public void accept(Visitor v);
        // 询问
        public void ask();
    }

    public static class WangHost implements Host {
        public void accept(Visitor v) {
            System.out.println("握手");
            ask();
            v.visit(this);

        }

        @Override
        public void ask() {
            System.out.println(" 欢迎，你想要点什么？");
        }
    }

    public static class LiHost implements Host {
        public void accept(Visitor v) {
            System.out.println("拥抱");
            ask();
            v.visit(this);
        }

        @Override
        public void ask() {
            System.out.println(" 欢迎，你想要点什么？");
        }
    }

    public static void main(String[] args) {
        System.out.println("风和日丽的一天 我作为客人");

        Visitor me = new MeVisitor();

        System.out.println("上午去了小王家，得到了热情的 款待");

        Host wang = new WangHost();
        wang.accept(me);

        System.out.println("下午去了小李家，得到了热情的 款待");

        Host li = new LiHost();
        li.accept(me);
    }
}
