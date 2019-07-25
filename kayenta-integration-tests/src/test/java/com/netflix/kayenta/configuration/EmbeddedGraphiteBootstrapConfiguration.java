package com.netflix.kayenta.configuration;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;

import com.netflix.kayenta.utils.EnvironmentUtils;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

@Slf4j
@Configuration
public class EmbeddedGraphiteBootstrapConfiguration {
  private static final int PICKLE_RECEIVER_PORT = 2004;
  private static final int HTTP_PORT = 80;

  @Bean(name = "graphiteWaitStrategy")
  public WaitStrategy graphiteWaitStrategy() {
    return new HostPortWaitStrategy();
  }

  @Bean(name = "graphite", destroyMethod = "stop")
  public GenericContainer graphite(
      ConfigurableEnvironment environment, WaitStrategy graphiteWaitStrategy) {

    GenericContainer container =
        new GenericContainer("graphiteapp/graphite-statsd:1.1.5-12")
            .withLogConsumer(containerLogsConsumer(log))
            .withExposedPorts(PICKLE_RECEIVER_PORT)
            .waitingFor(graphiteWaitStrategy)
            .withStartupTimeout(Duration.ofSeconds(30));
    container.start();

    Map<String, Object> map = registerEnvironment(environment, container);
    log.info("Started Graphite server. Connection details: {}", map);
    return container;
  }

  @NotNull
  private Map<String, Object> registerEnvironment(
      ConfigurableEnvironment environment, GenericContainer container) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("embedded.graphite.picklePort", container.getMappedPort(PICKLE_RECEIVER_PORT));
    map.put("embedded.graphite.httpPort", container.getMappedPort(HTTP_PORT));
    EnvironmentUtils.registerPropertySource("embeddedGraphiteInfo", environment, map);
    return map;
  }
}
