package checks.naming;

class BadClassName {
  class badClassName { }
  class GoodClassName { }

  interface should_not_be_checked_interface { }
  enum should_not_be_checked_enum { }
  @interface should_not_be_checked_annotation { }
}

