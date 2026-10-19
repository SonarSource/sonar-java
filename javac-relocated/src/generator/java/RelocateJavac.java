import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
  private static final String ORIGINAL_OPTIONS = "jdk.internal.opt";
  private static final String RELOCATED_OPTIONS = "org.sonar.java.javac.internal.opt";
  private static final String ORIGINAL_OPTIONS_PATH = "jdk/internal/opt";
  private static final String RELOCATED_OPTIONS_PATH = "org/sonar/java/javac/internal/opt";
  private static final String EXACT_CONVERSIONS_SOURCE = "src/java.base/share/classes/java/lang/runtime/ExactConversionsSupport.java";
  private static final Path DEFAULT_OUTPUT = Path.of("javac-relocated/src/main/java");

  private RelocateJavac() {
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 5 && "--download".equals(args[0])) {
      generateFromDownload(args[1], args[2], args[3], Path.of(args[4]).toAbsolutePath().normalize());
    } else if ((args.length == 4 || args.length == 5) && "--local".equals(args[0])) {
      Path output = args.length == 5 ? Path.of(args[4]) : DEFAULT_OUTPUT;
      generateFromLocal(Path.of(args[1]).toAbsolutePath().normalize(), args[2], args[3], output.toAbsolutePath().normalize());
    } else {
      throw new IllegalArgumentException("Usage: java RelocateJavac.java --download <url> <tag> <commit> <output> | --local <openjdk-root> <tag> <commit> [output]");
    }
  }

  private static void generateFromDownload(String url, String tag, String commit, Path output) throws Exception {
    String version = version("source.url", url, tag, commit, output);
    Path marker = output.resolve(RELOCATED_COMPILER_PATH).resolve("version.txt");
    if (Files.isRegularFile(marker) && Files.readString(marker).equals(version)
      && Files.isRegularFile(output.resolve(RELOCATED_COMPILER_PATH).resolve("main/JavaCompiler.java"))) {
      System.out.println("Relocated javac is current: " + tag);
      return;
    }
    Path workspace = Files.createTempDirectory("sonar-javac-source-");
    try {
      Path archive = workspace.resolve("openjdk.zip");
      download(url, archive);
      Path root = extract(archive, workspace.resolve("openjdk"), commit);
      generate(root, output, version, url, tag, commit);
    } finally {
      deleteTree(workspace);
    }
  }

  private static void generateFromLocal(Path root, String tag, String commit, Path output) throws Exception {
    String version = version("source.local", root.toString(), tag, commit, output);
    generate(root, output, version, root.toString(), tag, commit);
  }

  private static String version(String sourceKey, String source, String tag, String commit, Path output) throws Exception {
    byte[] support = Files.readAllBytes(annotationSupportSource(output));
    String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(support));
    return sourceKey + "=" + source + "\nsource.tag=" + tag + "\nsource.commit=" + commit
      + "\nrelocator.version=9\nannotation.support.sha256=" + digest + "\n";
  }

  private static void download(String url, Path archive) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(10)).build();
    HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(archive));
    if (response.statusCode() != 200) {
      throw new IOException("Failed to download OpenJDK source: HTTP " + response.statusCode() + " from " + url);
    }
  }

  private static Path extract(Path archive, Path destination, String commit) throws IOException {
    String prefix = "jdk-" + commit + "/";
    boolean foundCompiler = false;
    try (ZipInputStream input = new ZipInputStream(Files.newInputStream(archive))) {
      for (ZipEntry entry; (entry = input.getNextEntry()) != null;) {
        String name = entry.getName();
        if (!name.startsWith(prefix)) {
          throw new IOException("Unexpected OpenJDK archive entry: " + name);
        }
        String relative = name.substring(prefix.length());
        if (!entry.isDirectory() && wantedSource(relative)) {
          Path target = destination.resolve(relative).normalize();
          if (!target.startsWith(destination)) {
            throw new IOException("Unsafe OpenJDK archive entry: " + name);
          }
          Files.createDirectories(target.getParent());
          Files.copy(input, target);
          foundCompiler |= relative.equals("src/jdk.compiler/share/classes/com/sun/tools/javac/main/JavaCompiler.java");
        }
        input.closeEntry();
      }
    }
    if (!foundCompiler || !Files.isRegularFile(destination.resolve("LICENSE"))) {
      throw new IOException("Archive does not contain the expected OpenJDK compiler source and license");
    }
    return destination;
  }

  private static boolean wantedSource(String path) {
    return path.equals("LICENSE")
      || path.equals(EXACT_CONVERSIONS_SOURCE)
      || (path.startsWith("src/jdk.compiler/share/classes/com/sun/tools/javac/")
        && !excludedCompilerSource(Path.of(path.substring("src/jdk.compiler/share/classes/com/sun/tools/javac/".length()))))
      || path.startsWith("src/jdk.compiler/share/classes/com/sun/source/")
      || path.startsWith("src/jdk.compiler/share/classes/com/sun/tools/doclint/")
      || path.equals("src/jdk.internal.opt/share/classes/jdk/internal/opt/CommandLine.java")
      || path.startsWith("make/langtools/tools/compileproperties/")
      || path.startsWith("make/langtools/tools/propertiesparser/")
      || path.startsWith("make/langtools/tools/flagsgenerator/");
  }

  private static boolean excludedCompilerSource(Path relative) {
    String path = relative.toString().replace('\\', '/');
    return path.startsWith("launcher/")
      || path.equals("api/JavacTaskPool.java")
      || path.equals("api/WrappingJavaFileManager.java")
      || path.equals("main/JavacToolProvider.java")
      || path.equals("platform/JDKPlatformProvider.java")
      || path.endsWith("/package-info.java")
      || path.equals("package-info.java")
      || path.startsWith("resources/launcher");
  }

  private static void generate(Path root, Path output, String version, String source, String tag, String commit) throws Exception {
    Path share = root.resolve("src/jdk.compiler/share/classes");
    if (!Files.isDirectory(share.resolve(ORIGINAL_COMPILER_PATH)) || !Files.isDirectory(share.resolve(ORIGINAL_TREES_PATH))) {
      throw new IllegalArgumentException("Not an extracted OpenJDK source tree: " + root);
    }
    if (Files.isDirectory(root.resolve(".git"))) {
      verifyRevision(root, commit);
    }
    Path staging = Files.createTempDirectory(output.getParent(), "javac-relocated-");
    Path workspace = Files.createTempDirectory("sonar-javac-tools-");
    try {
      Path generated = generateOpenJdkSources(root, workspace.resolve("generated"), tag);
      int count = copyTree(share.resolve(ORIGINAL_COMPILER_PATH), staging, ORIGINAL_COMPILER_PATH);
      count += copyTree(share.resolve(ORIGINAL_TREES_PATH), staging, ORIGINAL_TREES_PATH);
      count += copyTree(share.resolve(ORIGINAL_DOCLINT_PATH), staging, ORIGINAL_DOCLINT_PATH);
      count += copyTree(root.resolve("src/jdk.internal.opt/share/classes").resolve(ORIGINAL_OPTIONS_PATH), staging, ORIGINAL_OPTIONS_PATH);
      count += copyTree(generated.resolve(ORIGINAL_COMPILER_PATH), staging, ORIGINAL_COMPILER_PATH);
      count += copyExactConversionsSupport(root, staging);
      Path docCommentKind = staging.resolve(RELOCATED_COMPILER_PATH).resolve("tree/DocCommentKind.java");
      Files.writeString(docCommentKind,
        "package org.sonar.java.javac.tree;\n\npublic enum DocCommentKind {\n  TRADITIONAL,\n  END_OF_LINE\n}\n",
        StandardCharsets.UTF_8);
      count++;
      Path annotationSupport = staging.resolve(RELOCATED_COMPILER_PATH).resolve("model/AnnotationProxySupport.java");
      Files.copy(annotationSupportSource(output), annotationSupport);
      count++;
      Files.copy(root.resolve("LICENSE"), staging.resolve("OPENJDK-LICENSE"));
      Files.writeString(staging.resolve("RELOCATION.txt"),
        "Source: " + source + "\nTag: " + tag + "\nCommit: " + commit + "\n"
          + ORIGINAL_COMPILER + " -> " + RELOCATED_COMPILER + "\n"
          + ORIGINAL_TREES + " -> " + RELOCATED_TREES + "\n"
          + ORIGINAL_DOCLINT + " -> " + RELOCATED_DOCLINT + "\n"
          + ORIGINAL_OPTIONS + " -> " + RELOCATED_OPTIONS + "\n"
          + "Files: " + count + "\n", StandardCharsets.UTF_8);
      Files.writeString(staging.resolve(RELOCATED_COMPILER_PATH).resolve("version.txt"), version, StandardCharsets.UTF_8);
      deleteTree(output);
      Files.move(staging, output);
      System.out.println("Relocated " + count + " files into " + output);
    } finally {
      deleteTree(staging);
      deleteTree(workspace);
    }
  }

  private static Path annotationSupportSource(Path output) {
    for (Path candidate : List.of(
      output.getParent().getParent().getParent().resolve("src/generator/resources/AnnotationProxySupport.java"),
      Path.of("src/generator/resources/AnnotationProxySupport.java"),
      Path.of("javac-relocated/src/generator/resources/AnnotationProxySupport.java"))) {
      if (Files.isRegularFile(candidate)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("Cannot find AnnotationProxySupport.java");
  }

  private static void verifyRevision(Path root, String commit) throws Exception {
    String revision = run(List.of("git", "-C", root.toString(), "rev-parse", "HEAD"), root).trim();
    if (!commit.equals(revision)) {
      throw new IllegalArgumentException("Expected OpenJDK commit " + commit + ", found " + revision);
    }
  }

  private static Path generateOpenJdkSources(Path root, Path generated, String tag) throws Exception {
    Path tools = root.resolve("make/langtools/tools");
    Path classes = generated.resolve("tools-classes");
    Path toolSources = generated.resolve("tools-sources");
    Files.createDirectories(classes);
    List<String> compile = new ArrayList<>(List.of(javaExecutable("javac"), "-d", classes.toString()));
    for (String directory : List.of("compileproperties", "propertiesparser", "flagsgenerator")) {
      try (var files = Files.walk(tools.resolve(directory))) {
        for (Path file : files.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
          Path relocated = toolSources.resolve(tools.relativize(file));
          Files.createDirectories(relocated.getParent());
          String source = Files.readString(file, StandardCharsets.UTF_8);
          if (file.getFileName().toString().equals("FlagsGenerator.java")) {
            source = source.replace(", _ ->", ", ignored ->");
          }
          Files.writeString(relocated, source, StandardCharsets.UTF_8);
          compile.add(relocated.toString());
        }
      }
    }
    run(compile, root);
    Path template = tools.resolve("propertiesparser/resources/templates.properties");
    Path templateCopy = classes.resolve("propertiesparser/resources/templates.properties");
    Files.createDirectories(templateCopy.getParent());
    Files.copy(template, templateCopy);

    Path sources = root.resolve("src/jdk.compiler/share/classes/com/sun/tools/javac");
    Path resources = sources.resolve("resources");
    Path generatedResources = generated.resolve("com/sun/tools/javac/resources");
    Files.createDirectories(generatedResources);
    List<String> propertiesCommand = new ArrayList<>(List.of(javaExecutable("java"), "-cp", classes.toString(), "compileproperties.CompileProperties", "-quiet"));
    try (var files = Files.list(resources)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".properties"))
        .filter(path -> !path.getFileName().toString().startsWith("launcher"))
        .sorted().toList()) {
        propertiesCommand.addAll(List.of("-compile", file.toString(), generatedResources.resolve(file.getFileName().toString().replace(".properties", ".java")).toString(), "java.util.ListResourceBundle"));
      }
    }
    Matcher release = Pattern.compile("jdk-(\\d+).*").matcher(tag);
    if (!release.matches()) {
      throw new IllegalArgumentException("Expected an OpenJDK tag such as jdk-27+35: " + tag);
    }
    String javaVersion = release.group(1);
    Path versionProperties = generatedResources.resolve("version.properties");
    Files.writeString(versionProperties, "jdk=" + javaVersion + "\nfull=" + javaVersion + "\nrelease=" + javaVersion + "\n", StandardCharsets.UTF_8);
    propertiesCommand.addAll(List.of("-compile", versionProperties.toString(), generatedResources.resolve("version.java").toString(), "java.util.ListResourceBundle"));
    run(propertiesCommand, root);
    run(List.of(javaExecutable("java"), "-cp", classes.toString(), "propertiesparser.PropertiesParser",
      "-compile", resources.resolve("compiler.properties").toString(), generatedResources.toString()), root);
    Path flags = generated.resolve("com/sun/tools/javac/code/FlagsEnum.java");
    Files.createDirectories(flags.getParent());
    run(List.of(javaExecutable("java"), "-cp", classes.toString(), "flagsgenerator.FlagsGenerator",
      sources.resolve("code/Flags.java").toString(), flags.toString()), root);
    return generated;
  }

  private static String javaExecutable(String name) {
    return Path.of(System.getProperty("java.home"), "bin", name).toString();
  }

  private static String run(List<String> command, Path directory) throws Exception {
    Process process = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start();
    String output;
    try (InputStream input = process.getInputStream()) {
      output = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
    if (process.waitFor() != 0) {
      throw new IOException("Command failed: " + String.join(" ", command) + "\n" + output);
    }
    return output;
  }

  private static int copyTree(Path input, Path output, String root) throws IOException {
    int copied = 0;
    try (var files = Files.walk(input)) {
      for (Path file : files.filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(".java") || path.toString().endsWith(".properties"))
        .filter(path -> !excludedCompilerSource(input.relativize(path)))
        .sorted(Comparator.naturalOrder()).toList()) {
        Path relative = Path.of(root).resolve(input.relativize(file));
        String relocated = relative.toString()
          .replace(ORIGINAL_COMPILER_PATH, RELOCATED_COMPILER_PATH)
          .replace(ORIGINAL_TREES_PATH, RELOCATED_TREES_PATH)
          .replace(ORIGINAL_DOCLINT_PATH, RELOCATED_DOCLINT_PATH);
        relocated = relocated.replace(ORIGINAL_OPTIONS_PATH, RELOCATED_OPTIONS_PATH);
        Path destination = output.resolve(relocated);
        Files.createDirectories(destination.getParent());
        String source = Files.readString(file, StandardCharsets.UTF_8)
          .replace(ORIGINAL_COMPILER, RELOCATED_COMPILER)
          .replace(ORIGINAL_TREES, RELOCATED_TREES)
          .replace(ORIGINAL_DOCLINT, RELOCATED_DOCLINT);
        source = source.replace(ORIGINAL_OPTIONS, RELOCATED_OPTIONS);
        source = source.replace("import javax.lang.model.util.Elements.DocCommentKind;",
          "import org.sonar.java.javac.tree.DocCommentKind;");
        if (file.toString().endsWith("/code/Source.java") || file.toString().endsWith("/processing/PrintingProcessor.java")) {
          source = source.replace("SourceVersion.RELEASE_27", "SourceVersion.RELEASE_21");
          for (int version = 22; version <= 27; version++) {
            source = source.replace("RELEASE_" + version, "RELEASE_21");
          }
        }
        if (file.toString().endsWith("/model/JavacElements.java")) {
          source = source.replace("import org.sonar.java.javac.tree.DocCommentTable;",
            "import org.sonar.java.javac.tree.DocCommentTable;\nimport org.sonar.java.javac.tree.DocCommentKind;")
            .replace("@Override @DefinedBy(Api.LANGUAGE_MODEL)\n    public TypeElement getEnumConstantBody",
              "@DefinedBy(Api.LANGUAGE_MODEL)\n    public TypeElement getEnumConstantBody")
            .replace("public DocCommentKind getDocCommentKind(Element e)",
              "public org.sonar.java.javac.tree.DocCommentKind getRelocatedDocCommentKind(Element e)");
        }
        if (file.toString().endsWith("/comp/LambdaToMethod.java")) {
          source = source.replace("implMethodName, _ ->", "implMethodName, ignored ->");
        }
        if (file.toString().endsWith("/comp/ExhaustivenessComputer.java")) {
          source = source.replace("catch (NumberFormatException _)", "catch (NumberFormatException ignored)")
            .replace("key, _ ->", "key, ignored ->");
        }
        if (file.toString().endsWith("/tree/TreeInfo.java")) {
          source = source.replace("names, _ -> true", "names, ignored -> true");
        }
        if (file.toString().endsWith("/comp/Flow.java")) {
          source = source.replace("case BindingPattern _ ->", "case BindingPattern ignored ->");
        }
        if (file.toString().endsWith("/code/TypeTag.java") || file.toString().endsWith("/code/Types.java")) {
          source = source.replace("import java.lang.runtime.ExactConversionsSupport;",
            "import org.sonar.java.javac.runtime.ExactConversionsSupport;");
        }
        if (file.toString().endsWith("/tree/TreeScanner.java") || file.toString().endsWith("/source/tree/AnyPatternTree.java")) {
          source = source.replace("import jdk.internal.javac.PreviewFeature;\n", "");
        }
        if (file.toString().endsWith("/main/Option.java")) {
          source = source.replace("import jdk.internal.misc.VM;", "import java.lang.management.ManagementFactory;")
            .replace("VM.getRuntimeArguments()", "ManagementFactory.getRuntimeMXBean().getInputArguments().toArray(String[]::new)");
        }
        if (file.toString().endsWith("/file/Locations.java")) {
          source = source.replace("import jdk.internal.jmod.JmodFile;\n", "")
            .replace("JmodFile.checkMagic(p);", "try (var jmod = Files.newInputStream(p)) {\n"
              + "                            byte[] magic = jmod.readNBytes(4);\n"
              + "                            if (magic.length != 4 || magic[0] != 0x4A || magic[1] != 0x4D || magic[2] != 1 || magic[3] != 0) {\n"
              + "                                throw new IOException(\"Invalid JMOD file: \" + p);\n"
              + "                            }\n"
              + "                        }");
        }
        if (file.toString().endsWith("/model/AnnotationProxyMaker.java")) {
          source = source.replace("import sun.reflect.annotation.*;", "import org.sonar.java.javac.model.AnnotationProxySupport.ExceptionProxy;\n"
              + "import org.sonar.java.javac.model.AnnotationProxySupport.EnumConstantNotPresentExceptionProxy;")
            .replace("AnnotationParser.annotationForMap", "AnnotationProxySupport.annotationForMap")
            .replace("AnnotationType.invocationHandlerReturnType", "AnnotationProxySupport.invocationHandlerReturnType");
        }
        Files.writeString(destination, source, StandardCharsets.UTF_8);
        copied++;
      }
    }
    return copied;
  }

  private static int copyExactConversionsSupport(Path root, Path output) throws IOException {
    Path destination = output.resolve(RELOCATED_COMPILER_PATH).resolve("runtime/ExactConversionsSupport.java");
    Files.createDirectories(destination.getParent());
    String source = Files.readString(root.resolve(EXACT_CONVERSIONS_SOURCE), StandardCharsets.UTF_8)
      .replace("package java.lang.runtime;", "package org.sonar.java.javac.runtime;");
    Files.writeString(destination, source, StandardCharsets.UTF_8);
    return 1;
  }

  private static void deleteTree(Path directory) throws IOException {
    if (Files.exists(directory)) {
      try (var paths = Files.walk(directory)) {
        for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
          Files.delete(path);
        }
      }
    }
  }
}
