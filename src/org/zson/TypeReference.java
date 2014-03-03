package org.zson;

import java.lang.reflect.Type;

public abstract class TypeReference<T>{
  
  public Type getType(){
    return getClass().getGenericSuperclass();
  }

}
