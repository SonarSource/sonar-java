class Bar {

  private class Indirect extends Baz {
    int method() {
    }
  }

  private class Baz extends Qix {

  }

  private class Qix extends Indirect {

  }
}
