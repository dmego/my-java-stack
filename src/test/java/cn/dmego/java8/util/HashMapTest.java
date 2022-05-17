package cn.dmego.java8.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static cn.dmego.java8.util.HashMap.MAXIMUM_CAPACITY;

/**
 * @author dmego
 * @date 2022/01/17 20:53
 */
public class HashMapTest {

    @Test
    public void test_hashMapInfiniteLoop() {

        HashMap<Integer, String> map = new HashMap<>(2, 0.75f);
        map.put(1, "A");

        new Thread(()->{
            map.put(2,"B");
            System.out.println(map);
        }).start();

        new Thread(()->{
            map.put(3,"C");
            System.out.println(map);
        }).start();
    }

    @Test
    public void test_resize_max() {

        HashMap<Integer, Object>  map = new HashMap<>(MAXIMUM_CAPACITY / 2);
        for (int i = 1; i <= MAXIMUM_CAPACITY / 2; i++) {
            map.put(i, i);
        }
        System.out.println("size: "+map.size());
        System.out.println("capacity: "+map.capacity());
        System.out.println("threshold: "+map.threshold);
    }


    @Test
    public void test_for() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("1", "a");
        map.put("4", "d");
        map.put("2", "b");
        map.put("9", "i");
        map.put("3", "c");

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        System.out.println("-------");

        Set<String> keySet = map.keySet();
        for (String key : keySet) {
            System.out.println(key + ": " + map.get(key));
        }
    }

    @Test
    public void test_for_profiler() throws InterruptedException {
        HashMap<Integer, Object> map = new HashMap<>(20_000_000);
       for (int i = 0; i < 20_000_000; i++) {
           map.put(i, i);
       }

        System.out.println(map.size()); // 实际的键值对数量
        System.out.println(map.capacity());
        System.out.println(map.table.length); // 桶的实际大小
        System.out.println(map.threshold); // 什么时候需要扩容

        Thread thread1 = new Thread(()->{
            long start = System.currentTimeMillis();
            for (Map.Entry<Integer, Object> entry : map.entrySet()) {
                entry.getKey();
                entry.getValue();
            }

            System.out.println("entrySet: " + (System.currentTimeMillis() - start));

        });

        Thread thread2 = new Thread(()->{
            long start = System.currentTimeMillis();
            Set<Integer> keySet = map.keySet();
            for (Integer key : keySet) {
                map.get(key);
            }
            System.out.println("keySet: " + (System.currentTimeMillis() - start));

        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();



    }

    @Test
    public void test_iterator() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("1", "a");
        map.put("4", "d");
        map.put("2", "b");
        map.put("9", "i");
        map.put("3", "c");

        Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            System.out.println(entry.getKey() + ": " + entry.getValue());
            if (entry.getKey().equals("4")) {
                map.remove(entry.getKey());
            }
        }

    }

    @Test
    public void test_table_size_of() {
        int n = 18 - 1;
        System.out.println(Integer.toBinaryString(n));
        n |= n >>> 1;
        System.out.println(Integer.toBinaryString(n));
        n |= n >>> 2;
        System.out.println(Integer.toBinaryString(n));
        n |= n >>> 4;
        System.out.println(Integer.toBinaryString(n));
        n |= n >>> 8;
        System.out.println(Integer.toBinaryString(n));
        n |= n >>> 16;
        System.out.println(Integer.toBinaryString(n));
        System.out.println((n < 0) ? 1 : n + 1);
    }


    @Test
    public void test_resize_hash() {
        System.out.println(Integer.toBinaryString(16 - 1));
        System.out.println(Integer.toBinaryString(32 - 1));
        System.out.println(Integer.toBinaryString((16 - 1) & HashMap.hash("a")));
        System.out.println(Integer.toBinaryString((32 - 1) & HashMap.hash("a")));
    }



    @Test
    public void test_tableSizeFor_vs_roundUpToPowerOf2 () throws InterruptedException {
        int roundSize = 1000000000;
        Thread thread1 = new Thread(()->{
            long start = System.currentTimeMillis();
            for (int i = 1; i <= roundSize; i++) {
               tableSizeFor(i);
            }
            System.out.println("tableSizeFor: " + (System.currentTimeMillis() - start));
        });

        Thread thread2 = new Thread(()->{
            long start = System.currentTimeMillis();
            for (int i = 1; i <= roundSize; i++) {
                roundUpToPowerOf2(i);
            }
            System.out.println("roundUpToPowerOf2: " + (System.currentTimeMillis() - start));
        });

        Thread thread3 = new Thread(()->{
            long start = System.currentTimeMillis();
            for (int i = 1; i <= roundSize; i++) {
                tableSizeFor_JDK11(i);
            }
            System.out.println("tableSizeFor_JDK11: " + (System.currentTimeMillis() - start));
        });

        Thread thread4 = new Thread(()->{
            for (int i = 1; i <= roundSize; i++) {
                int jdk8 = tableSizeFor(i);
                int jdk7 = roundUpToPowerOf2(i);
                int jdk11 = tableSizeFor_JDK11(i);
                Assert.assertTrue(jdk7 == jdk8 && jdk8 == jdk11);
            }
        });

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();
    }

    @Test
    public void test_01() {

        System.out.println(tableSizeFor_JDK11(-1));
        System.out.println(tableSizeFor(-1));
        System.out.println(roundUpToPowerOf2(-1));

    }


    static int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    static int roundUpToPowerOf2(int cap) {
        return cap >= MAXIMUM_CAPACITY
                ? MAXIMUM_CAPACITY
                : (cap > 1) ? Integer.highestOneBit((cap - 1) << 1) : 1;
    }

    static int tableSizeFor_JDK11(int cap) {
        int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }


    @Test
    public void test_jdk11_tablesizefor() {
        System.out.println(Integer.toBinaryString(-1));
        System.out.println(Integer.toBinaryString(-1 >>> 16));
    }


    @Test
    public void test_putAll_bug() throws NoSuchFieldException, IllegalAccessException {

        HashMap<Object, Object> a = new HashMap<>();
        fill12(a);
        HashMap<Object, Object> b = new HashMap<>(24);
        fill12(b);
        HashMap<Object, Object> c = new HashMap<>(a);
        HashMap<Object, Object> d = new HashMap<>();
        d.putAll(a);
        System.out.println("a : " + getArrayLength(a));
        System.out.println("b : " + getArrayLength(b));
        System.out.println("c : " + getArrayLength(c));
        System.out.println("d : " + getArrayLength(d));
        System.out.println(Math.ceil(12 / 0.75));
        System.out.println(Math.ceil(24 / 0.75));
        System.out.println(Math.ceil(36 / 0.75));
        System.out.println(Math.ceil(48 / 0.75));

    }
    public static void fill12(Map<Object, Object> map) {
        for (int i = 0; i < 24; i++) {
            map.put(i, i);
        }
    }

    public static int getArrayLength(Map<Object, Object> map) throws NoSuchFieldException, IllegalAccessException {
        Field field = HashMap.class.getDeclaredField("table");
        field.setAccessible(true);
        Object table = field.get(map);
        return Array.getLength(table);
    }


    @Test
    public void test_max_capacity() {
        int size = tableSizeFor(Integer.MAX_VALUE);
        System.out.println(size == MAXIMUM_CAPACITY);
    }

    @Test
    public void test_Iterator() {
        Map<String, String> map = new HashMap<>(64);
        map.put("24", "Idx：2");
        map.put("46", "Idx：2");
        map.put("68", "Idx：2");
        map.put("29", "Idx：7");
        map.put("150", "Idx：12");
        map.put("172", "Idx：12");
        map.put("194", "Idx：12");
        map.put("271", "Idx：12");
        System.out.println("排序01：");
        for (String key : map.keySet()) {
            System.out.print(key + " ");
        }

        map.put("293", "Idx：12");
        map.put("370", "Idx：12");
        map.put("392", "Idx：12");
        map.put("491", "Idx：12");
        map.put("590", "Idx：12");
        System.out.println("\n\n排序02：");
        for (String key : map.keySet()) {
            System.out.print(key + " ");
        }

        map.remove("293");
        map.remove("370");
        map.remove("392");
        map.remove("491");
        map.remove("590");
        System.out.println("\n\n排序03：");
        for (String key : map.keySet()) {
            System.out.print(key + " ");
        }

    }

}
