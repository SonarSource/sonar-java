package org.sonar.custom;

// Parameters for this test should be:
//    factoryMethod = org.sonar.custom.ResourceFactory#createResource
//    closingMethod = org.sonar.custom.GenericResource#closeResource


public class GenericResource {
  
  public static void correct(String name) {
    GenericResource resource = new ResourceFactory().createResource(name);
    try {
      resource.use();
    } finally {
      resource.closeResource(name);
    }
  }
  
  public static void correct(int channel) {
    GenericResource resource = new ResourceFactory().createResource(channel);
    try {
      resource.use();
    } finally {
      resource.closeResource(channel);
    }
  }
  
  public static void wrong(String name) {
    GenericResource resource = new ResourceFactory().createResource(name);  // Noncompliant
    resource.use();
  }
  
  public static void wrong(int channel) {
    GenericResource resource = new ResourceFactory().createResource(channel);  // Noncompliant
    resource.use();
  }
  
  public GenericResource(String name) {
  }
  
  public GenericResource(int channel) {
    // Used to check differentiation between signature
  }
  
  public void use() {}
  
  public void closeResource(String name) {}
  
  public void closeResource(int channel) {}
  
}

public class ResourceFactory {
  
  public OpenedResource createResource(String name) {
    return new GenericResource(name);  // Compliant because the opened resource is returned
  }
  
  public OpenedResource createResource(int channel) {
    return new GenericResource(channel);  // Compliant because the opened resource is returned
  }
}
