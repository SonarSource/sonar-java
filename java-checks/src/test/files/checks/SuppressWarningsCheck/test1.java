@SuppressWarnings("unused") // Noncompliant [[sc=2;ec=18]] {{Suppressing warnings is not allowed}}
class A {
  
  private final String BOXING = "boxing";
  
  @SuppressWarnings("all") // Noncompliant
  private void f() {
  }
  
  @SuppressWarnings({"unchecked", "cast"}) // Noncompliant
  private void g() {
  }
  
  @SuppressWarnings(BOXING) // Noncompliant
  private void h() {
  }
  
  @SuppressWarnings({BOXING, "all"}) // Noncompliant
  private void i() {
  }
  
  @SuppressWarnings("boxing") // Noncompliant
  private void j() {
  }
  
  @Override
  private String toString() {
    return "";
  }
  
}
