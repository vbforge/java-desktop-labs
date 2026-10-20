package com.vbforge.sortscope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import javafx.application.Platform;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SortEngineTest {

    @BeforeAll
    static void initJavaFX() {
        // We must initialize the JavaFX toolkit because CNode extends Rectangle.
        // This avoids the "Toolkit not initialized" exception.
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialized
        }
    }

    private CNode[] createArray(int size, long seed) {
        CNode[] arr = new CNode[size];
        Random r = new Random(seed);
        for (int i = 0; i < size; i++) {
            arr[i] = new CNode(r.nextInt(1000));
        }
        return arr;
    }

    private void assertSorted(CNode[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            assertTrue(arr[i].getValue() <= arr[i + 1].getValue(), 
                "Array is not sorted at index " + i + ": " + arr[i].getValue() + " > " + arr[i + 1].getValue());
        }
    }

    @Test
    void testBubbleSort() {
        CNode[] arr = createArray(50, 42L);
        new BubbleSort(10).startSort(arr);
        assertSorted(arr);
    }

    @Test
    void testSelectionSort() {
        CNode[] arr = createArray(50, 43L);
        new SelectionSort(10).startSort(arr);
        assertSorted(arr);
    }

    @Test
    void testInsertionSort() {
        CNode[] arr = createArray(50, 44L);
        new InsertionSort(10).startSort(arr);
        assertSorted(arr);
    }

    @Test
    void testMergeSort() {
        CNode[] arr = createArray(50, 45L);
        new MergeSort(10).startSort(arr);
        assertSorted(arr);
    }

    @Test
    void testQuickSort() {
        CNode[] arr = createArray(50, 46L);
        new QuickSort(10).startSort(arr);
        assertSorted(arr);
    }

    @Test
    void testHeapSort() {
        CNode[] arr = createArray(50, 47L);
        new HeapSort(10).startSort(arr);
        assertSorted(arr);
    }
    
    @Test
    void testEmptyArray() {
        CNode[] arr = new CNode[0];
        new BubbleSort(10).startSort(arr);
        assertEquals(0, arr.length);
    }

    @Test
    void testAlreadySortedArray() {
        CNode[] arr = new CNode[5];
        for (int i = 0; i < 5; i++) arr[i] = new CNode(i);
        
        new BubbleSort(10).startSort(arr);
        assertSorted(arr);
        
        // Bubble sort should make 4 comparisons and 0 moves on an already sorted array
        AbstractSort sorter = new BubbleSort(10);
        CNode[] arr2 = new CNode[5];
        for (int i = 0; i < 5; i++) arr2[i] = new CNode(i);
        sorter.startSort(arr2);
        assertEquals(4, sorter.comparisons);
        assertEquals(0, sorter.moves);
    }
}