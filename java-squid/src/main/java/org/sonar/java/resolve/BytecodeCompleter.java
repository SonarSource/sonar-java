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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
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
import org.objectweb.asm.TypePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.bytecode.ClassLoaderBuilder;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BytecodeCompleter implements Symbol.Completer {

  private static final Logger LOG = LoggerFactory.getLogger(BytecodeCompleter.class);

  private static final int ACCEPTABLE_BYTECODE_FLAGS = Flags.ACCESS_FLAGS |
      Flags.INTERFACE | Flags.ANNOTATION | Flags.ENUM |
      Flags.STATIC | Flags.FINAL | Flags.SYNCHRONIZED | Flags.VOLATILE | Flags.TRANSIENT | Flags.NATIVE |
      Flags.ABSTRACT | Flags.STRICTFP;

  private Symbols symbols;
  private final List<File> projectClasspath;

  /**
   * Indexed by flat name.
   */
  private final Map<String, Symbol.TypeSymbol> classes = new HashMap<String, Symbol.TypeSymbol>();
  private final Map<String, Symbol.PackageSymbol> packages = new HashMap<String, Symbol.PackageSymbol>();

  private ClassLoader classLoader;

  public BytecodeCompleter(List<File> projectClasspath) {
    this.projectClasspath = projectClasspath;
  }

  public void init(Symbols symbols) {
    this.symbols = symbols;
  }

  public Symbol.TypeSymbol registerClass(Symbol.TypeSymbol classSymbol) {
    String flatName = formFullName(classSymbol);
    Preconditions.checkState(!classes.containsKey(flatName), "Registering class 2 times : "+flatName);
    classes.put(flatName, classSymbol);
    return classSymbol;
  }

  @Override
  public void complete(Symbol symbol) {
    LOG.debug("Completing symbol : " + symbol.name);
    String bytecodeName = formFullName(symbol);
    Symbol.TypeSymbol classSymbol = getClassSymbol(bytecodeName);
    Preconditions.checkState(classSymbol == symbol);

    InputStream inputStream = null;
    ClassReader classReader = null;
    try {
      inputStream = inputStreamFor(bytecodeName);
      classReader = new ClassReader(inputStream);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    } finally {
      Closeables.closeQuietly(inputStream);
    }
    if (classReader != null) {
      classReader.accept(new BytecodeVisitor((Symbol.TypeSymbol) symbol), ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
    }
  }

  private InputStream inputStreamFor(String fullname) {
    return getClassLoader().getResourceAsStream(Convert.bytecodeName(fullname) + ".class");
  }

  private ClassLoader getClassLoader() {
    if (classLoader == null) {
      classLoader = ClassLoaderBuilder.create(projectClasspath);
    }
    return classLoader;
  }

  public String formFullName(Symbol symbol) {
    return formFullName(symbol.name, symbol.owner);
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

  @VisibleForTesting
  Symbol.TypeSymbol getClassSymbol(String bytecodeName) {
    return getClassSymbol(bytecodeName, 0);
  }

  // FIXME(Godin): or parameter must be renamed, or should not receive flat name, in a former case - first transformation in this method seems useless
  private Symbol.TypeSymbol getClassSymbol(String bytecodeName, int flags) {
    String flatName = Convert.flatName(bytecodeName);
    Symbol.TypeSymbol symbol = classes.get(flatName);
    if (symbol == null) {
      String shortName = Convert.shortName(flatName);
      String packageName = Convert.packagePart(flatName);
      String enclosingClassName = Convert.enclosingClassName(shortName);
      if (StringUtils.isNotEmpty(enclosingClassName)) {
        //handle innerClasses
        symbol = new Symbol.TypeSymbol(filterBytecodeFlags(flags), Convert.innerClassName(shortName), getClassSymbol(Convert.fullName(packageName,enclosingClassName)));
      } else {
        symbol = new Symbol.TypeSymbol(filterBytecodeFlags(flags), shortName, enterPackage(packageName));
      }
      symbol.members = new Scope(symbol);

      // (Godin): IOException will happen without this condition in case of missing class:
      if (getClassLoader().getResource(Convert.bytecodeName(flatName) + ".class") != null) {
        symbol.completer = this;
      } else {
        LOG.error("Class not found: " + bytecodeName);
        // TODO(Godin): why only interfaces, but not supertype for example?
        ((Type.ClassType) symbol.type).interfaces = ImmutableList.of();
      }

      classes.put(flatName, symbol);
    }
    return symbol;
  }

  private int filterBytecodeFlags(int flags) {
    return flags & ACCEPTABLE_BYTECODE_FLAGS;
  }

  /**
   * <b>Note:</b> Attempt to find something like "java.class" on case-insensitive file system can result in unwanted loading of "JAVA.class".
   * This method performs check of class name within file in order to avoid such situation.
   * This is definitely not the best solution in terms of performance, but acceptable for now.
   *
   * @return symbol for requested class, if corresponding class file exists, and {@link Resolve.SymbolNotFound} otherwise
   */
  // TODO(Godin): Method name is misleading because of lazy loading.
  public Symbol loadClass(String fullname) {
    // TODO(Godin): avoid unnecessary checks of the same class

    // TODO(Godin): pull out conversion of name from the next method to avoid unnecessary conversion afterwards:
    InputStream inputStream = inputStreamFor(fullname);
    String bytecodeName = Convert.bytecodeName(fullname);

    if (inputStream == null) {
      return new Resolve.SymbolNotFound();
    }

    try {
      ClassReader classReader = new ClassReader(inputStream);
      String className = classReader.getClassName();
      if (!className.equals(bytecodeName)) {
        return new Resolve.SymbolNotFound();
      }
    } catch (IOException e) {
      throw Throwables.propagate(e);
    } finally {
      Closeables.closeQuietly(inputStream);
    }

    return getClassSymbol(fullname);
  }

  public Symbol.PackageSymbol enterPackage(String fullname) {
    if (StringUtils.isBlank(fullname)) {
      return symbols.defaultPackage;
    }
    Symbol.PackageSymbol result = packages.get(fullname);
    if (result == null) {
      result = new Symbol.PackageSymbol(fullname, symbols.defaultPackage);
      packages.put(fullname, result);
    }
    return result;
  }

  public void done() {
    if (classLoader != null && classLoader instanceof Closeable) {
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

    private Symbol.TypeSymbol getClassSymbol(String bytecodeName) {
      return BytecodeCompleter.this.getClassSymbol(Convert.flatName(bytecodeName));
    }

    private Symbol.TypeSymbol getClassSymbol(String bytecodeName, int flags) {
      return BytecodeCompleter.this.getClassSymbol(Convert.flatName(bytecodeName), flags);
    }

    @Override
    public void visit(int version, int flags, String name, @Nullable String signature, @Nullable String superName, @Nullable String[] interfaces) {
      Preconditions.checkState(name.endsWith(classSymbol.name), "Name : '"+name+"' should ends with "+classSymbol.name);
      Preconditions.checkState(!isSynthetic(flags), name+" is synthetic");
      className = name;
      classSymbol.flags = filterBytecodeFlags(flags);
      classSymbol.members = new Scope(classSymbol);
      if (superName == null) {
        Preconditions.checkState("java/lang/Object".equals(className), "superName must be null only for java/lang/Object, but not for " + className);
        // TODO(Godin): what about interfaces and annotations
      } else {
        ((Type.ClassType) classSymbol.type).supertype = getClassSymbol(superName).type;
      }
      ((Type.ClassType) classSymbol.type).interfaces = getCompletedClassSymbolsType(interfaces);
    }

    @Override
    public void visitSource(@Nullable String source, @Nullable String debug) {
      throw new IllegalStateException();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * In other words must be called only for anonymous classes or named classes declared within methods,
     * which must not be processed by {@link BytecodeCompleter}, therefore this method always throws {@link java.lang.IllegalStateException}.
     *
     * @throws java.lang.IllegalStateException always
     */
    @Override
    public void visitOuterClass(String owner, String name, String desc) {
      throw new IllegalStateException();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      // (Godin): can return AnnotationVisitor to read annotations
      return null;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
      // (Godin): can return AnnotationVisitor to read annotations
      return null;
    }

    @Override
    public void visitAttribute(Attribute attr) {
      // skip non standard attributes
    }

    @Override
    public void visitInnerClass(String name, @Nullable String outerName, @Nullable String innerName, int flags) {
      if (!isSynthetic(flags)) {
        // TODO what about flags?
        if (innerName == null) {
          // anonymous class
        } else if (outerName == null) {
          // named class declared within method
        } else if (className.equals(outerName)) {
          defineInnerClass(name, flags);
        } else if (className.equals(name)) {
          defineOuterClass(outerName, innerName, flags);
        } else {
          // FIXME(Godin): for example if loading started from "C1.C2.C3" in case of
          // class C1 { class C2 { class C3 { } } }
          // then name="C1$C2", outerName="C1" and innerName="C3"
        }
      }
    }

    /**
     * Invoked when current class classified as outer class of some inner class.
     * Adds inner class as member.
     */
    private void defineInnerClass(String bytecodeName, int flags) {
      Symbol.TypeSymbol innerClass = getClassSymbol(bytecodeName, flags);
      Preconditions.checkState(innerClass.owner == classSymbol, "Innerclass : "+innerClass.owner.getName()+" and classSymbol : "+classSymbol.getName()+" are not the same.");
      classSymbol.members.enter(innerClass);
    }

    /**
     * Invoked when current class classified as inner class.
     * Owner of inner classes - is some outer class,
     * which is either already completed, and thus already has this inner class as member,
     * either will be completed by {@link BytecodeCompleter}, and thus will have this inner class as member (see {@link #defineInnerClass(String, int)}).
     */
    private void defineOuterClass(String outerName, String innerName, int flags) {
      Symbol.TypeSymbol outerClassSymbol = getClassSymbol(outerName, flags);
      Preconditions.checkState(outerClassSymbol.completer == null || outerClassSymbol.completer instanceof BytecodeCompleter);
      classSymbol.name = innerName;
      classSymbol.owner = outerClassSymbol;
    }

    @Override
    public FieldVisitor visitField(int flags, String name, String desc, @Nullable String signature, @Nullable Object value) {
      Preconditions.checkNotNull(name);
      Preconditions.checkNotNull(desc);
      if (!isSynthetic(flags)) {
        //Flags from asm lib are defined in Opcodes class and map to flags defined in Flags class
        Symbol.VariableSymbol symbol = new Symbol.VariableSymbol(filterBytecodeFlags(flags), name, convertAsmType(org.objectweb.asm.Type.getType(desc)), classSymbol);
        classSymbol.members.enter(symbol);
      }
      // (Godin): can return FieldVisitor to read annotations
      return null;
    }

    @Override
    public MethodVisitor visitMethod(int flags, String name, String desc, @Nullable String signature, @Nullable String[] exceptions) {
      Preconditions.checkNotNull(name);
      Preconditions.checkNotNull(desc);
      if (!isSynthetic(flags)) {
        Preconditions.checkState((flags & Opcodes.ACC_BRIDGE) == 0, "bridge method not marked as synthetic in class " + className);
        // TODO(Godin): according to JVMS 4.7.24 - parameter can be marked as synthetic
        Type.MethodType type = new Type.MethodType(
            convertAsmTypes(org.objectweb.asm.Type.getArgumentTypes(desc)),
            convertAsmType(org.objectweb.asm.Type.getReturnType(desc)),
            getCompletedClassSymbolsType(exceptions),
            classSymbol
        );
        Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(filterBytecodeFlags(flags), name, type, classSymbol);
        classSymbol.members.enter(methodSymbol);
      }
      // (Godin): can return MethodVisitor to read annotations
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
          result = symbols.voidType;
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

    private List<Type> getCompletedClassSymbolsType(@Nullable String[] bytecodeNames) {
      if (bytecodeNames == null) {
        return ImmutableList.of();
      }
      ImmutableList.Builder<Type> types = ImmutableList.builder();
      for (String bytecodeName : bytecodeNames) {
        types.add(getClassSymbol(bytecodeName).type);
      }
      return types.build();
    }

  }

  /**
   * Compiler marks all artifacts not presented in the source code as {@link Flags#SYNTHETIC}.
   */
  static boolean isSynthetic(int flags) {
    return (flags & Flags.SYNTHETIC) != 0;
  }

}
