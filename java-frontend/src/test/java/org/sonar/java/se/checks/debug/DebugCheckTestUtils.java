/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.se.checks.debug;

import java.util.ArrayList;
import java.util.Collections;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.ExplodedGraphWalker;
import org.sonar.java.se.JavaCheckVerifier;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.plugins.java.api.JavaFileScannerContext;

public final class DebugCheckTestUtils {

  static void verifyIssuesWithMaxSteps(String sourcefile, SECheck check, int maxSteps) {
    BehaviorCache behaviorCache = new BehaviorCache(new SquidClassLoader(new ArrayList<>()));
    SymbolicExecutionVisitor sev = new SymbolicExecutionVisitor(Collections.singletonList(check), behaviorCache) {
      @Override
      protected ExplodedGraphWalker getWalker() {
        return new ExplodedGraphWalker(Collections.singletonList(check), behaviorCache, (SemanticModel) context.getSemanticModel()) {
          @Override
          protected int maxSteps() {
            return maxSteps;
          }
        };
      }

      @Override
      public void scanFile(JavaFileScannerContext context) {
        super.scanFile(context);
        // the check has been executed, but we still need to call the scan manually to report the issues
        check.scanFile(context);
      }
    };
    JavaCheckVerifier.verify(sourcefile, sev);
  }
}
