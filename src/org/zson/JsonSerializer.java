package org.zson;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * json序列化工具,默认不序列化transient和static变量
 * 
 * @author zhou
 * 
 */
@SuppressWarnings("unchecked")
public final class JsonSerializer{
  private final List<Serializer> serializers = new ArrayList<Serializer>();
  private static final String separator = "\n";
  static final char QUOTE = '"';

  private String dateFormat = "yyyy-MM-dd HH:mm:ss";
  private boolean ignoreParentAttr = false;
  private boolean escapeChar = false;
  private boolean ignoreNull = true;
  private boolean unicode = false;

  private int depth = 1;
  private String indent;

  public JsonSerializer(){
    this.serializers.add(new BooleanNumberSerializer());
    this.serializers.add(new CharSeqEnumSerializer());
    this.serializers.add(new DateSerializer());
    this.serializers.add(new CollectionArraySerializer());
    this.serializers.add(new MapSerializer());
    this.serializers.add(new ObjectSerializer());
  }

  public String serialize(Object obj){
    String result = new ObjectSerializer().serialize(obj);
    depth = 1;
    return unicode && result != null ? Unicoder.encode(result) : result;
  }

  /**
   * 设定日期格式
   * 
   * @param pattern
   * @return
   */
  public JsonSerializer dateFormat(String pattern){
    this.dateFormat = pattern;
    return this;
  }

  /**
   * 是否忽略父类属性(默认不忽略)
   * 
   * @param ignore
   * @return
   */
  public JsonSerializer ignoreParentAttr(boolean ignore){
    this.ignoreParentAttr = ignore;
    return this;
  }

  /**
   * 忽略null值(默认忽略)
   * 
   * @param ignore
   * @return
   */
  public JsonSerializer ignoreNull(boolean ignore){
    this.ignoreNull = ignore;
    if(!ignoreNull) serializers.add(new NullSerializer());
    return this;
  }

  /**
   * 设置缩进间隔
   * 
   * @param indentSpace
   * @return
   */
  public JsonSerializer prettyFormat(int indentSpace){
    indent = "";
    for(int i = 0; i < indentSpace; i++){
      indent += " ";
    }
    return this;
  }

  /**
   * 是否使用unicode编码字符串,默认不启用
   * 
   * @param unicode
   * @return
   */
  public JsonSerializer unicodeOutput(boolean unicode){
    this.unicode = unicode;
    return this;
  }

  /**
   * 是否对特殊字符转义(默认不转义)
   * 
   * @param escape
   * @return
   */
  public JsonSerializer escapeChar(boolean escape){
    this.escapeChar = escape;
    return this;
  }

  private static String trimLeft(String target){
    int index = 0;
    for(; index < target.length(); index++){
      if(target.charAt(index) != ' ') break;
    }

    return target.substring(index);
  }

  public static interface Serializer{
    /**
     * 是否能序列化此对象
     * 
     * @param obj
     * @return
     */
    boolean canSerialize(Object obj);

    /**
     * 序列化目标对象 返回序列化后内容
     * 
     * @param obj
     * @return
     */
    String serializeObject(Object obj);
  }

  private static class BooleanNumberSerializer implements Serializer{

    public boolean canSerialize(Object obj){
      return obj instanceof Boolean || obj instanceof Number;
    }

    public String serializeObject(Object obj){
      return obj.toString();
    }

  }

  private static class NullSerializer implements Serializer{

    public boolean canSerialize(Object obj){
      return obj == null;
    }

    public String serializeObject(Object obj){
      return "null";
    }

  }

  private class CharSeqEnumSerializer implements Serializer{

    public boolean canSerialize(Object obj){
      return obj instanceof CharSequence || obj instanceof Enum;
    }

    public String serializeObject(Object obj){
      String result = obj.toString();
      if(escapeChar){
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < result.length(); i++){
          char c = result.charAt(i);
          if(c == '"' || c == '\\') builder.append("\\");
          
          builder.append(c == '\t' ? "\\t" : c);
        }

        result = builder.toString();
      }

      return QUOTE + result + QUOTE;
    }

  }

  private class DateSerializer implements Serializer{

    public boolean canSerialize(Object obj){
      return obj instanceof Date;
    }

    public String serializeObject(Object obj){
      DateFormat format = new SimpleDateFormat(dateFormat);
      return QUOTE + format.format(obj) + QUOTE;
    }

  }

  private class CollectionArraySerializer extends ObjectSerializer{

    public boolean canSerialize(Object obj){
      return obj != null && obj.getClass().isArray() || obj instanceof Collection;
    }

    public String serializeObject(Object obj){
      StringBuilder result = new StringBuilder();
      beforeSerialize(obj, result);
      int length = result.length();

      serializeArray(obj, result);
      serializeCollection(obj, result);

      if(result.length() != length) super.afterSerializeAttrs(result);

      afterSerialize(obj, result);
      return result.toString();
    }

    private void serializeArray(Object obj, StringBuilder content){
      if(!obj.getClass().isArray()) return;

      Object[] objs = (Object[]) obj;
      for(Object temp : objs){
        if(temp == null && ignoreNull) continue;

        content.append(serialize(temp));
        afterSerializeAttr(temp, content);
      }
    }

    private void serializeCollection(Object obj, StringBuilder content){
      if(!(obj instanceof Collection)) return;

      Collection<Object> collec = (Collection<Object>) obj;
      for(Object temp : collec){
        if(temp == null && ignoreNull) continue;

        content.append(serialize(temp));
        afterSerializeAttr(temp, content);
      }
    }

    void beforeSerialize(Object obj, StringBuilder content){
      appIndent(content);
      content.append('[');
      appSeparator(content);
      depth++;
    }

    void afterSerialize(Object obj, StringBuilder content){
      depth--;
      appSeparator(content);
      appIndent(content);
      content.append(']');
      appSeparator(content);
    }

  }

  private class MapSerializer extends ObjectSerializer{

    public boolean canSerialize(Object obj){
      return obj instanceof Map;
    }

    public String serializeObject(Object obj){
      StringBuilder result = new StringBuilder();
      beforeSerialize(obj, result);

      int length = result.length();
      super.serializeAttrs(obj, result);

      serializeAttrs(obj, result);
      if(result.length() != length) afterSerializeAttrs(result);

      afterSerialize(obj, result);
      return result.toString();
    }

    void serializeAttrs(Object obj, StringBuilder content){
      Map<String, Object> map = (Map<String, Object>) obj;
      Object lastValue = null;
      for(Map.Entry<String, Object> entry : map.entrySet()){
        if(entry.getValue() == null && ignoreNull) continue;

        appIndent(content);
        lastValue = entry.getValue();
        content.append(QUOTE).append(entry.getKey()).append(QUOTE).append(':');
        content.append(trimLeft(serialize(lastValue)));
        afterSerializeAttr(lastValue, content);
      }

    }

  }

  private class ObjectSerializer implements Serializer{
    private static final String EXCLUDE_PACAKGE = "java";

    public boolean canSerialize(Object obj){
      return obj != null;
    }

    public String serializeObject(Object obj){
      StringBuilder result = new StringBuilder();
      beforeSerialize(obj, result);

      int length = result.length();
      serializeAttrs(obj, result);

      if(result.length() != length) afterSerializeAttrs(result);

      afterSerialize(obj, result);
      return result.toString();
    }

    /**
     * 序列化对象前调用
     * 
     * @param obj
     * @param content
     */
    void beforeSerialize(Object obj, StringBuilder content){
      appIndent(content);
      content.append("{");
      appSeparator(content);
      depth++;
    }

    /**
     * 序列化对象后调用
     * 
     * @param obj
     * @param content
     */
    void afterSerialize(Object obj, StringBuilder content){
      depth--;
      appSeparator(content);
      appIndent(content);
      content.append("}");
      appSeparator(content);
    }

    /**
     * 序列化所有属性后调用
     * 
     * @param content
     */
    void afterSerializeAttrs(StringBuilder content){
      if(indent == null) content.deleteCharAt(content.length() - 1);
      else
        // 1个换行符 1个','
        content.delete(content.length() - 2, content.length());
    }

    /**
     * 序列化单个属性后调用
     * 
     * @param field
     * @param content
     */
    void afterSerializeAttr(Object field, StringBuilder content){
      if(indent == null || field == null || isPrimitive(field)){
        content.append(',');
        appSeparator(content);
      }else{
        content.insert(content.length() - 1, ',');
      }
    }

    /**
     * 序列化所有属性
     * 
     * @param obj
     * @param content
     */
    void serializeAttrs(Object obj, StringBuilder content){
      Class<?> clazz = obj.getClass();
      while(!clazz.getName().startsWith(EXCLUDE_PACAKGE) && !ignoreParentAttr){
        serializeAttrs(obj, content, clazz);

        clazz = clazz.getSuperclass();
      }
    }

    private void serializeAttrs(Object obj, StringBuilder content, Class<?> clazz){
      try{
        Field[] fields = clazz.getDeclaredFields();
        for(Field field : fields){
          if(Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;

          Object fieldValue = null;
          boolean access = field.isAccessible();
          field.setAccessible(true);
          fieldValue = field.get(obj);
          field.setAccessible(access);

          if(fieldValue == null && ignoreNull) continue;

          appIndent(content);
          content.append(QUOTE).append(field.getName()).append(QUOTE).append(':');
          content.append(trimLeft(serialize(fieldValue)));
          afterSerializeAttr(fieldValue, content);
        }

      }catch(Exception e){
        e.printStackTrace();
      }
    }

    public String serialize(Object obj){
      String result = null;
      for(Serializer serializer : serializers){
        if(serializer.canSerialize(obj)){
          result = serializer.serializeObject(obj);
          break;
        }
      }

      return result;
    }

    protected void appIndent(StringBuilder content){
      if(indent == null) return;

      for(int i = 1; i < depth; i++){
        content.append(indent);
      }
    }

    protected void appSeparator(StringBuilder content){
      if(indent == null) return;

      content.append(separator);
    }

    private boolean isPrimitive(Object obj){
      return obj instanceof Boolean || obj instanceof Number || obj instanceof CharSequence
          || obj instanceof Date || obj instanceof Enum;
    }

  }

}
