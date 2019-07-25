package com.netflix.kayenta.utils;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;
import org.awaitility.core.ThrowingRunnable;

public class AwaitilityUtils {

  public static void awaitThirtySecondsUntil(ThrowingRunnable throwingRunnable) {
    await()
        .atMost(30, TimeUnit.SECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .untilAsserted(throwingRunnable);
  }
}
