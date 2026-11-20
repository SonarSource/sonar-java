/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.semsitter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Map;
import org.sonarsource.semsitter.wire.Fact;
import org.sonarsource.semsitter.wire.FactValue;
import org.sonarsource.semsitter.wire.Prop;
import org.sonarsource.semsitter.wire.Symbol;
import org.sonarsource.semsitter.wire.TypeRef;

/**
 * Reads the typed facts of a SemSitter document as the JSON objects {@link JSema} reasons on, the shape the
 * {@code get-types} answers share. Empty collections and unset fields are left out, as the server leaves them out.
 */
final class DocumentFacts {

  private DocumentFacts() {
  }

  static JsonObject symbol(Symbol symbol) {
    JsonObject object = new JsonObject();
    object.addProperty("id", symbol.getId());
    object.addProperty("kind", symbol.getKind());
    object.addProperty("name", symbol.getName());
    if (!symbol.getOwner().isEmpty()) {
      object.addProperty("owner", symbol.getOwner());
    }
    if (symbol.hasPlace()) {
      object.addProperty("start", symbol.getPlace().getStartByte());
      object.addProperty("end", symbol.getPlace().getEndByte());
      object.addProperty("line", symbol.getPlace().getLine());
      object.addProperty("col", symbol.getPlace().getColumn());
    }
    if (symbol.getFactsCount() > 0) {
      object.add("facts", values(symbol.getFactsMap()));
    }
    return object;
  }

  static JsonObject values(Map<String, FactValue> facts) {
    JsonObject object = new JsonObject();
    facts.forEach((key, value) -> {
      JsonElement element = value(value);
      if (element != null) {
        object.add(key, element);
      }
    });
    return object;
  }

  static JsonElement value(FactValue value) {
    return switch (value.getValueCase()) {
      case FLAG -> new JsonPrimitive(value.getFlag());
      case IDS -> strings(value.getIds().getValuesList());
      case TEXT -> new JsonPrimitive(value.getText());
      case TYPE -> type(value.getType());
      case VALUE_NOT_SET -> null;
    };
  }

  static JsonObject type(TypeRef type) {
    JsonObject object = new JsonObject();
    put(object, "kind", type.getKind());
    put(object, "id", type.getId());
    put(object, "origin", type.getOrigin());
    if (type.getArgumentsCount() > 0) {
      object.add("args", types(type.getArgumentsList()));
    }
    if (type.hasElement()) {
      object.add("elem", type(type.getElement()));
    }
    if (type.getMembersCount() > 0) {
      object.add("members", types(type.getMembersList()));
    }
    put(object, "bound", type.getBound());
    if (type.getMutable()) {
      object.addProperty("mutable", true);
    }
    if (type.getNamesCount() > 0) {
      object.add("names", strings(type.getNamesList()));
    }
    put(object, "value", type.getValue());
    put(object, "source", type.getSource());
    put(object, "via", type.getVia());
    put(object, "reason", type.getReason());
    return object;
  }

  static JsonObject fact(Fact fact) {
    JsonObject object = new JsonObject();
    put(object, "kind", fact.getKind());
    put(object, "tier", fact.getTier());
    put(object, "parent", fact.getParent());
    if (fact.getPropsCount() > 0) {
      JsonObject props = new JsonObject();
      for (Prop prop : fact.getPropsList()) {
        props.add(prop.getName(), strings(prop.getValuesList()));
      }
      object.add("props", props);
    }
    edges(object, "extends", fact.getExtendsList());
    edges(object, "implements", fact.getImplementsList());
    edges(object, "permits", fact.getPermitsList());
    edges(object, "returns", fact.getReturnsList());
    edges(object, "type", fact.getTypeList());
    edges(object, "params", fact.getParamsList());
    edges(object, "overrides", fact.getOverridesList());
    edges(object, "annotations", fact.getAnnotationsList());
    return object;
  }

  private static void edges(JsonObject object, String key, List<String> targets) {
    if (!targets.isEmpty()) {
      object.add(key, strings(targets));
    }
  }

  private static void put(JsonObject object, String key, String value) {
    if (!value.isEmpty()) {
      object.addProperty(key, value);
    }
  }

  private static JsonArray strings(List<String> values) {
    JsonArray array = new JsonArray();
    values.forEach(array::add);
    return array;
  }

  private static JsonArray types(List<TypeRef> types) {
    JsonArray array = new JsonArray();
    types.forEach(type -> array.add(type(type)));
    return array;
  }
}
