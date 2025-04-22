package checks;

public class RemoveTypeFromUnusedPatternCheck_Java21 {
  record Guest(String name, String email, String phoneNumber) {
  }
  public void foo(Object o){
    if(o instanceof Guest(String name, String email, String phoneNumber)){} // compliant not java 22
  }
}
