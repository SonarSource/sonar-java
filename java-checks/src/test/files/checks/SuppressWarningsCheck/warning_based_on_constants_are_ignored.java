@SuppressWarnings("unused") // Noncompliant {{Suppressing the 'unused' warning is not allowed}}
class A {
  
  private final String BOXING = "boxing";
  
  @SuppressWarnings("all") // Noncompliant
  private void f() {
  }
  
  @SuppressWarnings({"unchecked", "cast"}) // Noncompliant
  private void g() {
  }
  
  @SuppressWarnings(BOXING) // corner case: will not be detected, as using constants is not a proper use
  private void h() {
  }
  
  @SuppressWarnings({BOXING, "all"}) // Noncompliant
  private void i() {
  }
  
  @SuppressWarnings("boxing")
  private void j() {
  }
  
  @Override
  private String toString() {
    return "";
  }
  
}
