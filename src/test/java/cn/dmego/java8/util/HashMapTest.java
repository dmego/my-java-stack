package cn.dmego.java8.util;

import org.junit.Test;

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
    public void test_01() {
        System.out.println(125 % 4);
        System.out.println(125 & 3);
    }

}
