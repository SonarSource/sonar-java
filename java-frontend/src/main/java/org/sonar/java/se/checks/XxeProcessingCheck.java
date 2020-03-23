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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.ProgramState.SymbolicValueSymbol;
import org.sonar.java.se.checks.XxeProperty.AttributeDTD;
import org.sonar.java.se.checks.XxeProperty.AttributeSchema;
import org.sonar.java.se.checks.XxeProperty.AttributeStyleSheet;
import org.sonar.java.se.checks.XxeProperty.FeatureDisallowDoctypeDecl;
import org.sonar.java.se.checks.XxeProperty.FeatureExternalGeneralEntities;
import org.sonar.java.se.checks.XxeProperty.FeatureIsSupportingExternalEntities;
import org.sonar.java.se.checks.XxeProperty.FeatureLoadExternalDtd;
import org.sonar.java.se.checks.XxeProperty.FeatureSupportDtd;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2755")
public class XxeProcessingCheck extends SECheck {

  private static final String BOOLEAN = "boolean";
  private static final String NEW_INSTANCE = "newInstance";

  private static final String JAVA_LANG_OBJECT = "java.lang.Object";
  private static final String JAVA_LANG_STRING = "java.lang.String";

  // XMLInputFactory
  private static final String XML_INPUT_FACTORY = "javax.xml.stream.XMLInputFactory";
  private static final MethodMatchers XML_INPUT_FACTORY_NEW_INSTANCE = MethodMatchers.create()
    .ofTypes(XML_INPUT_FACTORY)
    .names(NEW_INSTANCE, "newFactory")
    .withAnyParameters()
    .build();

  // DocumentBuilderFactory
  private static final String DOCUMENT_BUILDER_FACTORY = "javax.xml.parsers.DocumentBuilderFactory";
  private static final MethodMatchers DOCUMENT_BUILDER_FACTORY_NEW_INSTANCE = MethodMatchers.create()
    .ofTypes(DOCUMENT_BUILDER_FACTORY)
    .names(NEW_INSTANCE)
    .withAnyParameters()
    .build();

  // SAXParserFactory
  private static final String SAX_PARSER_FACTORY = "javax.xml.parsers.SAXParserFactory";
  private static final String SAX_PARSER = "javax.xml.parsers.SAXParser";
  private static final MethodMatchers SAX_PARSER_FACTORY_NEW_INSTANCE = MethodMatchers.create()
    .ofTypes(SAX_PARSER_FACTORY)
    .names(NEW_INSTANCE)
    .withAnyParameters()
    .build();

  // SchemaFactory and Validator
  private static final String SCHEMA_FACTORY = "javax.xml.validation.SchemaFactory";
  private static final MethodMatchers SCHEMA_FACTORY_NEW_INSTANCE = MethodMatchers.create()
    .ofTypes(SCHEMA_FACTORY)
    .names(NEW_INSTANCE)
    .withAnyParameters()
    .build();
  private static final String VALIDATOR = "javax.xml.validation.Validator";

  // TransformerFactory
  private static final String TRANSFORMER_FACTORY = "javax.xml.transform.TransformerFactory";
  private static final MethodMatchers TRANSFORMER_FACTORY_NEW_INSTANCE = MethodMatchers.create()
    .ofSubTypes(TRANSFORMER_FACTORY)
    .names(NEW_INSTANCE)
    .withAnyParameters()
    .build();

  // TransformerFactory
  private static final String XML_READER = "org.xml.sax.XMLReader";
  private static final MethodMatchers CREATE_XML_READER = MethodMatchers.create()
    .ofTypes("org.xml.sax.helpers.XMLReaderFactory")
    .names("createXMLReader")
    .withAnyParameters()
    .build();

  // SAXBuilder
  private static final String SAX_BUILDER = "org.jdom2.input.SAXBuilder";
  private static final MethodMatchers SAX_BUILDER_CONSTRUCTOR = MethodMatchers.create()
    .ofTypes(SAX_BUILDER)
    .constructor()
    .withAnyParameters()
    .build();

  // SAXReader
  private static final String SAX_READER = "org.dom4j.io.SAXReader";
  private static final MethodMatchers SAX_READER_CONSTRUCTOR = MethodMatchers.create()
    .ofTypes(SAX_READER)
    .constructor()
    .withAnyParameters()
    .build();

  private static final Map<MethodMatchers, Predicate<ConstraintsByDomain>> CONDITIONS_FOR_SECURED_BY_TYPE =
    ImmutableMap.<MethodMatchers, Predicate<ConstraintsByDomain>>builder()
      .put(XML_INPUT_FACTORY_NEW_INSTANCE,
        c -> (c.hasConstraint(AttributeDTD.SECURED) && c.hasConstraint(AttributeSchema.SECURED))
          || c.hasConstraint(FeatureSupportDtd.SECURED)
          || c.hasConstraint(FeatureIsSupportingExternalEntities.SECURED))
      .put(DOCUMENT_BUILDER_FACTORY_NEW_INSTANCE,
        c -> (c.hasConstraint(AttributeDTD.SECURED) && c.hasConstraint(AttributeSchema.SECURED))
          || c.hasConstraint(FeatureDisallowDoctypeDecl.SECURED)
          || c.hasConstraint(FeatureLoadExternalDtd.SECURED)
          || c.hasConstraint(FeatureExternalGeneralEntities.SECURED))
      .put(SAX_PARSER_FACTORY_NEW_INSTANCE,
        c -> (c.hasConstraint(AttributeDTD.SECURED) && c.hasConstraint(AttributeSchema.SECURED))
          || c.hasConstraint(FeatureDisallowDoctypeDecl.SECURED)
          || c.hasConstraint(FeatureExternalGeneralEntities.SECURED))
      .put(SCHEMA_FACTORY_NEW_INSTANCE,
        c -> c.hasConstraint(AttributeDTD.SECURED) && c.hasConstraint(AttributeSchema.SECURED))
      .put(TRANSFORMER_FACTORY_NEW_INSTANCE,
        c -> c.hasConstraint(AttributeDTD.SECURED) && c.hasConstraint(AttributeStyleSheet.SECURED))
      .put(CREATE_XML_READER,
        c -> (c.hasConstraint(AttributeDTD.SECURED) && c.hasConstraint(AttributeSchema.SECURED))
          || c.hasConstraint(FeatureDisallowDoctypeDecl.SECURED)
          || c.hasConstraint(FeatureExternalGeneralEntities.SECURED))
      .build();

  private static final Map<MethodMatchers, Predicate<ConstraintsByDomain>> CONDITIONS_FOR_SECURED_BY_TYPE_NEW_CLASS = ImmutableMap.of(
    SAX_BUILDER_CONSTRUCTOR,
    c -> (c.hasConstraint(AttributeDTD.SECURED) && c.hasConstraint(AttributeSchema.SECURED))
      || c.hasConstraint(FeatureDisallowDoctypeDecl.SECURED),
    SAX_READER_CONSTRUCTOR,
    c -> c.hasConstraint(FeatureDisallowDoctypeDecl.SECURED)
      || c.hasConstraint(FeatureExternalGeneralEntities.SECURED));

  private static final MethodMatchers FEATURES_AND_PROPERTIES_SETTERS = MethodMatchers.or(
    MethodMatchers.create().ofSubTypes(DOCUMENT_BUILDER_FACTORY, TRANSFORMER_FACTORY)
      .names("setAttribute").addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_OBJECT).build(),
    MethodMatchers.create().ofSubTypes(XML_INPUT_FACTORY, SAX_PARSER, SCHEMA_FACTORY, VALIDATOR, XML_READER, SAX_BUILDER)
      .names("setProperty").addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_OBJECT).build(),
    MethodMatchers.create().ofSubTypes(DOCUMENT_BUILDER_FACTORY, SAX_PARSER_FACTORY, XML_READER, SAX_BUILDER, SAX_READER)
      .names("setFeature").addParametersMatcher(JAVA_LANG_STRING, BOOLEAN).build());

  private static final MethodMatchers TRANSFERRING_METHOD_CALLS = MethodMatchers.or(
    MethodMatchers.create().ofTypes(SAX_PARSER_FACTORY).names("newSAXParser").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(SCHEMA_FACTORY).names("newSchema").withAnyParameters().build(),
    MethodMatchers.create().ofTypes("javax.xml.validation.Schema").names("newValidator").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(SAX_PARSER).names("getXMLReader").withAnyParameters().build()
  );

  private static final MethodMatchers PARSING_METHODS = MethodMatchers.or(
    MethodMatchers.create().ofSubTypes(DOCUMENT_BUILDER_FACTORY).names("newDocumentBuilder").addWithoutParametersMatcher().build(),
    MethodMatchers.create().ofSubTypes(TRANSFORMER_FACTORY).names("newTransformer").withAnyParameters().build(),
    MethodMatchers.create().ofSubTypes(XML_INPUT_FACTORY).name(n -> n.startsWith("create")).withAnyParameters().build(),
    MethodMatchers.create().ofSubTypes(VALIDATOR).names("validate").withAnyParameters().build(),
    MethodMatchers.create().ofSubTypes(SAX_PARSER).names("parse").withAnyParameters().build(),
    MethodMatchers.create().ofSubTypes(XML_READER).names("parse").withAnyParameters().build(),
    MethodMatchers.create().ofSubTypes(SAX_BUILDER).names("build").withAnyParameters().build(),
    MethodMatchers.create().ofSubTypes(SAX_READER).names("read").withAnyParameters().build()
  );

  private static final List<XxeProperty> PROPERTIES_TO_CHECK = ImmutableList.<XxeProperty>builder()
    .add(FeatureSupportDtd.values())
    .add(FeatureIsSupportingExternalEntities.values())
    .add(FeatureDisallowDoctypeDecl.values())
    .add(FeatureExternalGeneralEntities.values())
    .add(FeatureLoadExternalDtd.values())
    .add(AttributeDTD.values())
    .add(AttributeSchema.values())
    .add(AttributeStyleSheet.values())
    .build();

  private static final List<Class<? extends Constraint>> FLOW_CONSTRAINT_DOMAIN = ImmutableList.<Class<? extends Constraint>>builder()
    .add(AttributeDTD.class)
    .add(AttributeSchema.class)
    .add(AttributeStyleSheet.class)
    .build();

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    PreStatementVisitor visitor = new PreStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  private class PreStatementVisitor extends CheckerTreeNodeVisitor {

    private final ConstraintManager constraintManager;
    private final CheckerContext context;

    private PreStatementVisitor(CheckerContext context) {
      super(context.getState());
      this.constraintManager = context.getConstraintManager();
      this.context = context;
    }

    @Override
    public void visitNewClass(NewClassTree newClass) {
      for (Map.Entry<MethodMatchers, Predicate<ConstraintsByDomain>> entry : CONDITIONS_FOR_SECURED_BY_TYPE_NEW_CLASS.entrySet()) {
        if (entry.getKey().matches(newClass)) {
          constraintManager
            .setValueFactory(() -> new XxeSymbolicValue(newClass.identifier(), entry.getValue()));
          break;
        }
      }
    }

    @Override
    public void visitVariable(VariableTree tree) {
      programState = addNamedConstraint(tree.initializer(), programState);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {

      // Test initialisation of XML processing API
      for (Map.Entry<MethodMatchers, Predicate<ConstraintsByDomain>> entry : CONDITIONS_FOR_SECURED_BY_TYPE.entrySet()) {
        if (entry.getKey().matches(mit)) {
          constraintManager
            .setValueFactory(() -> new XxeSymbolicValue(ExpressionUtils.methodName(mit), entry.getValue()));
          break;
        }
      }

      if (TRANSFERRING_METHOD_CALLS.matches(mit)) {
        // transfer same SV to the result of the method invocation.
        constraintManager.setValueFactory(() -> programState.peekValue(mit.arguments().size()));
      } else if (FEATURES_AND_PROPERTIES_SETTERS.matches(mit)) {
        // check secured by attribute or feature
        Arguments arguments = mit.arguments();
        for (XxeProperty property : PROPERTIES_TO_CHECK) {
          programState = checkArguments(programState, arguments, property);
        }
      }

      // Test if API is used without any protection against XXE.
      if (PARSING_METHODS.matches(mit)) {
        SymbolicValue peek = programState.peekValue(mit.arguments().size());

        if (peek instanceof XxeSymbolicValue) {
          XxeSymbolicValue xxeSymbolicValue = (XxeSymbolicValue) peek;
          reportIfNotSecured(context, xxeSymbolicValue, programState.getConstraints(xxeSymbolicValue));
        }
      }
    }

    private ProgramState checkArguments(ProgramState state, Arguments arguments, XxeProperty property) {
      if (isSettingProperty(state, arguments.get(0), property)) {
        SymbolicValue sv1 = state.peekValue();
        ExpressionTree arg1 = arguments.get(1);
        if (property.isSecuring(sv1, arg1)) {
          return state.addConstraint(state.peekValue(2), property.securedConstraint());
        } else if (property.isUnsecuring(sv1, arg1)) {
          return state.addConstraint(state.peekValue(2), property.unsecuredConstraint());
        }
      }
      return state;
    }

    boolean isSettingProperty(ProgramState state, ExpressionTree arg0, XxeProperty property) {
      if (arg0.asConstant(String.class).filter(property::isNamed).isPresent()) {
        return true;
      }

      ConstraintsByDomain constraintsByDomain = state.getConstraints(state.peekValue(1));
      return constraintsByDomain != null && constraintsByDomain.hasConstraint(property.namedConstraint());
    }
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    PostStatementVisitor visitor = new PostStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  private static ProgramState addNamedConstraint(@Nullable ExpressionTree expressionTree, ProgramState state) {
    if (expressionTree != null) {
      SymbolicValue sv = state.peekValue();
      if (sv != null) {
        Optional<String> value = expressionTree.asConstant(String.class);
        if (value.isPresent()) {
          for (XxeProperty property : PROPERTIES_TO_CHECK) {
            if (property.isNamed(value.get())) {
              return state.addConstraint(sv, property.namedConstraint());
            }
          }
        }
      }
    }
    return state;
  }

  private static class PostStatementVisitor extends CheckerTreeNodeVisitor {

    private PostStatementVisitor(CheckerContext context) {
      super(context.getState());
    }

    @Override
    public void visitNewClass(NewClassTree newClass) {
      SymbolicValue peek = programState.peekValue();
      if (peek != null && CONDITIONS_FOR_SECURED_BY_TYPE_NEW_CLASS.keySet().stream().anyMatch(mm -> mm.matches(newClass))) {
        programState = programState.addConstraint(peek, XxeSensitive.SENSITIVE);
      }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      SymbolicValue peek = programState.peekValue();
      if (peek != null && CONDITIONS_FOR_SECURED_BY_TYPE.keySet().stream().anyMatch(mm -> mm.matches(mit))) {
        programState = programState.addConstraint(peek, XxeSensitive.SENSITIVE);
      }
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      SymbolicValueSymbol peek = programState.peekValueSymbol();
      Symbol symbol = peek.symbol();
      SymbolicValue sv = peek.symbolicValue();
      if (symbol != null && sv instanceof XxeSymbolicValue) {
        ((XxeSymbolicValue) sv).setField(ProgramState.isField(symbol));
      }

      // Add a constraint when the tree assign the property key to a variable.
      programState = addNamedConstraint(tree.expression(), programState);
    }
  }

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    ProgramState endState = context.getState();
    if (endState.exitingOnRuntimeException()) {
      return;
    }

    // We want to report only when the unsecured factory is returned, if it is the case, it will be on the top of the stack.
    SymbolicValue peek = endState.peekValue();
    if (peek instanceof XxeSymbolicValue) {
      XxeSymbolicValue xxeSV = (XxeSymbolicValue) peek;
      reportIfNotSecured(context, xxeSV, endState.getConstraints(xxeSV));
    }
  }

  private void reportIfNotSecured(CheckerContext context, XxeSymbolicValue xxeSV, @Nullable ConstraintsByDomain constraintsByDomain) {
    if (!xxeSV.isField && !isSecuredByProperty(xxeSV, constraintsByDomain)) {
      context.reportIssue(xxeSV.init,
        this,
        "Disable access to external entities in XML parsing.",
        FlowComputation.flowWithoutExceptions(context.getNode(), xxeSV,
          c -> c == AttributeDTD.UNSECURED
            || c == AttributeSchema.UNSECURED
            || c == AttributeStyleSheet.UNSECURED
          , FLOW_CONSTRAINT_DOMAIN));
    }
  }

  private static boolean isSecuredByProperty(XxeSymbolicValue sv, @Nullable ConstraintsByDomain constraintsByDomain) {
    return constraintsByDomain == null || sv.conditionForSecured.test(constraintsByDomain);
  }

  private enum XxeSensitive implements Constraint {
    SENSITIVE
  }

  private static class XxeSymbolicValue extends SymbolicValue {
    private final Tree init;
    private final Predicate<ConstraintsByDomain> conditionForSecured;
    private boolean isField;

    private XxeSymbolicValue(Tree init, Predicate<ConstraintsByDomain> conditionForSecured) {
      this.init = init;
      this.isField = false;
      this.conditionForSecured = conditionForSecured;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      XxeSymbolicValue that = (XxeSymbolicValue) o;
      return isField == that.isField &&
        init.equals(that.init) &&
        conditionForSecured.equals(that.conditionForSecured);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), init, conditionForSecured, isField);
    }

    public void setField(boolean isField) {
      this.isField = isField;
    }
  }
}
