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
  private final Map<DependencyVersionImpl.CacheKey, DependencyVersionInference> inferenceImplementations = new HashMap<>();

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
   * When the library follows standard naming, the builder can be used to make
   * initialization simpler.
   */
  static class DependencyVersionInferenceBuilder {
    private String groupId;
    private String artifactId;
    private String attributeName = "Implementation-Version";

    DependencyVersionInferenceBuilder groupId(String g) {
      groupId = g;
      return this;
    }

    DependencyVersionInferenceBuilder artifactId(String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    DependencyVersionInferenceBuilder attributeName(String attributeName) {
      this.attributeName = attributeName;
      return this;
    }

    DependencyVersionInference build() {
      return new DependencyVersionInference.FallbackInference(
        new DependencyVersionInference.ByNameInference(makeStandardJarPattern(artifactId),
          groupId, artifactId),
        new DependencyVersionInference.ManifestInference(attributeName,
          groupId, artifactId));
    }
  }

  static DependencyVersionInferenceBuilder builder() {
    return new DependencyVersionInferenceBuilder();
  }

  @VisibleForTesting
  static final Pattern LOMBOK_PATTERN = makeStandardJarPattern("lombok");

  public static DependencyVersionInferenceService make() {
    DependencyVersionInferenceService service = new DependencyVersionInferenceService();
    Stream.of(
      builder().groupId("org.projectlombok").artifactId("lombok").attributeName("Lombok-Version").build(),
      builder().groupId("org.springframework.boot").artifactId("spring-boot").build(),
      builder().groupId("org.springframework").artifactId("spring-core").build()
    ).forEach(service::addImplementation);
    return service;
  }

}
