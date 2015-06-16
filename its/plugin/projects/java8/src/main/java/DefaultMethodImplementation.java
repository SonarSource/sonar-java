public class DefaultMethodImplementation implements DefaultMethodsInterface{

  public static void main(String[] args) {
    DefaultMethodImplementation plop = new DefaultMethodImplementation();
    System.out.println(plop.method1());
    System.out.println(plop.methodGeneric("MyString"));
    plop.voidMethod();
  }

}
