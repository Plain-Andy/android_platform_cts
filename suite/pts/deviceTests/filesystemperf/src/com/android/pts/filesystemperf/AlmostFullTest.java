/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.pts.filesystemperf;

import android.cts.util.TimeoutReq;
import com.android.pts.util.MeasureRun;
import com.android.pts.util.MeasureTime;
import com.android.pts.util.PtsAndroidTestCase;
import com.android.pts.util.ReportLog;
import com.android.pts.util.Stat;
import com.android.pts.util.SystemUtil;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AlmostFullTest extends PtsAndroidTestCase {

    private static final String DIR_INITIAL_FILL = "INITIAL_FILL";
    private static final String DIR_SEQ_UPDATE = "SEQ_UPDATE";
    private static final String DIR_RANDOM_WR = "RANDOM_WR";
    private static final String DIR_RANDOM_RD = "RANDOM_RD";
    private static final String TAG = "AlmostFullTest";

    private static final long FREE_SPACE_FINAL = 1000L * 1024 * 1024L;

    // test runner creates multiple instances at the begging.
    // use that to fill disk only once.
    private static AtomicInteger mRefCounter = new AtomicInteger(0);
    private static AtomicBoolean mDiskFilled = new AtomicBoolean(false);

    public AlmostFullTest() {
        int currentCounter = mRefCounter.incrementAndGet();
        Log.i(TAG, "++currentCounter: " + currentCounter);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (mDiskFilled.compareAndSet(false, true)) {
            Log.i(TAG, "Filling disk");
            // initial fill done in two stage as disk can be filled by other components
            long freeDisk = SystemUtil.getFreeDiskSize(getContext());
            long diskToFill = freeDisk - FREE_SPACE_FINAL;
            Log.i(TAG, "free disk " + freeDisk + ", to fill " + diskToFill);
            final long MAX_FILE_SIZE_TO_FILL = 1024L * 1024L * 1024L;
            long filled = 0;
            while (filled < diskToFill) {
                long toFill = diskToFill - filled;
                if (toFill > MAX_FILE_SIZE_TO_FILL) {
                    toFill = MAX_FILE_SIZE_TO_FILL;
                }
                Log.i(TAG, "Generating file " + toFill);
                FileUtil.createNewFilledFile(getContext(),
                        DIR_INITIAL_FILL, toFill);
                filled += toFill;
            }
        }
        Log.i(TAG, "free disk " + SystemUtil.getFreeDiskSize(getContext()));
    }

    @Override
    protected void tearDown() throws Exception {
        Log.i(TAG, "tearDown free disk " + SystemUtil.getFreeDiskSize(getContext()));
        int currentCounter = mRefCounter.decrementAndGet();
        Log.i(TAG, "--currentCounter: " + currentCounter);
        if (currentCounter == 0) {
            FileUtil.removeFileOrDir(getContext(), DIR_INITIAL_FILL);
        }
        FileUtil.removeFileOrDir(getContext(), DIR_SEQ_UPDATE);
        FileUtil.removeFileOrDir(getContext(), DIR_RANDOM_WR);
        FileUtil.removeFileOrDir(getContext(), DIR_RANDOM_RD);
        Log.i(TAG, "tearDown free disk " + SystemUtil.getFreeDiskSize(getContext()));
        super.tearDown();
    }

    @TimeoutReq(minutes = 30)
    public void testSequentialUpdate() throws IOException {
        // now about freeSpaceToLeave should be left
        // and try updating exceeding the free space size
        final long FILE_SIZE = 400L * 1024L * 1024L;
        long freeDisk = SystemUtil.getFreeDiskSize(getContext());
        Log.i(TAG, "Now free space is " + freeDisk);
        assertTrue(freeDisk > FILE_SIZE);
        final int BUFFER_SIZE = 10 * 1024 * 1024;
        final int NUMBER_REPETITION = 10;
        FileUtil.doSequentialUpdateTest(getContext(), DIR_SEQ_UPDATE, getReportLog(), FILE_SIZE,
                BUFFER_SIZE, NUMBER_REPETITION);
    }

    //TODO: file size too small and caching will give wrong better result.
    //      needs to flush cache by reading big files per each read.
    @TimeoutReq(minutes = 60)
    public void testRandomRead() throws IOException {
        final int BUFFER_SIZE = 4 * 1024;
        final long fileSize = 400L * 1024L * 1024L;
        FileUtil.doRandomReadTest(getContext(), DIR_RANDOM_RD, getReportLog(), fileSize,
                BUFFER_SIZE);
    }

    @TimeoutReq(minutes = 60)
    public void testRandomUpdate() throws IOException {
        final int BUFFER_SIZE = 4 * 1024;
        final long fileSize = 400L * 1024L * 1024L;
        FileUtil.doRandomWriteTest(getContext(), DIR_RANDOM_WR, getReportLog(), fileSize,
                BUFFER_SIZE);
    }
}