package cn.dmego.java8.util;

import org.junit.Test;

import java.util.Map;

/**
 * @author dmego
 * @date 2022/02/21 22:52
 */
public class LinkedHashMapTest {

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
         cache.put(1, 1);
         cache.put(2,2);
         cache.put(3,3);
         cache.put(4,4);
        System.out.println(cache.get(1));

    }

}
