package checks;

import java.util.List;

import org.eclipse.draw2d.geometry.Vector;

class ReturnEmptyArrayNotNullCheckSample {

  public ReturnEmptyArrayNotNullCheckSample() {
    return null;
  }

  public int f11() {
    return null;
  }
}

class ReturnEmptyArrayNotNullCheckSampleB {
  @Unknown
  public int[] gul() {
    return null;  // Compliant, Unknown annotation...
  }
}

class ReturnEmptyArrayNotNullCheckSampleC implements SomethingUnknown {
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

  public UnknownType[] f3() {
    return null; // Compliant
  }

  public UnknownList<Object> f4() {
    return null; // Compliant
  }
}

class ReturnEmptyArrayNotNullCheckSampleD implements ItemProcessor<Integer, List<String>> {
  @Override
  public List<String> process(Integer i) {
    return null; // Compliant: even when ItemProcessor is not imported (unknown)
  }

  public List<String> process2(Integer i) {
    return null; // Noncompliant
  }
}

class ReturnUnknownVector {
  public Vector returnNull() {
    return null; // Compliant
  }

  public unknown.origin.Vector returnNull2() {
    return null; // Compliant
  }

  public java.util.Vector<Object> returnNull3() {
    return null; // Noncompliant
  }

}
