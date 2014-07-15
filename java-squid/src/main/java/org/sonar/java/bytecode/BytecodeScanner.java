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
package org.sonar.java.bytecode;

import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmClassProvider;
import org.sonar.java.bytecode.asm.AsmClassProvider.DETAIL_LEVEL;
import org.sonar.java.bytecode.asm.AsmClassProviderImpl;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.squidbridge.api.CodeScanner;
import org.sonar.squidbridge.api.CodeVisitor;
import org.sonar.squidbridge.api.SourceClass;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.indexer.QueryByType;
import org.sonar.squidbridge.indexer.SquidIndex;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class BytecodeScanner extends CodeScanner<BytecodeVisitor> {

  private final SquidIndex indexer;

  public BytecodeScanner(SquidIndex indexer) {
    this.indexer = indexer;
  }

  public BytecodeScanner scan(Collection<File> bytecodeFilesOrDirectories) {
    ClassLoader classLoader = ClassLoaderBuilder.create(bytecodeFilesOrDirectories);
    Collection<SourceCode> classes = indexer.search(new QueryByType(SourceClass.class));
    scanClasses(classes, new AsmClassProviderImpl(classLoader));
    // TODO unchecked cast
    ((SquidClassLoader) classLoader).close();
    return this;
  }

  public BytecodeScanner scanDirectory(File bytecodeDirectory) {
    return scan(Arrays.asList(bytecodeDirectory));
  }

  protected BytecodeScanner scanClasses(Collection<SourceCode> classes, AsmClassProvider classProvider) {
    loadByteCodeInformation(classes, classProvider);
    linkVirtualMethods(classes, classProvider);
    notifyBytecodeVisitors(classes, classProvider);
    return this;
  }

  private void linkVirtualMethods(Collection<SourceCode> classes, AsmClassProvider classProvider) {
    VirtualMethodsLinker linker = new VirtualMethodsLinker();
    for (SourceCode sourceCode : classes) {
      AsmClass asmClass = classProvider.getClass(sourceCode.getKey(), DETAIL_LEVEL.STRUCTURE_AND_CALLS);
      for (AsmMethod method : asmClass.getMethods()) {
        linker.process(method);
      }
    }
  }

  private void notifyBytecodeVisitors(Collection<SourceCode> classes, AsmClassProvider classProvider) {
    BytecodeVisitor[] visitorArray = getVisitors().toArray(new BytecodeVisitor[getVisitors().size()]);
    for (SourceCode sourceCode : classes) {
      AsmClass asmClass = classProvider.getClass(sourceCode.getKey(), DETAIL_LEVEL.STRUCTURE_AND_CALLS);
      BytecodeVisitorNotifier visitorNotifier = new BytecodeVisitorNotifier(asmClass, visitorArray);
      visitorNotifier.notifyVisitors(indexer);
    }
  }

  private void loadByteCodeInformation(Collection<SourceCode> classes, AsmClassProvider classProvider) {
    for (SourceCode sourceCode : classes) {
      classProvider.getClass(sourceCode.getKey(), DETAIL_LEVEL.STRUCTURE_AND_CALLS);
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
