package com.xiegeo.cssr.analyze;

public class IntArray{
	private static final boolean CHECK_INDEX = true;
	private int size;
	private int[] array;
	
	public IntArray(int initialCapacity) {
		super();
		array = new int[initialCapacity];
		size = 0;
	}


	public int size() {
		return size;
	}
	
	public void add(int e) {
		if (size >= array.length) {
			ensureCapacity((size+1)*2);
		}
		array[size] = e;
		size++;
	}
	public int get(int index) {
		if (CHECK_INDEX && (index>=size || index<0) ) {
			throw new IndexOutOfBoundsException("index:"+index+" size:"+size);
		}
		return array[index];
	}
	
	public void ensureCapacity(int minCapacity) {
		if (minCapacity > array.length) {
			int[] newArray = new int[minCapacity];
			System.arraycopy(array, 0, newArray, 0, size);
			array = newArray;
		}
	}
	
	public void clear() {
		size = 0;
	}



}
