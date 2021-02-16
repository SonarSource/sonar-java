
class A {

  void symbolSetToNull() {
    Object a = new Object();
    a = null; // flow@npe {{Implies 'a' is null.}}
    a.toString(); // Noncompliant [[flows=npe]] {{A "NullPointerException" could be thrown; "a" is nullable here.}}  flow@npe {{'a' is dereferenced.}}
  }


  void combined(Object a) {
    Object b = new Object();
    if (a == null) { // flow@comb {{Implies 'a' is null.}}
      b = a; // flow@comb {{Implies 'b' has the same value as 'a'.}}
      b.toString(); // Noncompliant [[flows=comb]] flow@comb {{'b' is dereferenced.}}
    }
  }

  public void loops() {
    int totalGSSEdges = 0;
    int maxPopped = 0;
    List<String> strings = Collections.emptyList();
    for (String gss : strings) {
      String edge = gss; // missing flow message - see SONARJAVA-2049
      while (edge != null) { // flow@loop {{Implies 'edge' can be null.}}
        totalGSSEdges++;
        edge = edge.substring(1);
      }
      maxPopped = Math.max(maxPopped, gss.toUpperCase() == null ? 0 : 1); // Noncompliant [[flows=loop]] flow@loop {{'gss' is dereferenced.}}
    }
  }


  void exceptions() {
    try {
      Thread.sleep(0); // flow@ex1 {{'InterruptedException' is thrown.}} flow@ex2 {{Exception is thrown.}}
    } catch (Exception ex) { // flow@ex1,ex2 {{Implies 'ex' is not null.}}  flow@ex1 {{'InterruptedException' is caught.}} flow@ex2 {{'Exception' is caught.}}
      if (ex != null) { // Noncompliant [[flows=ex1,ex2]] {{Remove this expression which always evaluates to "true"}}   flow@ex1,ex2 {{Expression is always true.}}
        ex.getClause();
      }
    }
  }

  void invocation_target(Object a) {
    a.toString(); // flow@target {{Implies 'a' is not null.}}
    if (a == null) { // Noncompliant [[flows=target]] flow@target {{Expression is always false.}}

    }
  }

}

