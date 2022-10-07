package hs.mediasystem.db.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.Arrays;
import java.util.List;

/**
 * A module for Jackson which allows it to determine the subtype during deserialization
 * based on a sealed class hierarchy.
 */
public class SealedClassesModule extends SimpleModule {

  @Override
  public void setupModule(SetupContext context) {
    context.appendAnnotationIntrospector(new SealedClassesAnnotationIntrospector());
  }

  private static class SealedClassesAnnotationIntrospector extends JacksonAnnotationIntrospector {
    @Override
    public List<NamedType> findSubtypes(Annotated annotated) {
      if(annotated.getAnnotated() instanceof Class<?> cls && cls.isSealed()) {
        Class<?>[] permittedSubclasses = cls.getPermittedSubclasses();

        if(permittedSubclasses.length > 0) {
          return Arrays.stream(permittedSubclasses).map(NamedType::new).toList();
        }
      }

      return null;
    }

    @Override
    protected TypeResolverBuilder<?> _findTypeResolver(MapperConfig<?> config, Annotated annotated, JavaType baseType) {
      if(annotated.getAnnotated() instanceof Class<?> cls && cls.isSealed()) {
        return super._findTypeResolver(config, AnnotatedClassResolver.resolveWithoutSuperTypes(config, Dummy.class), baseType);
      }

      return super._findTypeResolver(config, annotated, baseType);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    private static class Dummy {
    }
  }
}