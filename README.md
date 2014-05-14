Zson
====

Zson is a library for reading and writing json in Java. Its designed to be simple and light, yet complete.

### it supports:
    1. date format
    2. null process
    3. unicode encode/decode
    4. extends attributes process

### How to use Zson:
    //Serialize
    List<SomeBean> list=new ArrayList<SomeBean>();
    String json=new JsonSerializer().prettyFormat(2).ignoreNull(false).unicodeOutput(true)
                                    .dateFormat("yyyy-MM-dd").serialize(list);
    
    //Deserilize
    Type type=new TypeReference<List<SomeBean>>(){}.getType();
    List<SomeBean> result=(List<SomeBean>)new JsonDeserializer().deserialize(json,type);
    
### 作者博客
  (http://www.zhouhaocheng.cn)