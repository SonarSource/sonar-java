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

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.measures.Metric;

@Rule(key = DITCheck.RULE_KEY, priority = Priority.MAJOR)
public class DITCheck extends BytecodeVisitor {

  public static final String RULE_KEY = "MaximumInheritanceDepth";
  public static final int DEFAULT_MAX = 5;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX)
  private Integer max = DEFAULT_MAX;

  @Override
  public void visitClass(AsmClass asmClass) {
    SourceClass sourceClass = getSourceClass(asmClass);
    int dit = sourceClass.getInt(Metric.DIT);
    if (dit > max) {
      CheckMessage message = new CheckMessage(this, "This class has " + dit + " parents which is greater than " + max + " authorized.");
      message.setLine(sourceClass.getStartAtLine());
      message.setCost(dit - max);
      sourceClass.getParent(SourceFile.class).log(message);
    }
  }

  public void setMax(int max) {
    this.max = max;
  }

  @Override
  public String toString() {
    return RULE_KEY + " rule";
  }

}
