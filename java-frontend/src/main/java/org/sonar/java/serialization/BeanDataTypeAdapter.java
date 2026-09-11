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
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.model.springcontext.BeanDefinitionGatherer;
import org.sonar.java.model.springcontext.BeanDefinitionGatherer.BeanData;
import org.sonar.java.model.springcontext.InjectionPoint;
import org.sonar.java.reporting.AnalyzerMessage;

import static org.sonar.java.serialization.JsonUtils.DEPENDENCIES;
import static org.sonar.java.serialization.JsonUtils.INJECTION_POINTS;
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
 * <p>The input file of the bean and its {@link BeanData#dependingBeans()} are not serialized: the former is known
 * from the cache entry being read, and the latter is derived from the dependency injection points.
 */
public final class BeanDataTypeAdapter extends TypeAdapter<BeanData> {

  private final InputFile inputFile;
  private final InjectionPointTypeAdapter injectionPointAdapter;

  /**
   * @param inputFile The file the cache entry belongs to, against which locations are restored when reading. The
   *                  file is not part of the serialized form, since a cache entry only ever holds one file's data.
   */
  public BeanDataTypeAdapter(InputFile inputFile) {
    this.inputFile = inputFile;
    this.injectionPointAdapter = new InjectionPointTypeAdapter(inputFile);
  }

  @Override
  public void write(JsonWriter out, BeanData bean) throws IOException {
    out.beginObject();
    out.name(NAME).value(bean.beanName());
    out.name(TYPE).value(bean.type());
    out.name(PACKAGE).value(bean.beanPackage());
    out.name(SPAN);
    TextSpanTypeAdapter.getInstance().write(out, bean.textSpan());
    out.name(PRIMARY).value(bean.isPrimary());
    out.name(PROFILES).value(bean.profiles());
    out.name(DEPENDENCIES);
    writeDependencies(out, bean.dependencyInjectionPoints());
    out.name(TYPE_HIERARCHY);
    writeStrings(out, bean.typeHierarchy());
    out.endObject();
  }

  @Override
  public BeanData read(JsonReader in) throws IOException {
    String beanName = null;
    String type = null;
    String beanPackage = null;
    AnalyzerMessage.TextSpan span = null;
    Boolean isPrimary = null;
    String profiles = null;
    boolean profilesRead = false;
    Map<String, Set<InjectionPoint>> injectionPoints = null;
    Set<String> typeHierarchy = null;
    in.beginObject();
    while (in.hasNext()) {
      switch (in.nextName()) {
        case NAME -> beanName = readString(in);
        case TYPE -> type = readString(in);
        case PACKAGE -> beanPackage = readString(in);
        case SPAN -> span = TextSpanTypeAdapter.getInstance().read(in);
        case PRIMARY -> isPrimary = in.nextBoolean();
        case PROFILES -> {
          profiles = readNullableString(in);
          profilesRead = true;
        }
        case DEPENDENCIES -> injectionPoints = readDependencies(in);
        case TYPE_HIERARCHY -> typeHierarchy = readStrings(in);
        default -> in.skipValue();
      }
    }
    in.endObject();
    if (!profilesRead) {
      throw missingProperty(PROFILES);
    }
    Map<String, Set<InjectionPoint>> dependencies = required(injectionPoints, DEPENDENCIES);
    return new BeanData(
      required(beanName, NAME),
      required(type, TYPE),
      required(beanPackage, PACKAGE),
      inputFile,
      required(span, SPAN),
      required(isPrimary, PRIMARY),
      profiles,
      BeanDefinitionGatherer.projectToNames(dependencies),
      dependencies,
      required(typeHierarchy, TYPE_HIERARCHY)
    );
  }

  private void writeDependencies(JsonWriter out, Map<String, Set<InjectionPoint>> dependencies) throws IOException {
    out.beginArray();
    for (Map.Entry<String, Set<InjectionPoint>> dependency : dependencies.entrySet()) {
      out.beginObject();
      out.name(TYPE).value(dependency.getKey());
      out.name(INJECTION_POINTS);
      out.beginArray();
      for (InjectionPoint injectionPoint : dependency.getValue()) {
        injectionPointAdapter.write(out, injectionPoint);
      }
      out.endArray();
      out.endObject();
    }
    out.endArray();
  }

  private Map<String, Set<InjectionPoint>> readDependencies(JsonReader in) throws IOException {
    Map<String, Set<InjectionPoint>> dependencies = new LinkedHashMap<>();
    in.beginArray();
    while (in.hasNext()) {
      String type = null;
      Set<InjectionPoint> injectionPoints = null;
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

  private Set<InjectionPoint> readInjectionPoints(JsonReader in) throws IOException {
    Set<InjectionPoint> injectionPoints = new LinkedHashSet<>();
    in.beginArray();
    while (in.hasNext()) {
      injectionPoints.add(injectionPointAdapter.read(in));
    }
    in.endArray();
    return injectionPoints;
  }
}
