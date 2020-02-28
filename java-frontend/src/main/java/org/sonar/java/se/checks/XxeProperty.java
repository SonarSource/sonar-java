/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.util.function.BiPredicate;
import javax.annotation.Nullable;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.tree.ExpressionTree;

/**
 * A property is independently a XXE feature or XXE attribute
 */
public interface XxeProperty {

  String UNSECURED_USE_EMPTY_STRING_TO_PROTECT_AGAINST_XXE = "unsecured. Set to \"\" (empty string) to protect against XXE";

  XxePropertyHolder properties();

  default String propertyName() {
    return properties().propertyName;
  }

  default Constraint securedConstraint() {
    return properties().secured;
  }

  default boolean isSecuring(@Nullable SymbolicValue sv1, ExpressionTree arg1) {
    return properties().securing.test(sv1, arg1);
  }

  default Constraint unsecuredConstraint() {
    return properties().unsecured;
  }

  default boolean isUnsecuring(@Nullable SymbolicValue sv1, ExpressionTree arg1) {
    return properties().unsecuring.test(sv1, arg1);
  }

  static boolean isSetToEmptyString(@Nullable SymbolicValue sv1, ExpressionTree arg1) {
    return arg1.asConstant(String.class).filter(String::isEmpty).isPresent();
  }

  static boolean isSetToNonEmptyString(@Nullable SymbolicValue sv1, ExpressionTree arg1) {
    return !isSetToEmptyString(sv1, arg1);
  }

  static boolean isSetToFalse(@Nullable SymbolicValue sv1, ExpressionTree arg1) {
    return sv1 == SymbolicValue.FALSE_LITERAL
      || arg1.asConstant(String.class).filter("false"::equalsIgnoreCase).isPresent();
  }

  static boolean isSetToTrue(@Nullable SymbolicValue sv1, ExpressionTree arg1) {
    return sv1 == SymbolicValue.TRUE_LITERAL
      || arg1.asConstant(String.class).filter("true"::equalsIgnoreCase).isPresent();
  }

  static class XxePropertyHolder {
    private final String propertyName;

    private final BiPredicate<SymbolicValue, ExpressionTree> securing;
    private final Constraint secured;

    private final BiPredicate<SymbolicValue, ExpressionTree> unsecuring;
    private final Constraint unsecured;

    public XxePropertyHolder(String propertyName,
      BiPredicate<SymbolicValue, ExpressionTree> securing, Constraint secured,
      BiPredicate<SymbolicValue, ExpressionTree> unsecuring, Constraint unsecured) {
      this.propertyName = propertyName;
      this.securing = securing;
      this.secured = secured;
      this.unsecuring = unsecuring;
      this.unsecured = unsecured;
    }
  }

  enum AttributeDTD implements Constraint, XxeProperty {
    UNSECURED, SECURED;

    private static final XxePropertyHolder PROPERTIES = new XxePropertyHolder(
      "http://javax.xml.XMLConstants/property/accessExternalDTD",
      XxeProperty::isSetToEmptyString, SECURED,
      XxeProperty::isSetToNonEmptyString, UNSECURED);

    @Override
    public XxePropertyHolder properties() {
      return PROPERTIES;
    }

    @Override
    public String valueAsString() {
      return UNSECURED_USE_EMPTY_STRING_TO_PROTECT_AGAINST_XXE;
    }
  }

  enum AttributeSchema implements Constraint, XxeProperty {
    UNSECURED, SECURED;

    private static final XxePropertyHolder PROPERTIES = new XxePropertyHolder(
      "http://javax.xml.XMLConstants/property/accessExternalSchema",
      XxeProperty::isSetToEmptyString, SECURED,
      XxeProperty::isSetToNonEmptyString, UNSECURED);

    @Override
    public XxePropertyHolder properties() {
      return PROPERTIES;
    }

    @Override
    public String valueAsString() {
      return UNSECURED_USE_EMPTY_STRING_TO_PROTECT_AGAINST_XXE;
    }
  }

  enum AttributeStyleSheet implements Constraint, XxeProperty {
    UNSECURED, SECURED;

    private static final XxePropertyHolder PROPERTIES = new XxePropertyHolder(
      "http://javax.xml.XMLConstants/property/accessExternalStylesheet",
      XxeProperty::isSetToEmptyString, SECURED,
      XxeProperty::isSetToNonEmptyString, UNSECURED);

    @Override
    public XxePropertyHolder properties() {
      return PROPERTIES;
    }

    @Override
    public String valueAsString() {
      return UNSECURED_USE_EMPTY_STRING_TO_PROTECT_AGAINST_XXE;
    }
  }

  enum FeatureSupportDtd implements Constraint, XxeProperty {
    UNSECURED, SECURED;

    private static final XxePropertyHolder PROPERTIES = new XxePropertyHolder(
      "javax.xml.stream.supportDTD",
      XxeProperty::isSetToFalse, SECURED,
      XxeProperty::isSetToTrue, UNSECURED);

    @Override
    public XxePropertyHolder properties() {
      return PROPERTIES;
    }
  }

  enum FeatureIsSupportingExternalEntities implements Constraint, XxeProperty {
    UNSECURED, SECURED;

    private static final XxePropertyHolder PROPERTIES = new XxePropertyHolder(
      "javax.xml.stream.isSupportingExternalEntities",
      XxeProperty::isSetToFalse, SECURED,
      XxeProperty::isSetToTrue, UNSECURED);

    @Override
    public XxePropertyHolder properties() {
      return PROPERTIES;
    }
  }

  enum FeatureDisallowDoctypeDecl implements Constraint, XxeProperty {
    UNSECURED, SECURED;

    private static final XxePropertyHolder PROPERTIES = new XxePropertyHolder(
      "http://apache.org/xml/features/disallow-doctype-decl",
      XxeProperty::isSetToTrue, SECURED,
      XxeProperty::isSetToFalse, UNSECURED);

    @Override
    public XxePropertyHolder properties() {
      return PROPERTIES;
    }
  }

  enum FeatureLoadExternalDtd implements Constraint, XxeProperty {
    UNSECURED, SECURED;

    private static final XxePropertyHolder PROPERTIES = new XxePropertyHolder(
      "http://apache.org/xml/features/nonvalidating/load-external-dtd",
      XxeProperty::isSetToFalse, SECURED,
      XxeProperty::isSetToTrue, UNSECURED);

    @Override
    public XxePropertyHolder properties() {
      return PROPERTIES;
    }
  }

  enum FeatureExternalGeneralEntities implements Constraint, XxeProperty {
    UNSECURED, SECURED;

    private static final XxePropertyHolder PROPERTIES = new XxePropertyHolder(
      "http://xml.org/sax/features/external-general-entities",
      XxeProperty::isSetToFalse, SECURED,
      XxeProperty::isSetToTrue, UNSECURED);

    @Override
    public XxePropertyHolder properties() {
      return PROPERTIES;
    }
  }
}
