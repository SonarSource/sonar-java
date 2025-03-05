package org.sonar.java.classpath;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.plugins.java.api.classpath.DependencyVersion;

import static org.sonar.java.classpath.DependencyVersionInference.VERSION_REGEX;

public class DependencyVersionInferenceService {
  private Map<DependencyVersionImpl.CacheKey, DependencyVersionInference> inferenceImplementations = new HashMap<>();

  private void addImplementation(DependencyVersionInference inference) {
    inferenceImplementations.put(
      new DependencyVersionImpl.CacheKey(inference.getGroupId(), inference.getArtifactId()),
      inference);
  }

  private DependencyVersionInferenceService() {
  }

  public Optional<DependencyVersion> infer(String groupId, String artifactId, List<File> classpath) {
    DependencyVersionInference inference = inferenceImplementations.get(new DependencyVersionImpl.CacheKey(groupId, artifactId));
    if (inference == null) {
      return Optional.empty();
    }
    return inference.infer(classpath);
  }

  static Pattern makeStandardJarPattern(String artifactId) {
    return Pattern.compile(artifactId + "-" + VERSION_REGEX + "\\.jar");
  }

  @VisibleForTesting
  static Pattern LOMBOK_PATTERN = makeStandardJarPattern("lombok");

  public static DependencyVersionInferenceService make() {
    DependencyVersionInferenceService service = new DependencyVersionInferenceService();
    Stream.of(
      new DependencyVersionInference.FallbackInference(
        new DependencyVersionInference.ManifestInference("Lombok-Version", "org.projectlombok", "lombok"),
        new DependencyVersionInference.ByNameInference(LOMBOK_PATTERN, "org.projectlombok", "lombok")),
      new DependencyVersionInference.FallbackInference(
        new DependencyVersionInference.ManifestInference("Implementation-Version", "org.springframework.boot", "spring-boot"),
        new DependencyVersionInference.ByNameInference(makeStandardJarPattern("spring-boot"),
        "org.springframework.boot", "spring-boot"))
    ).forEach(service::addImplementation);
    return service;
  }

}
