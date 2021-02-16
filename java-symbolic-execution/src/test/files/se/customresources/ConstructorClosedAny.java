package org.sonar.custom;

// Parameters for this test should be:
//    constructor   = "org.sonar.custom.GenericResource";
//    closingMethod = "org.sonar.custom.GenericResource#closeResource";


public class GenericResource {
  
  public static void correct(String name) {
    GenericResource resource = new GenericResource(name);
    try {
      resource.use();
    } finally {
      resource.closeResource(name);
    }
  }
  
  public static void correct(int channel) {
    GenericResource resource = new GenericResource(channel);
    try {
      resource.use();
    } finally {
      resource.closeResource(channel);
    }
  }
  
  public static void wrong(String name) {
    GenericResource resource = new GenericResource(name);  // Noncompliant {{Close this "GenericResource".}}
    resource.use();
  }
  
  public static void wrong(int channel) {
    GenericResource resource = new GenericResource(channel);  // Noncompliant {{Close this "GenericResource".}}
    resource.use();
  }
  
  public GenericResource(String name) {
  }
  
  public GenericResource(int channel) {
    // Used to check differentiation between signature
  }
  
  public void use() {}
  
  public void closeResource(String name) {}
  
  public void closeResource(int id) {}
  
}
