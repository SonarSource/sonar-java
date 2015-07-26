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

import com.google.common.collect.Sets;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceFile;

import java.util.List;
import java.util.Set;

@Rule(
  key = "RedundantThrowsDeclarationCheck",
  name = "Throws declarations should not be superfluous",
  tags = {"error-handling", "security"},
  priority = Priority.MINOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class RedundantThrowsDeclarationCheck extends BytecodeVisitor {

  private AsmClass asmClass;

  @Override
  public void visitClass(AsmClass asmClass) {
    this.asmClass = asmClass;
  }

  @Override
  public void visitMethod(AsmMethod asmMethod) {
    int line = getMethodLineNumber(asmMethod);
    if (line > 0) {
      Set<String> reportedExceptions = Sets.newHashSet();

      List<AsmClass> thrownClasses = asmMethod.getThrows();
      for (AsmClass thrownClass : thrownClasses) {
        String thrownClassName = thrownClass.getDisplayName();

        if (!reportedExceptions.contains(thrownClassName)) {
          String issueMessage = getIssueMessage(thrownClasses, thrownClass);

          if (issueMessage != null) {
            reportedExceptions.add(thrownClassName);

            CheckMessage message = new CheckMessage(this, issueMessage);
            message.setLine(line);
            SourceFile file = getSourceFile(asmClass);
            file.log(message);
          }
        }
      }
    }
  }

  private static String getIssueMessage(List<AsmClass> thrownClasses, AsmClass thrownClass) {
    String thrownClassName = thrownClass.getDisplayName();
    if (isSubClassOfAny(thrownClass, thrownClasses)) {
      return "Remove the declaration of thrown exception '" + thrownClassName + "' which is a subclass of another one.";
    } else if (isSubClassOfRuntimeException(thrownClass)) {
      return "Remove the declaration of thrown exception '" + thrownClassName + "' which is a runtime exception.";
    } else if (isDeclaredMoreThanOnce(thrownClass, thrownClasses)) {
      return "Remove the redundant '" + thrownClassName + "' thrown exception declaration(s).";
    }
    return null;
  }

  private static boolean isDeclaredMoreThanOnce(AsmClass thrownClass, List<AsmClass> thrownClassCandidates) {
    int matches = 0;

    for (AsmClass thrownClassCandidate : thrownClassCandidates) {
      if (thrownClass.equals(thrownClassCandidate)) {
        matches++;
      }
    }

    return matches > 1;
  }

  private static boolean isSubClassOfAny(AsmClass thrownClass, List<AsmClass> thrownClassCandidates) {
    for (AsmClass current = thrownClass.getSuperClass(); current != null; current = current.getSuperClass()) {
      for (AsmClass thrownClassCandidate : thrownClassCandidates) {
        if (current.equals(thrownClassCandidate)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isSubClassOfRuntimeException(AsmClass thrownClass) {
    for (AsmClass current = thrownClass; current != null; current = current.getSuperClass()) {
      if ("java/lang/RuntimeException".equals(current.getInternalName())) {
        return true;
      }
    }
    return false;
  }

}
