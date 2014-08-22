class EmptyBlock {
  static /* NOK */ {
  }

  static /* OK */ {
    doSomething();
  }

  static /* OK */ {
    // comment
  }

  /* NOK */ {
  }

  /* OK */ {
    doSomething();
  }

  /* OK */ {
    // comment
  }

  void method() {
    for (int i = 0; i < 10; i++) /* NOK */ {
    }
    for (int i = 0; i < 10; i++); // OK
    for (int i = 0; i < 10; i++) /* OK */ {
      // comment
    }

    switch (1) {
      case 1: // OK
      case 2:
        break;
    }

    switch (1) /* NOK */ {
    }

    try /* NOK */ {
    } catch (Exception e) /* NOK */ {
    } finally /* NOK */ {
    }

    try /* OK */ {
      doSomething();
    } catch (Exception e) /* OK */ {
      doSomething();
    } finally /* OK */ {
      doSomething();
    }

    try /* OK */ {
      // comment
    } catch (Exception e) /* OK */ {
      // comment
    } finally /* OK */ {
      // comment
    }

    synchronized (this) /* NOK */ {
    }

    synchronized (this) /* OK */ {
      doSomething();
    }

    synchronized (this) /* OK */ {
      // comment
    }
  }

  void anotherMethod() /* OK */ {
  }

  static {
    try {
      stream = new ObjectOutputStream(new OutputStream() {
        public void write(int b) {}
      });
    } catch (IOException cannotHappen) /*NOK*/
    {
    }
  }
}
