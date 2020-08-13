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
package org.sonar.java.se.xproc;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.java.model.Sema;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;

public class MethodBehaviorJsonAdapter implements JsonSerializer<MethodBehavior>, JsonDeserializer<MethodBehavior> {

  private static final String JSON_CONSTRAINT_DELIMITER = ".";
  private static final String JSON_IS_EXCEPTIONAL = "isExceptional";
  private static final String JSON_THROWN_EXCEPTION = "exception";
  private static final String JSON_RESULT_CONSTRAINTS = "resultConstaint";
  private static final String JSON_RESULT_INDEX = "resultIndex";
  private static final String JSON_PARAMETERS_CONSTRAINTS = "parametersConstraints";
  private static final String JSON_DECLARED_EXCEPTIONS = "declaredExceptions";
  private static final String JSON_VARARGS = "varArgs";
  private static final String JSON_ARITY = "arity";
  private static final String JSON_SIGNATURE = "signature";
  private static final String JSON_YIELDS = "yields";

  private final Sema sema;

  public MethodBehaviorJsonAdapter(Sema sema) {
    this.sema = sema;
  }

  public static Gson gson(Sema sema) {
    return new GsonBuilder()
      .registerTypeAdapter(MethodBehavior.class, new MethodBehaviorJsonAdapter(sema))
      .serializeNulls()
      .setPrettyPrinting()
      .create();
  }

  @Override
  public MethodBehavior deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
    JsonObject jsonMB = (JsonObject) json;
    MethodBehavior mb = new MethodBehavior(
      jsonMB.get(JSON_SIGNATURE).getAsString(),
      jsonMB.get(JSON_VARARGS).getAsBoolean());

    List<String> declaredExceptions = new ArrayList<>();
    jsonMB.get(JSON_DECLARED_EXCEPTIONS)
      .getAsJsonArray()
      .forEach(exception -> declaredExceptions.add(exception.getAsString()));

    jsonMB.get(JSON_YIELDS)
      .getAsJsonArray()
      .forEach(yield -> mb.addYield(yieldsFromJson(mb, jsonMB.get(JSON_ARITY).getAsInt(), (JsonObject) yield)));

    mb.completed();
    return mb;
  }

  private static MethodYield yieldsFromJson(MethodBehavior behavior, int arity, JsonObject methodYield) {
    MethodYield result;
    if (methodYield.get(JSON_IS_EXCEPTIONAL).getAsBoolean()) {
      ExceptionalYield exceptionalYield = new ExceptionalYield(behavior);
      exceptionalYield.setExceptionType(methodYield.get(JSON_THROWN_EXCEPTION).getAsString());
      result = exceptionalYield;
    } else {
      HappyPathYield happyPathYield = new HappyPathYield(behavior);
      happyPathYield.setResult(methodYield.get(JSON_RESULT_INDEX).getAsInt(), constraintsByDomainFromJson(methodYield.get(JSON_RESULT_CONSTRAINTS)));
      result = happyPathYield;
    }
    result.parametersConstraints.addAll(constraintsFromJson(arity, methodYield.get(JSON_PARAMETERS_CONSTRAINTS).getAsJsonArray()));
    return result;
  }

  private static List<ConstraintsByDomain> constraintsFromJson(int arity, JsonArray parameterConstraints) {
    List<ConstraintsByDomain> parametersConstraintsByDomain = new ArrayList<>(arity);
    parameterConstraints.forEach(constraint -> parametersConstraintsByDomain.add(constraintsByDomainFromJson(constraint)));
    return parametersConstraintsByDomain;
  }

  @CheckForNull
  private static ConstraintsByDomain constraintsByDomainFromJson(JsonElement jsonConstraintsByDomain) {
    if (jsonConstraintsByDomain.isJsonNull()) {
      return null;
    }
    JsonArray constraintsByDomainJsonArray = jsonConstraintsByDomain.getAsJsonArray();
    ConstraintsByDomain constraintsByDomain = ConstraintsByDomain.empty();
    for (int i = 0; i < constraintsByDomainJsonArray.size(); i++) {
      String domainConstraint = constraintsByDomainJsonArray.get(i).getAsString();
      int delimiterPosition = domainConstraint.indexOf(JSON_CONSTRAINT_DELIMITER);
      Preconditions.checkState(delimiterPosition != -1 && delimiterPosition == domainConstraint.lastIndexOf(JSON_CONSTRAINT_DELIMITER));
      String domain = domainConstraint.substring(0, delimiterPosition);
      String constraint = domainConstraint.substring(delimiterPosition + 1);
      switch (domain) {
        case "ObjectConstraint":
          constraintsByDomain = constraintsByDomain.put(ObjectConstraint.valueOf(constraint));
          break;
        case "BooleanConstraint":
          constraintsByDomain = constraintsByDomain.put(BooleanConstraint.valueOf(constraint));
          break;
        default:
          String msg = String.format("Unsupported Domain constraint \"%s\". Only BooleanConstraint (TRUE, FALSE) and ObjectConstraint (NULL, NOT_NULL) are supported.", domain);
          throw new IllegalStateException(msg);
      }
    }
    return constraintsByDomain;
  }

  @Override
  public JsonElement serialize(MethodBehavior src, Type typeOfSrc, JsonSerializationContext context) {
    Preconditions.checkState(src.isComplete());

    JsonObject mb = new JsonObject();
    mb.addProperty(JSON_SIGNATURE, src.signature());
    mb.addProperty(JSON_ARITY, src.methodArity());
    mb.addProperty(JSON_VARARGS, src.isMethodVarArgs());

    List<String> declaredExceptions = src.getDeclaredExceptions();
    JsonArray jsonDeclaredExceptions = new JsonArray(declaredExceptions.size());
    declaredExceptions.forEach(jsonDeclaredExceptions::add);
    mb.add(JSON_DECLARED_EXCEPTIONS, jsonDeclaredExceptions);

    List<MethodYield> yields = src.yields();
    JsonArray jsonYields = new JsonArray(yields.size());
    yields.forEach(my -> jsonYields.add(toJson(my, src.methodArity())));

    mb.add(JSON_YIELDS, jsonYields);

    return mb;
  }

  private JsonElement toJson(MethodYield methodYield, int arity) {
    JsonObject yield = new JsonObject();
    JsonArray jsonParameterConstraints = new JsonArray();
    for (int i = 0; i < arity; i++) {
      jsonParameterConstraints.add(toJson(methodYield.parametersConstraints.get(i)));
    }
    yield.add(JSON_PARAMETERS_CONSTRAINTS, jsonParameterConstraints);
    boolean isExceptional = false;
    if (methodYield instanceof HappyPathYield) {
      HappyPathYield happyPathYield = (HappyPathYield) methodYield;
      yield.addProperty(JSON_RESULT_INDEX, happyPathYield.resultIndex());
      yield.add(JSON_RESULT_CONSTRAINTS, toJson(happyPathYield.resultConstraint()));
    } else if (methodYield instanceof ExceptionalYield) {
      ExceptionalYield exceptionalYield = (ExceptionalYield) methodYield;
      isExceptional = true;
      yield.addProperty(JSON_THROWN_EXCEPTION, exceptionalYield.exceptionType(sema).fullyQualifiedName());
    } else {
      throw new IllegalStateException("Hardcoded yields should only be HappyPathYield or ExceptionalYield.");
    }
    yield.addProperty(JSON_IS_EXCEPTIONAL, isExceptional);
    return yield;
  }

  private static JsonElement toJson(@Nullable ConstraintsByDomain constraints) {
    if (constraints == null) {
      return JsonNull.INSTANCE;
    }
    JsonArray jsonConstraints = new JsonArray();
    constraints.forEach((domain, constraint) -> jsonConstraints.add(domain.getSimpleName() + JSON_CONSTRAINT_DELIMITER + constraint.toString()));
    return jsonConstraints;
  }
}
