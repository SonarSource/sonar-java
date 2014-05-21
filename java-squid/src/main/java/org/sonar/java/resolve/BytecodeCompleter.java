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
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import org.apache.commons.lang.StringUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.bytecode.ClassLoaderBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BytecodeCompleter implements Symbol.Completer{

  private static final Logger LOG = LoggerFactory.getLogger(BytecodeCompleter.class);
  public static List<File> PROJECT_CLASSPATH = Lists.newArrayList(new File("target/test-classes"), new File("target/classes"));
  public static Symbol.PackageSymbol defaultPackage = new Symbol.PackageSymbol(null, null);
  private ClassLoader classLoader;
  private Map<String, Symbol.TypeSymbol> classes = new HashMap<String, Symbol.TypeSymbol>();
  private Map<String, Symbol.PackageSymbol> packages = new HashMap<String, Symbol.PackageSymbol>();

  @Override
  public void complete(Symbol symbol) {
    LOG.debug("Completing symbol : " + symbol.name);
    String bytecodeName = formFullName(symbol.name, symbol.owner);
    InputStream inputStream = null;
    try {
      inputStream = inputStreamFor(bytecodeName);
      ClassReader classReader = new ClassReader(inputStream);
//      Symbol.TypeSymbol classSymbol = getClassSymbol(bytecodeName);
//      Preconditions.checkState(classSymbol == symbol);
      classReader.accept(new BytecodeVisitor((Symbol.TypeSymbol) symbol), 0);
    } catch (Exception e) {
      ((Type.ClassType) symbol.type).interfaces = Lists.newArrayList();
      LOG.error("Cannot complete type : " + bytecodeName + "  " + e.getMessage());
    } finally {
      Closeables.closeQuietly(inputStream);
    }
  }

  private InputStream inputStreamFor(String fullname) {
    if(classLoader == null ) {
      classLoader = ClassLoaderBuilder.create(PROJECT_CLASSPATH);
    }
    return classLoader.getResourceAsStream(Convert.bytecodeName(fullname) + ".class");
  }

  public String formFullName(String name, Symbol site) {
    String result = name;
    Symbol owner = site;
    while(owner!=null && owner.name!=null) {
      //Handle inner classes, if owner is a type, separate by $
      String separator = ".";
      if(owner.kind == Symbol.TYP) {
        separator = "$";
      }
      result = owner.name +separator+ result;

      owner = owner.owner();
    }
    return result;
  }

  public Symbol.TypeSymbol getClassSymbol(String bytecodeName) {
    String flatName = Convert.flatName(bytecodeName);
    Symbol.TypeSymbol symbol = classes.get(flatName);
    if (symbol == null) {
      // flags not specified
      String shortName = Convert.shortName(flatName);
      String packageName = Convert.packagePart(flatName);
      String enclosingClassName = Convert.enclosingClassName(shortName);
      if(StringUtils.isNotEmpty(enclosingClassName)) {
        //handle innerClasses
        symbol = new Symbol.TypeSymbol(0, Convert.innerClassName(shortName), getClassSymbol(Convert.bytecodeName(packageName+"."+enclosingClassName)));
      }else{
        symbol = new Symbol.TypeSymbol(0, shortName, enterPackage(packageName));
      }
      symbol.members = new Scope(symbol);
      symbol.completer = this;
      classes.put(flatName, symbol);
    }
    return symbol;
  }

  /**
   * Load a class from bytecode.
   * @param env
   * @param fullname class name.
   * @return symbolNotFound if the class was not resolved.
   */
  public Symbol loadClass(Resolve.Env env, Symbol owner, String fullname, String name) {
    try {
      ClassReader asmReader = new ClassReader(inputStreamFor(fullname));
      Symbol.TypeSymbol type = new Symbol.TypeSymbol(Flags.PUBLIC, name, owner);
      type.members = new Scope(type);
      type.completer = this;
      return type;
    } catch (IOException e) {
      return new Resolve.SymbolNotFound();
    }
  }

  public Symbol.PackageSymbol enterPackage(String fullname) {
    if(StringUtils.isBlank(fullname)) {
      return defaultPackage;
    }
    Symbol.PackageSymbol result = packages.get(fullname);
    if(result==null) {
      result = new Symbol.PackageSymbol(Convert.shortName(fullname), enterPackage(Convert.packagePart(fullname)));
      result.completer = this;
      packages.put(fullname, result);
    }
    return result;
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
    public void visit(int version, int flags, String name, String signature, String superName, String[] interfaces) {
      Preconditions.checkState(name.endsWith(classSymbol.name));
      className = name;
      classSymbol.flags = flags;
      classSymbol.members = new Scope(classSymbol);
      if (superName == null) {
        // TODO superName == null only for java/lang/Object?
        Preconditions.checkState("java/lang/Object".equals(className));
      } else {
        ((Type.ClassType)classSymbol.type).supertype = getCompletedClassSymbol(superName).type;
      }
      ((Type.ClassType)classSymbol.type).interfaces = getCompletedClassSymbolsType(interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
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
    public void visitInnerClass(String name, String outerName, String innerName, int flags) {
      if (!isSynthetic(flags)) {
        // TODO what about flags?
        if (className.equals(outerName)) {
          defineInnerClass(name);
        } else if (className.equals(name)) {
          defineOuterClass(outerName, innerName);
        } else {
          // TODO wtf?
        }
      }
    }

    /**
     * Invoked when current class classified as outer class of some inner class.
     * Completes inner class.
     */
    private void defineInnerClass(String bytecodeName) {
      Symbol.TypeSymbol innerClass = getCompletedClassSymbol(bytecodeName);
      Preconditions.checkState(innerClass.owner == classSymbol);
    }

    /**
     * Invoked when current class classified as inner class.
     * Completes outer class. Owner of inner classes - is an outer class.
     */
    private void defineOuterClass(String outerName, String innerName) {
      Symbol.TypeSymbol outerClassSymbol = getCompletedClassSymbol(outerName);
      classSymbol.name = innerName;
      classSymbol.owner = outerClassSymbol;
      outerClassSymbol.members.enter(classSymbol);
    }

    //TODO fields and methods

    /**
     * If at this point there is no owner of current class, then this is a top-level class,
     * because outer classes always will be completed before inner classes - see {@link #defineOuterClass(String, String)}.
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

    private Symbol.TypeSymbol getCompletedClassSymbol(String bytecodeName) {
      Symbol.TypeSymbol symbol = getClassSymbol(bytecodeName);
      symbol.complete();
      return symbol;
    }

    /**
     * Used to complete types of interfaces.
     * @param bytecodeNames bytecodeNames of interfaces to complete.
     * @return List of the types of those interfaces.
     */
    private List<Type> getCompletedClassSymbolsType(String[] bytecodeNames) {
      ImmutableList.Builder<Type> types = ImmutableList.builder();
      for (String bytecodeName : bytecodeNames) {
        types.add(getCompletedClassSymbol(bytecodeName).type);
      }
      return types.build();
    }

  }

  static boolean isSynthetic(int flags) {
    // TODO Flags.BRIDGE
    return (flags & Flags.SYNTHETIC) == Flags.SYNTHETIC;
  }

}
