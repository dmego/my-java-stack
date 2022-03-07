package cn.dmego.java8.util;

import org.junit.Test;

import java.util.Iterator;
import java.util.List;

/**
 * @author dmego
 * @date 2022/02/24 17:18
 */
public class LinkedListTest {

    @Test
    public void test_init_list() {
        List<String> list = new LinkedList<String>() {{
            add("2");
        }};
        // 初始化
        List<Integer> list2 = new ArrayList<>(Collections.nCopies(10, 1));
        System.out.println(list2);
    }

    /**
     * ArrayList 和 LinkedList 头插耗时比较
     */
    @Test
    public void test_add_cost() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            ArrayList<Integer> arrayList = new ArrayList<>();
            long start = System.currentTimeMillis();
            for (int i = 0; i < 100000; i++) {
                arrayList.add(arrayList.size() >> 1, i);
            }
            System.out.println("arrayList cost: " + (System.currentTimeMillis() - start));
        });

        Thread thread2 = new Thread(() -> {
            LinkedList<Integer> linkedList = new LinkedList<>();
            long start = System.currentTimeMillis();
            for (int i = 0; i < 100000; i++) {
                linkedList.add(linkedList.size() >> 1, i);
            }
            System.out.println("linkedList cost: " + (System.currentTimeMillis() - start));
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

    }


    @Test
    public void test_iterator_cost() {

        LinkedList<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < 100000; i++) {
            linkedList.add(i);
        }

        long start = System.currentTimeMillis();
        for (int i = 0; i < linkedList.size(); i++) {
            linkedList.get(i);
        }
        System.out.println("for:" + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        linkedList.forEach(item -> {
            //
        });
        System.out.println("foreach: "+ (System.currentTimeMillis() - start));


        start = System.currentTimeMillis();
        Iterator<Integer> iterator = linkedList.iterator();
        while (iterator.hasNext()) {
            Integer next = iterator.next();
        }
        System.out.println("iterator: "+ (System.currentTimeMillis() - start));

    }


    @Test
    public void test_iterator_remove() {
        List<Integer> list = new LinkedList<>();
        for (int i = 0; i < 5; i++) {
            list.add(i);
        }

        Iterator<Integer> iterator = list.iterator();
        while (iterator.hasNext()) {
            Integer next = iterator.next();
            if (next == 3) {
                iterator.remove();
            } else {
                System.out.print(next + ", ");
            }

        }
    }

}
