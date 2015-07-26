class A {
    
  String fieldNameWithPasswordInIt = retrievePassword();
  
  private void a() {
    String variable1 = "blabla";
    String variable2 = "login=a&password=xxx"; // Noncompliant
    String variable3 = "login=a&password=";

    String variableNameWithPasswordInIt = "xxx"; // Noncompliant
    String otherVariableNameWithPasswordInIt;
    fieldNameWithPasswordInIt = "xx"; // Noncompliant
    fieldNameWithPasswordInIt = retrievePassword();
    this.fieldNameWithPasswordInIt = "xx"; // Noncompliant
    this.fieldNameWithPasswordInIt = retrievePassword();
    variable1 = "xx";
 
    String[] array = {};
    array[0] = "xx";
    
    A myA = new A();
    myA.setProperty("password", "xxxxx"); // Noncompliant
    myA.setProperty("password", new Object());
    myA.setProperty("xxxxx", "password");
    myA.setProperty(12, "xxxxx");
    myA.setProperty(new Object(), new Object());
    
    MyUnknownClass.myUnknownMethod("password", "xxxxx"); // Noncompliant
  }

  private String retrievePassword() {
    return null;
  }
  
  private void setProperty(Object property, Object Value) {
  }
  
}