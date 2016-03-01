@SuppressWarnings("all")
class CompleteHierarchyOfTypes {

  static class Foo extends Bar.Baz { // Bar.Baz cannot be resolved if hierarchy of Bar is incomplete
  }

  static class Bar extends Base {
  }

  static class Base {
    static class Baz {
    }
  }

}
