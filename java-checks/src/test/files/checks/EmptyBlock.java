class EmptyBlock {
  // Noncompliant@+1 [[sc=10;ec=11]] {{Either remove or fill this block of code.}}
  static {
  }

  static {
    doSomething();
  }

  static {
    // comment
  }

  // Noncompliant@+1
  {
  }

  {
    doSomething();
  }

  {
    // comment
  }

  void method() {
    for (int i = 0; i < 10; i++)
    // Noncompliant@+1
    {
    }
    for (int i = 0; i < 10; i++);
    for (int i = 0; i < 10; i++) {
      // comment
    }

    switch (1) {
      case 1: // OK
      case 2:
        break;
    }

    // Noncompliant@+1 [[sc=16;ec=17]] {{Either remove or fill this block of code.}}
    switch (1) {
    }

    // Noncompliant@+1
    try {
    } catch (Exception e)
    // Noncompliant@+1
    {
    } finally
    // Noncompliant@+1
    {
    }

    try {
      doSomething();
    } catch (Exception e) {
      doSomething();
    } finally {
      doSomething();
    }

    try {
      // comment
    } catch (Exception e) {
      // comment
    } finally {
      // comment
    }

    synchronized (this)
    // Noncompliant@+1
    {
    }

    synchronized (this) {
      doSomething();
    }

    // Noncompliant@+1
    synchronized (this) {
      // comment
    }
  }

  void anotherMethod() {
  }

  static {
    try {
      stream = new ObjectOutputStream(new OutputStream() {
        public void write(int b) {}
      });
    }
    // Noncompliant@+1
    catch (IOException cannotHappen) {
    }
  }
}

class EmptyLambda {
  java.util.function.Consumer<String> c = s -> {};

  void foo(java.util.function.Consumer<String> c){
    foo(s ->{});
  }
}
