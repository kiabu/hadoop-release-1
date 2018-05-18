/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.fs.azurebfs.contracts.services;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;

/**
 * AbfsThrottlingNetworkTrafficMetrics holds network traffic metrics.
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public interface AbfsThrottlingNetworkTrafficMetrics {
  /**
   * Gets throttling network metrics for write operations.
   * @return throttling network metrics for write operations.
   */
  AbfsThrottlingNetworkThroughputMetrics getWriteMetrics();

  /**
   * Gets throttling network metrics for read operations.
   * @return throttling network metrics for read operations.
   */
  AbfsThrottlingNetworkThroughputMetrics getReadMetrics();

  /**
   * Gets the start time of traffic metrics collection.
   * @return the start time of traffic metrics collection.
   */
  long getStartTime();

  /**
   * Gets the end time of traffic metrics collection.
   * @return the end time of traffic metrics collection.
   */
  long getEndTime();

  /**
   * Stops traffic metrics collection.
   */
  void end();
}