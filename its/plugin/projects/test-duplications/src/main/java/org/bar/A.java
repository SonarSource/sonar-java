package org.bar;

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

  void bar() {
    Object object = null;
    try {
      object = new Object();
    } catch (Exception e) {
      System.out.println(object);
    } finally {
      System.out.println("foo");
    }
    Object object2;
    try{
      object2 = qix();
    } finally {
      System.out.println("bar");
    }
    object2.toString(); // not accessible with null value
  }

  abstract Object qix() throws RuntimeException;
}
