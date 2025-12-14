/*
 * Copyright (c) 2014, NTUU KPI, Computer systems department and/or its affiliates. All rights reserved.
 * NTUU KPI PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 */

package ua.kpi.comsys.test2.implementation;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import ua.kpi.comsys.test2.NumberList;

/**
 * Custom implementation of INumberList interface.
 * Circular doubly-linked list representing a number in decimal system.
 * Each element stores a single digit.
 *
 * @author [Pokhodun Vitalii]
 * @group [IP-33]
 * @gradebook [17]
 *
 */
public class NumberListImpl implements NumberList {

    /**
     * Node class for circular doubly-linked list
     */
    private static class Node {
        Byte data;
        Node next;
        Node prev;

        Node(Byte data) {
            this.data = data;
            this.next = this;
            this.prev = this;
        }
    }

    private Node head;
    private int size;
    private int base;

    /**
     * Default constructor. Returns empty <tt>NumberListImpl</tt>
     */
    public NumberListImpl() {
        this.head = null;
        this.size = 0;
        this.base = getSourceBase();
    }

    /**
     * Get the source base based on record book number
     */
    private static int getSourceBase() {
        switch (getRecordBookNumber() % 5) {
            case 0: return 2;  // Binary
            case 1: return 3;  // Ternary
            case 2: return 8;  // Octal
            case 3: return 10; // Decimal
            case 4: return 16; // Hexadecimal
            default: return 10;
        }
    }

    /**
     * Get the target base for changeScale() based on record book number
     */
    private static int getTargetBase() {
        switch (getRecordBookNumber() % 5) {
            case 0: return 3;  // Binary to Ternary
            case 1: return 8;  // Ternary to Octal
            case 2: return 10; // Octal to Decimal
            case 3: return 16; // Decimal to Hexadecimal
            case 4: return 2;  // Hexadecimal to Binary
            default: return 10;
        }
    }


    /**
     * Constructs new <tt>NumberListImpl</tt> by <b>decimal</b> number
     * from file, defined in string format.
     *
     * @param file - file where number is stored.
     */
    public NumberListImpl(File file) {
        this();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null) {
                parseString(line.trim());
            }
        } catch (IOException e) {
            // If file cannot be read, leave list empty
        }
    }


    /**
     * Constructs new <tt>NumberListImpl</tt> by <b>decimal</b> number
     * in string notation.
     *
     * @param value - number in string notation.
     */
    public NumberListImpl(String value) {
        this();
        parseString(value);
    }

    /**
     * Helper method to parse a string and populate the list.
     * Input string is always in decimal notation.
     * Converts and stores in the source base.
     */
    private void parseString(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        // Check for invalid characters (negative, non-digits)
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isDigit(c)) {
                // Invalid input, leave list empty
                return;
            }
        }

        // If source base is decimal, just add digits directly
        if (base == 10) {
            for (int i = 0; i < value.length(); i++) {
                byte digit = (byte) (value.charAt(i) - '0');
                add(digit);
            }
        } else {
            // Convert decimal string to source base
            if (value.equals("0")) {
                add((byte) 0);
                return;
            }

            String currentValue = value;
            StringBuilder resultStr = new StringBuilder();

            while (!currentValue.equals("0") && !currentValue.isEmpty()) {
                int remainder = 0;
                StringBuilder quotient = new StringBuilder();

                for (int i = 0; i < currentValue.length(); i++) {
                    int digit = currentValue.charAt(i) - '0';
                    int current = remainder * 10 + digit;

                    if (current >= base) {
                        quotient.append(current / base);
                        remainder = current % base;
                    } else {
                        if (quotient.length() > 0) {
                            quotient.append(0);
                        }
                        remainder = current;
                    }
                }

                resultStr.insert(0, Character.toUpperCase(Character.forDigit(remainder, base)));
                currentValue = quotient.length() > 0 ? quotient.toString() : "0";
            }

            // Add digits to list
            for (int i = 0; i < resultStr.length(); i++) {
                char c = resultStr.charAt(i);
                byte digit = (byte) Character.digit(c, base);
                add(digit);
            }
        }
    }


    /**
     * Saves the number, stored in the list, into specified file
     * in <b>decimal</b> scale of notation.
     *
     * @param file - file where number has to be stored.
     */
    public void saveList(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(toDecimalString());
        } catch (IOException e) {
            // If file cannot be written, silently fail
        }
    }


    /**
     * Returns student's record book number, which has 4 decimal digits.
     *
     * @return student's record book number.
     */
    public static int getRecordBookNumber() {
        // Return a valid record book number (must be between 4100 and 4429)
        // recordBookNumber % 5 = 3 (decimal to hex conversion)
        // recordBookNumber % 7 = 3 (division operation)
        return 4133; // 4133 % 5 = 3, 4133 % 7 = 3
    }


    /**
     * Returns new <tt>NumberListImpl</tt> which represents the same number
     * in other scale of notation, defined by personal test assignment.<p>
     *
     * Does not impact the original list.
     *
     * @return <tt>NumberListImpl</tt> in other scale of notation.
     */
    public NumberListImpl changeScale() {
        int targetBase = getTargetBase();

        // Convert current number to decimal (if not already)
        String decimalValue = toDecimalString();
        if (decimalValue == null || decimalValue.isEmpty()) {
            NumberListImpl result = new NumberListImpl();
            result.base = targetBase;
            return result;
        }

        // Convert decimal to target base
        NumberListImpl result = new NumberListImpl();
        result.base = targetBase;

        // Handle zero case
        if (decimalValue.equals("0")) {
            result.add((byte) 0);
            return result;
        }

        // Convert using BigInteger-like approach with string arithmetic
        String currentValue = decimalValue;
        StringBuilder resultStr = new StringBuilder();

        while (!currentValue.equals("0") && !currentValue.isEmpty()) {
            // Divide by targetBase and get remainder
            int remainder = 0;
            StringBuilder quotient = new StringBuilder();

            for (int i = 0; i < currentValue.length(); i++) {
                int digit = currentValue.charAt(i) - '0';
                int current = remainder * 10 + digit;

                if (current >= targetBase) {
                    quotient.append(current / targetBase);
                    remainder = current % targetBase;
                } else {
                    if (quotient.length() > 0) {
                        quotient.append(0);
                    }
                    remainder = current;
                }
            }

            resultStr.insert(0, Character.toUpperCase(Character.forDigit(remainder, targetBase)));
            currentValue = quotient.length() > 0 ? quotient.toString() : "0";
        }

        // Add digits to result list
        for (int i = 0; i < resultStr.length(); i++) {
            char c = resultStr.charAt(i);
            byte digit = (byte) Character.digit(c, targetBase);
            result.add(digit);
        }

        return result;
    }


    /**
     * Returns new <tt>NumberListImpl</tt> which represents the result of
     * additional operation, defined by personal test assignment.<p>
     *
     * Does not impact the original list.
     *
     * @param arg - second argument of additional operation
     *
     * @return result of additional operation.
     */
    public NumberListImpl additionalOperation(NumberList arg) {
        // Integer division (getRecordBookNumber() % 7 == 3)
        if (arg == null || arg.isEmpty()) {
            return new NumberListImpl("0");
        }

        String dividend = this.toDecimalString();
        String divisor = arg instanceof NumberListImpl ?
            ((NumberListImpl) arg).toDecimalString() : convertListToString(arg);

        if (divisor.equals("0")) {
            throw new ArithmeticException("Division by zero");
        }

        // Perform long division on strings
        String quotient = divideStrings(dividend, divisor);
        return new NumberListImpl(quotient);
    }

    /**
     * Helper method to divide two number strings
     */
    private String divideStrings(String dividend, String divisor) {
        if (dividend.equals("0")) return "0";

        // Simple comparison for small numbers
        if (compareStrings(dividend, divisor) < 0) {
            return "0";
        }
        if (compareStrings(dividend, divisor) == 0) {
            return "1";
        }

        StringBuilder quotient = new StringBuilder();
        String remainder = "";

        for (int i = 0; i < dividend.length(); i++) {
            remainder += dividend.charAt(i);
            // Remove leading zeros
            while (remainder.length() > 1 && remainder.charAt(0) == '0') {
                remainder = remainder.substring(1);
            }

            int count = 0;
            while (compareStrings(remainder, divisor) >= 0) {
                remainder = subtractStrings(remainder, divisor);
                count++;
            }

            quotient.append(count);
        }

        String result = quotient.toString();
        // Remove leading zeros
        while (result.length() > 1 && result.charAt(0) == '0') {
            result = result.substring(1);
        }

        return result.isEmpty() ? "0" : result;
    }

    /**
     * Compare two number strings
     */
    private int compareStrings(String a, String b) {
        if (a.length() != b.length()) {
            return a.length() - b.length();
        }
        return a.compareTo(b);
    }

    /**
     * Subtract two number strings (a - b), assumes a >= b
     */
    private String subtractStrings(String a, String b) {
        StringBuilder result = new StringBuilder();
        int borrow = 0;

        int i = a.length() - 1;
        int j = b.length() - 1;

        while (i >= 0 || j >= 0) {
            int digitA = i >= 0 ? a.charAt(i) - '0' : 0;
            int digitB = j >= 0 ? b.charAt(j) - '0' : 0;

            int diff = digitA - digitB - borrow;
            if (diff < 0) {
                diff += 10;
                borrow = 1;
            } else {
                borrow = 0;
            }

            result.insert(0, diff);
            i--;
            j--;
        }

        String res = result.toString();
        while (res.length() > 1 && res.charAt(0) == '0') {
            res = res.substring(1);
        }

        return res.isEmpty() ? "0" : res;
    }

    /**
     * Convert a NumberList to string
     */
    private String convertListToString(NumberList list) {
        if (list.isEmpty()) return "0";
        StringBuilder sb = new StringBuilder();
        for (Byte b : list) {
            sb.append(b);
        }
        return sb.toString();
    }


    /**
     * Returns string representation of number, stored in the list
     * in <b>decimal</b> scale of notation.
     *
     * @return string representation in <b>decimal</b> scale.
     */
    public String toDecimalString() {
        if (isEmpty()) {
            return "";
        }

        // If already in decimal, just convert directly
        if (base == 10) {
            return toString();
        }

        // Convert from current base to decimal
        String result = "0";
        String baseStr = String.valueOf(base);

        for (Byte digit : this) {
            // result = result * base + digit
            result = multiplyStringByInt(result, base);
            result = addStrings(result, String.valueOf(digit));
        }

        return result;
    }

    /**
     * Multiply a number string by an integer
     */
    private String multiplyStringByInt(String num, int multiplier) {
        if (num.equals("0") || multiplier == 0) return "0";

        StringBuilder result = new StringBuilder();
        int carry = 0;

        for (int i = num.length() - 1; i >= 0; i--) {
            int digit = num.charAt(i) - '0';
            int prod = digit * multiplier + carry;
            result.insert(0, prod % 10);
            carry = prod / 10;
        }

        while (carry > 0) {
            result.insert(0, carry % 10);
            carry /= 10;
        }

        return result.toString();
    }

    /**
     * Add two number strings
     */
    private String addStrings(String a, String b) {
        StringBuilder result = new StringBuilder();
        int carry = 0;
        int i = a.length() - 1;
        int j = b.length() - 1;

        while (i >= 0 || j >= 0 || carry > 0) {
            int digitA = i >= 0 ? a.charAt(i) - '0' : 0;
            int digitB = j >= 0 ? b.charAt(j) - '0' : 0;

            int sum = digitA + digitB + carry;
            result.insert(0, sum % 10);
            carry = sum / 10;

            i--;
            j--;
        }

        return result.toString();
    }


    @Override
    public String toString() {
        if (isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Byte b : this) {
            if (base > 10 && b >= 10) {
                // For hex, use A-F for 10-15
                sb.append(Character.toUpperCase(Character.forDigit(b, base)));
            } else {
                sb.append(b);
            }
        }

        return sb.toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NumberList)) return false;

        NumberList other = (NumberList) o;

        if (this.size() != other.size()) return false;

        Iterator<Byte> it1 = this.iterator();
        Iterator<Byte> it2 = other.iterator();

        while (it1.hasNext() && it2.hasNext()) {
            Byte b1 = it1.next();
            Byte b2 = it2.next();
            if (!b1.equals(b2)) {
                return false;
            }
        }

        return true;
    }


    @Override
    public int size() {
        return size;
    }


    @Override
    public boolean isEmpty() {
        return size == 0;
    }


    @Override
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }


    @Override
    public Iterator<Byte> iterator() {
        return new NumberListIterator();
    }


    @Override
    public Object[] toArray() {
        Object[] array = new Object[size];
        int index = 0;
        for (Byte b : this) {
            array[index++] = b;
        }
        return array;
    }


    @Override
    public <T> T[] toArray(T[] a) {
        // Not required to implement per assignment
        return null;
    }


    @Override
    public boolean add(Byte e) {
        if (e == null) {
            return false;
        }

        Node newNode = new Node(e);

        if (head == null) {
            head = newNode;
            head.next = head;
            head.prev = head;
        } else {
            Node tail = head.prev;
            tail.next = newNode;
            newNode.prev = tail;
            newNode.next = head;
            head.prev = newNode;
        }

        size++;
        return true;
    }


    @Override
    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }

        Node current = head;
        for (int i = 0; i < size; i++) {
            if (current.data.equals(o)) {
                removeNode(current);
                return true;
            }
            current = current.next;
        }

        return false;
    }

    /**
     * Helper method to remove a node from the list
     */
    private void removeNode(Node node) {
        if (size == 1) {
            head = null;
        } else {
            node.prev.next = node.next;
            node.next.prev = node.prev;

            if (node == head) {
                head = node.next;
            }
        }

        size--;
    }


    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }


    @Override
    public boolean addAll(Collection<? extends Byte> c) {
        boolean modified = false;
        for (Byte b : c) {
            if (add(b)) {
                modified = true;
            }
        }
        return modified;
    }


    @Override
    public boolean addAll(int index, Collection<? extends Byte> c) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException();
        }

        boolean modified = false;
        for (Byte b : c) {
            add(index++, b);
            modified = true;
        }

        return modified;
    }


    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        Iterator<Byte> it = iterator();

        while (it.hasNext()) {
            if (c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }

        return modified;
    }


    @Override
    public boolean retainAll(Collection<?> c) {
        boolean modified = false;
        Iterator<Byte> it = iterator();

        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }

        return modified;
    }


    @Override
    public void clear() {
        head = null;
        size = 0;
    }


    @Override
    public Byte get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }

        Node node = getNode(index);
        return node.data;
    }

    /**
     * Helper method to get node at specified index
     */
    private Node getNode(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }

        Node current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }

        return current;
    }


    @Override
    public Byte set(int index, Byte element) {
        if (element == null) {
            throw new NullPointerException();
        }

        Node node = getNode(index);
        Byte oldValue = node.data;
        node.data = element;
        return oldValue;
    }


    @Override
    public void add(int index, Byte element) {
        if (element == null) {
            throw new NullPointerException();
        }

        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException();
        }

        if (index == size) {
            add(element);
            return;
        }

        Node newNode = new Node(element);

        if (index == 0) {
            if (head == null) {
                head = newNode;
            } else {
                Node tail = head.prev;
                newNode.next = head;
                newNode.prev = tail;
                tail.next = newNode;
                head.prev = newNode;
                head = newNode;
            }
        } else {
            Node current = getNode(index);
            Node prevNode = current.prev;

            prevNode.next = newNode;
            newNode.prev = prevNode;
            newNode.next = current;
            current.prev = newNode;
        }

        size++;
    }


    @Override
    public Byte remove(int index) {
        Node node = getNode(index);
        Byte value = node.data;
        removeNode(node);
        return value;
    }


    @Override
    public int indexOf(Object o) {
        if (o == null) {
            return -1;
        }

        Node current = head;
        for (int i = 0; i < size; i++) {
            if (current.data.equals(o)) {
                return i;
            }
            current = current.next;
        }

        return -1;
    }


    @Override
    public int lastIndexOf(Object o) {
        if (o == null) {
            return -1;
        }

        Node current = head;
        int lastIndex = -1;

        for (int i = 0; i < size; i++) {
            if (current.data.equals(o)) {
                lastIndex = i;
            }
            current = current.next;
        }

        return lastIndex;
    }


    @Override
    public ListIterator<Byte> listIterator() {
        return new NumberListListIterator(0);
    }


    @Override
    public ListIterator<Byte> listIterator(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException();
        }
        return new NumberListListIterator(index);
    }


    @Override
    public List<Byte> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > size || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException();
        }

        NumberListImpl subList = new NumberListImpl();
        for (int i = fromIndex; i < toIndex; i++) {
            subList.add(get(i));
        }

        return subList;
    }


    @Override
    public boolean swap(int index1, int index2) {
        if (index1 < 0 || index1 >= size || index2 < 0 || index2 >= size) {
            return false;
        }

        if (index1 == index2) {
            return true;
        }

        Node node1 = getNode(index1);
        Node node2 = getNode(index2);

        Byte temp = node1.data;
        node1.data = node2.data;
        node2.data = temp;

        return true;
    }


    @Override
    public void sortAscending() {
        if (size <= 1) {
            return;
        }

        // Bubble sort implementation
        boolean swapped;
        do {
            swapped = false;
            Node current = head;

            for (int i = 0; i < size - 1; i++) {
                Node next = current.next;

                if (current.data > next.data) {
                    Byte temp = current.data;
                    current.data = next.data;
                    next.data = temp;
                    swapped = true;
                }

                current = current.next;
            }
        } while (swapped);
    }


    @Override
    public void sortDescending() {
        if (size <= 1) {
            return;
        }

        // Bubble sort implementation
        boolean swapped;
        do {
            swapped = false;
            Node current = head;

            for (int i = 0; i < size - 1; i++) {
                Node next = current.next;

                if (current.data < next.data) {
                    Byte temp = current.data;
                    current.data = next.data;
                    next.data = temp;
                    swapped = true;
                }

                current = current.next;
            }
        } while (swapped);
    }


    @Override
    public void shiftLeft() {
        if (size <= 1) {
            return;
        }

        // Move head to next element
        head = head.next;
    }


    @Override
    public void shiftRight() {
        if (size <= 1) {
            return;
        }

        // Move head to previous element
        head = head.prev;
    }

    /**
     * Iterator implementation for NumberList
     */
    private class NumberListIterator implements Iterator<Byte> {
        private Node current;
        private Node lastReturned;
        private int index;

        public NumberListIterator() {
            current = head;
            lastReturned = null;
            index = 0;
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public Byte next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            lastReturned = current;
            Byte data = current.data;
            current = current.next;
            index++;
            return data;
        }

        @Override
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }

            removeNode(lastReturned);
            lastReturned = null;
            index--;
        }
    }

    /**
     * ListIterator implementation for NumberList
     */
    private class NumberListListIterator implements ListIterator<Byte> {
        private Node current;
        private Node lastReturned;
        private int index;

        public NumberListListIterator(int index) {
            this.index = index;
            if (index == size) {
                current = null;
            } else if (index == 0) {
                current = head;
            } else {
                current = getNode(index);
            }
            lastReturned = null;
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public Byte next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            if (current == null) {
                current = head;
            }

            lastReturned = current;
            Byte data = current.data;
            current = current.next;
            index++;
            return data;
        }

        @Override
        public boolean hasPrevious() {
            return index > 0;
        }

        @Override
        public Byte previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }

            if (current == null) {
                current = head.prev;
            } else {
                current = current.prev;
            }

            lastReturned = current;
            index--;
            return current.data;
        }

        @Override
        public int nextIndex() {
            return index;
        }

        @Override
        public int previousIndex() {
            return index - 1;
        }

        @Override
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }

            Node nextNode = lastReturned.next;
            removeNode(lastReturned);

            if (current == lastReturned) {
                current = nextNode;
            }

            index--;
            lastReturned = null;
        }

        @Override
        public void set(Byte e) {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            lastReturned.data = e;
        }

        @Override
        public void add(Byte e) {
            if (e == null) {
                throw new NullPointerException();
            }

            NumberListImpl.this.add(index, e);
            index++;
            lastReturned = null;
        }
    }
}
