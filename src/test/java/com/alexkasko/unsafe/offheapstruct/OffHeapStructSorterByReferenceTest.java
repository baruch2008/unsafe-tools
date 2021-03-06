/*
 * Copyright 2014 Alex Kasko (alexkasko.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alexkasko.unsafe.offheapstruct;

import com.alexkasko.unsafe.bytearray.ByteArrayTool;
import com.alexkasko.unsafe.offheap.OffHeapDisposableIterable;
import com.alexkasko.unsafe.offheap.OffHeapDisposableIterator;
import org.junit.Test;

import java.util.*;

import static com.alexkasko.unsafe.offheap.OffHeapUtils.free;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * User: alexkasko
 * Date: 7/8/14
 */
public class OffHeapStructSorterByReferenceTest {
    private static final ByteArrayTool bt = ByteArrayTool.get();
//    private static final int LENGTH = 1000000;
    private static final int LENGTH = 10000;

    @Test
    public void testComparator() {
//        for(int j=0; j< 100000; j++) {
        OffHeapStructArray arr = null;
        try {
//            System.out.println(j);
//            Random random = new Random(j);
            Random random = new Random(42);
            long[] heapHeaders = new long[LENGTH];
            Map<Long, List<Long>> heapPayloads = new HashMap<Long, List<Long>>();
            arr = new OffHeapStructArray(LENGTH, 16);
            byte[] buf = new byte[16];
            long header = 0;
            for (int i = 0; i < LENGTH; i++) {
                long payload = random.nextInt();
                if (0 == i % 5) {
                    header = random.nextInt();
                }
                heapHeaders[i] = header;
                List<Long> existed = heapPayloads.get(header);
                if (null != existed) {
                    existed.add(payload);
                } else {
                    List<Long> li = new ArrayList<Long>();
                    li.add(payload);
                    heapPayloads.put(header, li);
                }
                bt.putLong(buf, 0, payload);
                bt.putLong(buf, 8, header);
                arr.set(i, buf);
            }
            // standard sort for heap array
//            System.out.println(Arrays.toString(heapHeaders));
            Arrays.sort(heapHeaders);
//            System.out.println(Arrays.toString(heapHeaders));
            // off-heap sort
//            System.out.println(toStringList(arr));
            OffHeapDisposableIterable<byte[]> sorted = OffHeapStructSorterByReference.sortedIterable(arr, new LongComp());
            OffHeapDisposableIterator<byte[]> iter = sorted.iterator();
//            System.out.println(toStringList(arr));
            // compare results
            for (int i = 0; i < LENGTH; i++) {
                buf = iter.next();
                long head = bt.getLong(buf, 8);
                assertEquals(head, heapHeaders[i]);
                long payl = bt.getLong(buf, 0);
                assertTrue(heapPayloads.get(head).remove(payl));
            }
        } finally {
            free(arr);
        }
//        }
    }

    private static class LongComp implements Comparator<OffHeapStructAccessor> {
        @Override
        public int compare(OffHeapStructAccessor o1, OffHeapStructAccessor o2) {
            long l1 = o1.getLong(8);
            long l2 = o2.getLong(8);
            if(l1 > l2) return 1;
            if(l1 < l2) return -1;
            return 0;
        }
    }
}
