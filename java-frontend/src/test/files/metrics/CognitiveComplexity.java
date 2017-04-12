class A { // total: 25;

  static boolean a;
  static {
    a = true || false && true || false; // +3
  }

  boolean b;

  {
    b = true || false; // +1
  }

  boolean c, d, e, f;

  public boolean extraConditions() {  // +3
    return a && b || foo(b && c);
  }
  public boolean extraConditions2() { // +2
    return a && (b || c) || d;
  }
  public void extraConditions3() { // +3
    if (a && b || c || d) {}
  }
  public void extraConditions4() { // +5
    if (a && b || c && d || e) {}
  }
  public void extraConditions5() { // +5
    if (a || b && c || d && e) {}
  }
  public void extraConditions6() { // +3
    if (a && b && c || d || e) {}
  }
}
