package cn.dmego.java8.util;

import org.junit.Test;

import java.util.Map;

/**
 * @author dmego
 * @date 2022/02/21 22:52
 */
public class LinkedHashMapTest {

    @Test
    public void test_link_last() {
        String a = null;
        String c = "c";
        String b = a;
        a = c;
        System.out.println("a:" + a + ", b:" + b + ", c:" + c);

        // 方法内定义的基本类型，值存储在栈上， = 进行赋值操作
        int z = 1;
        int x = z;
        x++;
        System.out.println("z:" + z + ", x: " + x);


        // 方法内定义的引用类型，变量在栈上定义，引用的地址(引用指向的对象)在堆上
        StringBuffer b1 = new StringBuffer();
        StringBuffer b2 = null;
        StringBuffer b3 = b2; // StringBuffer b3 表示定义一个引用变量，= b2 表示指向 b2 引用所指向的地址，b2 指向 null, 所有 b3 也指向 null
        b2 = b1; // b2 改变指向的地址，指向 b1 引用所指向的地址，这不影响 b3, 因为 b3 指向不会改变
        b1.append("hello");
        // NPE
        //System.out.println(b3.toString());

        class Text {
            private String word;

            public void printWord() {
                System.out.println("word: " + this.word);
            }

            public void setWord(String word) {
                this.word = word;
            }
        }

        Text t1 = new Text(); // t1 指向一个新 new 的 Text 类型对象(假设名为 text)
        t1.setWord("hello"); // 给 text 对象的 word 属性赋值为 hello
        Text t2 = t1; // 新建一个引用 t2 指向 t1 所指向的地址，也就是指向 text 对象
        t2.setWord("hello World"); // 给 text 对象的 word 属性赋值为 hello world
        t1.printWord(); // 因为 t1 和 t2 指向的地址是同一个对象，所以 t2 对 text 对象的操作， t1 也会有改变
    }

    @Test
    public void test_access_order() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        //LinkedHashMap<String, String> map = new LinkedHashMap<>(16, 0.75f, true);
        map.put("1", "a");
        map.put("4", "b");
        map.put("2", "c");
        System.out.println(map);
        map.get("4");
        System.out.println(map);
        map.put("3", "d");
        System.out.println(map);
    }


    /**
     * 使用 LinkedHashMap 实现 LRU 缓存
     */
    @Test
    public void test_LRU_cache() {

         class MyLRUCache<K, V> extends LinkedHashMap<K, V> {
             // 缓存容量
             private final int maxCacheSize;
             public MyLRUCache(int cacheSize) {
                 // accessOrder: true, 表示按照访问顺序排序
                 super(cacheSize, 0.75f, true);
                 this.maxCacheSize = cacheSize;
             }

             // 重写 removeEldestEntry 方法
             @Override
             protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                 // 当 size 超过最大容量时，返回 true 表示需要删除最旧一个元素
                 return size() > maxCacheSize;
             }
         }

         MyLRUCache<Integer, Integer> cache = new MyLRUCache<>(3);
         cache.put(1,1);
         cache.put(2,2);
         cache.put(3,3);
         cache.put(4,4);
         //System.out.println(cache.get(1));

    }

}
