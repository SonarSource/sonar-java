/*
 * $Id: UntypedAttribute.java 471754 2006-11-06 14:55:09Z husted $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.struts.tiles;

/**
 * Common implementation of attribute definition.
 */
public class UntypedAttribute implements AttributeDefinition {

    /**
     * Role associated to this attribute.
     */
    protected String role = null;

    protected Object value=null;

    /**
     * Constructor.
     * @param value Object to store.
     */
    public UntypedAttribute(Object value) {
        this.value = value;
    }

    /**
     * Constructor.
     * @param value Object to store.
     * @param role Asociated role.
     */
    public UntypedAttribute(Object value, String role) {
        this.value = value;
        this.role = role;
    }

    /**
     * Get role.
     */
    public String getRole() {
        return role;
    }

    /**
     * Set role.
     * @param role Associated role.
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Get value.
     */
    public Object getValue() {
        return value;
    }

    /**
     * Set value.
     * @param value New value.
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Get String representation of this object.
     */
    public String toString() {
        return value.toString();
    }

}
