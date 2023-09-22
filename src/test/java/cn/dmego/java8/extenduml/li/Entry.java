package cn.dmego.java8.extenduml.li;

import cn.dmego.java8.extenduml.Node;

public class Entry<K,V> extends Node<K,V> {
    Entry<K,V> before, after;

    Entry(int hash, K key, V value, Node<K,V> next) {
        super(hash, key, value, next);
    }
}
