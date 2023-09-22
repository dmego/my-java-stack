package cn.dmego.java8.extenduml;

import cn.dmego.java8.util.HashMap;

public class Node<K,V> implements Entry<K,V> {
    final int hash; // hash(key) 重新计算的 hash 值
    final K key;
    V value;
   Node<K,V> next; // 链表的下一个元素

    public Node(int hash, K key, V value, Node<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
    }


}
