package cs224n.util;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class WeakReferenceList<E> implements List<E> {
  public static interface RefreshFunction<E>{
    public E get(int i);
    public int size();
  }

  private final HashMap<Integer,WeakReference<E>> cache = new HashMap<Integer, WeakReference<E>>();
  private final RefreshFunction<E> refresh;

  public WeakReferenceList(RefreshFunction<E> refresh){
    this.refresh = refresh;
  }

  public int size() {
    return refresh.size();
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public boolean contains(Object o) {
    for(Object cand : this){
      if(cand.equals(o)){ return true; }
    }
    return false;
  }

  public Iterator<E> iterator() {
    return new Iterator<E>(){
      private int index = 0;
      public boolean hasNext() {
        return index < size();
      }
      public E next() {
        index += 1;
        return get(index-1);
      }
      public void remove() {
        throw new RuntimeException("NOT IMPLEMENTED");
      }
    };
  }

  public Object[] toArray() {
    Object[] rtn = new Object[size()];
    for(int i=0; i<size(); i++){
      rtn[i] = get(i);
    }
    return rtn;
  }

  public <T> T[] toArray(T[] ts) {
    for(int i=0; i<size(); i++){
      ts[i] = (T) get(i);
    }
    return ts;
  }

  public boolean add(E e) {
    throw new UnsupportedOperationException("Add is not supported");
  }

  public boolean remove(Object o) {
    throw new UnsupportedOperationException("Remove is not supported");
  }

  public boolean containsAll(Collection<?> objects) {
    for(Object o : objects){
      if(!contains(o)){ return false; }
    }
    return true;
  }

  public boolean addAll(Collection<? extends E> es) {
    for(E e : es){ add(e); }
    return true;
  }

  public boolean addAll(int i, Collection<? extends E> es) {
    throw new UnsupportedOperationException("The 'ell is this method?");
  }

  public boolean removeAll(Collection<?> objects) {
    for(Object o : objects){ remove(o); }
    return true;
  }

  public boolean retainAll(Collection<?> objects) {
    throw new UnsupportedOperationException("Retain all not supported");
  }

  public void clear() {
    throw new UnsupportedOperationException("clear not supported");
  }

  public E get(int i) {
    if(i < 0 || i >= size()){ throw new IndexOutOfBoundsException(""+i); }
    E rtn = null;
    while(rtn == null){
      //(never accessed before)
      if(!cache.containsKey(i)){ cache.put(i, new WeakReference<E>(refresh.get(i))); }
      //(reference might have died)
      WeakReference<E> ref = cache.get(i);
      if(ref.get() == null){
        ref = new WeakReference<E>(refresh.get(i));
        cache.put(i,ref);
      }
      //(in theory, should be not null)
      rtn = ref.get();
    }
    return rtn;
  }

  public E set(int i, E e) {
    throw new UnsupportedOperationException("set not supported");
  }

  public void add(int i, E e) {
    throw new UnsupportedOperationException("The 'ell is this method?");
  }

  public E remove(int i) {
    throw new UnsupportedOperationException("remove not implemented");
  }

  public int indexOf(Object o) {
    int i=0;
    for(E e : this){
      if(e.equals(o)){ return i; }
      i += 1;
    }
    return -1;
  }

  public int lastIndexOf(Object o) {
    int i=0;
    int retVal = -1;
    for(E e : this){
      if(e.equals(o)){ retVal = i; }
      i += 1;
    }
    return retVal;
  }

  public ListIterator<E> listIterator() {
    throw new UnsupportedOperationException("*Waves hand* this is not the method you're looking for...");
  }

  public ListIterator<E> listIterator(int i) {
    throw new UnsupportedOperationException("Nope. Chuck Testa.");
  }

  public List<E> subList(int i, int i1) {
    throw new UnsupportedOperationException("Not impossible, I was just lazy :/");
  }
}
