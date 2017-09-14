class A {
  private void f() {
    try {
    } catch (Exception e) {                     // Noncompliant {{Either log or rethrow this exception.}} [[sc=14;ec=25]]
    } catch (Exception e) {                     // Compliant
      System.out.println(e);
    } catch (Exception e) {                     // Noncompliant
      System.out.println("foo", e.getMessage());
    } catch (Exception e) {                     // Compliant
      System.out.println("", e);
    } catch (Exception f) {                     // Noncompliant
      System.out.println("", e);
    } catch (Exception f) {                     // Compliant
      System.out.println("", f);
    } catch (Exception e) {                     // Compliant
      System.out.println("", e);
      try {
      } catch (Exception f) {                   // Noncompliant
      }
    } catch (Exception e) {
      try {
      } catch (Exception f) {                   // Noncompliant {{Either log or rethrow this exception.}}
        System.out.println("", e);
      }
    } catch (RuntimeException e) {
      try {
      } catch (Exception f) {                   // Compliant
        System.out.println("", f);
      }
      System.out.println("", e);
    }
  }

  private void g() {
    System.out.println();
  }

  private void h() {
    try {
      /* ... */
    } catch (Exception e) {                     // Compliant
      throw Throwables.propagate(e);
    } catch (RuntimeException e) {              // Compliant - propagation
      throw e;
    } catch (Exception e) {                     // Noncompliant
      throw new RuntimeException("context");
    }

    try {
      /* ... */
    } catch (Exception e) {                      // Compliant
      throw new RuntimeException("context", e);
    }

    try {
    } catch (Exception e) {                      // Compliant
      throw e;
    } finally {
    }

    try {
    } catch (Exception e) {                      // Noncompliant
      int a;
    } catch (Throwable e) {                      // Noncompliant
    }

    try {
    } catch (IOException e) {                    // Compliant
      throw Throwables.propagate(e);
    }

    try {
    } catch (IOException e) {                    // Compliant
      throw new RuntimeException(e);
    } catch (Exception e) {                      // Noncompliant
      throw new RuntimeException(e.getMessage());
    } catch (Exception e) {                      // Compliant
      throw Throwables.propagate(e);
    }

    try {
    } catch (Exception e) {                      // Compliant
      throw e;
    } catch (Exception ex) {
      throw new XNIException(ex);
    }


    try {
    } catch (NumberFormatException e) {          // Compliant
      return 0;
    } catch (InterruptedException e) {           // Compliant
      /* do nothing */
    } catch (ParseException e) {                 // Compliant
    } catch (MalformedURLException e) {          // Compliant
    } catch (java.time.format.DateTimeParseException e) {          // Compliant
    }

    try {
    } catch (Exception e) {                      // Compliant
       foo(someContextVariable, e);
    } catch (Exception e) {                      // Compliant
      throw (Exception)new Foo("bar").initCause(e);
    } catch (Exception e) {                      // Compliant
      foo(null, e).bar();
    } catch (Exception e) {                      // Compliant
      throw foo(e).bar();
    } catch (Exception e) {                      // Noncompliant
      throw e.getCause();
    } catch (Exception e) {                      // Compliant
      throw (Exception)e;
    } catch (Exception e) {                      // Compliant
      throw (e);
    } catch (Exception e) {                      // Noncompliant
      throw (e).getClause();
    } catch (Exception e) {                      // Compliant
      Exception e2 = e;
      throw e2;
    } catch (Exception e) {                      // Compliant
      Exception foo = new RuntimeException(e);
    } catch (Exception e) {
      Exception foo = (e);
    } catch (Exception e) {                      // Compliant
      Exception foo;
      foo = e;
    } catch (java.lang.NumberFormatException e) { // Compliant
    } catch (java.net.MalformedURLException e) {  // Compliant
    } catch (java.time.format.DateTimeParseException e) {          // Compliant
    } catch (java.text.ParseException e) {        // Compliant
    } catch (java.text.foo e) {                   // Noncompliant
    } catch (java.foo.ParseException e) {         // Noncompliant [[sc=14;ec=39]]
    } catch (foo.text.ParseException e) {         // Noncompliant
    } catch (text.ParseException e) {             // Noncompliant
    } catch (foo.java.text.ParseException e) {    // Noncompliant
    } catch (Exception e) {                       // Compliant
      Exception foo = false ? e : null;
    } catch (Exception e) {                       // Compliant
      Exception foo = false ? null : e;
    } catch (Exception e) {                       // Compliant
      Exception e2;
      foo = (e2 = e) ? null : null;
    } catch (Exception e) {                       // Compliant
      throw wrapHttpException ? handleHttpException(e) : null;
    } catch (Exception e) {                       // Compliant
      throw wrapHttpException ? null : e;
    }
    catch (Exception e) {                     // Noncompliant
      try {
      } catch (Exception f) {                   // Noncompliant
       System.out.println("", e.getCause());
      }
    }
  }

  void bar(Class<?> clazz) {
    try {
      clazz.getMethod("bar", new Class[0]);
    } catch (NoSuchMethodException e) { // Compliant
      // do nothing
    } catch (Exception e) { // Noncompliant
      System.out.println("", e.getCause());
    }
  }

  MyEnum foo() {
    try {
      return Enum.valueOf(MyEnum.class, "C");
    } catch (IllegalArgumentException e) { // Compliant
      return null;
    }
  }

  MyEnum qix() {
    try {
      return MyEnum.valueOf("C");
    } catch (IllegalArgumentException e) { // Compliant
      return null;
    }
  }

  MyEnum gul() {
    try {
      new A() {
        void bul() throws Exception {
          MyEnum.valueOf("C");
        }
      };
      java.util.function.Function<String, MyEnum> getValue = (name) -> MyEnum.valueOf(name);
      return MyEnum.valueOf("C");
    } catch (IllegalArgumentException e) { // Compliant
      return null;
    }
  }

  private enum MyEnum {
    A, B;
  }
}
