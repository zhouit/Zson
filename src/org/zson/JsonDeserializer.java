package org.zson;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * josn反序列化工具
 * 
 * @author zhou
 * 
 */
@SuppressWarnings("unchecked")
public final class JsonDeserializer{
  private final List<Deserializer> deserializers = new ArrayList<Deserializer>();
  private String dateFormat = "yyyy-MM-dd HH:mm:ss";

  public JsonDeserializer(){
    this.deserializers.add(new PrimitiveDeserializer());
    this.deserializers.add(new CharSequenceDeserializer());
    this.deserializers.add(new DateDeserializer());
    this.deserializers.add(new EnumDeserializer());
    this.deserializers.add(new ArrayDeserializer());
    this.deserializers.add(new CollectionDeserializer());
    this.deserializers.add(new MapDeserializer());
    this.deserializers.add(new ObjectDeserializer());
  }

  public JsonDeserializer dateFormat(String pattern){
    this.dateFormat = pattern;
    return this;
  }

  private Deserializer findDeserializer(Class<?> clazz){
    for(Deserializer desc : deserializers){
      if(desc.canDeserialize(clazz)){
        return desc;
      }
    }

    return null;
  }

  public <T> T deserialize(String source, Class<T> clazz){
    Json json = Json.from(source);
    return new ObjectDeserializer().jsonToObject(json, clazz);
  }

  public Object deserialize(String source, Type type){
    Json json = Json.from(source);
    ParameterizedType pt = (ParameterizedType) type;
    return new ObjectDeserializer().jsonToObject(json, pt.getActualTypeArguments()[0]);
  }

  public Object deserialize(String source){
    Json json = Json.from(source);
    return new ObjectDeserializer().jsonToObject(json);
  }

  private static boolean hasSuper(Class<?> clazz, Class<?> suprz){
    Class<?> temp = clazz;
    while(temp != null && temp != Object.class){
      if(temp == suprz)
        return true;

      temp = temp.getSuperclass();
    }

    return false;
  }

  static boolean hasInterface(Class<?> clazz, Class<?> face){
    if(clazz == face)
      return true;

    Queue<Class<?>> queue = new ArrayDeque<Class<?>>();
    Class<?> tempClass = clazz;
    do{
      for(Class<?> temp : tempClass.getInterfaces()){
        if(temp == face)
          return true;
        queue.offer(temp);
      }

      while(!queue.isEmpty()){
        Class<?> first = queue.remove();
        for(Class<?> temp : first.getInterfaces()){
          if(temp == face)
            return true;
          queue.add(temp);
        }
      }
      tempClass = tempClass.getSuperclass();
    }while(tempClass != Object.class && tempClass != null);

    return false;
  }

  static interface Deserializer{
    boolean canDeserialize(Class<?> clazz);

    Object fromJson(Json json, Class<?> clazz);
  }

  static interface ComplexDeserializer extends Deserializer{
    Object fromJson(Json json, Class<?> rawClass, Type[] types);
  }

  private static class PrimitiveDeserializer implements Deserializer{

    public boolean canDeserialize(Class<?> clazz){
      return clazz.isPrimitive() || hasSuper(clazz, Number.class) || isWrapClass(clazz);
    }

    private boolean isWrapClass(Class<?> clazz){
      try{
        return ((Class<?>) clazz.getField("TYPE").get(null)).isPrimitive();
      }catch(Exception e){
        return false;
      }
    }

    public Object fromJson(Json json, Class<?> clazz){
      if(clazz == byte.class || clazz == Byte.class)
        return json.getDataAsByte();
      else if(clazz == Integer.TYPE || clazz == Integer.class)
        return json.getDataAsInteger();
      else if(clazz == Long.TYPE || clazz == Long.class)
        return json.getDataAsLong();
      else if(clazz == Float.TYPE || clazz == Float.class)
        return json.getDataAsFloat();
      else if(clazz == Double.TYPE || clazz == Double.class)
        return json.getDataAsDouble();
      else if(clazz == Character.TYPE || clazz == Character.class){
        return json.getDataAsString().charAt(0);
      }else if(clazz == BigInteger.class)
        return new BigInteger(json.getDataAsString());
      else if(clazz == BigDecimal.class)
        return new BigDecimal(json.getDataAsString());

      return null;
    }

  }

  private static class CharSequenceDeserializer implements Deserializer{
    public boolean canDeserialize(Class<?> clazz){
      return hasInterface(clazz, CharSequence.class);
    }

    public Object fromJson(Json json, Class<?> clazz){
      if(clazz == String.class)
        return json.getDataAsString();
      else if(clazz == StringBuilder.class)
        return new StringBuffer(json.getDataAsString());
      else if(clazz == StringBuffer.class)
        return new StringBuffer(json.getDataAsString());

      return null;
    }
  }

  private class DateDeserializer implements Deserializer{
    public boolean canDeserialize(Class<?> clazz){
      return hasSuper(clazz, Date.class);
    }

    public Object fromJson(Json json, Class<?> clazz){
      SimpleDateFormat format = new SimpleDateFormat(dateFormat);
      Object result = null;
      try{
        result = format.parse(json.getDataAsString());
      }catch(ParseException e){
        throw new RuntimeException("can't parse date " + json);
      }

      return result;
    }
  }

  private static class EnumDeserializer implements Deserializer{
    public boolean canDeserialize(Class<?> clazz){
      return clazz.isEnum();
    }

    public Object fromJson(Json json, Class<?> clazz){
      Object[] objs = clazz.getEnumConstants();
      for(Object obj : objs){
        if(json.getDataAsString().equals(obj.toString())){
          return obj;
        }
      }

      return null;
    }
  }

  private class ArrayDeserializer extends ObjectDeserializer implements Deserializer{
    public boolean canDeserialize(Class<?> clazz){
      return clazz.isArray();
    }

    public Object fromJson(Json json, Class<?> clazz){
      Object[] result = new Object[json.getArray().size()];
      Class<?> com = clazz.getComponentType();
      for(int i = 0; i < result.length; i++){
        result[i] = jsonToObject(json.getArray().get(i), com);
      }

      return result;
    }
  }

  private class CollectionDeserializer extends ObjectDeserializer implements
      ComplexDeserializer{
    public boolean canDeserialize(Class<?> clazz){
      return hasInterface(clazz, Collection.class);
    }

    public Object fromJson(Json json, Class<?> clazz){
      return fromJson(json, clazz, null);
    }

    public Object fromJson(Json json, Class<?> rawClass, Type[] types){
      Collection<Object> result = null;
      if(rawClass.isInterface()){
        if(hasInterface(rawClass, SortedSet.class)){
          result = new TreeSet<Object>();
        }else if(hasInterface(rawClass, Set.class)){
          result = new HashSet<Object>();
        }else{
          result = new LinkedList<Object>();
        }
      }else{
        try{
          result = (Collection<Object>) rawClass.newInstance();
        }catch(Exception e){
          result = new LinkedList<Object>();
        }
      }

      for(Json temp : json.getArray()){
        if(types == null || types.length == 0){
          result.add(jsonToObject(temp));
        }else if(types.length == 1){
          if(types[0] instanceof Class){
            result.add(jsonToObject(temp, (Class<?>) types[0]));
          }else if(types[0] instanceof ParameterizedType){
            result.add(jsonToObject(temp, (ParameterizedType) types[0]));
          }
        }
      }

      return result;
    }
  }

  private class MapDeserializer extends ObjectDeserializer implements ComplexDeserializer{

    public boolean canDeserialize(Class<?> clazz){
      return hasInterface(clazz, Map.class);
    }

    @Override
    public Object fromJson(Json json, Class<?> clazz){
      return fromJson(json, clazz, null);
    }

    public Object fromJson(Json json, Class<?> rawClass, Type[] types){
      Map<String, Object> result = null;
      try{
        result = (Map<String, Object>) rawClass.newInstance();
      }catch(Exception e){
        result = new LinkedHashMap<String, Object>();
      }

      if(types == null || types.length == 0){
        for(Map.Entry<String, Json> entry : json.getObject().entrySet()){
          result.put(entry.getKey(), jsonToObject(entry.getValue()));
        }
      }else if(types.length == 2){
        if(types[0] != String.class)
          throw new RuntimeException("only supports String keys");
        for(Map.Entry<String, Json> entry : json.getObject().entrySet()){
          if(types[1] instanceof Class){
            result.put(entry.getKey(), jsonToObject(entry.getValue(), (Class<?>) types[1]));
          }else{
            result.put(entry.getKey(),
                jsonToObject(entry.getValue(), (ParameterizedType) types[1]));
          }
        }
      }

      return result;
    }
  }

  private class ObjectDeserializer implements Deserializer{

    public boolean canDeserialize(Class<?> clazz){
      return !clazz.isInterface() && !clazz.isArray();
    }

    public Object fromJson(Json json, Class<?> clazz){
      Object obj = null;
      try{
        obj = clazz.newInstance();
        PropertyDescriptor[] pds = Introspector.getBeanInfo(clazz,
            Introspector.USE_ALL_BEANINFO).getPropertyDescriptors();
        for(Map.Entry<String, Json> entry : json.getObject().entrySet()){
          for(PropertyDescriptor pd : pds){
            if(pd.getName().equals(entry.getKey())){
              Method write = pd.getWriteMethod();
              if(write == null){
                break;
              }else{
                Object value = null;
                Type[] types = write.getGenericParameterTypes();
                if(types.length == 1 && types[0] instanceof ParameterizedType){
                  value = jsonToObject(entry.getValue(), (ParameterizedType) types[0]);
                }else{
                  value = jsonToObject(entry.getValue(), pd.getPropertyType());
                }

                write.invoke(obj, value);
                break;
              }
            }
          }
        }
      }catch(Exception e){
        e.printStackTrace();
      }

      return obj;
    }

    Object jsonToObject(Json json){
      if(json.isObject())
        return new MapDeserializer().fromJson(json, HashMap.class);
      if(json.isArray())
        return new CollectionDeserializer().fromJson(json, LinkedList.class);

      return json.getValue();
    }

    <T> T jsonToObject(Json json, Class<T> clazz){
      return (T) findDeserializer(clazz).fromJson(json, clazz);
    }

    <T> T jsonToObject(Json json, Type type){
      if(type instanceof Class)
        return jsonToObject(json, (Class<T>) type);

      ParameterizedType ptype = (ParameterizedType) type;
      Class<?> rawClass = (Class<?>) ptype.getRawType();
      Type[] types = ptype.getActualTypeArguments();
      Deserializer des = findDeserializer(rawClass);
      if(des instanceof ComplexDeserializer){
        return (T) ((ComplexDeserializer) des).fromJson(json, rawClass, types);
      }

      return (T) des.fromJson(json, rawClass);
    }

  }

}
