package hs.mediasystem.runner.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import hs.ddif.annotations.Assisted;
import hs.ddif.annotations.Produces;
import hs.ddif.core.Injector;
import hs.ddif.core.api.CandidateRegistry;
import hs.ddif.core.util.Annotations;
import hs.ddif.jsr330.Injectors;
import hs.ddif.plugins.ComponentScannerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

public class BasicSetup {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

  public static Injector create() throws IOException {
    Injector injector = Injectors.autoDiscovering();
    JsonNode node = OBJECT_MAPPER.readTree(new File("mediasystem.yaml"));

    injector.registerInstance(node, named("configuration"));
    injector.registerInstance(injector.getInstanceResolver());
    injector.registerInstance(injector.getCandidateRegistry());

    /*
     * Setup a component scanner factory
     */

    injector.registerInstance(new ComponentScannerFactory(
      new AnnotatedElement[] {Named.class, Singleton.class},
      new AnnotatedElement[] {Inject.class, Produces.class},
      new AnnotatedElement[] {Inject.class, Produces.class},
      new AnnotatedElement[] {Inject.class},
      c -> !c.isAnnotationPresent(Assisted.class)
    ));

    /*
     * Add configuration fields to Injector
     */

    addConfigurationToInjector(injector.getCandidateRegistry(), node, "");

    return injector;
  }

  private static void addConfigurationToInjector(CandidateRegistry registry, JsonNode parent, String prefix) {
    Iterator<Entry<String, JsonNode>> fields = parent.fields();

    while(fields.hasNext()) {
      Entry<String, JsonNode> entry = fields.next();
      JsonNode node = entry.getValue();

      if(node.isObject()) {
        addConfigurationToInjector(registry, node, prefix + entry.getKey() + ".");

        registry.registerInstance(
          new ConfigurationMap(OBJECT_MAPPER.convertValue(node, new TypeReference<Map<String, Object>>() {})),
          named(prefix + entry.getKey())
        );
      }
      else if(node.isArray()) {
        for(JsonNode item : node) {
          registry.registerInstance(item.asText(), named(prefix + entry.getKey()));
        }
      }
      else {
        if(node.isIntegralNumber()) {
          registry.registerInstance(node.asLong(), named(prefix + entry.getKey()));
        }
        else if(node.isBoolean()) {
          registry.registerInstance(node.asBoolean(), named(prefix + entry.getKey()));
        }
        else {
          registry.registerInstance(node.asText(), named(prefix + entry.getKey()));
        }
      }
    }
  }

  private static class ConfigurationMap extends HashMap<String, Object> {
    public ConfigurationMap(Map<String, Object> map) {
      super(map);
    }
  }

  private static final Annotation named(String name) {
    return Annotations.of(Named.class, Map.of("value", name));
  }
}
