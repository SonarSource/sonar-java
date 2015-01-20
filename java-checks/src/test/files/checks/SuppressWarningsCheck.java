@SuppressWarnings("unused")
class A {
  
  private final String BOXING = "boxing";
  
  @SuppressWarnings("all")
  private void f() {
  }
  
  @SuppressWarnings({"unchecked", "cast"})
  private void g() {
  }
  
  @SuppressWarnings(BOXING) // corner case: will not be detected, as using constants is not a proper use
  private void h() {
  }
  
  @SuppressWarnings({BOXING, "all"}) // "boxing" will not be detected (corner case), but "all" will be detected
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