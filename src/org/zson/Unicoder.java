package org.zson;

/**
 * 用于编解码unicode
 * 
 * @author zhou
 *
 */
final class Unicoder{

  private Unicoder(){
  }

  /**
   * 字符串转unicode
   * 
   * @param str
   * @return
   */
  public static String encode(String src){
    String result = "";
    for(int i = 0; i < src.length(); i++){
      int chr1 = (char) src.charAt(i);
      if(chr1 >= 19968 && chr1 <= 171941){// 汉字范围 \u4e00-\u9fa5 (中文)
        result += "\\u" + Integer.toHexString(chr1);
      }else{
        result += src.charAt(i);
      }
    }
    return result;
  }

  /**
   * unicode转字符串
   * 
   * @param in
   * @return
   */
  public static String decode(String in){
    String working = in;
    int index;
    index = working.indexOf("\\u");
    while(index > -1){
      int length = working.length();
      if(index > (length - 6)) break;

      int numStart = index + 2;
      int numFinish = numStart + 4;
      String substring = working.substring(numStart, numFinish);
      int number = Integer.parseInt(substring, 16);
      String stringStart = working.substring(0, index);
      String stringEnd = working.substring(numFinish);
      working = stringStart + ((char) number) + stringEnd;
      index = working.indexOf("\\u");
    }
    return working;
  }

}
