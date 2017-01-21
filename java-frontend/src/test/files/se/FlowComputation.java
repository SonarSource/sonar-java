
class A {

  void symbolSetToNull() {
    Object a = new Object();
    a = null; // flow@npe {{'a' is assigned null}}
    a.toString(); // Noncompliant [[flows=npe]] {{NullPointerException might be thrown as 'a' is nullable here}}  flow@npe {{'a' is dereferenced}}
  }


  void combined(Object a) {
    Object b = new Object();
    if (a == null) { // flow@comb {{Implies 'a' is null}}
      b = a; // flow@comb {{'b' is assigned null}}
      b.toString(); // Noncompliant [[flows=comb]] flow@comb {{'b' is dereferenced}}
    }
  }

  public void loops() {
    int totalGSSEdges = 0;
    int maxPopped = 0;
    List<String> strings = Collections.emptyList();
    for (String gss : strings) { // flow@loop {{...}}
      String edge = gss; // missing flow message - see SONARJAVA-2049
      while (edge != null) { // flow@loop {{Implies 'edge' is null}}
        totalGSSEdges++;
        edge = edge.substring(1);
      }
      maxPopped = Math.max(maxPopped, gss.toUpperCase() == null ? 0 : 1); // Noncompliant [[flows=loop]] flow@loop {{'gss' is dereferenced}}
    }
  }


  void exceptions() {
    try {
      Thread.sleep(0);
    } catch (Exception ex) { // flow@ex {{Implies 'ex' is non-null}}
      if (ex != null) { // Noncompliant [[flows=ex]] {{Change this condition so that it does not always evaluate to "true"}}   flow@ex {{Condition is always true}}
        ex.getClause();
      }
    }
  }

}

