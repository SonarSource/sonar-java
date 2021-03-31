class ChangeMethodContractCheck extends Unknown {
  @Override
  void foo(@javax.annotation.Nonnull Object a) { } // compliant : we cannot check the overriden method
}
