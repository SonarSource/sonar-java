class StaticFieldAfterField {
  int f1;
  static int f2;
}

class FieldAfterConstructor {
  FieldAfterConstructor() {
  }
  int field;
}

class ConstructorAfterMethod {
  void method() {
  }
  FieldAfterConstructor() {
  }
}

class PublicAfterProtected {
  protected int f1;
  public int f2;
}

class ProtectedAfterPackageLocal {
  int f1;
  protected int f2;
}

class PackageLocalAfterPrivate {
  private int f1;
  int f2;
}

class Ok {
  public static int f1;
  protected static int f1;
  /* package local */ static int f1;
  private static int f1;

  public int f2;
  protected int f2;
  /* package local */ int f2;
  private int f2;

  public Ok() {
  }
  protected Ok() {
  }
  /* package local */ Ok() {
  }
  private Ok() {
  }

  public void method() {
  }
  protected void method() {
  }
  /* package local */ void method() {
  }
  private void method() {
  }
}
