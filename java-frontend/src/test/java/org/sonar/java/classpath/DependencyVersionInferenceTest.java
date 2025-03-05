package org.sonar.java.classpath;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

class DependencyVersionInferenceTest {
  String path = "/home/romain.brenguier/.m2/repository/org/springframework/boot/spring-boot/3.4.1/spring-boot-3.4.1.jar,/home/romain.brenguier/git/examples/target/classes,/home/romain.brenguier/.sdkman/candidates/java/current/lib/jrt-fs.jar,/home/romain.brenguier/.m2/repository/org/projectlombok/lombok/1.18.30/lombok-1.18.30.jar,/home/romain.brenguier/.m2/repository/org/jetbrains/annotations/25.0.0/annotations-25.0.0.jar";
  List<File> classpath = Arrays.stream(path.split(",")).map(File::new).toList();

  @Test
  void inferByName() {
    // Arrange
    DependencyVersionInference lombokInference = new DependencyVersionInference.ByNameInference(DependencyVersionInference.LOMBOK_PATTERN, "org.projectlombok", "lombok");

    // Act
    Optional<Version> version = lombokInference.infer(classpath);

    // Assert
    assertEquals(1, version.get().major());
    assertEquals(18, version.get().minor());
    assertEquals(30, version.get().patch());
    assertEquals(null, version.get().qualifier());
  }

  @Test
  void inferByReflection() {
    // Arrange
    DependencyVersionInference lombokInference = new DependencyVersionInference.ReflectiveInference();

    // Act
    Optional<Version> version = lombokInference.infer(classpath);

    // Assert
    assertEquals(1, version.get().major());
    assertEquals(18, version.get().minor());
    assertEquals(30, version.get().patch());
    assertEquals(null, version.get().qualifier());
  }

  @Test
  void inferByManifest() {
    // Arrange
    DependencyVersionInference lombokInference =
      new DependencyVersionInference.ManifestInference("Lombok-Version", "org.projectlombok", "lombok");

    // Act
    Optional<Version> version = lombokInference.infer(classpath);

    // Assert
    assertEquals(1, version.get().major());
    assertEquals(18, version.get().minor());
    assertEquals(30, version.get().patch());
    assertEquals(null, version.get().qualifier());
  }

  @Test
  void inferenceImplementations() {
    // Act
    Optional<Version> version =
      DependencyVersionInference.inferenceImplementations.stream()
        .filter(i -> i.handles("org.springframework.boot", "spring-boot"))
        .filter(i -> i instanceof DependencyVersionInference.ManifestInference)
        .map(i -> i.infer(classpath))
        .flatMap(Optional::stream)
        .findFirst();

    // Assert
    assertEquals(3, version.get().major());
    assertEquals(4, version.get().minor());
    assertEquals(1, version.get().patch());
    assertEquals(null, version.get().qualifier());
  }
}
