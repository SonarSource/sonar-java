package org.foo;

abstract class A {

  void duplicatedMethod(boolean b, Object[] array1, Object[] array2) {
    if (b) {
      System.out.println("hello duplication");
    }
    for (Object item : array1) {
      if (item instanceof A) {
        System.out.println(item);
      } else {
        return;
      }
    }
    for (Object item : array2) {
      if (item instanceof A) {
        System.out.println(item);
      } else {
        return;
      }
    }
  }

  void foo(MyEnum v) {
    switch (v) {
      case A:
        System.out.println(MyEnum.A.name());
        break;
      case B:
        System.out.println(MyEnum.B.name());
        break;
      case C:
        System.out.println(MyEnum.C.name());
        break;
      default:
        System.out.println("This is not possible");
        break;
    }
  }
}

enum MyEnum {
  A, B, C
}
