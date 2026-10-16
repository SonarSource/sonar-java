import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RelocateJavac {

  private static final String ORIGINAL_COMPILER = "com.sun.tools.javac";
  private static final String RELOCATED_COMPILER = "org.sonar.java.javac";
  private static final String ORIGINAL_TREES = "com.sun.source";
  private static final String RELOCATED_TREES = "org.sonar.java.javac.source";
  private static final String ORIGINAL_DOCLINT = "com.sun.tools.doclint";
  private static final String RELOCATED_DOCLINT = "org.sonar.java.javac.doclint";
  private static final String ORIGINAL_COMPILER_PATH = "com/sun/tools/javac";
  private static final String RELOCATED_COMPILER_PATH = "org/sonar/java/javac";
  private static final String ORIGINAL_TREES_PATH = "com/sun/source";
  private static final String RELOCATED_TREES_PATH = "org/sonar/java/javac/source";
  private static final String ORIGINAL_DOCLINT_PATH = "com/sun/tools/doclint";
  private static final String RELOCATED_DOCLINT_PATH = "org/sonar/java/javac/doclint";
  private static final String OPENJDK_COMMIT = "815ff4dc327fe17f2433c7d115a5a503af10f3c4";

  private RelocateJavac() {
  }

  public static void main(String[] args) throws IOException {
    if (args.length < 1 || args.length > 2) {
      throw new IllegalArgumentException("Usage: java improving-java/RelocateJavac.java <openjdk-root> [output-directory]");
    }
    Path jdkRoot = Path.of(args[0]).toAbsolutePath().normalize();
    Path output = args.length == 2
      ? Path.of(args[1]).toAbsolutePath().normalize()
      : Path.of("java-frontend/src/main/relocated-javac").toAbsolutePath().normalize();
    Path share = jdkRoot.resolve("src/jdk.compiler/share/classes");
    if (!Files.isDirectory(share.resolve(ORIGINAL_COMPILER_PATH)) || !Files.isDirectory(share.resolve(ORIGINAL_TREES_PATH))) {
      throw new IllegalArgumentException("Not an extracted OpenJDK source tree: " + jdkRoot);
    }
    if (Files.exists(jdkRoot.resolve(".git"))) {
      verifyRevision(jdkRoot);
    }
    Path generated = generatedSources(jdkRoot);
    Files.createDirectories(output);
    int count = copyTree(share.resolve(ORIGINAL_COMPILER_PATH), output, ORIGINAL_COMPILER_PATH);
    count += copyTree(share.resolve(ORIGINAL_TREES_PATH), output, ORIGINAL_TREES_PATH);
    count += copyTree(share.resolve(ORIGINAL_DOCLINT_PATH), output, ORIGINAL_DOCLINT_PATH);
    count += copyTree(generated.resolve(ORIGINAL_COMPILER_PATH), output, ORIGINAL_COMPILER_PATH);
    Files.copy(jdkRoot.resolve("LICENSE"), output.resolve("OPENJDK-LICENSE"), StandardCopyOption.REPLACE_EXISTING);
    Files.writeString(output.resolve("RELOCATION.txt"),
      "Source: " + jdkRoot + "\nExpected tag: jdk-27+35\nExpected commit: " + OPENJDK_COMMIT + "\n"
        + ORIGINAL_COMPILER + " -> " + RELOCATED_COMPILER + "\n"
        + ORIGINAL_TREES + " -> " + RELOCATED_TREES + "\n"
        + ORIGINAL_DOCLINT + " -> " + RELOCATED_DOCLINT + "\n"
        + "Files: " + count + "\n",
      StandardCharsets.UTF_8);
    System.out.println("Relocated " + count + " files into " + output);
  }

  private static void verifyRevision(Path jdkRoot) throws IOException {
    Process process = new ProcessBuilder("git", "-C", jdkRoot.toString(), "rev-parse", "HEAD")
      .redirectErrorStream(true).start();
    String revision = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    try {
      if (process.waitFor() != 0 || !OPENJDK_COMMIT.equals(revision)) {
        throw new IllegalArgumentException("Expected OpenJDK jdk-27+35 at " + OPENJDK_COMMIT + ", found " + revision);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while checking OpenJDK revision", e);
    }
  }

  private static Path generatedSources(Path jdkRoot) throws IOException {
    List<Path> matches = new ArrayList<>();
    Path build = jdkRoot.resolve("build");
    if (Files.isDirectory(build)) {
      try (var configurations = Files.list(build)) {
        for (Path configuration : configurations.toList()) {
          Path generated = configuration.resolve("support/gensrc/jdk.compiler");
          if (Files.isDirectory(generated.resolve(ORIGINAL_COMPILER_PATH))) {
            matches.add(generated);
          }
        }
      }
    }
    if (matches.size() != 1) {
      throw new IllegalArgumentException("Expected one built jdk.compiler under " + build + ", found " + matches.size());
    }
    return matches.get(0);
  }

  private static int copyTree(Path input, Path output, String root) throws IOException {
    int copied = 0;
    try (var files = Files.walk(input)) {
      for (Path file : files.filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(".java") || path.toString().endsWith(".properties"))
        .sorted(Comparator.naturalOrder()).toList()) {
        Path relative = Path.of(root).resolve(input.relativize(file));
        String relocated = relative.toString()
          .replace(ORIGINAL_COMPILER_PATH, RELOCATED_COMPILER_PATH)
          .replace(ORIGINAL_TREES_PATH, RELOCATED_TREES_PATH)
          .replace(ORIGINAL_DOCLINT_PATH, RELOCATED_DOCLINT_PATH);
        Path destination = output.resolve(relocated);
        Files.createDirectories(destination.getParent());
        if (file.toString().endsWith(".java") || file.toString().endsWith(".properties")) {
          String source = Files.readString(file, StandardCharsets.UTF_8)
            .replace(ORIGINAL_COMPILER, RELOCATED_COMPILER)
            .replace(ORIGINAL_TREES, RELOCATED_TREES)
            .replace(ORIGINAL_DOCLINT, RELOCATED_DOCLINT);
          if (file.toString().endsWith("/code/Source.java") || file.toString().endsWith("/processing/PrintingProcessor.java")) {
            source = source.replace("SourceVersion.RELEASE_27", "SourceVersion.RELEASE_26")
              .replace("case JDK27  -> RELEASE_27;", "case JDK27  -> RELEASE_26;");
          }
          Files.writeString(destination, source, StandardCharsets.UTF_8);
        } else {
          Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        copied++;
      }
    }
    return copied;
  }
}
