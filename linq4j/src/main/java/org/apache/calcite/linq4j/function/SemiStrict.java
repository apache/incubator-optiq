/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.linq4j.function;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation applied to a user-defined function that indicates that
 * the function always returns null if one or more of its arguments
 * are null but also may return null at other times.
 *
 * <p>Compare with {@link Strict}:
 * <ul>
 *   <li>A strict function returns null if and only if it has a null argument
 *   <li>A semi-strict function returns null if it has a null argument
 * </ul>
 */
@Target({METHOD, TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Experimental
public @interface SemiStrict {
}

// End SemiStrict.java
