Zson
====

Zson is a library for reading and writing json in Java. Its designed to be simple and light, yet complete.

### How to use Zson:
    //Serialize
    List<SomeBean> list=new ArrayList<SomeBean>();
    String json=new JsonSerializer().prettyFormat(2).ignoreNull(false)
                                    .dateFormat("yyyy-MM-dd").serialize(list);
    
    //Deserilize
    Type type=new TypeReference<List<SomeBean>>(){}.getType();
    List<SomeBean> result=new JsonDeserializer().deserialize(json,type);
    
### 作者博客
  (http://www.zhouhaocheng.cn)