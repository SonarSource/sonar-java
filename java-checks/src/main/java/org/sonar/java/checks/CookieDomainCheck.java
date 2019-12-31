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
package org.sonar.java.checks;

import com.google.common.net.InternetDomainName;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S3331")
public class CookieDomainCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition("javax.servlet.http.Cookie").name("setDomain").addParameter("java.lang.String"),
      MethodMatcher.create().typeDefinition("java.net.HttpCookie").name("setDomain").addParameter("java.lang.String")
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree arg = mit.arguments().get(0);
    String cookieDomain = ExpressionsHelper.getConstantValueAsString(arg).value();
    if (cookieDomain == null || cookieDomain.isEmpty()) {
      return;
    }

    String domainName = cookieDomain.substring(1);
    if (InternetDomainName.from(domainName).isPublicSuffix()) {
      reportIssue(arg, MessageFormat.format("Do not set cookies for ''{0}'' as it is a public suffix.", domainName));
    } else if (!domainName.contains(".")) {
      reportIssue(arg, "Specify at least a second-level cookie domain.");
    }
  }
}
