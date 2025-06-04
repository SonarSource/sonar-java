package org.sonar.plugins.java.api;

import java.util.Set;

public class ProjectContextModel {

  private final Set<String> springComponents;

  public ProjectContextModel(Set<String> springComponents) {
    this.springComponents = springComponents;
  }

  public boolean isSpringComponent(String fullyQualifiedName) {
    return springComponents.contains(fullyQualifiedName);
  }

}
