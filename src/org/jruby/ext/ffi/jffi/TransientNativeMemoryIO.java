/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2011 Wayne Meissner
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.PageManager;
import org.jruby.Ruby;
import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.util.WeakReferenceReaper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class TransientNativeMemoryIO extends BoundedNativeMemoryIO implements DirectMemoryIO {
    /** Keeps strong references to the memory bucket until cleanup */
    private static final Map<Magazine, Boolean> referenceSet = new ConcurrentHashMap<Magazine, Boolean>();
    private static final ThreadLocal<Magazine> currentMagazine = new ThreadLocal<Magazine>();

    private final Object sentinel;

    /**
     * Allocates native memory, aligned to a minimum boundary.
     *
     * @param runtime The Ruby runtime
     * @param size The number of bytes to allocate
     * @param align The minimum alignment of the memory
     * @param clear Whether the memory should be cleared (zeroed)
     * @return A new {@link org.jruby.ext.ffi.AllocatedDirectMemoryIO}
     */
    static DirectMemoryIO allocateAligned(Ruby runtime, int size, int align, boolean clear) {
        if (size > 1024) {
            return AllocatedNativeMemoryIO.allocateAligned(runtime, size, align, clear);
        }

        Magazine magazine = currentMagazine.get();
        Object sentinel = magazine != null ? magazine.get() : null;
        long address = 0;

        if (sentinel == null || (address = magazine.allocate(size, align)) == 0) {
            referenceSet.put(magazine = new Magazine(sentinel = new Object()), Boolean.TRUE);
            currentMagazine.set(magazine);
            address = magazine.allocate(size, align);
        }

        return new TransientNativeMemoryIO(runtime, sentinel, address, size);
    }

    private TransientNativeMemoryIO(Ruby runtime, Object sentinel, long address, int size) {
        super(runtime, address, size);
        this.sentinel = sentinel;
    }
    
    private static long align(long offset, long align) {
        return (offset + align - 1L) & ~(align - 1L);
    }

    /**
     * Holder for a group of memory allocations.
     */
    private static final class Magazine extends WeakReferenceReaper<Object> implements Runnable {
        private final PageManager pm;
        private final long page;
        private final long end;
        private long memory;

        Magazine(Object sentinel) {
            super(sentinel);
            this.pm = PageManager.getInstance();
            this.memory = this.page = pm.allocatePages(1, PageManager.PROT_READ | PageManager.PROT_WRITE);
            this.end = this.page + pm.pageSize();
        }

        long allocate(int size, int align) {
            long address = align(this.memory, align);
            if (address + size <= end) {
                memory = address + size;
                return address;
            }

            return 0L;
        }
        
        long getBytesUsed() {
            return memory - page;
        }

        public final void run() {
            pm.freePages(page, 1);
            referenceSet.remove(this);

        }
    }
}
