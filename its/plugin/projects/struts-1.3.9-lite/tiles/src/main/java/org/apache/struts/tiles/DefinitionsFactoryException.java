/*
 * $Id: DefinitionsFactoryException.java 471754 2006-11-06 14:55:09Z husted $
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
   * Exception thrown when an error occurs while the factory tries to
   * create a new instance mapper.
   */
public class DefinitionsFactoryException extends TilesException
{
  /**
    * Constructor.
    */
  public DefinitionsFactoryException()
    {
    super();
    this.exception = null;
  }

  /**
    * Constructor.
    * @param message The error or warning message.
    */
  public DefinitionsFactoryException(String message)
    {
    super(message);
    this.exception = null;
  }


  /**
    * Create a new <code>DefinitionsFactoryException</code> wrapping an existing exception.
    *
    * <p>The existing exception will be embedded in the new
    * one and its message will become the default message for
    * the DefinitionsFactoryException.</p>
    *
    * @param e The exception to be wrapped.
    */
  public DefinitionsFactoryException(Exception e)
  {
    super();
    this.exception = e;
  }


  /**
    * Create a new <code>DefinitionsFactoryException</code> from an existing exception.
    *
    * <p>The existing exception will be embedded in the new
    * one, but the new exception will have its own message.</p>
    *
    * @param message The detail message.
    * @param e The exception to be wrapped.
    */
  public DefinitionsFactoryException(String message, Exception e)
  {
    super(message);
    this.exception = e;
  }


  /**
    * Return a detail message for this exception.
    *
    * <p>If there is a embedded exception, and if the DefinitionsFactoryException
    * has no detail message of its own, this method will return
    * the detail message from the embedded exception.</p>
    *
    * @return The error or warning message.
    */
  public String getMessage ()
  {
    String message = super.getMessage ();

    if (message == null && exception != null) {
      return exception.getMessage();
    } else {
      return message;
    }
  }


  /**
    * Return the embedded exception, if any.
    * @return The embedded exception, or <code>null</code> if there is none.
    */
  public Exception getException ()
  {
    return exception;
  }

  //////////////////////////////////////////////////////////////////////
  // Internal state.
  //////////////////////////////////////////////////////////////////////


  /**
   * Any "wrapped" exception will be exposed when this is serialized.
   * @serial
   */
  private Exception exception;
}
