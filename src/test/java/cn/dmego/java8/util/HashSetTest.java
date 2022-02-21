package cn.dmego.java8.util;

import org.junit.Test;

/**
 * @author dmego
 * @date 2022/01/20 20:25
 */
public class HashSetTest {

    @Test
    public void test_add() {
        HashSet<Object> set = new HashSet<>();
        String s1 = new String("abc");
        String s2 = "abc";
        set.add(s1);
        set.add(s2);
        System.out.println(set);


        try {
            System.out.println("2222");
            return;
        } catch (Exception e) {

        } finally {
            System.out.println("111");
        }


    }

}
