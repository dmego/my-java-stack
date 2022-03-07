package cn.dmego.java8.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author dmego
 * @date 2022/02/22 20:16
 */
public class ArrayListTest {

    @Test
    public void test_arrayList() {
        List<String> list = new ArrayList<>(10);
        list.add("0");
        list.add(2, "1");
        System.out.println(list.get(2));
        System.out.println(list.get(0));
    }

    @Test
    public void test_init_arrayList() {
        List<String> list1 = new ArrayList<>();
        list1.add("1");
        ArrayList<String> list2 = new ArrayList<String>() {{
            add("1");
            add("2");
        }};
        List<String> list3 = new ArrayList<>(Arrays.asList("1","2"));
    }

    @Test
    public void t(){
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        System.out.println("通过数组转换：" + (list1.toArray().getClass() == Object[].class));

        ArrayList<Integer> list2 = new ArrayList<>(Arrays.asList(1, 2, 3));
        System.out.println("通过集合转换：" + (list2.toArray().getClass() == Object[].class));
    }

    // 将 a b c d e f g 元素添加到 list，如何将 get() 元素时的复杂度降到 O(1)
    // 思路就是利用 hash, 将元素添加到指定位置
    @Test
    public void test_get_in_o1() {
        // 初始化容量为 8 的 list
        List<String> list = new ArrayList<>(8);
        // index 0 添加 0, 将 list 进行初始化操作
        list.add("0");
        //
        list.add("a".hashCode() & (8 - 1), "a");
        list.add("b".hashCode() & (8 - 1), "b");
        list.add("c".hashCode() & (8 - 1), "c");
        list.add("d".hashCode() & (8 - 1), "d");
        list.add("e".hashCode() & (8 - 1), "e");
        list.add("f".hashCode() & (8 - 1), "f");
        list.add("g".hashCode() & (8 - 1), "g");


        System.out.println(list.get("a".hashCode() & ( 8 - 1)));

    }


    @Test
    public void test_remove_fast_fail() {
        List<String> list = new ArrayList<>(Arrays.asList("1", "2", "3", "4", "5"));
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            System.out.print(next+", ");
            if (next.equals("4")) {
                list.remove(4);
            }
        }
    }

    @Test
    public void test_new_arrayList_collection() {

        Object[] array1 = Arrays.asList(1, 2, 3).toArray();
        System.out.println(array1.getClass());
        System.out.println(Integer[].class == array1.getClass());
    }

    @Test
    public void test_array_copy() {
        Long[] array1 = new Long[]{1L, 2L, 3L};
        Object[] array2 = new Object[5];
        System.arraycopy(array1, 0, array2, 0, 3);
        System.out.println(Arrays.toString(array2)); // [1, 2, 3, null, null]
    }


}
