package hs.mediasystem.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

public abstract class ConfigurationProvider<T> implements Provider<T> {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .registerModule(new ParameterNamesModule());

  private final Class<T> configClass;
  private final String path;
  private final T defaultValue;

  @Inject @Named("configuration") private JsonNode config;

  public ConfigurationProvider(Class<T> configClass, String path, T defaultValue) {
    this.configClass = configClass;
    this.path = path;
    this.defaultValue = defaultValue;
  }

  @Override
  public T get() {
    JsonNode node = config.findPath(path);

    return node.isMissingNode() ? defaultValue : OBJECT_MAPPER.convertValue(node, configClass);
  }
}
