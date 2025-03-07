package org.sonar.plugins.java.api;

import java.util.Optional;
import java.util.function.BiFunction;
import org.sonar.plugins.java.api.classpath.DependencyVersion;

public interface DependencyVersionAware {

  boolean isCompatibleWithDependencies(BiFunction<String, String, Optional<DependencyVersion>> dependencyFinder);

}
