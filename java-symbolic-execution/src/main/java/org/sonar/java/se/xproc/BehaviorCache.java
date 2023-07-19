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
package org.sonar.java.se.xproc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.model.JUtils;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodTree;

public class BehaviorCache {

  private static final Logger LOG = LoggerFactory.getLogger(BehaviorCache.class);

  private SymbolicExecutionVisitor sev;

  @VisibleForTesting
  public final Map<String, MethodBehavior> behaviors = new LinkedHashMap<>();
  private Map<String, MethodBehavior> hardcodedBehaviors = null;

  public void setFileContext(@Nullable SymbolicExecutionVisitor sev) {
    this.sev = sev;
  }

  public void cleanup() {
    behaviors.clear();
  }

  @VisibleForTesting
  Map<String, MethodBehavior> hardcodedBehaviors() {
    if (hardcodedBehaviors == null) {
      hardcodedBehaviors = HardcodedMethodBehaviors.load();
      LOG.debug(String.format("[SE] Loaded %d hardcoded method behaviors.", hardcodedBehaviors.size()));
    }
    return hardcodedBehaviors;
  }

  public MethodBehavior methodBehaviorForSymbol(Symbol.MethodSymbol symbol) {
    String signature = symbol.signature();
    boolean varArgs = JUtils.isVarArgsMethod(symbol);
    return behaviors.computeIfAbsent(signature, k -> new MethodBehavior(signature, varArgs));
  }

  @CheckForNull
  public MethodBehavior get(Symbol.MethodSymbol symbol) {
    return get(symbol.signature(), symbol);
  }

  @CheckForNull
  public MethodBehavior get(String signature) {
    return get(signature, null);
  }

  @CheckForNull
  private MethodBehavior get(String signature, @Nullable Symbol.MethodSymbol symbol) {
    MethodBehavior mb = behaviors.get(signature);
    if(mb != null) {
      return mb;
    }
    if (symbol != null) {
      MethodTree declaration = symbol.declaration();
      if (SymbolicExecutionVisitor.methodCanNotBeOverriden(symbol) && declaration != null) {
        sev.execute(declaration);
        return behaviors.get(signature);
      }
    }

    return hardcodedBehaviors().get(signature);
  }

  /**
   * Do not trigger any new computation of method behavior, just check if there is a known method behavior for the symbol.
   *
   * @param signature The targeted method.
   * @return null for methods having no computed method behavior yet, or its method behavior, based on source or hardcoded set
   */
  @CheckForNull
  public MethodBehavior peek(String signature) {
    // directly query the cache, to not trigger computation of new method behaviors
    MethodBehavior mb = behaviors.get(signature);
    if (mb != null) {
      return mb;
    }
    // check for hardcoded signatures
    return hardcodedBehaviors().get(signature);
  }

  static class HardcodedMethodBehaviors {
    private static final String UNABLE_LOAD_MSG = "[SE] Unable to load hardcoded method behaviors. Defaulting to no hardcoded method behaviors.";

    private static final String[] BEHAVIORS_RESOURCES = {
      "java.lang.json",
      "java.util.json",
      "com.google.common.base.json",
      "org.apache.commons.collections.json",
      "org.apache.commons.lang.json",
      "org.apache.commons.lang3.json",
      "org.apache.logging.log4j.core.util.json",
      "org.eclipse.core.runtime.json",
      "org.springframework.util.json"
    };

    private static final Type LIST_OF_METHOD_BEHAVIORS_TYPE = new TypeToken<List<MethodBehavior>>() {}.getType();

    private final Map<String, MethodBehavior> storedHardcodedMethodBehaviors;

    private HardcodedMethodBehaviors() {
      this.storedHardcodedMethodBehaviors = loadHardcodedBehaviors();
    }

    private static HardcodedMethodBehaviors uniqueInstance = null;

    private static HardcodedMethodBehaviors uniqueInstance() {
      if (uniqueInstance == null) {
        uniqueInstance = new HardcodedMethodBehaviors();
      }
      return uniqueInstance;
    }

    public static Map<String, MethodBehavior> load() {
      return uniqueInstance().storedHardcodedMethodBehaviors;
    }

    private static Map<String, MethodBehavior> loadHardcodedBehaviors() {
      return loadHardcodedBehaviors(
        () -> Arrays.stream(BEHAVIORS_RESOURCES)
          .map(BehaviorCache.class::getResourceAsStream)
          .collect(Collectors.toList()));
    }

    @VisibleForTesting
    static Map<String, MethodBehavior> loadHardcodedBehaviors(Supplier<List<InputStream>> methodBehaviorStreamsSupplier) {
      Map<String, MethodBehavior> result = new LinkedHashMap<>();
      Gson gson = MethodBehaviorJsonAdapter.gson();
      for (InputStream serializedStream : methodBehaviorStreamsSupplier.get()) {
        if (serializedStream == null) {
          LOG.debug(UNABLE_LOAD_MSG);
          return Collections.emptyMap();
        }
        try (Reader reader = new InputStreamReader(serializedStream, StandardCharsets.UTF_8)) {
          List<MethodBehavior> deserialized = gson.fromJson(reader, LIST_OF_METHOD_BEHAVIORS_TYPE);
          deserialized.forEach(methodBehavior -> result.put(methodBehavior.signature(), methodBehavior));
        } catch (Exception e) {
          LOG.error(UNABLE_LOAD_MSG, e);
          return Collections.emptyMap();
        }
      }
      return Collections.unmodifiableMap(result);
    }
  }
}
