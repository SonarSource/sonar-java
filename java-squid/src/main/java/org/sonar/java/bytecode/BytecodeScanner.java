/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.bytecode;

import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmClassProvider;
import org.sonar.java.bytecode.asm.AsmClassProvider.DETAIL_LEVEL;
import org.sonar.java.bytecode.asm.AsmClassProviderImpl;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.squidbridge.api.AnalysisException;
import org.sonar.squidbridge.api.CodeScanner;
import org.sonar.squidbridge.api.CodeVisitor;
import org.sonar.squidbridge.indexer.SquidIndex;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

public class BytecodeScanner extends CodeScanner<BytecodeVisitor> {

  private final SquidIndex indexer;
  private JavaResourceLocator javaResourceLocator;

  public BytecodeScanner(SquidIndex indexer, JavaResourceLocator javaResourceLocator) {
    this.indexer = indexer;
    this.javaResourceLocator = javaResourceLocator;
  }

  public BytecodeScanner scan(Collection<File> bytecodeFilesOrDirectories) {
    ClassLoader classLoader = ClassLoaderBuilder.create(bytecodeFilesOrDirectories);
    scanClasses(javaResourceLocator.classKeys(), new AsmClassProviderImpl(classLoader));
    // TODO unchecked cast
    ((SquidClassLoader) classLoader).close();
    return this;
  }

  protected BytecodeScanner scanClasses(Collection<String> classes, AsmClassProvider classProvider) {
    loadByteCodeInformation(classes, classProvider);
    linkVirtualMethods(classes, classProvider);
    notifyBytecodeVisitors(classes, classProvider);
    return this;
  }

  private static void linkVirtualMethods(Collection<String> keys, AsmClassProvider classProvider) {
    VirtualMethodsLinker linker = new VirtualMethodsLinker();
    for (String key : keys) {
      AsmClass asmClass = classProvider.getClass(key, DETAIL_LEVEL.STRUCTURE_AND_CALLS);
      for (AsmMethod method : asmClass.getMethods()) {
        linker.process(method);
      }
    }
  }

  private void notifyBytecodeVisitors(Collection<String> keys, AsmClassProvider classProvider) {
    BytecodeVisitor[] visitorArray = getVisitors().toArray(new BytecodeVisitor[getVisitors().size()]);
    for (String key : keys) {
      try {
        AsmClass asmClass = classProvider.getClass(key, DETAIL_LEVEL.STRUCTURE_AND_CALLS);
        BytecodeVisitorNotifier visitorNotifier = new BytecodeVisitorNotifier(asmClass, visitorArray);
        visitorNotifier.notifyVisitors(indexer, javaResourceLocator);
      } catch (Exception exception) {
        throw new AnalysisException("Unable to analyze .class file " + key, exception);
      }
    }
  }

  private static void loadByteCodeInformation(Collection<String> keys, AsmClassProvider classProvider) {
    for (String key : keys) {
      classProvider.getClass(key, DETAIL_LEVEL.STRUCTURE_AND_CALLS);
    }
  }

  @Override
  public Collection<Class<? extends BytecodeVisitor>> getVisitorClasses() {
    return Collections.emptyList();
  }

  @Override
  public void accept(CodeVisitor visitor) {
    if (visitor instanceof BytecodeVisitor) {
      super.accept(visitor);
    }
  }

}
