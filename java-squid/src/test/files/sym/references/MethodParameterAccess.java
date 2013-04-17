package references;

@SuppressWarnings("all")
class MethodParameterAccess {

  public void method(Param param) {
    param = null;
    param.field = 1;
  }

  static class Param {
    int field;
  }

}
