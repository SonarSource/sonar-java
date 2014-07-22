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
package org.sonar.plugins.java.bridges;

import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Violation;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceFile;

import java.util.Locale;
import java.util.Set;

public class ChecksBridge extends Bridge {

  @Override
  public void onFile(SourceFile squidFile, Resource sonarFile) {
    if (squidFile.hasCheckMessages()) {
      Set<CheckMessage> messages = squidFile.getCheckMessages();
      for (CheckMessage checkMessage : messages) {
        final ActiveRule rule;
        Object check = checkMessage.getCheck();
        if (check instanceof RuleKey) {
          // VisitorsBridge uses RuleKey
          RuleKey ruleKey = (RuleKey) check;
          rule = profile.getActiveRule(ruleKey.repository(), ruleKey.rule());
        } else {
          rule = checkFactory.getActiveRule(checkMessage.getCheck());
        }
        if (rule == null) {
          // rule not active
          continue;
        }
        Violation violation = Violation.create(rule, sonarFile);
        violation.setLineId(checkMessage.getLine());
        violation.setMessage(checkMessage.getText(Locale.ENGLISH));
        violation.setCost(checkMessage.getCost());
        context.saveViolation(violation, checkMessage.isBypassExclusion());
      }
      // Remove from memory:
      messages.clear();
    }
  }

}
