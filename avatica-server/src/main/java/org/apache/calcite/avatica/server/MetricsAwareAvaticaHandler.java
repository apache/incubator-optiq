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
package org.apache.calcite.avatica.server;

import com.codahale.metrics.MetricRegistry;

/**
 * An {@link AvaticaHandler} that is capable of collecting metrics.
 */
public interface MetricsAwareAvaticaHandler extends AvaticaHandler {

  /**
   * General prefix for all metrics in a handler.
   */
  String HANDLER_PREFIX = "Handler.";

  /**
   * Name for timing requests from users
   */
  String REQUEST_TIMER_NAME = HANDLER_PREFIX + "RequestTimings";

  /**
   * @return An instance of the {@link MetricRegistry} for this AvaticaHandler.
   */
  MetricRegistry getMetrics();

}

// End MetricsAwareAvaticaHandler.java
