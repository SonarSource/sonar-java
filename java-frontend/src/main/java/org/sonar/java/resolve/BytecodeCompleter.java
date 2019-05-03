/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import com.google.common.io.Closeables;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.plugins.java.api.semantic.Symbol;

public class BytecodeCompleter implements JavaSymbol.Completer {

  public static final int ASM_API_VERSION = Opcodes.ASM7;

  private Symbols symbols;
  private final ParametrizedTypeCache parametrizedTypeCache;
  private final SquidClassLoader classLoader;
  /**
   * Indexed by flat name.
   */
  private final Map<String, JavaSymbol.TypeJavaSymbol> classes = new HashMap<>();
  private final Map<String, JavaSymbol.PackageJavaSymbol> packages = new HashMap<>();
  private final Map<JavaSymbol.TypeJavaSymbol, Map<String, Object>> constantValues = new HashMap<>();

  private Set<String> classesNotFound = new TreeSet<>();

  public BytecodeCompleter(SquidClassLoader classLoader, ParametrizedTypeCache parametrizedTypeCache) {
    this.classLoader = classLoader;
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
    String bytecodeName = formFullName(symbol);
    if(symbol.isPackageSymbol()) {
      bytecodeName = bytecodeName + ".package-info";
    }
    JavaSymbol.TypeJavaSymbol classSymbol = getClassSymbol(bytecodeName);
    if(symbol.isPackageSymbol()) {
      ((JavaSymbol.PackageJavaSymbol) symbol).packageInfo = classSymbol;
    }
    Preconditions.checkState(symbol.isPackageSymbol() || classSymbol == symbol);

    byte[] bytes = classLoader.getBytesForClass(bytecodeName);
    if (bytes != null) {
      ClassReader classReader = new ClassReader(bytes);
      classReader.accept(
        new BytecodeVisitor(this, symbols, classSymbol, parametrizedTypeCache),
        ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
    }
  }

  /**
   * This method reads constant values in bytecode. It should be called when resolving semantics from source.
   */
  @CheckForNull
  public Object constantValue(Symbol owner, String constantName) {
    if (constantValues.containsKey(owner)) {
      return constantValues.get(owner).get(constantName);
    }
    if (owner.isTypeSymbol()) {
      JavaSymbol.TypeJavaSymbol typeSymbol = (JavaSymbol.TypeJavaSymbol) owner;
      String bytecodeName = typeSymbol.getFullyQualifiedName();
      byte[] bytes = classLoader.getBytesForClass(bytecodeName);
      if (bytes != null) {
        Map<String, Object> valuesByFieldName = new HashMap<>();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(
          new ClassVisitor(ASM_API_VERSION) {
            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
              valuesByFieldName.put(name, value);
              return super.visitField(access, name, descriptor, signature, value);
            }
          },
          ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        constantValues.put(typeSymbol, valuesByFieldName);
        return valuesByFieldName.get(constantName);
      }
    }
    return null;
  }


  @Nullable
  private InputStream inputStreamFor(String fullname) {
    return classLoader.getResourceAsStream(Convert.bytecodeName(fullname) + ".class");
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
        owner = getEnclosingClass(shortName, packageName);
      }
      int classFlags = Flags.filterAccessBytecodeFlags(flags);
      if (owner != null) {
        //handle innerClasses
        String name = Convert.innerClassName(Convert.shortName(owner.getFullyQualifiedName()), shortName);
        symbol = new JavaSymbol.TypeJavaSymbol(classFlags, name, owner, bytecodeName);
      } else {
        symbol = new JavaSymbol.TypeJavaSymbol(classFlags, shortName, enterPackage(packageName));
      }
      symbol.members = new Scope(symbol);
      symbol.typeParameters = new Scope(symbol);

      // (Godin): IOException will happen without this condition in case of missing class:
      if (classLoader.getResource(Convert.bytecodeName(flatName) + ".class") != null) {
        symbol.completer = this;
      } else {
        // Do not log missing annotation as they are not necessarily required in classpath for compiling
        if (!bytecodeName.endsWith("package-info") && isNotAnnotation(flags)) {
          classesNotFound.add(bytecodeName);
        }
        ((ClassJavaType) symbol.type).interfaces = Collections.emptyList();
        ((ClassJavaType) symbol.type).supertype = Symbols.unknownType;
      }

      classes.put(flatName, symbol);
    }
    return symbol;
  }

  private static boolean isNotAnnotation(int flags) {
    return (flags & Flags.ANNOTATION) == 0;
  }

  @Nullable
  private JavaSymbol.TypeJavaSymbol getEnclosingClass(String shortName, String packageName) {
    JavaSymbol.TypeJavaSymbol owner = null;
    String enclosingClassName = Convert.enclosingClassName(shortName);
    if (StringUtils.isNotEmpty(enclosingClassName)) {
      enclosingClassName = Convert.fullName(packageName, enclosingClassName);
      InputStream inputStream = null;
      try {
        inputStream = inputStreamFor(enclosingClassName);
        while (inputStream == null && enclosingClassName.endsWith("$")) {
          enclosingClassName = enclosingClassName.substring(0, enclosingClassName.length() - 1);
          inputStream = inputStreamFor(enclosingClassName);
        }
      } finally {
        Closeables.closeQuietly(inputStream);
      }
      owner = getClassSymbol(enclosingClassName);
    }
    return owner;
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

    byte[] bytesForClass = classLoader.getBytesForClass(fullname);
    if (bytesForClass == null) {
      return new Resolve.JavaSymbolNotFound();
    }

    ClassReader classReader = new ClassReader(bytesForClass);
    String className = classReader.getClassName();
    if (!className.equals(Convert.bytecodeName(fullname))) {
      return new Resolve.JavaSymbolNotFound();
    }
    return getClassSymbol(fullname);
  }

  public JavaSymbol.PackageJavaSymbol enterPackage(String fullname) {
    if (StringUtils.isBlank(fullname)) {
      return symbols.defaultPackage;
    }
    return packages.computeIfAbsent(fullname, name -> {
      JavaSymbol.PackageJavaSymbol pck  = new JavaSymbol.PackageJavaSymbol(fullname, symbols.defaultPackage);
      pck.completer = this;
      return pck;
    });
  }

  public Set<String> classesNotFound() {
    return classesNotFound;
  }
}
