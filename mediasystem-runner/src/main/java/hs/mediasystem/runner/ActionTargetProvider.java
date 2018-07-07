package hs.mediasystem.runner;

import hs.ddif.core.util.TypeUtils;
import hs.mediasystem.framework.actions.DummyExposedProperty;
import hs.mediasystem.framework.actions.Expose;
import hs.mediasystem.framework.actions.ExposedActionObjectProperty;
import hs.mediasystem.framework.actions.ExposedBooleanProperty;
import hs.mediasystem.framework.actions.ExposedListBackedObjectProperty;
import hs.mediasystem.framework.actions.ExposedMember;
import hs.mediasystem.framework.actions.ExposedMethod;
import hs.mediasystem.framework.actions.ExposedNumberProperty;
import hs.mediasystem.framework.actions.Member;
import hs.mediasystem.framework.actions.ValueBuilder;
import hs.mediasystem.util.BeanUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;

import javax.inject.Singleton;

@Singleton
public class ActionTargetProvider {
  private static final Map<Class<?>, List<ExposedMember>> exposedMembersByClass = new HashMap<>();
  private static final Map<Member, ExposedMember> exposedPropertiesByMember = new HashMap<>();
//  private static final Logger LOGGER = Logger.getLogger(ActionTargetProvider.class.getName());

  public List<ActionTarget> getActionTargets(Class<?> rootClass) {
    return createActionTargets(rootClass, new ArrayList<>());
  }

  private List<ActionTarget> createActionTargets(Class<?> rootClass, List<ExposedMember> currentPath) {
    List<ActionTarget> actionTargets = new ArrayList<>();

    for(ExposedMember exposedMember : findExposedMembers(rootClass)) {
//      LOGGER.fine("Attempting property : " + exposedMember.getMember() + " ---> " + exposedMember.getMember().getType());

      currentPath.add(exposedMember);

      if(Property.class.isAssignableFrom(exposedMember.getMember().getType())) {
        Class<?> propertyClass = TypeUtils.determineTypeOfImplementedType(exposedMember.getMember().getGenericType(), Property.class);

        actionTargets.add(new ActionTarget(currentPath));

        if(propertyClass != null) {
          actionTargets.addAll(createActionTargets(propertyClass, currentPath));
        }
      }
      else if(exposedMember instanceof ExposedMethod) {
        actionTargets.add(new ActionTarget(currentPath));
      }
      else {
        throw new IllegalStateException("Unhandled exposed member: " + exposedMember.getMember().getType());
      }

      currentPath.remove(currentPath.size() - 1);
    }

    return actionTargets;
  }

  private static List<ExposedMember> findExposedMembers(Class<?> presentationClass) {
    if(!exposedMembersByClass.containsKey(presentationClass)) {
      cacheExposedMembers(presentationClass);
    }

    return exposedMembersByClass.get(presentationClass);
  }

  private static ExposedMember getExposedMember(Member member) {
    ExposedMember exposedProperty = exposedPropertiesByMember.get(member);

    if(exposedProperty == null) {
      exposedProperty = createExposedMember(member);
      exposedPropertiesByMember.put(member, exposedProperty);
    }

    return exposedProperty;
  }

  private static ExposedMember createExposedMember(Member member) {
    Expose expose = member.getAnnotation(Expose.class);
    Class<?> cls = member.getDeclaringClass();
    Class<?> type = member.getType();
    String name = expose.name().isEmpty() ? member.getName() : expose.name();

    try {
      if(type.isAssignableFrom(ObjectProperty.class)) {
        if(!expose.values().isEmpty()) {
          return new ExposedListBackedObjectProperty(member, name, createMemberFromName(cls, expose.values()));
        }
        else if(expose.valueBuilder() != Expose.NullValueBuilder.class) {
          @SuppressWarnings("unchecked")
          ValueBuilder<Object> valueBuilder = (ValueBuilder<Object>)expose.valueBuilder().getDeclaredConstructor().newInstance();
          return new ExposedActionObjectProperty(member, name, valueBuilder);
        }
      }
      else if(type.isAssignableFrom(BooleanProperty.class)) {
        return new ExposedBooleanProperty(member, name);
      }
      else if(type.isAssignableFrom(IntegerProperty.class) || type.isAssignableFrom(LongProperty.class) || type.isAssignableFrom(FloatProperty.class) || type.isAssignableFrom(DoubleProperty.class)) {
        return new ExposedNumberProperty(member, name);
      }
      else if(type == void.class) {
        // Just a method that is exposed, create action for it
        return new ExposedMethod(member, name);
      }
    }
    catch(IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }

    return new DummyExposedProperty(member, name);
  }

  private static Member createMemberFromName(Class<?> cls, String name) {
    try {
      return new Member(cls.getDeclaredField(name));
    }
    catch(NoSuchFieldException e) {
      try {
        return new Member(cls.getDeclaredMethod(name));
      }
      catch(NoSuchMethodException e2) {
        try {
          return new Member(cls.getDeclaredMethod("is" + BeanUtils.capitalize(name)));
        }
        catch(NoSuchMethodException e3) {
          try {
            return new Member(cls.getDeclaredMethod("get" + BeanUtils.capitalize(name)));
          }
          catch(NoSuchMethodException e4) {
            throw new IllegalStateException(e2);
          }
        }
      }
    }
  }

  private static void cacheExposedMembers(Class<?> cls) {
    List<ExposedMember> exposedMembers = exposedMembersByClass.computeIfAbsent(cls, k -> new ArrayList<>());

    for(Field field : cls.getFields()) {
      Expose expose = field.getAnnotation(Expose.class);

      if(expose != null) {
        exposedMembers.add(getExposedMember(new Member(field)));

        Class<?> type = field.getType();

        if(type.isAssignableFrom(ObjectProperty.class)) {
          Type firstGenericType = ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];

          cacheExposedMembers(firstGenericType instanceof ParameterizedType ? (Class<?>)((ParameterizedType)firstGenericType).getRawType() : (Class<?>)firstGenericType);
        }
      }
    }

    for(Method method : cls.getMethods()) {
      Expose expose = method.getAnnotation(Expose.class);

      if(expose != null) {
        exposedMembers.add(getExposedMember(new Member(method)));
      }
    }
  }
}
