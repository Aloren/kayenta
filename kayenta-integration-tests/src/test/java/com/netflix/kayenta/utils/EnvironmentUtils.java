package com.netflix.kayenta.utils;

import java.util.Map;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class EnvironmentUtils {

  public static void registerPropertySource(
      String name, ConfigurableEnvironment environment, Map<String, Object> map) {
    MapPropertySource propertySource = new MapPropertySource(name, map);
    environment.getPropertySources().addFirst(propertySource);
  }
}
