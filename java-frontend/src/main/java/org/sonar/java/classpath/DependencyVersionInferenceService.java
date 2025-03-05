package org.sonar.java.classpath;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

  @VisibleForTesting
  public List<DependencyVersion> inferAll(List<File> classpath) {
    return inferenceImplementations.values().stream()
      .map(inference -> inference.infer(classpath))
      .flatMap(Optional::stream)
      .toList();
  }

  static Pattern makeStandardJarPattern(String artifactId) {
    return Pattern.compile(artifactId + "-" + VERSION_REGEX + "\\.jar");
  }

  /**
   * When the library follows standard naming and manifest structure, this method can be used to make
   * initialization simpler.
   */
  static DependencyVersionInference makeStandardInference(String groupId, String artifactId) {
    return new DependencyVersionInference.FallbackInference(
      new DependencyVersionInference.ByNameInference(makeStandardJarPattern(artifactId),
        groupId, artifactId),
      new DependencyVersionInference.ManifestInference("Implementation-Version",
        groupId, artifactId));
  }

  @VisibleForTesting
  static Pattern LOMBOK_PATTERN = makeStandardJarPattern("lombok");

  public static DependencyVersionInferenceService make() {
    DependencyVersionInferenceService service = new DependencyVersionInferenceService();
    Stream.of(
      new DependencyVersionInference.FallbackInference(
        new DependencyVersionInference.ManifestInference("Lombok-Version", "org.projectlombok", "lombok"),
        new DependencyVersionInference.ByNameInference(LOMBOK_PATTERN, "org.projectlombok", "lombok")),
      makeStandardInference("org.springframework.boot", "spring-boot"),
      makeStandardInference("org.springframework", "spring-core")
    ).forEach(service::addImplementation);
    return service;
  }

}
