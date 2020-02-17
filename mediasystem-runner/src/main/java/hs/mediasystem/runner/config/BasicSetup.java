package hs.mediasystem.runner.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import hs.ddif.core.Injector;
import hs.ddif.core.JustInTimeDiscoveryPolicy;
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
    Injector injector = new Injector(new JustInTimeDiscoveryPolicy());
    JsonNode node = OBJECT_MAPPER.readTree(new File("mediasystem.yaml"));

    injector.registerInstance(node, AnnotationDescriptor.named("configuration"));
    injector.registerInstance(injector);  // Register injector with itself

    /*
     * Add configuration fields to Injector
     */

    addConfigurationToInjector(injector, node, "");

    return injector;
  }

  private static void addConfigurationToInjector(Injector injector, JsonNode parent, String prefix) {
    Iterator<Entry<String, JsonNode>> fields = parent.fields();

    while(fields.hasNext()) {
      Entry<String, JsonNode> entry = fields.next();
      JsonNode node = entry.getValue();

      if(node.isObject()) {
        addConfigurationToInjector(injector, node, prefix + entry.getKey() + ".");

        injector.registerInstance(
          new ConfigurationMap(OBJECT_MAPPER.convertValue(node, new TypeReference<Map<String, Object>>() {})),
          AnnotationDescriptor.named(prefix + entry.getKey())
        );
      }
      else if(node.isArray()) {
        for(JsonNode item : node) {
          injector.registerInstance(item.asText(), AnnotationDescriptor.named(prefix + entry.getKey()));
        }
      }
      else {
        if(node.isIntegralNumber()) {
          injector.registerInstance(node.asLong(), AnnotationDescriptor.named(prefix + entry.getKey()));
        }
        else if(node.isBoolean()) {
          injector.registerInstance(node.asBoolean(), AnnotationDescriptor.named(prefix + entry.getKey()));
        }
        else {
          injector.registerInstance(node.asText(), AnnotationDescriptor.named(prefix + entry.getKey()));
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
