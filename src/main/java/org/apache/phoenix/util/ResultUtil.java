/*
 * Copyright 2010 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.phoenix.util;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * Static class for various methods that would be nice to have added to {@link org.apache.hadoop.hbase.client.Result}.
 * These methods work off of the raw bytes preventing the explosion of Result into object form.
 * 
 * @author jtaylor
 * @since 0.1
 */
public class ResultUtil {
    private ResultUtil() {
    }
    
    /**
     * Return a pointer into a potentially much bigger byte buffer that points to the key of a Result.
     * @param r
     */
    public static ImmutableBytesWritable getKey(Result r) {
        return getKey(r, 0);
    }
    
    public static void getKey(Result r, ImmutableBytesWritable key) {
        key.set(r.getRow());
        //key.set(getRawBytes(r), getKeyOffset(r), getKeyLength(r));
    }
    
    public static void getKey(KeyValue value, ImmutableBytesWritable key) {
        key.set(value.getBuffer(), value.getRowOffset(), value.getRowLength());
    }
    
    /**
     * Return a pointer into a potentially much bigger byte buffer that points to the key of a Result.
     * Use offset to return a subset of the key bytes, for example to skip the organization ID embedded
     * in all of our keys.
     * @param r
     * @param offset offset added to start of key and subtracted from key length (to select subset of key bytes)
     */
    public static ImmutableBytesWritable getKey(Result r, int offset) {
        return new ImmutableBytesWritable(getRawBytes(r), getKeyOffset(r) + offset, getKeyLength(r) - offset);
    }

    public static void getKey(Result r, int offset, int length, ImmutableBytesWritable key) {
        key.set(getRawBytes(r), getKeyOffset(r) + offset, length);
    }

    /**
     * Comparator for comparing the keys from two Results in-place, without allocating new byte arrays
     */
    public static final Comparator<Result> KEY_COMPARATOR = new Comparator<Result>() {

        @Override
        public int compare(Result r1, Result r2) {
            byte[] r1Bytes = getRawBytes(r1);
            byte[] r2Bytes = getRawBytes(r2);
            return Bytes.compareTo(r1Bytes, getKeyOffset(r1), getKeyLength(r1), r2Bytes, getKeyOffset(r2), getKeyLength(r2));
        }
        
    };
    
    /**
     * Get the offset into the Result byte array to the key.
     * @param r
     * @return
     */
    static int getKeyOffset(Result r) {
        // Special case for when Result was instantiated via KeyValue array (no bytes in that case) versus returned from a scanner
        return (r.getBytes() == null ? r.raw()[0].getOffset() : (r.getBytes().getOffset() + Bytes.SIZEOF_INT /* KV length in Result */)) + KeyValue.ROW_OFFSET /* key offset in KV */ + Bytes.SIZEOF_SHORT /* key length */;
    }
    
    static int getKeyLength(Result r) {
        // Key length stored right before key as a short
        return Bytes.toShort(getRawBytes(r), getKeyOffset(r) - Bytes.SIZEOF_SHORT);
    }
    
    static byte[] getRawBytes(Result r) {
        // Handle special case for when Result was instantiated via KeyValue array (no bytes in that case) versus returned from a scanner
        ImmutableBytesWritable rPtr = r.getBytes();
        if (rPtr != null)
            return rPtr.get();
        return r.raw()[0].getBuffer();
    }

    public static int compareKeys(Result r1, Result r2) {
        return Bytes.compareTo(getRawBytes(r1), getKeyOffset(r1), getKeyLength(r1), getRawBytes(r2), getKeyOffset(r2), getKeyLength(r2));
    }

    /**
     * Binary search for latest column value without allocating memory in the process
     */
    public static KeyValue getColumnLatest(Result r, byte[] family, byte[] qualifier) {
        byte[] rbytes = getRawBytes(r);
        int roffset = getKeyOffset(r);
        int rlength = getKeyLength(r);
        return getColumnLatest(r, rbytes, roffset, rlength, family, 0, family.length, qualifier, 0, qualifier.length);
    }

    public static KeyValue getSearchTerm(Result r, byte[] family, byte[] qualifier) {
        byte[] rbytes = getRawBytes(r);
        int roffset = getKeyOffset(r);
        int rlength = getKeyLength(r);
        return KeyValue.createFirstOnRow(rbytes, roffset, rlength, family, 0, family.length, qualifier, 0, qualifier.length);
    }
    /**
     * Binary search for latest column value without allocating memory in the process
     */
    public static KeyValue getColumnLatest(Result r, byte[] row, int roffset, int rlength, byte[] family, int foffset, int flength, byte[] qualifier, int qoffset, int qlength) {
        KeyValue searchTerm = KeyValue.createFirstOnRow(row, roffset, rlength, family, foffset, flength, qualifier, qoffset, qlength);
        return getColumnLatest(r,searchTerm);
        
    }

     /**
     * Binary search for latest column value without allocating memory in the process
     * @param r
     * @param searchTerm
     */
    public static KeyValue getColumnLatest(Result r, KeyValue searchTerm) {
        KeyValue [] kvs = r.raw(); // side effect possibly.
        if (kvs == null || kvs.length == 0) {
          return null;
        }
        
        // pos === ( -(insertion point) - 1)
        int pos = Arrays.binarySearch(kvs, searchTerm, KeyValue.COMPARATOR);
        // never will exact match
        if (pos < 0) {
          pos = (pos+1) * -1;
          // pos is now insertion point
        }
        if (pos == kvs.length) {
          return null; // doesn't exist
        }

        KeyValue kv = kvs[pos];
        if (Bytes.compareTo(kv.getBuffer(), kv.getFamilyOffset(), kv.getFamilyLength(),
                searchTerm.getBuffer(), searchTerm.getFamilyOffset(), searchTerm.getFamilyLength()) != 0) {
            return null;
        }
        if (Bytes.compareTo(kv.getBuffer(), kv.getQualifierOffset(), kv.getQualifierLength(),
                searchTerm.getBuffer(), searchTerm.getQualifierOffset(), searchTerm.getQualifierLength()) != 0) {
            return null;
        }
        return kv;
    }
}
