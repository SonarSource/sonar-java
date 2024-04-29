package checks;

import java.util.List;
import java.util.Map;

class ModifiersOrderCheckSample {
  public static void main(String[] args) {
  }

  static public void otherMain(String[] args) { // Noncompliant
  }

  public int a;


  @RequestMapping(value = "/restComTool", method = RequestMethod.GET)
  public
   @Annotation1 // Noncompliant {{Reorder the modifiers to comply with the Java Language Specification.}}
// ^^^^^^^^^^^^
  static
  Map<String, List<Object>> queries(@RequestParam(value = "id", required = true, defaultValue = "-1") final Long id,

                                    @RequestParam(value = "q", required = false, defaultValue = "") final String query,
                                    @RequestParam(value = "callback", required = false, defaultValue = "") final String callback){
    return null;
  }

 // Noncompliant@+2
  abstract
  public class A{}

  private final static int CREATE = 0, FIND = 1, NEW = 2, RELEASE = 3, N_NAMES = 4; // Noncompliant
//              ^^^^^^

  interface Bar{
    default void fun(){}
  }

  class B {
    @Annotation1
    private
    @Annotation2  // Compliant - annotation on type from java 8
      String foo;
    @Annotation1 @Annotation2 private String bar; // Compliant
    private @Annotation1 @Annotation2 String qix; // Compliant
  }

  @interface Annotation1 {}
  @interface Annotation2 {}

  @interface RequestParam { String value(); boolean required(); String defaultValue(); }
  @interface RequestMapping { String value(); RequestMethod method(); }
  enum RequestMethod { GET; }
}
