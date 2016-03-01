package references;

@SuppressWarnings(ExpressionInAnnotation.VALUE)
class ExpressionInAnnotation {

  public static final String VALUE = "all";

}

@interface one {
  String foo();
}
@interface two {
  String foo();
  String bar();
}
class A{
@one(foo="")
@two(foo="", bar="")
 void method(String foo){}
}