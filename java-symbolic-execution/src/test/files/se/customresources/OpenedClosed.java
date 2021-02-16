package org.sonar.custom;

// Parameters for this test should be:
//    openingMethod = org.sonar.custom.GenericResource#open(java.lang.String)
//    closingMethod = org.sonar.custom.GenericResource#closeResource(java.lang.String)


public class GenericResource {
  
  public static void correct(String name) {
    GenericResource resource = new GenericResource();
    resource.open(name);
    try {
      resource.use();
    } finally {
      resource.closeResource(name);
    }
  }
  
  public static void wrong(String name) {
    GenericResource resource = new GenericResource();
    resource.open(name);  // Noncompliant {{Close this "GenericResource".}}
    resource.use();
  }
  
  public static void okay(int channel) {
    GenericResource resource = new GenericResource();
    resource.open(channel);  // Compliant because not checked
    resource.use();
  }
  
  public void open(String name) {
  }
  
  public void open(int id) {
    // Used to check differentiation between signature
  }
  
  public void use() {}
  
  public void closeResource(String name) {}
  
  public void closeResource(int id) {}

  void transitive(String name, GenericResource param) {
    GenericResource local = new GenericResource();
    try {
      if (local == param) {
        param.open(name);
      }
    } finally {
      local.closeResource(name);
    }
  }

}
