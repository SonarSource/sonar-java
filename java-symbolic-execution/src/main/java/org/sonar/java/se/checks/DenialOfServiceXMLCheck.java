/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.se.checks;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.se.checks.XxeProperty.FeatureSecureProcessing;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.plugins.java.api.semantic.MethodMatchers;

import static org.sonar.java.se.checks.XxeProcessingCheck.NEW_DOCUMENT_BUILDER;
import static org.sonar.java.se.checks.XxeProcessingCheck.PARSING_METHODS;

/**
 * This check uses the symbolic value and constraints set by XxeProcessingCheck.
 * It must therefore always be executed afterwards.
 *
 * @see org.sonar.java.se.checks.XxeProcessingCheck
 */
@Rule(key = "S6376")
public class DenialOfServiceXMLCheck extends AbstractXMLProcessing {

  private static final List<Class<? extends Constraint>> DOMAINS = Collections.singletonList(FeatureSecureProcessing.class);

  private static final MethodMatchers PARSING_METHODS_DOS = MethodMatchers.or(
    PARSING_METHODS,
    // When "newDocumentBuilder" is called on the factory, there is no way to secure the processing anymore
    NEW_DOCUMENT_BUILDER
  );

  @Override
  protected MethodMatchers getParsingMethods() {
    return PARSING_METHODS_DOS;
  }

  protected boolean isUnSecuredByProperty(@Nullable ConstraintsByDomain constraintsByDomain) {
    if (constraintsByDomain == null) {
      // Not vulnerable unless some properties are explicitly set.
      return false;
    }
    return constraintsByDomain.hasConstraint(FeatureSecureProcessing.UNSECURED)
      && !constraintsByDomain.hasConstraint(XxeProperty.FeatureDisallowDoctypeDecl.SECURED);
  }

  @Override
  protected String getMessage() {
    return "Enable XML parsing limitations to prevent Denial of Service attacks.";
  }

  @Override
  protected boolean shouldTrackConstraint(Constraint constraint) {
    return constraint == FeatureSecureProcessing.UNSECURED;
  }

  @Override
  protected List<Class<? extends Constraint>> getDomains() {
    return DOMAINS;
  }
}
