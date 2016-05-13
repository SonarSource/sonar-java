class ParameterizedMethodInvocation {
  public <T extends ParameterizedMethodInvocation> T method() {
    fun(method());
    return null;
  }

  public <S> S fun(S first) {

  }


}