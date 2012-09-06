/*
 * Sonar Java
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

@Rule(key = "MaximumLackOfCohesionOfMethods", priority = Priority.MAJOR)
public class LCOM4Check extends BytecodeVisitor {

  public static final int DEFAULT_MAX = 1;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX)
  private Integer max = DEFAULT_MAX;

  @Override
  public void leaveClass(AsmClass asmClass) {
    SourceClass sourceClass = getSourceClass(asmClass);
    int lcom4 = sourceClass.getInt(Metric.LCOM4);
    if (lcom4 > max) {
      CheckMessage message = new CheckMessage(this, "This class has an LCOM4 of " + lcom4 + ", which is greater than " + max + " authorized.");
      message.setLine(sourceClass.getStartAtLine());
      message.setCost(lcom4 - max);
      sourceClass.getParent(SourceFile.class).log(message);
    }
  }

  public void setMax(int max) {
    this.max = max;
  }

}
