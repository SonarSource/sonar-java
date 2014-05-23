/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.resolve;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closeables;
import org.apache.commons.lang.StringUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.bytecode.ClassLoaderBuilder;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BytecodeCompleter implements Symbol.Completer {

  private static final Logger LOG = LoggerFactory.getLogger(BytecodeCompleter.class);

  private List<File> projectClasspath;
  private ClassLoader classLoader;
  private Map<String, Symbol.TypeSymbol> classes = new HashMap<String, Symbol.TypeSymbol>();
  private Map<String, Symbol.PackageSymbol> packages = new HashMap<String, Symbol.PackageSymbol>();

  private final Symbols symbols;

  public BytecodeCompleter(Symbols symbols, List<File> projectClasspath) {
    this.symbols = symbols;
    this.projectClasspath = projectClasspath;
  }

  @Override
  public void complete(Symbol symbol) {
    LOG.debug("Completing symbol : " + symbol.name);
    String bytecodeName = formFullName(symbol.name, symbol.owner);
    Symbol.TypeSymbol classSymbol = getClassSymbol(bytecodeName);
    Preconditions.checkState(classSymbol == symbol);
    InputStream inputStream = null;
    try {
      inputStream = inputStreamFor(bytecodeName);
      ClassReader classReader = new ClassReader(inputStream);
      classReader.accept(new BytecodeVisitor((Symbol.TypeSymbol) symbol), ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
    } catch (Exception e) {
      // TODO(Godin): why only interfaces, but not supertype for example?
      ((Type.ClassType) symbol.type).interfaces = ImmutableList.of();
      LOG.error("Cannot complete type : " + bytecodeName + "  " + e.getMessage(), e);
    } finally {
      Closeables.closeQuietly(inputStream);
    }
  }

  private InputStream inputStreamFor(String fullname) {
    if (classLoader == null) {
      classLoader = ClassLoaderBuilder.create(projectClasspath);
    }
    return classLoader.getResourceAsStream(Convert.bytecodeName(fullname) + ".class");
  }

  public String formFullName(String name, Symbol site) {
    String result = name;
    Symbol owner = site;
    while (owner != symbols.defaultPackage) {
      //Handle inner classes, if owner is a type, separate by $
      String separator = ".";
      if (owner.kind == Symbol.TYP) {
        separator = "$";
      }
      result = owner.name + separator + result;
      owner = owner.owner();
    }
    return result;
  }
  public Symbol.TypeSymbol getClassSymbol(String bytecodeName) {
    return getClassSymbol(bytecodeName, 0);
  }

  public Symbol.TypeSymbol getClassSymbol(String bytecodeName, int flags) {
    String flatName = Convert.flatName(bytecodeName);
    Symbol.TypeSymbol symbol = classes.get(flatName);
    if (symbol == null) {
      String shortName = Convert.shortName(flatName);
      String packageName = Convert.packagePart(flatName);
      String enclosingClassName = Convert.enclosingClassName(shortName);
      if (StringUtils.isNotEmpty(enclosingClassName)) {
        //handle innerClasses
        symbol = new Symbol.TypeSymbol(flags, Convert.innerClassName(shortName), getClassSymbol(Convert.bytecodeName(packageName + "." + enclosingClassName)));
      } else {
        symbol = new Symbol.TypeSymbol(flags, shortName, enterPackage(packageName));
      }
      symbol.members = new Scope(symbol);
      symbol.completer = this;
      classes.put(flatName, symbol);
    }
    return symbol;
  }

  /**
   * Load a class from bytecode.
   *
   * @param env
   * @param fullname class name.
   * @return symbolNotFound if the class was not resolved.
   */
  public Symbol loadClass(Resolve.Env env, Symbol owner, String fullname, String name) {
    InputStream inputStream = inputStreamFor(fullname);
    if (inputStream == null) {
      return new Resolve.SymbolNotFound();
    }
    Closeables.closeQuietly(inputStream);
    return getClassSymbol(Convert.bytecodeName(fullname));
  }

  public Symbol.PackageSymbol enterPackage(String fullname) {
    if (StringUtils.isBlank(fullname)) {
      return symbols.defaultPackage;
    }
    Symbol.PackageSymbol result = packages.get(fullname);
    if (result == null) {
      result = new Symbol.PackageSymbol(Convert.shortName(fullname), enterPackage(Convert.packagePart(fullname)));
      result.completer = this;
      packages.put(fullname, result);
    }
    return result;
  }

  public void done() {
    if(classLoader!=null && classLoader instanceof Closeable) {
      Closeables.closeQuietly((Closeable) classLoader);
    }
  }

  private class BytecodeVisitor extends ClassVisitor {

    private final Symbol.TypeSymbol classSymbol;

    /**
     * Name of current class in a format as it appears in bytecode, i.e. "org/example/MyClass$InnerClass".
     */
    private String className;

    private BytecodeVisitor(Symbol.TypeSymbol classSymbol) {
      super(Opcodes.ASM5);
      this.classSymbol = classSymbol;
    }

    @Override
    public void visit(int version, int flags, String name, @Nullable String signature, @Nullable String superName, @Nullable String[] interfaces) {
      Preconditions.checkState(name.endsWith(classSymbol.name));
      className = name;
      classSymbol.flags = flags;
      classSymbol.members = new Scope(classSymbol);
      if (superName == null) {
        Preconditions.checkState("java/lang/Object".equals(className));
      } else {
        ((Type.ClassType) classSymbol.type).supertype = getClassSymbol(superName).type;
      }
      ((Type.ClassType) classSymbol.type).interfaces = getCompletedClassSymbolsType(interfaces);
    }

    @Override
    public void visitSource(@Nullable String source, @Nullable String debug) {
      // nop
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
      throw new UnsupportedOperationException();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void visitAttribute(Attribute attr) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void visitInnerClass(String name, @Nullable String outerName, @Nullable String innerName, int flags) {
      if (!isSynthetic(flags)) {
        // TODO what about flags?
        if (className.equals(outerName)) {
          defineInnerClass(name, flags);
        } else if (className.equals(name)) {
          defineOuterClass(outerName, innerName, flags);
        } else {
          // TODO wtf?
        }
      }
    }

    /**
     * Invoked when current class classified as outer class of some inner class.
     * Completes inner class.
     */
    private void defineInnerClass(String bytecodeName, int flags) {
      Symbol.TypeSymbol innerClass = getClassSymbol(bytecodeName, flags);
      Preconditions.checkState(innerClass.owner == classSymbol);
      classSymbol.members.enter(innerClass);
    }

    /**
     * Invoked when current class classified as inner class.
     * Completes outer class. Owner of inner classes - is an outer class.
     */
    private void defineOuterClass(String outerName, String innerName, int flags) {
      Symbol.TypeSymbol outerClassSymbol = getClassSymbol(outerName, flags);
      classSymbol.name = innerName;
      classSymbol.owner = outerClassSymbol;
      outerClassSymbol.members.enter(classSymbol);
    }

    @Override
    public FieldVisitor visitField(int flags, String name, String desc, @Nullable String signature, @Nullable Object value) {
      if (!isSynthetic(flags)) {
        //Flags from asm lib are defined in Opcodes class and map to flags defined in Flags class
        Symbol.VariableSymbol symbol = new Symbol.VariableSymbol(flags, name, convertAsmType(org.objectweb.asm.Type.getType(desc)), classSymbol);
        classSymbol.members.enter(symbol);
      }
      // TODO implement FieldVisitor?
      return null;
    }

    @Override
    public MethodVisitor visitMethod(int flags, String name, String desc, @Nullable String signature, @Nullable String[] exceptions) {
      if (!isSynthetic(flags)) {
        Type.MethodType type = new Type.MethodType(
            convertAsmTypes(org.objectweb.asm.Type.getArgumentTypes(desc)),
            convertAsmType(org.objectweb.asm.Type.getReturnType(desc)),
            exceptions == null ? ImmutableList.<Type>of() : getCompletedClassSymbolsType(exceptions),
            classSymbol
        );
        Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(flags, name, type, classSymbol);
        classSymbol.members.enter(methodSymbol);
      }
      // TODO implement MethodVisitor?
      return null;
    }

    private List<Type> convertAsmTypes(org.objectweb.asm.Type[] asmTypes) {
      ImmutableList.Builder<Type> result = ImmutableList.builder();
      for (org.objectweb.asm.Type asmType : asmTypes) {
        result.add(convertAsmType(asmType));
      }
      return result.build();
    }

    private Type convertAsmType(org.objectweb.asm.Type asmType) {
      Type result;
      switch (asmType.getSort()) {
        case org.objectweb.asm.Type.OBJECT:
          result = getClassSymbol(asmType.getInternalName()).type;
          break;
        case org.objectweb.asm.Type.BYTE:
          result = symbols.byteType;
          break;
        case org.objectweb.asm.Type.CHAR:
          result = symbols.charType;
          break;
        case org.objectweb.asm.Type.SHORT:
          result = symbols.shortType;
          break;
        case org.objectweb.asm.Type.INT:
          result = symbols.intType;
          break;
        case org.objectweb.asm.Type.LONG:
          result = symbols.longType;
          break;
        case org.objectweb.asm.Type.FLOAT:
          result = symbols.floatType;
          break;
        case org.objectweb.asm.Type.DOUBLE:
          result = symbols.doubleType;
          break;
        case org.objectweb.asm.Type.BOOLEAN:
          result = symbols.booleanType;
          break;
        case org.objectweb.asm.Type.ARRAY:
          result = new Type.ArrayType(convertAsmType(asmType.getElementType()), symbols.arrayClass);
          break;
        case org.objectweb.asm.Type.VOID:
          // FIXME
          result = null;
          break;
        default:
          throw new IllegalArgumentException(asmType.toString());
      }
      return result;
    }

    /**
     * If at this point there is no owner of current class, then this is a top-level class,
     * because outer classes always will be completed before inner classes - see {@link #defineOuterClass(String, String, int)}.
     * Owner of top-level classes - is a package.
     */
    @Override
    public void visitEnd() {
      if (classSymbol.owner == null) {
        String flatName = className.replace('/', '.');
        classSymbol.name = flatName.substring(flatName.lastIndexOf('.') + 1);
        classSymbol.owner = enterPackage(flatName);
        Symbol.PackageSymbol owner = (Symbol.PackageSymbol) classSymbol.owner;
        if (owner.members == null) {
          // package was without classes so far
          owner.members = new Scope(owner);
        }
        owner.members.enter(classSymbol);
      }
    }

    /**
     * Used to complete types of interfaces.
     *
     * @param bytecodeNames bytecodeNames of interfaces to complete.
     * @return List of the types of those interfaces.
     */
    private List<Type> getCompletedClassSymbolsType(String[] bytecodeNames) {
      ImmutableList.Builder<Type> types = ImmutableList.builder();
      for (String bytecodeName : bytecodeNames) {
        types.add(getClassSymbol(bytecodeName).type);
      }
      return types.build();
    }

  }

  static boolean isSynthetic(int flags) {
    // TODO Flags.BRIDGE
    return (flags & Flags.SYNTHETIC) == Flags.SYNTHETIC;
  }

}
