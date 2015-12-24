class Foo {
  public static void main(String[] args) {
  }

  static public void main(String[] args) { // Noncompliant
  }

  public int a;

  // Noncompliant@+3 [[sc=3;ec=16]] {{Reorder the modifiers to comply with the Java Language Specification.}}
  @RequestMapping(value = "/restComTool", method = RequestMethod.GET)
  public
  @ResponseBody
  Map<String, List<Query>> queries(@RequestParam(value = "id", required = true, defaultValue = "-1") final Long id,
                                   @RequestParam(value = "q", required = false, defaultValue = "") final String query,
                                   @RequestParam(value = "callback", required = false, defaultValue = "") final String callback){}
  // Noncompliant@+2
  abstract
  public class A{}

  private final static int CREATE = 0, FIND = 1, NEW = 2, RELEASE = 3, N_NAMES = 4; // Noncompliant [[sc=17;ec=23]]
}

interface Bar{
  default void fun(){}
}


