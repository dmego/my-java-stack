package cn.dmego.java8.util.concurrent.atomic;
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
import java.util.function.LongBinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A package-local class holding common representation and mechanics
 * for classes supporting dynamic striping on 64bit values. The class
 * extends Number so that concrete subclasses must publicly do so.
 */
@SuppressWarnings("serial")
abstract class Striped64 extends Number {
    /*
     * This class maintains a lazily-initialized table of atomically
     * updated variables, plus an extra "base" field. The table size
     * is a power of two. Indexing uses masked per-thread hash codes.
     * Nearly all declarations in this class are package-private,
     * accessed directly by subclasses.
     *
     * Table entries are of class Cell; a variant of AtomicLong padded
     * (via @sun.misc.Contended) to reduce cache contention. Padding
     * is overkill for most Atomics because they are usually
     * irregularly scattered in memory and thus don't interfere much
     * with each other. But Atomic objects residing in arrays will
     * tend to be placed adjacent to each other, and so will most
     * often share cache lines (with a huge negative performance
     * impact) without this precaution.
     *
     * In part because Cells are relatively large, we avoid creating
     * them until they are needed.  When there is no contention, all
     * updates are made to the base field.  Upon first contention (a
     * failed CAS on base update), the table is initialized to size 2.
     * The table size is doubled upon further contention until
     * reaching the nearest power of two greater than or equal to the
     * number of CPUS. Table slots remain empty (null) until they are
     * needed.
     *
     * A single spinlock ("cellsBusy") is used for initializing and
     * resizing the table, as well as populating slots with new Cells.
     * There is no need for a blocking lock; when the lock is not
     * available, threads try other slots (or the base).  During these
     * retries, there is increased contention and reduced locality,
     * which is still better than alternatives.
     *
     * The Thread probe fields maintained via ThreadLocalRandom serve
     * as per-thread hash codes. We let them remain uninitialized as
     * zero (if they come in this way) until they contend at slot
     * 0. They are then initialized to values that typically do not
     * often conflict with others.  Contention and/or table collisions
     * are indicated by failed CASes when performing an update
     * operation. Upon a collision, if the table size is less than
     * the capacity, it is doubled in size unless some other thread
     * holds the lock. If a hashed slot is empty, and lock is
     * available, a new Cell is created. Otherwise, if the slot
     * exists, a CAS is tried.  Retries proceed by "double hashing",
     * using a secondary hash (Marsaglia XorShift) to try to find a
     * free slot.
     *
     * The table size is capped because, when there are more threads
     * than CPUs, supposing that each thread were bound to a CPU,
     * there would exist a perfect hash function mapping threads to
     * slots that eliminates collisions. When we reach capacity, we
     * search for this mapping by randomly varying the hash codes of
     * colliding threads.  Because search is random, and collisions
     * only become known via CAS failures, convergence can be slow,
     * and because threads are typically not bound to CPUS forever,
     * may not occur at all. However, despite these limitations,
     * observed contention rates are typically low in these cases.
     *
     * It is possible for a Cell to become unused when threads that
     * once hashed to it terminate, as well as in the case where
     * doubling the table causes no thread to hash to it under
     * expanded mask.  We do not try to detect or remove such cells,
     * under the assumption that for long-running instances, observed
     * contention levels will recur, so the cells will eventually be
     * needed again; and for short-lived ones, it does not matter.
     */

    /**
     * 加上 @sun.misc.Contended 注解，减少缓存行冲突
     * 通常情况下，缓存行填充(Padding)对于大多数原子操作来说都是不必要的，因为它们散落在不规则的内存中。
     * 但是对于存在于一个数组内的原子对象来说，这样的情况会发生变化，它们会产生相互影响，
     * 原因是因为它们在内存中的布局会相互紧挨着，并存在大量的共享相同的缓存行，而共享缓存行对于性能的影响将是非常巨大的
     *
     *
     * Padded variant of AtomicLong supporting only raw accesses plus CAS.
     *
     * JVM intrinsics note: It would be possible to use a release-only
     * form of CAS here, if it were provided.
     */
    @sun.misc.Contended static final class Cell {
        volatile long value;
        Cell(long x) { value = x; }
        final boolean cas(long cmp, long val) {
            return UNSAFE.compareAndSwapLong(this, valueOffset, cmp, val);
        }

        // Unsafe mechanics
        private static final sun.misc.Unsafe UNSAFE;
        private static final long valueOffset;
        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> ak = Cell.class;
                valueOffset = UNSAFE.objectFieldOffset
                        (ak.getDeclaredField("value"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /**
     * 当前计算机上的 CPU 核数，当 cell 数组进行扩容时会使用到
     * Number of CPUS, to place bound on table size
     */
    static final int NCPU = Runtime.getRuntime().availableProcessors();

    /**
     * cells 单元格数组，长度必须满足是 2 的次幂
     * Table of cells. When non-null, size is a power of 2.
     */
    transient volatile Cell[] cells;

    /**
     * 类似与 AtomicInteger 中的全局 value 值，在没有竞争的情况下，数据是直接累加在 base 值上
     * 或者当 cells 正在扩容时，也会将数据写入到 base 上
     * Base value, used mainly when there is no contention, but also as
     * a fallback during table initialization races. Updated via CAS.
     */
    transient volatile long base;

    /**
     * 表示 cells 上是否有锁：0 表示无锁状态; 1 表示其他线程已经持有了锁
     * 当线程初始化 cells 或者 cells 需要扩容时，都需要先获取锁
     * Spinlock (locked via CAS) used when resizing and/or creating Cells.
     */
    transient volatile int cellsBusy;

    /**
     * Package-private default constructor
     */
    Striped64() {
    }

    /**
     * 通过 CAS 操作更新 base 值
     * CASes the base field.
     */
    final boolean casBase(long cmp, long val) {
        return UNSAFE.compareAndSwapLong(this, BASE, cmp, val);
    }

    /**
     * 通过 CAS 操作修改 cellsBusy 的值
     * CAS 成功，返回 true 表示获取锁成功
     * CAS 失败，返回 false 表示获取锁失败
     * CASes the cellsBusy field from 0 to 1 to acquire lock.
     */
    final boolean casCellsBusy() {
        return UNSAFE.compareAndSwapInt(this, CELLSBUSY, 0, 1);
    }

    /**
     * 返回当前线程的 hash(threadLocalRandomProbe) 值
     * 相当于当前线程的 hash 值
     * Returns the probe value for the current thread.
     * Duplicated from ThreadLocalRandom because of packaging restrictions.
     */
    static final int getProbe() {
        return UNSAFE.getInt(Thread.currentThread(), PROBE);
    }

    /**
     * 重置当前线程的 hash(threadLocalRandomProbe) 值
     * Pseudo-randomly advances and records the given probe value for the
     * given thread.
     * Duplicated from ThreadLocalRandom because of packaging restrictions.
     */
    static final int advanceProbe(int probe) {
        probe ^= probe << 13;   // xorshift
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        UNSAFE.putInt(Thread.currentThread(), PROBE, probe);
        return probe;
    }

    /**
     * Handles cases of updates involving initialization, resizing,
     * creating new Cells, and/or contention. See above for
     * explanation. This method suffers the usual non-modularity
     * problems of optimistic retry code, relying on rechecked sets of
     * reads.
     *
     * @param x the value
     * @param fn the update function, or null for add (this convention
     * avoids the need for an extra field or function in LongAdder).
     * @param wasUncontended
     *  false: if CAS failed before call(如果 CAS 失败了)
     *  true: 进行初始化时，传入是的 true
     */
    final void longAccumulate(long x, LongBinaryOperator fn,
                              boolean wasUncontended) {
        // 存储线程的 probe 值
        int h;
        // 如果 getProbe() = 0，说明当前线程的 probe 还未初始化
        if ((h = getProbe()) == 0) {
            // 使用 ThreadLocalRandom 为当前线程强制随机初始化一个值
            ThreadLocalRandom.current(); // force initialization
            // 初始化完之后，重新获取 probe 值
            h = getProbe();
            // 并且设置 uncontended 为 true, 表示不存在竞争，因为重新初始化相当于重置一个线程。
            wasUncontended = true;
        }
        // 如果 hash 值取模映射得到的 cell 单元不是 null，则为 true, 这个值也可以看作是扩容意向
        boolean collide = false;                // True if last slot nonempty
        for (;;) {
            Cell[] as; // as 表示 cells 的引用
            Cell a;  // a 表示当前线程 hash cells 数组命中的单元格 cell
            int n; // 表示 cells 数组的长度
            long v; // 表示当前线程 hash cells 数组命中的单元格 cell 的 value 值
            // CASE 1: cells 已经被初始化
            if ((as = cells) != null && (n = as.length) > 0) {
                /*
                 * (n - 1) & h： 与运算生成一个 cells 数组下标
                 * (a = as[(n - 1) & h]) == null 表示当前下标单元格未被初始化，if 条件成立
                 */
                if ((a = as[(n - 1) & h]) == null) {
                    // 先判断时候有锁，没有则进入
                    if (cellsBusy == 0) {       // Try to attach new Cell
                        // new 一个 cell 单元格，存入 x 值（1）
                        Cell r = new Cell(x);   // Optimistically create
                        // 再次检查是否有锁，如果没有则调用 casCellsBusy 进行 CAS 加锁，加锁成功后，进入
                        if (cellsBusy == 0 && casCellsBusy()) {
                            // 表示是否创建完成
                            boolean created = false;
                            try {               // Recheck under lock
                                Cell[] rs; // cells 数组的引用
                                int m, j; // m 表示 cells 数组长度；j 表示为经计算 cells 数组中单元格为 null 的下标
                                /*
                                 * 在加锁成功后，对以下三个条件再次进行检查：
                                 * (rs = cells) != null： cells 数组不为 null
                                 * (m = rs.length) > 0：cells 数组长度 > 0
                                 *  rs[j = (m - 1) & h] == null: j 是经计算 cells 中的单元格下标，该单元格为 null
                                 */
                                if ((rs = cells) != null &&
                                        (m = rs.length) > 0 &&
                                        rs[j = (m - 1) & h] == null) {
                                    // 将 r, 也就是新初始化好的一个单元格放入 cells 数组的 j 下标处
                                    rs[j] = r;
                                    // created 设置为 true
                                    created = true;
                                }
                            } finally {
                                cellsBusy = 0; // 释放锁
                            }
                            if (created) // 如果 cell 初始化成功，跳出大的 for 循环
                                break;
                            // 如果 cell 没有初始化成功，continue 继续进入循环
                            continue;           // Slot is now non-empty
                        }
                    }
                    // 如果存在锁，将 collide(扩容意向) 置为 false，执行到下面的 advanceProbe()，重新计算 hash(h 值)，继续进行循环
                    collide = false;
                }
                // 如果是 CAS 失败进来的
                else if (!wasUncontended)       // CAS already known to fail
                    // 将 uncontended 重置为 true, 执行到下面的 advanceProbe()，重新计算 hash(h 值)，继续进行循环
                    wasUncontended = true;      // Continue after rehash
                /*
                 * 当前单元格 a 不为 null, 进行 CAS 操作，更新 value 值 (value + x)
                 * 如果 CAS 不成功，则执行到下面的 advanceProbe()，重新计算 hash(h 值)，继续进行循环
                 */
                else if (a.cas(v = a.value, ((fn == null) ? v + x :
                        fn.applyAsLong(v, x))))
                    break; // CAS 操作成功后，跳出大的 for 循环
                // 当 当前 cells 数组长度 >= CPU核数 或者 cells 不等于 as 值（被更新了），重置扩容意向 collide 为 false，执行到下面的 advanceProbe()，重新计算 hash(h 值)，继续进行循环
                else if (n >= NCPU || cells != as)
                    collide = false;            // At max size or stale
                /*
                 * 当扩容意向 collide 为 false 时，重置为 true, 然后执行到下面的 advanceProbe()，重新计算 hash(h 值)，继续进行循环
                 */
                else if (!collide)
                    collide = true;
                /*
                 * 当 collide = true 时，才进行扩容操作：先判断是否有加锁，没有的化使用 CAS 进行加锁
                 * 这里分析为什么当 n >= NCPU 时，就不进行扩容了：
                 *  - 当 (n >= NCPU || cells != as) 条件成立时，重置 collide 为 false, 重新计算 hash(h 值)，继续进行循环, 不会执行到扩容分支
                 *  - 当 (n >= NCPU || cells != as) 条件不成立时，
                 *      执行到 (!collide) 条件，除非上一次也是执行到 (!collide) 分支，否则 collide = false
                 *      重置 collide 为 true, 重新计算 hash(h 值)，继续进行循环
                 *  - 当 执行到 (!collide) 时，
                 *      如果 collide = true, 则会执行到 这里的扩容分支
                 *      否则，重置为 true, 重新计算 hash(h 值)，继续进行循环, 下次就有可能执行到这里的扩容分支
                 */
                else if (cellsBusy == 0 && casCellsBusy()) {
                    try {
                        if (cells == as) {      // Expand table unless stale
                            // 新建一个 2 倍容量的 数组 rs
                            Cell[] rs = new Cell[n << 1];
                            // for 循环将 cells 数组的值拷贝到 新数组 rs 中
                            for (int i = 0; i < n; ++i)
                                rs[i] = as[i];
                            // 将新数 rs 赋值给 cells 数组
                            cells = rs;
                        }
                    } finally {
                        cellsBusy = 0; // 释放锁
                    }
                    collide = false;
                    continue;                   // Retry with expanded table
                }
                // 重新计算 hash 哈希值，想当于换到不同的单元格位继续尝试
                h = advanceProbe(h);
            }
            // CASE 2: cells 没有加锁且没有初始化（cells = null），则尝试对它进行加锁，加锁成功后进行初始化 cells 数组
            else if (cellsBusy == 0 && cells == as && casCellsBusy()) {
                boolean init = false; // 表示 cells 是否初始化
                try {                           // Initialize table
                    // 再次对 cells 进行检查
                    if (cells == as) {
                        Cell[] rs = new Cell[2]; // 初始化 cells 数组，长度为 2
                        rs[h & 1] = new Cell(x); // h & 1 的取值是范围是 [0, 1]，也就是初始化 cells 数组其中一个单元格，存入x (1)
                        cells = rs; // 将初始化好的数组赋值给 cells
                        init = true; // init 设置为 true
                    }
                } finally {
                    cellsBusy = 0; // 释放锁
                }
                if (init) // 如果已经初始化，则跳出 for 循环
                    break;
            }
            // CASE 3: cells 正在进行初始化，则尝试在基数 base 上进行累加操作：对 base 进行 CAS 操作
            else if (casBase(v = base, ((fn == null) ? v + x :
                    fn.applyAsLong(v, x))))
                break;                          // Fall back on using base
        }
    }

    /**
     * Same as longAccumulate, but injecting long/double conversions
     * in too many places to sensibly merge with long version, given
     * the low-overhead requirements of this class. So must instead be
     * maintained by copy/paste/adapt.
     */
    final void doubleAccumulate(double x, DoubleBinaryOperator fn,
                                boolean wasUncontended) {
        int h;
        if ((h = getProbe()) == 0) {
            ThreadLocalRandom.current(); // force initialization
            h = getProbe();
            wasUncontended = true;
        }
        boolean collide = false;                // True if last slot nonempty
        for (;;) {
            Cell[] as; Cell a; int n; long v;
            if ((as = cells) != null && (n = as.length) > 0) {
                if ((a = as[(n - 1) & h]) == null) {
                    if (cellsBusy == 0) {       // Try to attach new Cell
                        Cell r = new Cell(Double.doubleToRawLongBits(x));
                        if (cellsBusy == 0 && casCellsBusy()) {
                            boolean created = false;
                            try {               // Recheck under lock
                                Cell[] rs; int m, j;
                                if ((rs = cells) != null &&
                                        (m = rs.length) > 0 &&
                                        rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            if (created)
                                break;
                            continue;           // Slot is now non-empty
                        }
                    }
                    collide = false;
                }
                else if (!wasUncontended)       // CAS already known to fail
                    wasUncontended = true;      // Continue after rehash
                else if (a.cas(v = a.value,
                        ((fn == null) ?
                                Double.doubleToRawLongBits
                                        (Double.longBitsToDouble(v) + x) :
                                Double.doubleToRawLongBits
                                        (fn.applyAsDouble
                                                (Double.longBitsToDouble(v), x)))))
                    break;
                else if (n >= NCPU || cells != as)
                    collide = false;            // At max size or stale
                else if (!collide)
                    collide = true;
                else if (cellsBusy == 0 && casCellsBusy()) {
                    try {
                        if (cells == as) {      // Expand table unless stale
                            Cell[] rs = new Cell[n << 1];
                            for (int i = 0; i < n; ++i)
                                rs[i] = as[i];
                            cells = rs;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                    collide = false;
                    continue;                   // Retry with expanded table
                }
                h = advanceProbe(h);
            }
            else if (cellsBusy == 0 && cells == as && casCellsBusy()) {
                boolean init = false;
                try {                           // Initialize table
                    if (cells == as) {
                        Cell[] rs = new Cell[2];
                        rs[h & 1] = new Cell(Double.doubleToRawLongBits(x));
                        cells = rs;
                        init = true;
                    }
                } finally {
                    cellsBusy = 0;
                }
                if (init)
                    break;
            }
            else if (casBase(v = base,
                    ((fn == null) ?
                            Double.doubleToRawLongBits
                                    (Double.longBitsToDouble(v) + x) :
                            Double.doubleToRawLongBits
                                    (fn.applyAsDouble
                                            (Double.longBitsToDouble(v), x)))))
                break;                          // Fall back on using base
        }
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long BASE;
    private static final long CELLSBUSY;
    private static final long PROBE;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> sk = Striped64.class;
            BASE = UNSAFE.objectFieldOffset
                    (sk.getDeclaredField("base"));
            CELLSBUSY = UNSAFE.objectFieldOffset
                    (sk.getDeclaredField("cellsBusy"));
            Class<?> tk = Thread.class;
            PROBE = UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("threadLocalRandomProbe"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
