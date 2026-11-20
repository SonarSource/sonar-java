package e;

class Routes {
  static final String ADMIN = Missing.PREFIX + "/admin";
  static final String LOCAL = "/local";

  @interface Path {
    String[] value();
  }

  enum Kind { A }

  @interface Tagged {
    Kind value();
  }

  @Path(ADMIN)
  void unknownConstant() {
  }

  @Path(LOCAL)
  void knownConstant() {
  }

  @Tagged(Kind.A)
  void enumConstant() {
  }
}
