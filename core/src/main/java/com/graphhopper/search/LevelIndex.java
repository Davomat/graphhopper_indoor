/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.search;

import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.Storable;
import com.graphhopper.util.BitUtil;
import com.graphhopper.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ottavio Campana
 * @author Peter Karich
 */
public class LevelIndex implements Storable<LevelIndex> {
    private static final Logger logger = LoggerFactory.getLogger(LevelIndex.class);
    private static final long START_POINTER = 1;
    private final DataAccess levels;
    private long bytePointer = START_POINTER;
    // minor optimization for the previous stored name
    private String lastLevel;
    private long lastIndex;

    public LevelIndex(Directory dir) {
        levels = dir.find("levels");
    }

    @Override
    public LevelIndex create(long initBytes) {
        levels.create(initBytes);
        return this;
    }

    @Override
    public boolean loadExisting() {
        if (levels.loadExisting()) {
            bytePointer = BitUtil.LITTLE.combineIntsToLong(levels.getHeader(0), levels.getHeader(4));
            return true;
        }

        return false;
    }

    /**
     * @return the byte pointer to the level
     */
    public long put(String level) {
        if (level == null || level.isEmpty()) {
            return 0;
        }
        if (level.equals(lastLevel)) {
            return lastIndex;
        }
        byte[] bytes = getBytes(level);
        long oldPointer = bytePointer;
        levels.ensureCapacity(bytePointer + 1 + bytes.length);
        byte[] sizeBytes = new byte[]{
                (byte) bytes.length
        };
        levels.setBytes(bytePointer, sizeBytes, sizeBytes.length);
        bytePointer++;
        levels.setBytes(bytePointer, bytes, bytes.length);
        bytePointer += bytes.length;
        lastLevel = level;
        lastIndex = oldPointer;
        return oldPointer;
    }

    private byte[] getBytes(String level) {
        byte[] bytes = null;
        for (int i = 0; i < 2; i++) {
            bytes = level.getBytes(Helper.UTF_CS);
            // we have to store the size of the array into *one* byte
            if (bytes.length > 255) {
                String newLevel = level.substring(0, 256 / 4);
                logger.info("Way level is too long: " + level + " truncated to " + newLevel);
                level = newLevel;
                continue;
            }
            break;
        }
        if (bytes.length > 255) {
            // really make sure no such problem exists
            throw new IllegalStateException("Way level is too long: " + level);
        }
        return bytes;
    }

    public String get(long pointer) {
        if (pointer < 0)
            throw new IllegalStateException("Pointer to access LevelIndex cannot be negative:" + pointer);

        // default
        if (pointer == 0)
            return "";

        byte[] sizeBytes = new byte[1];
        levels.getBytes(pointer, sizeBytes, 1);
        int size = sizeBytes[0] & 0xFF;
        byte[] bytes = new byte[size];
        levels.getBytes(pointer + sizeBytes.length, bytes, size);
        return new String(bytes, Helper.UTF_CS);
    }

    @Override
    public void flush() {
        levels.setHeader(0, BitUtil.LITTLE.getIntLow(bytePointer));
        levels.setHeader(4, BitUtil.LITTLE.getIntHigh(bytePointer));
        levels.flush();
    }

    @Override
    public void close() {
        levels.close();
    }

    @Override
    public boolean isClosed() {
        return levels.isClosed();
    }

    public void setSegmentSize(int segments) {
        levels.setSegmentSize(segments);
    }

    @Override
    public long getCapacity() {
        return levels.getCapacity();
    }

    public void copyTo(LevelIndex levelIndex) {
        levels.copyTo(levelIndex.levels);
    }
}
