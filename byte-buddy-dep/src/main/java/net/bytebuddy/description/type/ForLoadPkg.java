package net.bytebuddy.description.type;

public class ForLoadPkg {

    public static void main(String[] args) {
        PackageDescription.ForLoadedPackage pkg = new PackageDescription.ForLoadedPackage(ForLoadPkg.class.getPackage());
        TypeDescription.ForLoadedType thisclass = new TypeDescription.ForLoadedType(ForLoadPkg.class);
//        TypeDescription.ForLoadedType anotherclass = new TypeDescription.ForLoadedType(Enums.Demo.class);

        System.out.println("package name" + pkg.getName());

        System.out.println(pkg.getName() + " contains : " + thisclass.getName() + " : " + pkg.contains(thisclass));
//        System.out.println(pkg.getName() + " contains : " + anotherclass.getName() + " : " + pkg.contains(anotherclass));
    }

}
