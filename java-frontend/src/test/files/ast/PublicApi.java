import java.lang.Override;

//All identifier ending with public are expected to be public API per publicApiVisitorST
class A {

  //Constructors
  public A(){}
  A(int param){}
}

/**
 * Documented Class.
 */
public class documentedClassPublic {
  //constructors

  //fields
  int var1;
  /**
   * Documented variable.
   */
  public int documentedVarPublic;
  //Not documentation
  public static int var2Public;
  public final int var3Public;
  public static final int var3;

  //methods
  void method(){}

  /**
   * Documented Method.
   */
  public void documentedMethodInClassPublic(){}
  public static void method2Public(){}

  /**
   * Constructor documented.
   * @param param param
   */
  public documentedClassPublic(int param){}
}
public class undocumentedClassPublic {
  public Type undocumentedVar1Public, undocumentedVarPublic;
  /**
   * Doc.
   */
  public Type documentedVar1Public, documentedVarPublic;
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

interface interfaze{
  String[] undocumentedMethodPublic();

  /**
   * Documented method in interface.
   */
  String[][] documentedDbleArrayMethodPublic();

  @Override
  String method();

  public int constant = 0;
}
/**
 * Documented Class.
 */
public interface documentedInterfacePublic {
  void methodPublic();

  /**
   * Documentation.
   */
  java.lang.String documentedMethodPublic();

  /**
   * Documented.
   * @return a map.
   */
  Map<String,String> documentedGetPublic();

  /**
   * Documented method.
   */
  java.util.Map<String,String>[] documentedGetPublic();
}
public interface undocumentedInterfacePublic {

}

@interface annot{}
/**
 * Documented Annotation.
 */
public @interface documentedAnnotationPublic {
  String fooPublic();
  String constantInAnnotation = "";
}
public @interface undocumentedAnnotationPublic {

}

/**
 * Documented Class.
 */
@MyAnnotation()
public @TypeAnnot class documentedClassWithAnnotationPublic {

  private interface inter {
    void method();
    public void bar();
  }

}
interface deprec {
  /**
  * Documented public method.
  */
  @Deprecated
  void documentedMethodPublic();
}

class F {
  Anonymous i = new Anonymous() {
    @Override
    public void undocumentedMethod() {

    }
  };
}

class ClassWithGettersAndSetters {
  private int myVarGetSet;

  public ClassWithGettersAndSetters() {

  }
  public void myMethodPublic() {
  }

  public int getMyVarGetSet() {
    return myVarGetSet;
  }

  public void setMyVarGetSet(int myVarGetSet) {
    this.myVarGetSet = myVarGetSet;
  }
}