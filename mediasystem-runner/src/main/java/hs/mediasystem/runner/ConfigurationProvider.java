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

  @Inject @Named("configuration") private JsonNode config;

  public ConfigurationProvider(Class<T> configClass, String path) {
    this.configClass = configClass;
    this.path = path;
  }

  @Override
  public T get() {
    return OBJECT_MAPPER.convertValue(config.findPath(path), configClass);
  }
}
