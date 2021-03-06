/*
 * Copyright 2016 Aerospike, Inc.
 *
 * Portions may be licensed to Aerospike, Inc. under one or more contributor
 * license agreements WHICH ARE COMPATIBLE WITH THE APACHE LICENSE, VERSION 2.0.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.aerospike.delivery.db.aerospike;

import com.aerospike.delivery.OurOptions;
import com.aerospike.delivery.util.OurExecutor;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Metering implements Runnable {

  public static final Metering instance = new Metering();

  static volatile int jobQueryWithinRadius;
  static volatile int jobRadiusResults;
  static volatile int jobScans;
  static volatile int jobScanResults;
  static volatile int jobPuts;
  static volatile int jobGets;
  static volatile int droneScans;
  static volatile int droneScanResults;
  static volatile int dronePuts;
  static volatile int droneGets;

  private final int nbSeconds = 3;
  public volatile long renders;
  private volatile Future future;
  private volatile boolean isStopping;


  public static void start() {
    OurExecutor.instance.submit(Metering.instance);
  }

  public synchronized void stop() {
    isStopping = true;
    if (future != null) {
      future.cancel(true);
      future = null;
    }
  }

  @Override
  public synchronized void run() {
    if (isStopping) {
      isStopping = false;
      return;
    }
    future = null;
    printJobStats();
    printDroneStats();
    System.out.println(InfoParser.getClusterLatencyInfo(((AerospikeDatabase)OurOptions.instance.database).client));
    future = OurExecutor.instance.schedule(this, nbSeconds * 1000, TimeUnit.MILLISECONDS);
  }

  private void printJobStats() {
    System.out.format("%d jobs: circle %3d:%4d   puts %4d   gets %2d   scans %2d:%4d\n",
        renders,
        jobQueryWithinRadius / nbSeconds,
        jobRadiusResults     / nbSeconds,
        jobPuts              / nbSeconds,
        jobGets              / nbSeconds,
        jobScans             / nbSeconds,
        jobScanResults       / nbSeconds
    );
    jobQueryWithinRadius = 0;
    jobRadiusResults = 0;
    jobPuts = 0;
    jobGets = 0;
    jobScans = 0;
    jobScanResults = 0;
  }

  private void printDroneStats() {
    System.out.format("%d drones:                 puts %4d   gets %2d   scans %2d:%4d\n",
        renders,
        dronePuts              / nbSeconds,
        droneGets              / nbSeconds,
        droneScans             / nbSeconds,
        droneScanResults       / nbSeconds
    );
    dronePuts = 0;
    droneGets = 0;
    droneScans = 0;
    droneScanResults = 0;
  }

}
