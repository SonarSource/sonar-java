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
package org.sonar.java.serialization;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.sonar.java.model.springcontext.BeanDefinitionHolder;
import org.sonar.java.model.springcontext.BeanDefinitionKind;
import org.sonar.java.model.springcontext.InjectionPoint;
import org.sonar.java.reporting.AnalyzerMessage;

import static org.sonar.java.serialization.JsonUtils.DEPENDENCIES;
import static org.sonar.java.serialization.JsonUtils.INJECTION_POINTS;
import static org.sonar.java.serialization.JsonUtils.KIND;
import static org.sonar.java.serialization.JsonUtils.NAME;
import static org.sonar.java.serialization.JsonUtils.PACKAGE;
import static org.sonar.java.serialization.JsonUtils.PRIMARY;
import static org.sonar.java.serialization.JsonUtils.PROFILES;
import static org.sonar.java.serialization.JsonUtils.SPAN;
import static org.sonar.java.serialization.JsonUtils.TYPE;
import static org.sonar.java.serialization.JsonUtils.TYPE_HIERARCHY;
import static org.sonar.java.serialization.JsonUtils.missingProperty;
import static org.sonar.java.serialization.JsonUtils.readNullableString;
import static org.sonar.java.serialization.JsonUtils.readString;
import static org.sonar.java.serialization.JsonUtils.readStrings;
import static org.sonar.java.serialization.JsonUtils.required;
import static org.sonar.java.serialization.JsonUtils.writeStrings;

/**
 * JSON representation of a single cached Spring bean definition.
 *
 * <p>A {@link BeanDefinitionHolder.InputFileData} locates itself by text spans only, so no file needs to be
 * known to read one back: the gatherer pairs the restored beans with the file the cache entry was read for.
 */
public final class BeanDefinitionHolderTypeAdapter extends TypeAdapter<BeanDefinitionHolder.InputFileData> {

  private static final BeanDefinitionHolderTypeAdapter INSTANCE = new BeanDefinitionHolderTypeAdapter();

  private BeanDefinitionHolderTypeAdapter() {
  }

  public static BeanDefinitionHolderTypeAdapter getInstance() {
    return INSTANCE;
  }

  @Override
  public void write(JsonWriter out, BeanDefinitionHolder.InputFileData bean) throws IOException {
    out.beginObject();
    out.name(NAME).value(bean.beanName());
    out.name(TYPE).value(bean.type());
    out.name(KIND).value(bean.kind().name());
    out.name(PACKAGE).value(bean.beanPackage());
    out.name(SPAN);
    TextSpanTypeAdapter.getInstance().write(out, bean.textSpan());
    out.name(PRIMARY).value(bean.isPrimary());
    out.name(PROFILES).value(bean.profiles());
    out.name(DEPENDENCIES);
    writeDependencies(out, bean.dependencies());
    out.name(TYPE_HIERARCHY);
    writeStrings(out, bean.typeHierarchy());
    out.endObject();
  }

  @Override
  public BeanDefinitionHolder.InputFileData read(JsonReader in) throws IOException {
    String beanName = null;
    String type = null;
    BeanDefinitionKind kind = null;
    String beanPackage = null;
    AnalyzerMessage.TextSpan span = null;
    Boolean isPrimary = null;
    String profiles = null;
    boolean profilesRead = false;
    Map<String, Set<InjectionPoint.InputFileData>> dependencies = null;
    Set<String> typeHierarchy = null;
    in.beginObject();
    while (in.hasNext()) {
      switch (in.nextName()) {
        case NAME -> beanName = readString(in);
        case TYPE -> type = readString(in);
        case KIND -> kind = BeanDefinitionKind.valueOf(readString(in));
        case PACKAGE -> beanPackage = readString(in);
        case SPAN -> span = TextSpanTypeAdapter.getInstance().read(in);
        case PRIMARY -> isPrimary = in.nextBoolean();
        case PROFILES -> {
          profiles = readNullableString(in);
          profilesRead = true;
        }
        case DEPENDENCIES -> dependencies = readDependencies(in);
        case TYPE_HIERARCHY -> typeHierarchy = readStrings(in);
        default -> in.skipValue();
      }
    }
    in.endObject();
    if (!profilesRead) {
      throw missingProperty(PROFILES);
    }
    return new BeanDefinitionHolder.InputFileData(
      required(beanName, NAME),
      required(type, TYPE),
      required(kind, KIND),
      required(beanPackage, PACKAGE),
      required(span, SPAN),
      required(isPrimary, PRIMARY),
      profiles,
      required(dependencies, DEPENDENCIES),
      required(typeHierarchy, TYPE_HIERARCHY)
    );
  }

  private static void writeDependencies(JsonWriter out, Map<String, Set<InjectionPoint.InputFileData>> dependencies) throws IOException {
    out.beginArray();
    for (Map.Entry<String, Set<InjectionPoint.InputFileData>> dependency : dependencies.entrySet()) {
      out.beginObject();
      out.name(TYPE).value(dependency.getKey());
      out.name(INJECTION_POINTS);
      out.beginArray();
      for (InjectionPoint.InputFileData injectionPoint : dependency.getValue()) {
        InjectionPointTypeAdapter.getInstance().write(out, injectionPoint);
      }
      out.endArray();
      out.endObject();
    }
    out.endArray();
  }

  private static Map<String, Set<InjectionPoint.InputFileData>> readDependencies(JsonReader in) throws IOException {
    Map<String, Set<InjectionPoint.InputFileData>> dependencies = new LinkedHashMap<>();
    in.beginArray();
    while (in.hasNext()) {
      String type = null;
      Set<InjectionPoint.InputFileData> injectionPoints = null;
      in.beginObject();
      while (in.hasNext()) {
        switch (in.nextName()) {
          case TYPE -> type = readString(in);
          case INJECTION_POINTS -> injectionPoints = readInjectionPoints(in);
          default -> in.skipValue();
        }
      }
      in.endObject();
      dependencies.put(required(type, TYPE), required(injectionPoints, INJECTION_POINTS));
    }
    in.endArray();
    return dependencies;
  }

  private static Set<InjectionPoint.InputFileData> readInjectionPoints(JsonReader in) throws IOException {
    Set<InjectionPoint.InputFileData> injectionPoints = new LinkedHashSet<>();
    in.beginArray();
    while (in.hasNext()) {
      injectionPoints.add(InjectionPointTypeAdapter.getInstance().read(in));
    }
    in.endArray();
    return injectionPoints;
  }
}
