package hs.mediasystem.runner.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import hs.ddif.core.Injector;
import hs.ddif.core.inject.store.BeanDefinitionStore;
import hs.ddif.core.util.AnnotationDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class BasicSetup {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

  public static Injector create() throws IOException {
    Injector injector = new Injector(true);
    JsonNode node = OBJECT_MAPPER.readTree(new File("mediasystem.yaml"));

    injector.registerInstance(node, AnnotationDescriptor.named("configuration"));
    injector.registerInstance(injector.getInstantiator());  // Register instantiator
    injector.registerInstance(injector.getStore());  // Register store

    /*
     * Add configuration fields to Injector
     */

    addConfigurationToInjector(injector.getStore(), node, "");

    return injector;
  }

  private static void addConfigurationToInjector(BeanDefinitionStore store, JsonNode parent, String prefix) {
    Iterator<Entry<String, JsonNode>> fields = parent.fields();

    while(fields.hasNext()) {
      Entry<String, JsonNode> entry = fields.next();
      JsonNode node = entry.getValue();

      if(node.isObject()) {
        addConfigurationToInjector(store, node, prefix + entry.getKey() + ".");

        store.registerInstance(
          new ConfigurationMap(OBJECT_MAPPER.convertValue(node, new TypeReference<Map<String, Object>>() {})),
          AnnotationDescriptor.named(prefix + entry.getKey())
        );
      }
      else if(node.isArray()) {
        for(JsonNode item : node) {
          store.registerInstance(item.asText(), AnnotationDescriptor.named(prefix + entry.getKey()));
        }
      }
      else {
        if(node.isIntegralNumber()) {
          store.registerInstance(node.asLong(), AnnotationDescriptor.named(prefix + entry.getKey()));
        }
        else if(node.isBoolean()) {
          store.registerInstance(node.asBoolean(), AnnotationDescriptor.named(prefix + entry.getKey()));
        }
        else {
          store.registerInstance(node.asText(), AnnotationDescriptor.named(prefix + entry.getKey()));
        }
      }
    }
  }

  private static class ConfigurationMap extends HashMap<String, Object> {
    public ConfigurationMap(Map<String, Object> map) {
      super(map);
    }
  }
}
