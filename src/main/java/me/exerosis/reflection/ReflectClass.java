package me.exerosis.reflection;

import me.exerosis.reflection.exceptions.notfound.ConstructorNotFoundException;
import me.exerosis.reflection.exceptions.notfound.FieldNotFoundException;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReflectClass<T> {
    private Class<?> clazz;
    private T instance;
    private ArrayList<ReflectMethod> allMethods = new ArrayList<>();
    private ArrayList<ReflectField<Object>> allFields = new ArrayList<>();
    private ArrayList<Constructor<?>> allConstructors = new ArrayList<>();

    protected ReflectClass(Class<?> clazz) {
        this.clazz = clazz;
        fillLists();
    }

    @SuppressWarnings("unchecked")
    protected ReflectClass(T instance) {
        this.instance = instance;
        if (instance != null)
            clazz = instance.getClass();
        fillLists();
    }

    private void fillLists() {
        Constructor<?>[] constructors = clazz.getConstructors();
        allConstructors.addAll(Arrays.asList(constructors));
        allConstructors.addAll(Arrays.asList(clazz.getDeclaredConstructors()));

        for (Method method : clazz.getMethods())
            allMethods.add(new ReflectMethod(method));
        for (Field field : clazz.getFields())
            allFields.add(new ReflectField<>(field, instance));

        Class<?> loopClass = clazz;
        while (loopClass != null && !loopClass.equals(Class.class)) {
            for (Method method : loopClass.getDeclaredMethods())
                allMethods.add(new ReflectMethod(method));
            for (Field field : loopClass.getDeclaredFields())
                allFields.add(new ReflectField<>(field, instance));
            loopClass = loopClass.getSuperclass();
        }
    }

    //TODO add all potential getters :D
    //Field getters.
    public <K> ReflectField<K> getField(Class<K> type) {
        return getField(new ReflectClass<K>(type));
    }
    public <K> ReflectField<K> getField(ReflectClass<K> type) {
        return getField(type, 0);
    }

    public <K> ReflectField<K> getField(Class<K> type, int pos) {
        return getField(new ReflectClass<>(type), pos);
    }

    public <K> ReflectField<K> getField(ReflectClass<K> type, int pos) {
        return getField("", type, pos);
    }

    public <K> ReflectField<K> getField(Class<K> type, String name) {
        return getField(new ReflectClass<>(type), name);
    }

    public <K> ReflectField<K> getField(ReflectClass<K> type, String name) {
        return getField(name, type, -1);
    }

    public ReflectField<Object> getField(String name) {
        return getField(name, null, -1);
    }


    @SuppressWarnings("unchecked")
    private <K> ReflectField<K> getField(String name, ReflectClass<K> type, int pos) {
        int index = -1;
        for (ReflectField<Object> field : allFields) {
            if (field.getName().equals(name))
                return (ReflectField<K>) field;

            if (type == null)
                continue;

            /*if(CorrespondingType.isPrimitive(type)){
                if(field.getReflectType().isClassEqual(type))
            }*/
            if (type.getClazz().isAssignableFrom(field.getType())) {
                index++;
                if (index == pos)
                    return (ReflectField<K>) field;
            }
        }

        throw new FieldNotFoundException(clazz, name, type == null ? null : type.getClazz(), pos);
    }

    public Class<?>[] getGenericTypes() {
        Type[] genericTypes = ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments();
        Class<?>[] genericClassTypes = new Class<?>[genericTypes.length];
        for (int x = 0; x < genericTypes.length; x++)
            genericClassTypes[x] = (Class<?>) genericTypes[x];
        return genericClassTypes;
    }

    public Constructor<?> getConstructor(int index) {
        int x = 0;
        for (Constructor<?> constructor : allConstructors)
            if (x++ == index)
                return constructor;
        throw new ConstructorNotFoundException(clazz);
    }
    public Constructor<?> getConstructor(Class<?>... paramTypes) {
        Class<?>[] types = CorrespondingType.getPrimitive(paramTypes);
        for (Constructor<?> constructor : allConstructors) {
            Class<?>[] constructorTypes = CorrespondingType.getPrimitive(constructor.getParameterTypes());
            if (CorrespondingType.compare(types, constructorTypes))
                return constructor;
        }
        throw new ConstructorNotFoundException(clazz, types);
    }

    @SuppressWarnings("unchecked")
    public T newInstance(Object... args) {
        try {
            instance = (T) getConstructor(CorrespondingType.getPrimitive(args)).newInstance(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return instance;
    }

    //TODO Remake method getters to work with return type, params, and name as well as create exception for method not found!
    //Method getters.
    public ReflectMethod getMethodByReturn(Class<?> returnType) {
        for (ReflectMethod method : allMethods)
            if (returnType.equals(method.getReturnType()))
                return method;
        throw new RuntimeException("Method not found!");
    }

    public ReflectMethod getMethod(Class<?>... paramTypes) {
        ReflectClass<?>[] classes = new ReflectClass[paramTypes.length];
        for (int x = 0; x < classes.length; x++)
            classes[x] = Reflect.Class(paramTypes[x]);
        return getMethod(classes);
    }

    public ReflectMethod getMethod(ReflectClass<?>... paramTypes) {
        Class<?>[] t = CorrespondingType.getPrimitive(paramTypes);
        for (ReflectMethod method : getMethods()) {
            Class<?>[] types = CorrespondingType.getPrimitive(method.getParameterTypes());
            if (CorrespondingType.compare(types, t))
                return method;
        }
        return null;
    }

    public ReflectMethod getMethod(String name, Class<?>... paramTypes) {
        Class<?>[] t = CorrespondingType.getPrimitive(paramTypes);
        for (ReflectMethod method : getMethods()) {
            Class<?>[] types = CorrespondingType.getPrimitive(method.getParameterTypes());
            if (method.getName().equals(name) && CorrespondingType.compare(types, t))
                return method;
        }
        return null;
    }

    public ReflectMethod getMethod(String name) {
        for (ReflectMethod method : getMethods())
            if (method.getName().equals(name))
                return method;
        return null;
    }

    public boolean isInstance(Object object) {
        return clazz.isInstance(object);
    }

    //Fields and methods.
    public List<ReflectMethod> getMethods() {
        List<ReflectMethod> methods = new ArrayList<>();
        for (Method method : clazz.getMethods())
            methods.add(new ReflectMethod(method, instance));
        return methods;
    }

    public List<ReflectField<Object>> getFields() {
        List<ReflectField<Object>> fields = new ArrayList<>();
        for (Field field : clazz.getFields())
            fields.add(new ReflectField<>(field, instance));
        return fields;
    }

    //Declared fields and methods.
    public List<ReflectMethod> getDeclaredMethods() {
        List<ReflectMethod> declaredMethods = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods())
            declaredMethods.add(new ReflectMethod(method, instance));
        return declaredMethods;
    }

    public List<ReflectField<Object>> getDeclaredFields() {
        List<ReflectField<Object>> declaredFields = new ArrayList<>();
        for (Field field : clazz.getFields())
            declaredFields.add(new ReflectField<>(field, instance));
        return declaredFields;
    }

    //All fields and methods
    public ArrayList<ReflectMethod> getAllMethods() {
        return allMethods;
    }

    public ArrayList<ReflectField<Object>> getAllFields() {
        return allFields;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(T instance) {
        this.instance = instance;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public Package getPackage() {
        return clazz.getPackage();
    }

    public boolean isClassEqual(Class<?> otherClass) {
        return clazz.equals(otherClass);
    }

    public boolean isClassEqual(ReflectClass<?> otherClass) {
        return clazz.equals(otherClass.getClazz());
    }
}