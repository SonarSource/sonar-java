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
package org.sonar.java.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceFile;

@Rule(
  key = "CallToFileDeleteOnExitMethod",
  name = "\"deleteOnExit\" should not be used",
  tags = {"performance"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.MEMORY_EFFICIENCY)
@SqaleConstantRemediation("30min")
public class  CallToFileDeleteOnExitMethodCheck extends BytecodeVisitor {

  private AsmClass asmClass;

  @Override
  public void visitClass(AsmClass asmClass) {
    this.asmClass = asmClass;
  }

  @Override
  public void visitEdge(AsmEdge edge) {
    if (edge.getTo() instanceof AsmMethod) {
      AsmMethod targetMethod = (AsmMethod) edge.getTo();
      AsmClass targetClass = targetMethod.getParent();
      if ("java/io/File".equals(targetClass.getInternalName()) && "deleteOnExit()V".equals(targetMethod.getKey())) {
        SourceFile sourceFile = getSourceFile(asmClass);
        CheckMessage message = new CheckMessage(this, "Do not use method 'File#deleteOnExit()'.");
        message.setLine(edge.getSourceLineNumber());
        sourceFile.log(message);
      }
    }
  }

}
