//All identifier ending with public are expected to be public API per publicApiVisitorST
class A {

  //Constructors
  public A(){}
  A(int param){}
}

/**
 * Documented Class.
 */
public class DocumentedClassPublic {
  //constructors

  //fields
  int var1;
  public int varPublic;
  public static int var2Public;
  public final int var3Public;
  public static final int var3;

  //methods
  void method(){}
  public void methodPublic(){}
  public static void method2Public(){}

  public DocumentedClassPublic(int param){}
}
public class undocumentedClassPublic {

}

enum B{}

/**
 * Documented Enum.
 */
public enum documentedEnumPublic{
  A;
}
public enum undocumentedEnumPublic{
  A;
}

interface interfaze{}
/**
 * Documented Class.
 */
public interface documentedInterfacePublic {
  void methodPublic();
}
public interface undocumentedInterfacePublic {

}

@interface annot{}
/**
 * Documented Class.
 */
public @interface documentedAnnotationPublic {
  String fooPublic();
}
public @interface undocumentedAnnotationPublic {

}