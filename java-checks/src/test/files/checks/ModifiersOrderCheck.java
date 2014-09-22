class Foo {
  public static void main(String[] args) {  // Compliant
  }

  static public void main(String[] args) {  // Non-Compliant
  }

  public int a;

  @RequestMapping(value = "/restComTool", method = RequestMethod.GET)
  public
  @ResponseBody
  Map<String, List<Query>> queries(@RequestParam(value = "id", required = true, defaultValue = "-1") final Long id,
                                   @RequestParam(value = "q", required = false, defaultValue = "") final String query,
                                   @RequestParam(value = "callback", required = false, defaultValue = "") final String callback){}
  abstract
  public class A{}

  private final static int CREATE = 0, FIND = 1, NEW = 2, RELEASE = 3, N_NAMES = 4; //modifiers are shared between variable trees.
}

interface Bar{
  default void fun(){}
}


