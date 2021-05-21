package checks;

import java.util.List;

class ReturnEmptyArrayNotNullCheck {

  public ReturnEmptyArrayNotNullCheck() {
    return null;        
  }

  public int f11() {
    return null;        
  }
}

class ReturnEmptyArrayNotNullCheckB {
  @Unknown
  public int[] gul() {
    return null;  // Compliant, Unknown annotation...
  }
}

class ReturnEmptyArrayNotNullCheckC implements SomethingUnknown {
  @Override
  public List<String> process(Integer integer) throws Exception {
    return Collections.emptyList();
  }

  @Override
  public Integer[] process(Integer a) {
    return null; // Compliant, interface is unknown, but it can be an excluded method, do not report an issue to avoid FP.
  }

  public int[] f2() {
    return null; // Noncompliant
  }
}

class ReturnEmptyArrayNotNullCheckD implements ItemProcessor<Integer, List<String>> {
  @Override
  public List<String> process(Integer i) {
    return null; // Compliant: even when ItemProcessor is not imported (unknown)
  }

  public List<String> process2(Integer i) {
    return null; // Noncompliant
  }
}

class ReturnEmptyArrayNotNullCheckE implements ItemProcessor<Integer, List<String>> {
  public List<String> process(Integer i) {
    return null; // Compliant: even when ItemProcessor is not imported (unknown)
  }
}

class ReturnEmptyArrayNotNullCheckF implements ItemProcessor<Integer, List<String>> {
}

class ReturnEmptyArrayNotNullCheckG extends ReturnEmptyArrayNotNullCheckF {
  @Override
  public List<String> process(Integer i) {
    return null; // Noncompliant - FP, incomplete semantic prevent us to match the process method. Only happen in subclasses, acceptable corner case
  }
}
