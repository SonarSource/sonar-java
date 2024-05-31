package org.sonar.custom;

// Parameters for this test should be:
//    openingMethod = org.sonar.custom.GenericResource#open
//    closingMethod = org.sonar.custom.GenericResource#closeResource


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
  
  public static void correct(int channel) {
    GenericResource resource = new GenericResource();
    resource.open(channel);
    try {
      resource.use();
    } finally {
      resource.closeResource(channel);
    }
  }
  
  public static void wrong(String name) {
    GenericResource resource = new GenericResource();
    resource.open(name);  // Noncompliant {{Close this "GenericResource".}}
    resource.use();
  }
  
  public static void wrong(int channel) {
    GenericResource resource = new GenericResource();
    resource.open(channel); // Noncompliant {{Close this "GenericResource".}}
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
  
}
