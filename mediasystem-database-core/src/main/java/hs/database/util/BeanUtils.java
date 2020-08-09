package hs.database.util;

public class BeanUtils {

  public static boolean isGetterOrSetterName(String methodName) {
    return ((methodName.startsWith("get") || methodName.startsWith("set")) && Character.isUpperCase(methodName.charAt(3)))
        || (methodName.startsWith("is") && Character.isUpperCase(methodName.charAt(2)));
  }

  public static String toFieldName(String methodName) {
    String fieldName;

    if(methodName.startsWith("get") || methodName.startsWith("set")) {
      fieldName = methodName.substring(3);
    }
    else if(methodName.startsWith("is")) {
      fieldName = methodName.substring(2);
    }
    else {
      throw new IllegalArgumentException("not a getter/setter method name: " + methodName);
    }

    if(!Character.isUpperCase(fieldName.charAt(0))) {
      throw new IllegalArgumentException("not a getter/setter method name: " + methodName);
    }

    return Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
  }
}
