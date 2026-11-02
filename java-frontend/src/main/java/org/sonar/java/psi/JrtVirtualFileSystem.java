/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.psi;

import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class JrtVirtualFileSystem extends DeprecatedVirtualFileSystem implements AutoCloseable {

  private final String home;
  private final FileSystem image;
  private final URLClassLoader loader;
  private final Map<Path, VirtualFile> files = new HashMap<>();

  JrtVirtualFileSystem(Path jdkHome) throws IOException {
    home = jdkHome.toString();
    var providerJar = jdkHome.resolve("lib/jrt-fs.jar");
    if (!Files.isRegularFile(providerJar)) {
      throw new IOException("Missing selected JDK provider: " + providerJar);
    }
    loader = new URLClassLoader(new java.net.URL[] {providerJar.toUri().toURL()}, ClassLoader.getPlatformClassLoader()) {
      @Override
      protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!name.startsWith("jdk.internal.jrtfs.")) {
          return super.loadClass(name, resolve);
        }
        synchronized (getClassLoadingLock(name)) {
          var loaded = findLoadedClass(name);
          var result = loaded == null ? findClass(name) : loaded;
          if (resolve) {
            resolveClass(result);
          }
          return result;
        }
      }
    };
    try {
      var provider = (FileSystemProvider) loader.loadClass("jdk.internal.jrtfs.JrtFileSystemProvider").getConstructor().newInstance();
      image = provider.newFileSystem(URI.create("jrt:/"), Map.of("java.home", home));
    } catch (ReflectiveOperationException | IOException | RuntimeException | Error failure) {
      try {
        loader.close();
      } catch (IOException cleanupFailure) {
        failure.addSuppressed(cleanupFailure);
      }
      if (failure instanceof IOException io) {
        throw io;
      }
      if (failure instanceof Error error) {
        throw error;
      }
      throw new IllegalStateException("Cannot open selected JDK image: " + jdkHome, failure);
    }
  }

  List<VirtualFile> moduleRoots() throws IOException {
    try (var modules = Files.list(image.getPath("/modules"))) {
      return modules.sorted().map(this::file).toList();
    }
  }

  private synchronized VirtualFile file(Path path) {
    return files.computeIfAbsent(path, Entry::new);
  }

  @Override
  public String getProtocol() {
    return "jrt";
  }

  @Override
  public VirtualFile findFileByPath(String path) {
    if (!path.startsWith(home + "!")) {
      return null;
    }
    var entry = image.getPath(path.substring(home.length() + 1));
    return Files.exists(entry) ? file(entry) : null;
  }

  @Override
  public VirtualFile refreshAndFindFileByPath(String path) {
    return findFileByPath(path);
  }

  @Override
  public void refresh(boolean asynchronous) {
  }

  @Override
  public void close() throws IOException {
    files.clear();
    try {
      image.close();
    } finally {
      loader.close();
    }
  }

  private final class Entry extends VirtualFile {

    private final Path path;

    private Entry(Path path) {
      this.path = path;
    }

    @Override
    public String getName() {
      var name = path.getFileName();
      return name == null ? "" : name.toString();
    }

    @Override
    public JrtVirtualFileSystem getFileSystem() {
      return JrtVirtualFileSystem.this;
    }

    @Override
    public String getPath() {
      return home + "!" + path;
    }

    @Override
    public boolean isWritable() {
      return false;
    }

    @Override
    public boolean isDirectory() {
      return Files.isDirectory(path);
    }

    @Override
    public boolean isValid() {
      return image.isOpen();
    }

    @Override
    public VirtualFile getParent() {
      return path.getParent() == null ? null : file(path.getParent());
    }

    @Override
    public VirtualFile findChild(String name) {
      if (name.isEmpty() || name.contains("/") || name.contains("\\") || name.equals(".") || name.equals("..")) {
        return null;
      }
      var child = path.resolve(name);
      return Files.exists(child) ? file(child) : null;
    }

    @Override
    public VirtualFile[] getChildren() {
      if (!isDirectory()) {
        return EMPTY_ARRAY;
      }
      try (var children = Files.list(path)) {
        return children.map(JrtVirtualFileSystem.this::file).toArray(VirtualFile[]::new);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public long getTimeStamp() {
      return 0;
    }

    @Override
    public long getModificationStamp() {
      return 0;
    }

    @Override
    public long getLength() {
      try {
        return Files.size(path);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return Files.newInputStream(path);
    }

    @Override
    public byte[] contentsToByteArray() throws IOException {
      return Files.readAllBytes(path);
    }

    @Override
    public OutputStream getOutputStream(Object requestor, long stamp, long time) throws IOException {
      throw new IOException("JDK image is read-only");
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, Runnable postRunnable) {
      if (postRunnable != null) {
        postRunnable.run();
      }
    }
  }
}
