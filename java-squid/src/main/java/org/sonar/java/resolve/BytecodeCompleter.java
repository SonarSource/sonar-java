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


import com.google.common.collect.Lists;
import org.objectweb.asm.ClassReader;
import org.sonar.java.bytecode.ClassLoaderBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class BytecodeCompleter implements Symbol.Completer{

  public static List<File> PROJECT_CLASSPATH = Lists.newArrayList(new File("target/test-classes"), new File("target/classes"));
  private ClassLoader classLoader;

  @Override
  public void complete(Symbol symbol) {
    System.out.println("Completing symbol : "+symbol.name);
  }


  /**
   * Load a class from bytecode.
   * @param env
   * @param fullname class name.
   * @return symbolNotFound if the class was not resolved.
   */
  public Symbol loadClass(Resolve.Env env, Symbol owner, String fullname, String name) {
    if(classLoader == null ) {
      classLoader = ClassLoaderBuilder.create(PROJECT_CLASSPATH);
    }
    try {
      ClassReader asmReader = new ClassReader(classLoader.getResourceAsStream(fullname.replace('.', '/') + ".class"));
      Symbol.TypeSymbol type = new Symbol.TypeSymbol(Flags.PUBLIC, name, owner);
      System.out.println("==> "+fullname+" symbol as type found");
      type.members = new Scope(type);
//      ((Type.ClassType)type.type).interfaces = Arrays.asList(asmReader.getInterfaces());
//      ((Type.ClassType)type.type).supertype = new Symbol.TypeSymbol(Flags.PUBLIC, asmReader.getSuperName());
      type.completer = this;
      return type;
    } catch (IOException e) {
      return new Resolve.SymbolNotFound();
    }
  }
}
