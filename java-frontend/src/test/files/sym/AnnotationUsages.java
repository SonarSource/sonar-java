import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

class A {

  @Target(ElementType.TYPE_USE)
  static @interface MyAnnotation1 { }

  @Target(ElementType.TYPE_USE)
  static @interface MyAnnotation2 { }

  static @interface MyAnnotation3 { }

  void foo(String[] @MyAnnotation1 [] myList) {

    org.foo.@MyAnnotation2 A a;

    for (@MyAnnotation3 String[] object : myList) {
      // do something
    }
  }
}
