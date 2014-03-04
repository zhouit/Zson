package org.zson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Json抽象类
 * @author zhou
 *
 */
public class Json{
  List<Json> arrays;
  Map<String, Json> obj;
  Object data;

  public Json(){
  }

  public Json(Map<String, Json> attrs){
    this.obj = attrs;
  }

  public Json(List<Json> arrays){
    this.arrays = arrays;
  }

  public Json(Object data){
    this.data = data;
  }
  
  public static Json from(String json){
    return new JsonReader(json).parse();
  }

  public boolean isArray(){
    return arrays != null && !arrays.isEmpty();
  }

  public boolean isObject(){
    return obj != null && !obj.isEmpty();
  }

  public boolean isSimple(){
    return data != null && data.toString().length() != 0;
  }

  public boolean isNull(){
    return data == null;
  }

  public boolean isEmpty(){
    return !isArray() && !isObject() && !isSimple();
  }

  public List<Json> getArray(){
    return arrays;
  }

  public Map<String, Json> getObject(){
    return obj;
  }

  public Object getValue(){
    return data;
  }

  public String getDataAsString(){
    return data.toString();
  }

  public int getDataAsInteger(){
    return Integer.parseInt(data.toString());
  }

  public boolean getDataAsBoolean(){
    return Boolean.parseBoolean(data.toString());
  }

  public long getDataAsLong(){
    return Long.parseLong(data.toString());
  }

  public float getDataAsFloat(){
    return Float.parseFloat(data.toString());
  }

  public byte getDataAsByte(){
    return Byte.parseByte(data.toString());
  }

  public double getDataAsDouble(){
    return Double.parseDouble(data.toString());
  }

  public String toString(){
    if(isArray())
      return arrays.toString();
    if(isObject())
      return obj.toString();
    if(isSimple())
      return data.toString();

    return null;
  }

  private static final class JsonReader{
    final StringBuilder container = new StringBuilder();
    final boolean esacpe = true;
    int position = 0;
    String source;

    private JsonReader(String json){
      this.source = json;
    }

    Json parse(){
      skipBlank();

      Json result = objectStart();
      if(result == null)
        result = arrayStart();
      if(result == null)
        result = valueSegment();

      return result;
    }

    Json objectStart(){
      if(source.charAt(position) != '{')
        return null;

      position++;
      Map<String, Json> map = new HashMap<String, Json>();
      skipBlank();
      if(source.charAt(position) == '}'){
        position++;
        return new Json(map);
      }

      while(true){
        String field = keySegment();
        if(field.length() == 0)
          break;

        // 略过一个:和QUATO
        position++;
        skipBlank();
        position++;

        map.put(field, valueSegment());
        skipBlank();
        if(source.charAt(position) == '}')
          break;

        // 略过','
        position++;
      }
      // 略过"}"
      position++;
      return new Json(map);
    }

    Json arrayStart(){
      if(source.charAt(position) != '[')
        return null;

      position++;
      List<Json> list = new ArrayList<Json>();
      skipBlank();
      if(source.charAt(position) == ']'){
        position++;
        return new Json(list);  
      }
      
      while(true){
        Json node = valueSegment();
        if(node.isEmpty())
          break;

        list.add(node);
        skipBlank();
        if(source.charAt(position) == ']')
          break;

        // 略过','
        position++;
      }

      // 略过']'
      position++;
      return new Json(list);
    }

    private String keySegment(){
      skipBlank();
      // 略过双引号
      char quato = source.charAt(position++);
      for(; position < source.length(); position++){
        char temp = source.charAt(position);
        if(temp < 32)
          continue;
        if(temp == '\\' && esacpe && position + 1 < source.length()){
          container.append(source.charAt(++position));
          continue;
        }
        if(temp == quato || temp == '}')
          break;

        container.append(temp);
      }

      return getContent();
    }

    Json valueSegment(){
      skipBlank();

      char temp = source.charAt(position);
      Json json = null;
      switch(temp){
      case '{':
        json = objectStart();
        break;
      case '[':
        json = arrayStart();
        break;
      case '\'':
      case '"':
        json = stringStart(temp);
        break;
      default:
        json = boolnumStart();
        break;
      }

      return json;
    }

    Json stringStart(char quato){
      position++;
      for(; position < source.length(); position++){
        char newTemp = source.charAt(position);
        if(newTemp == '\\' && esacpe){
          if(position + 1 < source.length())
            container.append(source.charAt(++position));
          continue;
        }
        if(newTemp == quato){
          position++;
          break;
        }

        container.append(newTemp);
      }

      return new Json(getContent());
    }

    Json boolnumStart(){
      for(; position < source.length(); position++){
        char temp = source.charAt(position);
        if(temp < 32)
          continue;
        if(temp == '\\' && esacpe){
          if(position + 1 < source.length())
            container.append(source.charAt(++position));
          continue;
        }
        if(temp == ',' || temp == '}' || temp == ']'){
          break;
        }

        container.append(temp);
      }

      Object data = null;
      String value = getContent();
      if(value.matches("^-?[1-9]\\d*|0$"))
        data = Long.parseLong(value);
      else if(value.matches("^[1-9]\\d*\\.\\d*|0\\.\\d*|0$"))
        data = Double.parseDouble(value);
      else if("true".equals(value) || "false".equals(value))
        data = Boolean.parseBoolean(value);
      else if("null".equals(value))
        data = null;
      else
        throw new RuntimeException("parse json error near " + value + " at " + source);

      return new Json(data);
    }

    /**
     * 略过空白字符
     */
    private void skipBlank(){
      for(; position < source.length(); position++){
        if(source.charAt(position) > 32)
          break;
      }
    }

    private String getContent(){
      String result = container.toString();
      container.delete(0, container.length());
      if(result.length() == 0)
        result = null;
      return result;
    }

  }

}
