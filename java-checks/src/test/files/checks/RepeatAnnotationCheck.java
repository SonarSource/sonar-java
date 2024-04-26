class A{
  @SomeAnnotations({ // Noncompliant {{Remove the 'SomeAnnotations' wrapper from this annotation group}}
// ^^^^^^^^^^^^^^^
      @SomeAnnotation("a"),
      @SomeAnnotation("b"),
      @SomeAnnotation("c"),
  })
  void methodOne() {}

  // Java 8 style, thanks to JEP 120
  @SomeAnnotation("a")
  @SomeAnnotation("b")
  @SomeAnnotation("c")
  void methodTwo() {}

  @SomeAnnotation({})
  @some.pck.SomeAnnotations({ //Compliant because some.pck.SomeAnnotation is not solved and might not be @Reapeatable
      @some.pck.SomeAnnotation("a"),
      @some.pck.SomeAnnotation("b"),
      @some.pck.SomeAnnotation("c"),
  })
  @some.pck.SomeAnnotations({ //Compliant
      @SomeAnnotation("a"), //Might not be the same annotation
      @some.pck.SomeAnnotation("b"),
      @some.pck.SomeAnnotation("c"),
  })
  @SomeAnnotation({"a", "b", "c"})
  void methodThree(){}


  @java.lang.annotation.Repeatable
  @interface SomeAnnotation {

  }
}
