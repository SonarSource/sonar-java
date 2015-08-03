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

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.squidbridge.annotations.NoSqale;
import org.sonar.squidbridge.annotations.RuleTemplate;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceFile;

import java.util.Map;

@Rule(
  key = "ArchitecturalConstraint",
  name = "Architectural constraints should be followed",
  priority = Priority.MAJOR)
@RuleTemplate
@NoSqale
public class ArchitectureCheck extends BytecodeVisitor {

  @RuleProperty(description = "Optional. If this property is not defined, all classes should adhere to this constraint. Ex : **.web.**")
  private String fromClasses = "";

  @RuleProperty(description = "Mandatory. Ex : java.util.Vector, java.util.Hashtable, java.util.Enumeration")
  private String toClasses = "";

  private WildcardPattern[] fromPatterns;
  private WildcardPattern[] toPatterns;
  private AsmClass asmClass;
  private Map<String, CheckMessage> internalNames;

  public String getFromClasses() {
    return fromClasses;
  }

  public void setFromClasses(String patterns) {
    this.fromClasses = patterns;
  }

  public String getToClasses() {
    return toClasses;
  }

  public void setToClasses(String patterns) {
    this.toClasses = patterns;
  }

  @Override
  public void visitClass(AsmClass asmClass) {
    String nameAsmClass = asmClass.getInternalName();
    if (WildcardPattern.match(getFromPatterns(), nameAsmClass)) {
      this.asmClass = asmClass;
      this.internalNames = Maps.newHashMap();
    } else {
      this.asmClass = null;
    }
  }

  @Override
  public void leaveClass(AsmClass asmClass) {
    if (this.asmClass != null) {
      for (CheckMessage message : internalNames.values()) {
        SourceFile sourceFile = getSourceFile(asmClass);
        sourceFile.log(message);
      }
    }
  }

  @Override
  public void visitEdge(AsmEdge edge) {
    if (asmClass != null && edge != null) {
      String internalNameTargetClass = edge.getTargetAsmClass().getInternalName();
      if (!internalNames.containsKey(internalNameTargetClass)) {
        if (WildcardPattern.match(getToPatterns(), internalNameTargetClass)) {
          int sourceLineNumber = getSourceLineNumber(edge);
          logMessage(asmClass.getInternalName(), internalNameTargetClass, sourceLineNumber);
        }
      } else {
        int sourceLineNumber = getSourceLineNumber(edge);
        // we log only first occurrence with non-zero line number if exists
        Integer line = internalNames.get(internalNameTargetClass).getLine();
        if ((line == null || line == 0) && sourceLineNumber != 0) {
          logMessage(asmClass.getInternalName(), internalNameTargetClass, sourceLineNumber);
        }
      }
    }
  }

  private int getSourceLineNumber(AsmEdge edge) {
    if (edge.getSourceLineNumber() == 0 && edge.getFrom() instanceof AsmMethod) {
      int line = getMethodLineNumber((AsmMethod) edge.getFrom());
      if (line > 0) {
        return line;
      }
    }
    return edge.getSourceLineNumber();
  }

  private void logMessage(String fromClass, String toClass, int sourceLineNumber) {
    CheckMessage message = new CheckMessage(this, fromClass + " must not use " + toClass);
    if(sourceLineNumber != 0) {
      message.setLine(sourceLineNumber);
    }
    internalNames.put(toClass, message);
  }

  private WildcardPattern[] getFromPatterns() {
    if (fromPatterns == null) {
      fromPatterns = PatternUtils.createPatterns(StringUtils.defaultIfEmpty(fromClasses, "**"));
    }
    return fromPatterns;
  }

  private WildcardPattern[] getToPatterns() {
    if (toPatterns == null) {
      toPatterns = PatternUtils.createPatterns(toClasses);
    }
    return toPatterns;
  }
}
