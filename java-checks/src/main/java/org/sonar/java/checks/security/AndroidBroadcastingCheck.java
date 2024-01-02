/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.security;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

import static org.sonar.plugins.java.api.tree.Tree.Kind.NULL_LITERAL;

@Rule(key = "S5320")
public class AndroidBroadcastingCheck extends AbstractMethodDetection {

  private static final String MESSAGE = "Make sure that broadcasting intents is safe here.";

  private static final MethodMatchers SEND_BROADCAST = androidContext().names("sendBroadcast").withAnyParameters().build();
  private static final MethodMatchers SEND_BROADCAST_AS_USER = androidContext().names("sendBroadcastAsUser").withAnyParameters().build();
  private static final MethodMatchers SEND_ORDERED_BROADCAST = androidContext().names("sendOrderedBroadcast").withAnyParameters().build();
  private static final MethodMatchers SEND_ORDERED_BROADCAST_AS_USER = androidContext().names("sendOrderedBroadcastAsUser").withAnyParameters().build();
  private static final MethodMatchers SEND_STICKY_BROADCAST = androidContext().names("sendStickyBroadcast").withAnyParameters().build();
  private static final MethodMatchers SEND_STICKY_BROADCAST_AS_USER = androidContext().names("sendStickyBroadcastAsUser").withAnyParameters().build();
  private static final MethodMatchers SEND_STICKY_ORDERED_BROADCAST = androidContext().names("sendStickyOrderedBroadcast").withAnyParameters().build();
  private static final MethodMatchers SEND_STICKY_ORDERED_BROADCAST_AS_USER = androidContext().names("sendStickyOrderedBroadcastAsUser").withAnyParameters().build();
  private static final MethodMatchers STICKY_BROADCAST = MethodMatchers.or(SEND_STICKY_BROADCAST,
    SEND_STICKY_BROADCAST_AS_USER, SEND_STICKY_ORDERED_BROADCAST, SEND_STICKY_ORDERED_BROADCAST_AS_USER);

  private static MethodMatchers.NameBuilder androidContext() {
    return MethodMatchers.create().ofSubTypes("android.content.Context");
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      SEND_BROADCAST,
      SEND_BROADCAST_AS_USER,
      SEND_ORDERED_BROADCAST,
      SEND_ORDERED_BROADCAST_AS_USER,
      SEND_STICKY_BROADCAST,
      SEND_STICKY_BROADCAST_AS_USER,
      SEND_STICKY_ORDERED_BROADCAST,
      SEND_STICKY_ORDERED_BROADCAST_AS_USER
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (isSendBroadcast(mit) || isSendBroadcastAsUser(mit) || isSendOrderedBroadcast(mit) ||
      isSendOrderedBroadcastAsUser(mit) || STICKY_BROADCAST.matches(mit)) {
      reportIssue(mit.methodSelect(), MESSAGE);
    }
  }

  private static boolean isSendBroadcast(MethodInvocationTree mit) {
    return SEND_BROADCAST.matches(mit) && (mit.arguments().size() < 2 || mit.arguments().get(1).is(NULL_LITERAL));
  }

  private static boolean isSendBroadcastAsUser(MethodInvocationTree mit) {
    return SEND_BROADCAST_AS_USER.matches(mit) && (mit.arguments().size() < 3 || mit.arguments().get(2).is(NULL_LITERAL));
  }

  private static boolean isSendOrderedBroadcast(MethodInvocationTree mit) {
    return SEND_ORDERED_BROADCAST.matches(mit) && mit.arguments().size() > 1 && mit.arguments().get(1).is(NULL_LITERAL);
  }

  private static boolean isSendOrderedBroadcastAsUser(MethodInvocationTree mit) {
    return SEND_ORDERED_BROADCAST_AS_USER.matches(mit) && mit.arguments().size() > 2 && mit.arguments().get(2).is(NULL_LITERAL);
  }
}
