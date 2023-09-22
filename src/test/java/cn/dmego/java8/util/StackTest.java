package cn.dmego.java8.util;

import com.sun.jmx.remote.internal.ArrayQueue;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Stack;

/**
 * @author dmego
 * @date 2022/03/08 21:32
 */
public class StackTest {

    @Test
    public void test_stack_null() {

        Stack<String> stack = new Stack<>();
        stack.push(null);
        ArrayQueue<String> queue = new ArrayQueue<>(10);
        queue.add(null);
        ArrayDeque<String> deque = new ArrayDeque<>();
        deque.push(null);



    }
}
