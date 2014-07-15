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
package org.sonar.java.checks;

import org.sonar.java.bytecode.asm.AsmResource;

import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceFile;

@Rule(
  key = ThreadRunCheck.RULE_KEY,
  priority = Priority.CRITICAL,
  tags={"multithreading"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class ThreadRunCheck extends BytecodeVisitor {

  public static final String RULE_KEY = "S1217";

  private static final String THREAD_CLASS = "java/lang/Thread";
  private static final String RUNNABLE_CLASS = "java/lang/Runnable";

  private AsmClass asmClass;

  @Override
  public void visitClass(AsmClass asmClass) {
    this.asmClass = asmClass;
  }

  @Override
  public void visitEdge(AsmEdge edge) {
    if (notSuperCall(edge) && isCallToRun(edge) && isThreadOrRunnable(edge.getTargetAsmClass())) {
      SourceFile sourceFile = getSourceFile(asmClass);
      CheckMessage message = new CheckMessage(this, "Call the method Thread.start() to execute the content of the run() method in a dedicated thread.");
      message.setLine(edge.getSourceLineNumber());
      sourceFile.log(message);
    }
  }

  private boolean notSuperCall(AsmEdge edge) {
    return !(isCallFromRun(edge) && isCallToRun(edge) && isParentFromEqualsTarget(edge));
  }

  private boolean isParentFromEqualsTarget(AsmEdge edge) {
    boolean result = false;
    AsmClass superclass = edge.getFrom().getParent().getSuperClass();
    if (superclass != null) {
      result = superclass.equals(edge.getTargetAsmClass());
    }
    return result;
  }

  private static boolean isCallFromRun(AsmEdge edge) {
    return isCallRun(edge.getFrom());
  }

  private static boolean isCallToRun(AsmEdge edge) {
    return isCallRun(edge.getTo());
  }

  private static boolean isCallRun(AsmResource resource) {
    return resource instanceof AsmMethod && "run()V".equals(((AsmMethod) resource).getKey());
  }


  private static boolean isThreadOrRunnable(AsmClass asmClass) {
    AsmClass currentClass = asmClass;
    while (currentClass != null) {
      if (THREAD_CLASS.equals(currentClass.getInternalName()) ||
        RUNNABLE_CLASS.equals(currentClass.getInternalName())) {
        return true;
      }

      for (AsmClass implementedInterface : currentClass.getImplementedInterfaces()) {
        if (RUNNABLE_CLASS.equals(implementedInterface.getInternalName())) {
          return true;
        }
      }

      currentClass = currentClass.getSuperClass();
    }

    return false;
  }

  @Override
  public String toString() {
    return RULE_KEY + " rule";
  }

}
