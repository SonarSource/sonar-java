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
  Map<DependencyVersionImpl.CacheKey, DependencyVersionInference> inferenceImplementations = new HashMap<>();

  void addImplementation(DependencyVersionInference inference) {
    inferenceImplementations.put(
      new DependencyVersionImpl.CacheKey(inference.getGroupId(), inference.getArtifactId()),
      inference);
  }

  public Optional<DependencyVersion> infer(String groupId, String artifactId, List<File> classpath) {
    DependencyVersionInference inference = inferenceImplementations.get(new DependencyVersionImpl.CacheKey(groupId, artifactId));
    if (inference == null) {
      return Optional.empty();
    }
    return inference.infer(classpath);
  }

  @VisibleForTesting
  static Pattern LOMBOK_PATTERN = Pattern.compile("lombok-([0-9]+).([0-9]+).([0-9]+)([^0-9].*)?\\.jar");

  public static DependencyVersionInferenceService make() {
    DependencyVersionInferenceService service = new DependencyVersionInferenceService();
    Stream.of(
      new DependencyVersionInference.FallbackInference(
        new DependencyVersionInference.ManifestInference("Lombok-Version", "org.projectlombok", "lombok"),
        new DependencyVersionInference.ByNameInference(LOMBOK_PATTERN, "org.projectlombok", "lombok")),
      new DependencyVersionInference.FallbackInference(
        new DependencyVersionInference.ManifestInference("Implementation-Version", "org.springframework.boot", "spring-boot"),
        new DependencyVersionInference.ByNameInference(Pattern.compile("spring-boot-" + VERSION_REGEX + "\\.jar"),
        "org.springframework.boot", "spring-boot"))
    ).forEach(service::addImplementation);
    return service;
  }

}
