package checks;

class WildcardReturnParameterTypeCheck {
  public Collector<Integer, ?, Integer> getCollector() { // Compliant Collector second argument is ignored, second parameter is an implementation detail
    return null;
  }
  public Unknown<?, ?, ?> getSomething() { // Compliant, Unknown types are not covered
    return null;
  }
}
