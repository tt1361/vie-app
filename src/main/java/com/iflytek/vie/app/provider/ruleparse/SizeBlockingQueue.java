package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SizeBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, Serializable {
   private static final long serialVersionUID = -303800381894715988L;
   private final BlockingQueue<E> queue;
   private final int capacity;
   private final AtomicInteger size = new AtomicInteger();

   public SizeBlockingQueue(BlockingQueue<E> queue, int capacity) {
      assert capacity >= 0;

      this.queue = queue;
      this.capacity = capacity;
   }

   @Override
   public int size() {
      return this.size.get();
   }

   public int capacity() {
      return this.capacity;
   }

   @Override
   public Iterator<E> iterator() {
      final Iterator<E> it = this.queue.iterator();
      return new Iterator<E>() {
         E current;

         @Override
         public boolean hasNext() {
            return it.hasNext();
         }

         @Override
         public E next() {
            this.current = it.next();
            return this.current;
         }

         @Override
         public void remove() {
            if (SizeBlockingQueue.this.queue.remove(this.current)) {
               SizeBlockingQueue.this.size.decrementAndGet();
            }
         }
      };
   }

   @Override
   public E peek() {
      return this.queue.peek();
   }

   @Override
   public E poll() {
      E e = this.queue.poll();
      if (e != null) {
         this.size.decrementAndGet();
      }

      return e;
   }

   @Override
   public E poll(long timeout, TimeUnit unit) throws InterruptedException {
      E e = this.queue.poll(timeout, unit);
      if (e != null) {
         this.size.decrementAndGet();
      }

      return e;
   }

   @Override
   public boolean remove(Object o) {
      boolean v = this.queue.remove(o);
      if (v) {
         this.size.decrementAndGet();
      }

      return v;
   }

   public void forcePut(E e) throws InterruptedException {
      this.size.incrementAndGet();

      try {
         this.queue.put(e);
      } catch (InterruptedException var3) {
         this.size.decrementAndGet();
         throw var3;
      }
   }

   @Override
   public boolean offer(E e) {
      int count = this.size.incrementAndGet();
      if (count > this.capacity) {
         this.size.decrementAndGet();
         return false;
      } else {
         boolean offered = this.queue.offer(e);
         if (!offered) {
            this.size.decrementAndGet();
         }

         return offered;
      }
   }

   @Override
   public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
      throw new IllegalStateException("offer with timeout not allowed on size queue");
   }

   @Override
   public void put(E e) throws InterruptedException {
      throw new IllegalStateException("put not allowed on size queue");
   }

   @Override
   public E take() throws InterruptedException {
      try {
         E e = this.queue.take();
         this.size.decrementAndGet();
         return e;
      } catch (InterruptedException var3) {
         throw var3;
      }
   }

   @Override
   public int remainingCapacity() {
      return this.capacity - this.size.get();
   }

   @Override
   public int drainTo(Collection<? super E> c) {
      int v = this.queue.drainTo(c);
      this.size.addAndGet(-v);
      return v;
   }

   @Override
   public int drainTo(Collection<? super E> c, int maxElements) {
      int v = this.queue.drainTo(c, maxElements);
      this.size.addAndGet(-v);
      return v;
   }

   @Override
   public Object[] toArray() {
      return this.queue.toArray();
   }

   @Override
   public <T> T[] toArray(T[] a) {
      return (T[])this.queue.toArray(a);
   }

   @Override
   public boolean contains(Object o) {
      return this.queue.contains(o);
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      return this.queue.containsAll(c);
   }
}
