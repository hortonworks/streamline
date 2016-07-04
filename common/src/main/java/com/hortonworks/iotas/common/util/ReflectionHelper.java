package com.hortonworks.iotas.common.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class ReflectionHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ReflectionHelper.class);

    public static <T> T newInstance(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (T) Class.forName(className).newInstance();
    }

    public static <T> T invokeGetter(String propertyName, Object object) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String methodName = "get" + StringUtils.capitalize(propertyName);
        Method method = object.getClass().getMethod(methodName);
        return (T) method.invoke(object);
    }

    public static <T> T invokeSetter(String propertyName, Object object, Object valueToSet) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String methodName = "set" + StringUtils.capitalize(propertyName);
        Method method = object.getClass().getMethod(methodName, valueToSet.getClass());
        return (T) method.invoke(object, valueToSet);
    }

    /**
     * Given a class, this method returns a map of names of all the instance (non static) fields -> type.
     * if the class has any super class it also includes those fields.
     * @param clazz , not null
     * @return
     */
    public static Map<String, Class> getFieldNamesToTypes(Class clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        Map<String, Class> instanceVariableNamesToTypes = new HashMap<>();
        for(Field field : declaredFields) {
            if(!Modifier.isStatic(field.getModifiers())) {
                LOG.trace("clazz {} has field {} with type {}", clazz.getName(), field.getName(), field.getType().getName());
                instanceVariableNamesToTypes.put(field.getName(), field.getType());
            } else {
                LOG.trace("clazz {} has field {} with type {}, which is static so ignoring", clazz.getName(), field.getName(), field.getType().getName());
            }
        }

        if(!clazz.getSuperclass().equals(Object.class)) {
            instanceVariableNamesToTypes.putAll(getFieldNamesToTypes(clazz.getSuperclass()));
        }
        return instanceVariableNamesToTypes;
    }
}
