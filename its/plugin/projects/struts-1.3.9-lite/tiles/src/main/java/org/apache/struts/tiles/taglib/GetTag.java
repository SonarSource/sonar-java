/*
 * $Id: GetTag.java 471754 2006-11-06 14:55:09Z husted $
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


package org.apache.struts.tiles.taglib;

/**
 * This is the tag handler for &lt;tiles:get&gt;, which gets
 * content from the request scope and either includes the content or prints
 * it, depending upon the value of the content's <code>direct</code> attribute.
 *
 * This tag is intended to be compatible with the same tag from Templates (David Geary).
 * Implementation extends InsertTag for facility (no so well).
 * The only difference is the default value of attribute 'ignore', which is <code>true</code>
 * for this tag (default behavior of David Geary's templates).
 */
public class GetTag extends InsertTag {


    /**
     * Constructor.
     * Set default value for 'isErrorIgnored' to <code>true</code>.
     */
    public GetTag() {
        isErrorIgnored = true;
    }

    /**
     * Release all allocated resources.
     */
    public void release() {

        super.release();
        isErrorIgnored = true;
    }


}
