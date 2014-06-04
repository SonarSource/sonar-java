interface notAnnotated {
  public int transform(int a);
}
interface notAnnotatedWithTwoMethods {
  public int transform(int a);
  public int transformInto(int a);
}

interface notAnnotatedWithDefaultMethod {
  default public int transform(int a) {
    return a+1;
  }
}
interface notAnnotatedWithStatic {
  public static int transform(int a) {
    return a+1;
  }
}
@FunctionalInterface
interface Annotated {
  public int transform(int a);
}