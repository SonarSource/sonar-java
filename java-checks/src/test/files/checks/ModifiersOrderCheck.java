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
}

interface Bar{
  default void fun(){}
}


