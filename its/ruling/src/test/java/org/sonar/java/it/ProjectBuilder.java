/*
 * SonarQube Java
 * Copyright (C) 2013-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.it;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

public class ProjectBuilder {
  private static final Pattern SOURCE_PATTERN = Pattern.compile("\\<source\\>(?<version>[^\\<]++)\\</source\\>");
  private static final URI JAVA_17_LINUX = URI.create("https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.7%2B7/OpenJDK17U-jdk_x64_linux_hotspot_17.0.7_7.tar.gz");
  private static final URI JAVA_19_LINUX = URI.create("https://github.com/adoptium/temurin19-binaries/releases/download/jdk-19.0.2%2B7/OpenJDK19U-jdk_x64_linux_hotspot_19.0.2_7.tar.gz");
  private static final Map<Integer, Map<String, URI>> JAVA_VERSIONS = Map.of(
    17, Map.of("linux", JAVA_17_LINUX),
    19, Map.of("linux", JAVA_19_LINUX)
  );

  private static final Path CACHE_FOLDER = Path.of(".autoscan_cache");
  private static final Path DOWNLOADS_FOLDER = CACHE_FOLDER.resolve("downloads");
  private static final Path JDKS_FOLDER = CACHE_FOLDER.resolve("jdks");

  private static final Map<URI, String> SHA256_FINGERPRINTS = Map.of(
    JAVA_17_LINUX, "e9458b38e97358850902c2936a1bb5f35f6cffc59da9fcd28c63eab8dbbfbc3b",
    JAVA_19_LINUX, "3a3ba7a3f8c3a5999e2c91ea1dca843435a0d1c43737bd2f6822b2f02fc52165"
  );

  private Path project;

  public ProjectBuilder(Path project) {
    this.project = project;
  }

  public Path build() throws IOException, InterruptedException, NoSuchAlgorithmException {
    Path projectPath = project.toAbsolutePath();
    int javaVersion = extractSourceVersion(projectPath);
    System.out.println(projectPath + " uses Java" + javaVersion);
    Path jdk = getJDK(javaVersion, "linux");
    System.out.println("Will try to build with JDK" + javaVersion + " (" + jdk + ")" );
    if (!build(projectPath, jdk)) {
      throw new IllegalStateException("Could not build " + projectPath + " using " + jdk);
    }
    return jdk;
  }

  private static int extractSourceVersion(Path projectRoot) throws IOException {
    Path pomXml = projectRoot.resolve("pom.xml");
    String content;
    try (var stream = new FileInputStream(pomXml.toFile())) {
      content = new String(stream.readAllBytes());
    }
    Matcher matcher = SOURCE_PATTERN.matcher(content);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group("version").trim());
    }
    throw new IllegalStateException("Could not find source version in " + pomXml);
  }

  private static Path getJDK(Integer javaVersion, String platform) throws IOException, InterruptedException, NoSuchAlgorithmException {
    // Get the right JDK archive
    URI link = JAVA_VERSIONS.get(javaVersion).get(platform);

    // Set up directory to download
    Files.createDirectories(DOWNLOADS_FOLDER);

    // Download
    Path archive = DOWNLOADS_FOLDER.resolve(link.getPath().substring(link.getPath().lastIndexOf('/') + 1));
    download(link, archive, true);

    // Check the integrity of the JDK archive
    MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
    try (var in = new FileInputStream(archive.toFile()); var digestIn = new DigestInputStream(in, messageDigest)) {
      digestIn.readAllBytes();
    }
    byte[] digest = messageDigest.digest();
    StringBuilder builder = new StringBuilder(digest.length * 2);
    for (Byte b : digest) {
      builder.append(String.format("%02x", b));
    }
    String hexDigest = builder.toString();
    if (!hexDigest.equals(SHA256_FINGERPRINTS.get(link))) {
      throw new IllegalStateException("The archive for the distribution seems to be corrupted (sha256).");
    }

    // Extract from archive
    return extractArchive(archive, JDKS_FOLDER);
  }

  private static Path download(URI target, Path archive, boolean followRedirect) throws IOException, InterruptedException {
    System.out.println("Attempting to download " + target + " to " + archive);

    if (!archive.toFile().exists()) {
      HttpRequest request = HttpRequest.newBuilder()
        .header("accept", "application/gzip; charset=binary")
        .header("Accept-encoding", "identity")
        .GET()
        .uri(target)
        .build();
      HttpClient client = HttpClient.newHttpClient();
      HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      int statusCode = response.statusCode();
      if (400 <= statusCode) {
        throw new IllegalStateException("Could not download " + target + " (" + statusCode + ")");
      } else if (statusCode == 302) {
        if (followRedirect && response.headers().firstValue("location").isPresent()) {
          return download(URI.create(response.headers().firstValue("location").get()), archive, false);
        }
        throw new IllegalStateException("Could not download " + target + " (" + statusCode + ")");
      }
      try (InputStream in = response.body(); var out = new FileOutputStream(archive.toFile())) {
        in.transferTo(out);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return archive;
  }

  private static Path extractArchive(Path archive, Path outputDirectory) throws IOException {
    String mimeType = Files.probeContentType(archive);
    switch (mimeType) {
      case "application/gzip":
        Path topFolder = null;
        try (var in = new FileInputStream(archive.toFile());
             var gzFile = new GzipCompressorInputStream(in);
             var tarFile = new TarArchiveInputStream(gzFile)) {
          ArchiveEntry entry = null;
          while ((entry = tarFile.getNextEntry()) != null) {
            if (!tarFile.canReadEntryData(entry)) {
              // log something?
              continue;
            }
            String name = outputDirectory.resolve(entry.getName()).toAbsolutePath().toString();
            File f = new File(name);
            if (entry.isDirectory()) {
              if (topFolder == null) {
                topFolder = Path.of(name);
              }
              if (!f.isDirectory() && !f.mkdirs()) {
                throw new IOException("failed to create directory " + f);
              }
            } else {
              File parent = f.getParentFile();
              if (!parent.isDirectory() && !parent.mkdirs()) {
                throw new IOException("failed to create directory " + parent);
              }
              try (OutputStream o = Files.newOutputStream(f.toPath())) {
                IOUtils.copy(tarFile, o);
                if (parent.getName().equals("bin") ||
                  (parent.getName().equals("lib") && f.getName().equals("jexec") || f.getName().equals("jspawnhelper") )) {
                  f.setExecutable(true);
                }
              }
            }
          }
          return topFolder;
        }
      default:
        throw new UnsupportedOperationException("Cannot extract archive of type " + mimeType);
    }
  }

  public boolean build(Path project, Path jdk) throws IOException, InterruptedException {
    String[] command = new String[]{
      "mvn", "clean", "verify",
    };
    ProcessBuilder processBuilder = new ProcessBuilder(command)
      .inheritIO()
      .directory(project.toFile());
    processBuilder.environment().put("JAVA_HOME", jdk.toString());
    System.out.println(processBuilder.command() + " (" + processBuilder.environment() +")");
    Process process = processBuilder.start();
    int returnCode = process.waitFor();
    return returnCode == 0;
  }
}
