package complexity;

// class complexity: 6
public class Helloworld {

  private String field = null;

  // this is considered as a method (bug http://jira.codehaus.org/browse/SONAR-2152)
  // complexity: 1
  static {
    int i = 0;
    if (i > 5) {
      System.out.println("hello from static block");
    }
  }

  // method complexity: 1
  public Helloworld(String s) {
    this.field = s;
  }

  // accessor
  // complexity: 0
  public String getField() {
    return field;
  }

  // accessor
  // complexity: 0
  public void setField(String s) {
    this.field = s;
  }

  // method complexity: 3
  public void sayHello() {
    for (int i = 0; i < 5; i++) {
      if (field != null) {
        System.out.println(field);
      }
    }
  }
}
