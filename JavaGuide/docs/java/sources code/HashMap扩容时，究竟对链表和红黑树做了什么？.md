# HashMap扩容时，究竟对链表和红黑树做了什么？



我们知道 HashMap 的底层是由数组，链表，红黑树组成的，在 HashMap 做扩容操作时，除了把数组容量扩大为原来的两倍外，还会对所有元素重新计算 hash 值，因为长度扩大以后，hash值也随之改变。

如果是简单的 Node 对象，只需要重新计算下标放进去就可以了，如果是链表和红黑树，那么操作就会比较复杂，下面我们就来看下，JDK1.8 下的 HashMap 在扩容时对链表和红黑树做了哪些优化？

## rehash 时，链表怎么处理？

假设一个 HashMap 原本 bucket 大小为 16。下标 3 这个位置上的 19, 3, 35 由于索引冲突组成链表。

![img](https://pic3.zhimg.com/80/v2-b3d0984656c882dc13a1ad63df445072_720w.jpg)

当 HashMap 由 16 扩容到 32 时，19, 3, 35 重新 hash 之后拆成两条链表。

![img](https://pic4.zhimg.com/80/v2-e7825b68cd991c5b65e30f99ab1be2f7_720w.jpg)

查看 JDK1.8 HashMap 的源码，我们可以看到关于链表的优化操作如下：

```text
// 把原有链表拆成两个链表
// 链表1存放在低位（原索引位置）
Node<K,V> loHead = null, loTail = null;
// 链表2存放在高位（原索引 + 旧数组长度）
Node<K,V> hiHead = null, hiTail = null;
Node<K,V> next;
do {
    next = e.next;
    // 链表1
    if ((e.hash & oldCap) == 0) {
        if (loTail == null)
            loHead = e;
        else
            loTail.next = e;
        loTail = e;
    }
    // 链表2
    else {
        if (hiTail == null)
            hiHead = e;
        else
            hiTail.next = e;
        hiTail = e;
    }
} while ((e = next) != null);
// 链表1存放于原索引位置
if (loTail != null) {
    loTail.next = null;
    newTab[j] = loHead;
}
// 链表2存放原索引加上旧数组长度的偏移量
if (hiTail != null) {
    hiTail.next = null;
    newTab[j + oldCap] = hiHead;
}
```

正常我们是把所有元素都重新计算一下下标值，再决定放入哪个桶，JDK1.8 优化成直接把链表拆成高位和低位两条，通过位运算来决定放在原索引处或者原索引加原数组长度的偏移量处。我们通过位运算来分析下。

先回顾一下原 hash 的求余过程：

![img](https://pic1.zhimg.com/80/v2-6eee6a254c5dd378e8c46eceaa522d38_720w.jpg)

再看一下 rehash 时，判断时做的位操作，也就是这句 e.hash & oldCap：

![img](https://pic1.zhimg.com/80/v2-87994cf555c4afe5c7c9d262f5f3b7d4_720w.jpg)

再看下扩容后的实际求余过程：

![img](https://pic3.zhimg.com/80/v2-60dbe74c377f1cc32400c3ba4f401346_720w.jpg)

这波操作是不是很666，为什么 2 的整数幂 - 1可以作 & 操作可以代替求余计算，因为 2 的整数幂 - 1 的二进制比较特殊，就是一串 11111，与这串数字 1 作 & 操作，结果就是保留下原数字的低位，去掉原数字的高位，达到求余的效果。2 的整数幂的二进制也比较特殊，就是一个 1 后面跟上一串 0。

HashMap 的扩容都是扩大为原来大小的两倍，从二进制上看就是给这串数字加个 0，比如 16 -> 32 = 10000 -> 100000，那么他的 n - 1 就是 15 -> 32 = 1111 -> 11111。也就是多了一位，所以扩容后的下标可以从原有的下标推算出来。差异就在于上图我标红的地方，如果标红处是 0，那么扩容后再求余结果不变，如果标红处是 1，那么扩容后再求余就为原索引 + 原偏移量。如何判断标红处是 0 还是 1，就是把 e.hash & oldCap。

## rehash 时，红黑树怎么处理？

```text
// 红黑树转链表阈值
static final int UNTREEIFY_THRESHOLD = 6;

// 扩容操作
final Node<K,V>[] resize() {
    // ....
    else if (e instanceof TreeNode)
       ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
    // ...
}

final void split(HashMap<K,V> map, Node<K,V>[] tab, int index, int bit) {
    TreeNode<K,V> b = this;
    // Relink into lo and hi lists, preserving order
    // 和链表同样的套路，分成高位和低位
    TreeNode<K,V> loHead = null, loTail = null;
    TreeNode<K,V> hiHead = null, hiTail = null;
    int lc = 0, hc = 0;
    /**
      * TreeNode 是间接继承于 Node，保留了 next，可以像链表一样遍历
      * 这里的操作和链表的一毛一样
      */
    for (TreeNode<K,V> e = b, next; e != null; e = next) {
        next = (TreeNode<K,V>)e.next;
        e.next = null;
        // bit 就是 oldCap
        if ((e.hash & bit) == 0) {
            if ((e.prev = loTail) == null)
                loHead = e;
            else
            // 尾插
                loTail.next = e;
            loTail = e;
            ++lc;
        }
        else {
            if ((e.prev = hiTail) == null)
                hiHead = e;
            else
                hiTail.next = e;
            hiTail = e;
            ++hc;
        }
    }

    // 树化低位链表
    if (loHead != null) {
        // 如果 loHead 不为空，且链表长度小于等于 6，则将红黑树转成链表
        if (lc <= UNTREEIFY_THRESHOLD)
            tab[index] = loHead.untreeify(map);
        else {
            /**
              * hiHead == null 时，表明扩容后，
              * 所有节点仍在原位置，树结构不变，无需重新树化
              */
            tab[index] = loHead;
            if (hiHead != null) // (else is already treeified)
                loHead.treeify(tab);
        }
    }
    // 树化高位链表，逻辑与上面一致
    if (hiHead != null) {
        if (hc <= UNTREEIFY_THRESHOLD)
            tab[index + bit] = hiHead.untreeify(map);
        else {
            tab[index + bit] = hiHead;
            if (loHead != null)
                hiHead.treeify(tab);
        }
    }
}
```

从源码可以看出，红黑树的拆分和链表的逻辑基本一致，不同的地方在于，重新映射后，会将红黑树拆分成两条链表，根据链表的长度，判断需不需要把链表重新进行树化。