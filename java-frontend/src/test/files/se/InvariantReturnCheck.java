class A {
  private int foo(boolean a) { // Noncompliant [[flows=issue1]] {{Refactor this method to not always return the same value.}}
    int b = 12;
    if (a) {
      return b; // flow@issue1 [[order=3]]
    } else if (polop()) {
      return b;  // flow@issue1 [[order=2]]
    }
    return b; // flow@issue1 [[order=1]]
  }

  private int foo2(boolean a) {
    int b = 12;
    if (a) {
      return b;
    }
    return b - 1;
  }

  int foo3(boolean a) {
    int b = 12;
    if (a) {
      return b;
    }
    return b;  // false negative : caching of program states because of liveness of params makes the second path unexplored.
  }

  private String foo4(boolean a) { // Noncompliant
    String b = "foo";
    if (a) {
      return b;
    }
    return b;
  }

  void voidMethod() {
    doSomething();
  }

  private int doSomething() {
    System.out.println("");
    return 42;
  }

  private int getConstant2() {
    return 42;
  }

  private int getConstant3() {
    myList.stream().filter(item -> {
      return 0;
    }).collect(Collectors.toList());
    return 0;
  }

  private A() {
  }

  String constructComponentName() {
    synchronized (getClass()) {
      return base + nameCounter++;
    }
  }

  java.util.List<String> f() {
    System.out.println("");
    if (bool) {
      System.out.println("");
    }
    return new ArrayList<String>();
  }

  java.util.Map<String> f() {
    java.util.Map<String> foo = new java.util.HashMap<String>();
    if (bool) {
      return foo;
    }
    return foo;
  }

  private boolean fun(boolean a, boolean b) { // Noncompliant
    if (a) {
      return a;
    }
    if (b) {
      return b;
    }
    return true;
  }

  private boolean fun2(boolean a, boolean b) { // False negative because of constraints on relationship
    if (a) {
      return a;
    }
    if (b) {
      return b;
    }
    return a != b;
  }

  protected boolean isAssignable(final Class<?> dest, final Class<?> source) {

    if (dest.isAssignableFrom(source) ||
      ((dest == Boolean.TYPE) && (source == Boolean.class)) ||
      ((dest == Byte.TYPE) && (source == Byte.class)) ||
      ((dest == Character.TYPE) && (source == Character.class)) ||
      ((dest == Double.TYPE) && (source == Double.class)) ||
      ((dest == Float.TYPE) && (source == Float.class)) ||
      ((dest == Integer.TYPE) && (source == Integer.class)) ||
      ((dest == Long.TYPE) && (source == Long.class)) ||
      ((dest == Short.TYPE) && (source == Short.class))) {
      return (true);
    } else {
      return (false);
    }
  }

  public Object get(final String name) {

    // Return any non-null value for the specified property
    final Object value = values.get(name);
    if (value != null) {
      return (value);
    }

    // Return a null value for a non-primitive property
    final Class<?> type = getDynaProperty(name).getType();
    if (!type.isPrimitive()) {
      return (value);
    }

    // Manufacture default values for primitive properties
    if (type == Boolean.TYPE) {
      return (Boolean.FALSE);
    } else if (type == Byte.TYPE) {
      return (new Byte((byte) 0));
    } else if (type == Character.TYPE) {
      return (new Character((char) 0));
    } else if (type == Double.TYPE) {
      return (new Double(0.0));
    } else if (type == Float.TYPE) {
      return (new Float((float) 0.0));
    } else if (type == Integer.TYPE) {
      return (new Integer(0));
    } else if (type == Long.TYPE) {
      return (new Long(0));
    } else if (type == Short.TYPE) {
      return (new Short((short) 0));
    } else {
      return (null);
    }

  }

  // Example of a method that could raise issue based on returning the same SV AND same constraints on all the returned value.
  int plop(int a, boolean foo) { // Noncompliant
    int b = 0;
    if (a == b) {
      return b;
    }
    int c = b;
    return c;
  }

  java.util.Optional returnEmptyOptional(boolean p1) { // Noncompliant
    if (p1) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.empty();
  }

  private Object fun(Object o) { // Noncompliant
    if (o == null) {
      return o;
    }
    return null;
  }

  private boolean someMethod() {
    try {
      someExceptionalMethod();
    } catch (MyException e) {
      return false;
    }
    return true;
  }

}

class SONARJAVA3155 {
  // java.lang.Void cannot be instaniated, null is the only possible value for this type
  public Void call() throws Exception {
    if (a) {
      return null;
    }
    return null;
  }
}
