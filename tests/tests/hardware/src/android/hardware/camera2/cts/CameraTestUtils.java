/*
 * Copyright 2013 The Android Open Source Project
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

package android.hardware.camera2.cts;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CameraProperties;
import android.hardware.camera2.CameraPropertiesKeys;
import android.hardware.camera2.Size;
import android.media.Image;
import android.media.Image.Plane;
import android.util.Log;

import junit.framework.Assert;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A package private utility class for wrapping up the camera2 cts test common utility functions
 */
class CameraTestUtils extends Assert {
    private static final String TAG = "CameraTestUtils";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

    /**
     * <p>Read data from all planes of an Image into a contiguous unpadded, unpacked
     * 1-D linear byte array, such that it can be write into disk, or accessed by
     * software conveniently. It supports YUV_420_888/NV21/YV12 and JPEG input
     * Image format.</p>
     *
     * <p>For YUV_420_888/NV21/YV12/Y8/Y16, it returns a byte array that contains
     * the Y plane data first, followed by U(Cb), V(Cr) planes if there is any
     * (xstride = width, ystride = height for chroma and luma components).</p>
     *
     * <p>For JPEG, it returns a 1-D byte array contains a complete JPEG image.</p>
     */
    public static byte[] getDataFromImage(Image image) {
        assertNotNull("Invalid image:", image);
        int format = image.getFormat();
        int width = image.getWidth();
        int height = image.getHeight();
        int rowStride, pixelStride;
        byte[] data = null;

        // Read image data
        Plane[] planes = image.getPlanes();
        assertTrue("Fail to get image planes", planes != null && planes.length > 0);

        // Check image validity
        checkAndroidImageFormat(image);

        ByteBuffer buffer = null;
        // JPEG doesn't have pixelstride and rowstride, treat it as 1D buffer.
        if (format == ImageFormat.JPEG) {
            buffer = planes[0].getBuffer();
            assertNotNull("Fail to get jpeg ByteBuffer", buffer);
            data = new byte[buffer.capacity()];
            buffer.get(data);
            return data;
        }

        int offset = 0;
        data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        if(VERBOSE) Log.v(TAG, "get data from " + planes.length + " planes");
        for (int i = 0; i < planes.length; i++) {
            buffer = planes[i].getBuffer();
            assertNotNull("Fail to get bytebuffer from plane", buffer);
            rowStride = planes[i].getRowStride();
            assertTrue("rowStride should be no less than width", rowStride >= width);
            pixelStride = planes[i].getPixelStride();
            assertTrue("pixel stride " + pixelStride + " is invalid", pixelStride > 0);
            if (VERBOSE) {
                Log.v(TAG, "pixelStride " + pixelStride);
                Log.v(TAG, "rowStride " + rowStride);
                Log.v(TAG, "width " + width);
                Log.v(TAG, "height " + height);
            }
            // For multi-planar yuv images, assuming yuv420 with 2x2 chroma subsampling.
            int w = (i == 0) ? width : width / 2;
            int h = (i == 0) ? height : height / 2;
            assertTrue("rowStride " + rowStride + " should be >= width " + w , rowStride >= w);
            for (int row = 0; row < h; row++) {
                int bytesPerPixel = ImageFormat.getBitsPerPixel(format) / 8;
                if (pixelStride == bytesPerPixel) {
                    // Special case: optimized read of the entire row
                    int length = w * bytesPerPixel;
                    buffer.get(data, offset, length);
                    // Advance buffer the remainder of the row stride
                    buffer.position(buffer.position() + rowStride - length);
                    offset += length;
                } else {
                    // Generic case: should work for any pixelStride but slower.
                    // Use use intermediate buffer to avoid read byte-by-byte from
                    // DirectByteBuffer, which is very bad for performance
                    buffer.get(rowData, 0, rowStride);
                    for (int col = 0; col < w; col++) {
                        data[offset++] = rowData[col * pixelStride];
                    }
                }
            }
            if (VERBOSE) Log.v(TAG, "Finished reading data from plane " + i);
        }
        return data;
    }

    /**
     * <p>Check android image format validity for an image, only support below formats:</p>
     *
     * <p>YUV_420_888/NV21/YV12/Y8/Y16, can add more for future</p>
     */
    public static void checkAndroidImageFormat(Image image) {
        int format = image.getFormat();
        Plane[] planes = image.getPlanes();
        switch (format) {
          case ImageFormat.YUV_420_888:
          case ImageFormat.NV21:
          case ImageFormat.YV12:
              assertEquals("YUV420 format Images should have 3 planes", 3, planes.length);
              break;
          case ImageFormat.Y8:
          case ImageFormat.Y16:
              assertEquals("Y8/Y16 Image should have 1 plane", 1, planes.length);
              break;
          case ImageFormat.JPEG:
              assertEquals("Jpeg Image should have one plane", 1, planes.length);
              break;
          default:
              fail("Unsupported Image Format: " + format);
      }
    }

    public static void dumpFile(String fileName, byte[] data) {
        FileOutputStream outStream;
        try {
            Log.v(TAG, "output will be saved as " + fileName);
            outStream = new FileOutputStream(fileName);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to create debug output file " + fileName, ioe);
        }

        try {
            outStream.write(data);
            outStream.close();
        } catch (IOException ioe) {
            throw new RuntimeException("failed writing data to file " + fileName, ioe);
        }
    }

    public static Size[] getSupportedSizeForFormat(int format, CameraDevice camera)
            throws Exception {
        CameraMetadata.Key<Size[]> key = null;
        CameraProperties properties = camera.getProperties();
        assertNotNull("Can't get camera properties!", properties);
        switch (format) {
            case ImageFormat.JPEG:
                key = CameraPropertiesKeys.Scaler.AVAILABLE_JPEG_SIZES;
                break;
            case ImageFormat.YUV_420_888:
            case ImageFormat.YV12:
            case ImageFormat.NV21:
            case ImageFormat.Y8:
            case ImageFormat.Y16:
                key = CameraPropertiesKeys.Scaler.AVAILABLE_PROCESSED_SIZES;
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Invalid format specified 0x%x", format));
        }
        Size[] availableSizes = properties.get(key);
        if (VERBOSE) Log.v(TAG, "Supported sizes are: " + Arrays.deepToString(availableSizes));
        return availableSizes;
    }
}