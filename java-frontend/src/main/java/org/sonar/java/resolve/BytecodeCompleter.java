/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.resolve;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closeables;
import org.apache.commons.lang.StringUtils;
import org.objectweb.asm.ClassReader;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.bytecode.ClassLoaderBuilder;
import org.sonar.java.bytecode.loader.SquidClassLoader;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BytecodeCompleter implements JavaSymbol.Completer {

  private static final Logger LOG = Loggers.get(BytecodeCompleter.class);

  private static final int ACCEPTABLE_BYTECODE_FLAGS = Flags.ACCESS_FLAGS |
      Flags.INTERFACE | Flags.ANNOTATION | Flags.ENUM |
      Flags.STATIC | Flags.FINAL | Flags.SYNCHRONIZED | Flags.VOLATILE | Flags.TRANSIENT | Flags.VARARGS | Flags.NATIVE |
      Flags.ABSTRACT | Flags.STRICTFP | Flags.DEPRECATED;

  private Symbols symbols;
  private final List<File> projectClasspath;
  private final ParametrizedTypeCache parametrizedTypeCache;

  /**
   * Indexed by flat name.
   */
  private final Map<String, JavaSymbol.TypeJavaSymbol> classes = new HashMap<>();
  private final Map<String, JavaSymbol.PackageJavaSymbol> packages = new HashMap<>();

  private ClassLoader classLoader;

  public BytecodeCompleter(List<File> projectClasspath, ParametrizedTypeCache parametrizedTypeCache) {
    this.projectClasspath = projectClasspath;
    this.parametrizedTypeCache = parametrizedTypeCache;
  }

  public void init(Symbols symbols) {
    this.symbols = symbols;
  }

  public JavaSymbol.TypeJavaSymbol registerClass(JavaSymbol.TypeJavaSymbol classSymbol) {
    String flatName = formFullName(classSymbol);
    Preconditions.checkState(!classes.containsKey(flatName), "Registering class 2 times : %s", flatName);
    classes.put(flatName, classSymbol);
    return classSymbol;
  }

  @Override
  public void complete(JavaSymbol symbol) {
    LOG.debug("Completing symbol : " + symbol.name);
    String bytecodeName = formFullName(symbol);
    if(symbol.isPackageSymbol()) {
      bytecodeName = bytecodeName + ".package-info";
    }
    JavaSymbol.TypeJavaSymbol classSymbol = getClassSymbol(bytecodeName);
    if(symbol.isPackageSymbol()) {
      ((JavaSymbol.PackageJavaSymbol) symbol).packageInfo = classSymbol;
    }
    Preconditions.checkState(symbol.isPackageSymbol() || classSymbol == symbol);

    InputStream inputStream = null;
    ClassReader classReader = null;
    try {
      inputStream = inputStreamFor(bytecodeName);
      if(inputStream != null) {
        classReader = new ClassReader(inputStream);
      }
    } catch (IOException e) {
      throw Throwables.propagate(e);
    } finally {
      Closeables.closeQuietly(inputStream);
    }
    if (classReader != null) {
      classReader.accept(
          new BytecodeVisitor(this, symbols, classSymbol, parametrizedTypeCache),
          ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
    }
  }

  @Nullable
  private InputStream inputStreamFor(String fullname) {
    return getClassLoader().getResourceAsStream(Convert.bytecodeName(fullname) + ".class");
  }

  private ClassLoader getClassLoader() {
    if (classLoader == null) {
      classLoader = ClassLoaderBuilder.create(projectClasspath);
    }
    return classLoader;
  }

  private String formFullName(JavaSymbol symbol) {
    if(symbol.isTypeSymbol()) {
      return ((JavaSymbol.TypeJavaSymbol) symbol).getFullyQualifiedName();
    }
    return formFullName(symbol.name, symbol.owner);
  }

  String formFullName(String name, JavaSymbol site) {
    String result = name;
    JavaSymbol owner = site;
    while (owner != symbols.defaultPackage) {
      //Handle inner classes, if owner is a type, separate by $
      String separator = ".";
      if (owner.kind == JavaSymbol.TYP) {
        separator = "$";
      }
      result = owner.name + separator + result;
      owner = owner.owner();
    }
    return result;
  }

  @VisibleForTesting
  JavaSymbol.TypeJavaSymbol getClassSymbol(String bytecodeName) {
    return getClassSymbol(bytecodeName, 0);
  }

  // FIXME(Godin): or parameter must be renamed, or should not receive flat name, in a former case - first transformation in this method seems useless
  JavaSymbol.TypeJavaSymbol getClassSymbol(String bytecodeName, int flags) {
    return getClassSymbol(null, bytecodeName, flags);
  }
  public JavaSymbol.TypeJavaSymbol getClassSymbol(@Nullable JavaSymbol.TypeJavaSymbol classSymbolOwner, String bytecodeName, int flags) {
    String flatName = Convert.flatName(bytecodeName);
    JavaSymbol.TypeJavaSymbol symbol = classes.get(flatName);
    if (symbol == null) {
      String shortName = Convert.shortName(flatName);
      String packageName = Convert.packagePart(flatName);
      JavaSymbol.TypeJavaSymbol owner = classSymbolOwner;
      if(owner == null) {
        String enclosingClassName = Convert.enclosingClassName(shortName);
        if(StringUtils.isNotEmpty(enclosingClassName)) {
          owner = getClassSymbol(Convert.fullName(packageName, enclosingClassName));
        }
      }
      if ( owner != null) {
        //handle innerClasses
        String name = Convert.innerClassName(Convert.shortName(owner.getFullyQualifiedName()), shortName);
        symbol = new JavaSymbol.TypeJavaSymbol(filterBytecodeFlags(flags), name, owner, bytecodeName);
      } else {
        symbol = new JavaSymbol.TypeJavaSymbol(filterBytecodeFlags(flags), shortName, enterPackage(packageName));
      }
      symbol.members = new Scope(symbol);
      symbol.typeParameters = new Scope(symbol);

      // (Godin): IOException will happen without this condition in case of missing class:
      if (getClassLoader().getResource(Convert.bytecodeName(flatName) + ".class") != null) {
        symbol.completer = this;
      } else {
        if (!bytecodeName.endsWith("package-info")) {
          LOG.warn("Class not found: " + bytecodeName);
        }
        ((ClassJavaType) symbol.type).interfaces = ImmutableList.of();
        ((ClassJavaType) symbol.type).supertype = Symbols.unknownType;
      }

      classes.put(flatName, symbol);
    }
    return symbol;
  }

  public int filterBytecodeFlags(int flags) {
    return flags & ACCEPTABLE_BYTECODE_FLAGS;
  }

  /**
   * <b>Note:</b> Attempt to find something like "java.class" on case-insensitive file system can result in unwanted loading of "JAVA.class".
   * This method performs check of class name within file in order to avoid such situation.
   * This is definitely not the best solution in terms of performance, but acceptable for now.
   *
   * @return symbol for requested class, if corresponding class file exists, and {@link org.sonar.java.resolve.Resolve.JavaSymbolNotFound} otherwise
   */
  // TODO(Godin): Method name is misleading because of lazy loading.
  public JavaSymbol loadClass(String fullname) {
    JavaSymbol.TypeJavaSymbol symbol = classes.get(fullname);
    if (symbol != null) {
      return symbol;
    }

    // TODO(Godin): pull out conversion of name from the next method to avoid unnecessary conversion afterwards:
    InputStream inputStream = inputStreamFor(fullname);
    String bytecodeName = Convert.bytecodeName(fullname);

    if (inputStream == null) {
      return new Resolve.JavaSymbolNotFound();
    }

    try {
      ClassReader classReader = new ClassReader(inputStream);
      String className = classReader.getClassName();
      if (!className.equals(bytecodeName)) {
        return new Resolve.JavaSymbolNotFound();
      }
    } catch (IOException e) {
      throw Throwables.propagate(e);
    } finally {
      Closeables.closeQuietly(inputStream);
    }

    return getClassSymbol(fullname);
  }

  public JavaSymbol.PackageJavaSymbol enterPackage(String fullname) {
    if (StringUtils.isBlank(fullname)) {
      return symbols.defaultPackage;
    }
    JavaSymbol.PackageJavaSymbol result = packages.get(fullname);
    if (result == null) {
      result = new JavaSymbol.PackageJavaSymbol(fullname, symbols.defaultPackage);
      result.completer = this;
      packages.put(fullname, result);
    }
    return result;
  }

  public void done() {
    if (classLoader != null && classLoader instanceof SquidClassLoader) {
      ((SquidClassLoader) classLoader).close();
    }
  }

  /**
   * Compiler marks all artifacts not presented in the source code as {@link Flags#SYNTHETIC}.
   */
  static boolean isSynthetic(int flags) {
    return (flags & Flags.SYNTHETIC) != 0;
  }

}
