package org.sonar.java.classpath;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface DependencyVersionInference {

  Optional<Version> infer(List<File> classpath);

  boolean handles(String groupId, String artifactId);

  List<DependencyVersionInference> inferenceImplementations = Arrays.asList(
    new LombokByNameInference(), new ManifestInference(), new SpringByNameInference()
  );

  String VERSION_REGEX = "([0-9]+).([0-9]+).([0-9]+)([^0-9].*)?";
  Pattern VERSION_PATTERN = Pattern.compile(VERSION_REGEX);

  abstract class LombokInference implements DependencyVersionInference {

  }

  abstract class ByNameInference implements DependencyVersionInference {

    final Pattern pattern;
    final String groupId;
    final String artifactId;

    protected ByNameInference(Pattern pattern, String groupId, String artifactId) {
      this.pattern = pattern;
      this.groupId = groupId;
      this.artifactId = artifactId;
    }

    @Override
    public Optional<Version> infer(List<File> classpath) {
      for (File file : classpath) {
        Matcher matcher = pattern.matcher(file.getName());
        if (matcher.matches()) {
          return Optional.of(matcherToVersion(matcher));
        }
      }
      return Optional.empty();
    }

    @Override
    public boolean handles(String groupId, String artifactId) {
      return groupId.equals(this.groupId) && artifactId.equals(this.artifactId);
    }
  }

  class LombokByNameInference extends ByNameInference {

    static Pattern PATTERN = Pattern.compile("lombok-([0-9]+).([0-9]+).([0-9]+)([^0-9].*)?\\.jar");

    protected LombokByNameInference() {
      super(PATTERN, "org.projectlombok", "lombok");
    }

  }

  static Version matcherToVersion(Matcher matcher) {
    return new Version(
      Integer.parseInt(matcher.group(1)),
      Integer.parseInt(matcher.group(2)),
      Integer.parseInt(matcher.group(3)),
      matcher.group(4));
  }

  class ReflectiveInference extends LombokByNameInference {
    private static final String KNOWN_CLASS_NAME = "lombok.Lombok";

    @Override
    public Optional<Version> infer(List<File> classpath) {
      URLClassLoader loader = new URLClassLoader(classpath.stream().map(file -> {
        try {
          return file.toURL();
        } catch (MalformedURLException e) {
          return null;
        }
      }).filter(Objects::nonNull).toArray(URL[]::new), null);
      try {
        Class<?> knownClass = loader.loadClass(KNOWN_CLASS_NAME);
        String implementationVersion = knownClass.getPackage().getImplementationVersion();

        return Optional.of(matcherToVersion(VERSION_PATTERN.matcher(implementationVersion)));
      } catch (ClassNotFoundException e) {
        return Optional.empty();
      }
    }
  }

  class ManifestInference extends LombokByNameInference {

    private static final String ATTRIBUTE_NAME = "Lombok-Version";

    @Override
    public Optional<Version> infer(List<File> classpath) {
      Optional<File> lombokJar = classpath.stream().filter(file -> file.getName().startsWith("lombok-")).findFirst();
      if (lombokJar.isEmpty()) return Optional.empty();

      try {
        URL jarUrl = new URL("jar:file:" + lombokJar.get().getAbsolutePath() + "!/META-INF/MANIFEST.MF");
        JarURLConnection jarConnection = (JarURLConnection) jarUrl.openConnection();
        Manifest manifest = jarConnection.getManifest();

        if (manifest != null) {
          Attributes mainAttributes = manifest.getMainAttributes();
          if (mainAttributes != null) {
            Matcher matcher = VERSION_PATTERN.matcher(mainAttributes.getValue(ATTRIBUTE_NAME));
            if (matcher.matches()) {
              return Optional.of(matcherToVersion(matcher));
            }
          }
        }
      } catch (IOException ignored) {
      }
      return Optional.empty();
    }
  }

  class SpringByNameInference extends ByNameInference {

    static final Pattern PATTERN = Pattern.compile("spring-boot-" + VERSION_REGEX);

    protected SpringByNameInference() {
      super(PATTERN, "org.springframework.boot", "spring-boot");
    }
  }
}
