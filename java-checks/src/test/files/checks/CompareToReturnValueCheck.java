class A implements Comparable<A> {
  @Override
  public int compareTo(A a) {
    return Integer.MIN_VALUE; // Noncompliant
  }
  
  public int compareTo() {
    return Short.MIN_VALUE; // Compliant
  }
  
  public int getMinValue() {
    return Integer.MIN_VALUE; // Compliant
  }
  
  public int compareTo(int a) {
    return -1; // Compliant
  }
  
  public boolean compareTo(Boolean a) {
    return a; // Compliant
  }
  
  public Long compareTo(Long a) {
    return Long.MIN_VALUE; // Compliant
  }
  
  @Override
  public int compareTo(Short a) {
    return Integer.MAX_VALUE; // Compliant
  }
}