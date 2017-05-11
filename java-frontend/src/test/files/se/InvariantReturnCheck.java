class A {
  private int foo(boolean a) {
    int b = 12;
    if(a) {
      return b;
    }
    return b;  // Noncompliant
  }

  private int foo2(boolean a) {
    int b = 12;
    if(a) {
      return b;
    }
    return b - 1;
  }
  int foo3(boolean a) {
    int b = 12;
    if(a) {
      return b;
    }
    return b;  // false negative : caching of program states because of liveness of params makes the second path unexplored.
  }

  private String foo4(boolean a) {
    String b = "foo";
    if(a) {
      return b;
    }
    return b; // Noncompliant
  }

  void plop() {
    a;
  }

  private int doSomething() {
    System.out.println("");
    return 42;
  }
  private int getConstant2() {
    return 42;
  }

  private A() {}

  String constructComponentName() {
    synchronized (getClass()) {
      return base + nameCounter++;
    }
  }

  java.util.List<String> f() {
    System.out.println("");
    if(bool) {
      System.out.println("");
    }
    return new ArrayList<String>();
  }
  java.util.Map<String> f() {
    java.util.Map<String> foo = new java.util.HashMap<String>();
    if(bool) {
      return foo;
    }
    return foo;
  }

  private boolean fun(boolean a, boolean b) {
    if(a) {
      return a;
    }
    if(b) {
      return b;
    }
    return true; // Noncompliant
  }
  private boolean fun2(boolean a, boolean b) {
    if(a) {
      return a;
    }
    if(b) {
      return b;
    }
    return a != b ;
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

}

