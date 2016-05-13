class Outer {
  static class StaticInner {
    StaticInner(int i) {}
  }
  class NonStaticInner extends StaticInner {
    NonStaticInner(int i) {
      super(i); // no implicit arg, as StaticInner is static
    }
  }

  class Inner {
    Inner(int i) {}
  }
  class ChildInner extends Inner {
    ChildInner() {
      this(0); // extra Outer implicit arg
    }

    ChildInner(int i) {
      super(i); // extra Outer implicit arg
    }
  }
}
