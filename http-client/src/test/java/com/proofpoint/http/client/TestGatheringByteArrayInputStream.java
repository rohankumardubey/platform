/*
 * Copyright 2018 Proofpoint, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.proofpoint.http.client;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static java.lang.Math.min;
import static java.lang.System.arraycopy;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;

public class TestGatheringByteArrayInputStream
{
    @Test
    public void testNormal()
    {
        byte[] lastStringArray = "client".getBytes(UTF_8);
        byte[] lastArray = Arrays.copyOf(lastStringArray, lastStringArray.length + 32);
        arraycopy(lastStringArray, 0, lastArray, 0, lastStringArray.length);

        List<byte[]> buffers = List.of(
                "hello ".getBytes(UTF_8),
                "this ".getBytes(UTF_8),
                "is ".getBytes(UTF_8),
                "http ".getBytes(UTF_8),
                lastArray);
        byte[] expectedAll = "hello this is http client".getBytes();
        byte[] expectedPartial = "hello this".getBytes();

        try (GatheringByteArrayInputStream in = new GatheringByteArrayInputStream(buffers, expectedAll.length)) {
            byte[] buffer = new byte[expectedAll.length + 32];

            // read the first part
            assertEquals(in.read(buffer, 0, expectedPartial.length), expectedPartial.length);

            // verify the first part
            assertByteArrayEquals(buffer, 0, expectedPartial, 0, expectedPartial.length);

            // read the remaining part
            assertEquals(in.read(buffer, expectedPartial.length, buffer.length - expectedPartial.length), expectedAll.length - expectedPartial.length);

            // verify the whole string
            assertByteArrayEquals(buffer, 0, expectedAll, 0, expectedAll.length);

            // make sure there is no more data
            assertEquals(-1, in.read(buffer, 0, expectedAll.length));
        }
    }

    @Test
    public void testSingleByteRead()
    {
        byte[] expected = "This is a test for single byte read".getBytes(UTF_8);
        List<byte[]> buffers = List.of(
                "This ".getBytes(UTF_8),
                "is ".getBytes(UTF_8),
                "a test ".getBytes(UTF_8),
                "for".getBytes(UTF_8),
                " single byte read".getBytes(UTF_8));
        byte[] resultBuffer = new byte[expected.length];

        try (GatheringByteArrayInputStream in = new GatheringByteArrayInputStream(buffers, expected.length)) {
            for (int i = 0; i < expected.length; i++) {
                resultBuffer[i] = (byte) in.read();
            }
            assertByteArrayEquals(resultBuffer, 0, expected, 0, expected.length);
            assertEquals(in.read(), -1);
        }
    }

    @Test
    public void testNegativeSingleByteRead()
    {
        byte[] expected = new byte[1];
        expected[0] = -100;
        try (GatheringByteArrayInputStream in = new GatheringByteArrayInputStream(List.of(expected), expected.length)) {
            assertEquals(in.read(), expected[0] & 0x000000ff);
            assertEquals(in.read(), -1);
        }
    }

    @Test
    public void testSkip()
    {
        byte[] allDataBytes = "Hello, this is http client package, and I am just a test for GatheringByteArrayInputStream".getBytes(UTF_8);
        int length = allDataBytes.length;
        try (GatheringByteArrayInputStream in = new GatheringByteArrayInputStream(
                List.of(
                        Arrays.copyOfRange(allDataBytes, 0, length / 3),
                        Arrays.copyOfRange(allDataBytes, length / 3, length / 3 + length / 2),
                        Arrays.copyOfRange(allDataBytes, length / 3 + length / 2, length)),
                length)) {
            int skipped = length / 2;
            int firstPartLength = length / 4;
            int restPartLength = length - firstPartLength - skipped;
            byte[] actual = new byte[length - skipped + 10];

            // read the first part
            assertEquals(in.read(actual, 0, firstPartLength), firstPartLength);
            assertByteArrayEquals(actual, 0, allDataBytes, 0, firstPartLength);

            // skip some bytes
            assertEquals(in.skip(skipped), skipped);

            // read the rest part
            assertEquals(in.read(actual, firstPartLength, restPartLength + 10), restPartLength);
            assertByteArrayEquals(actual, firstPartLength, allDataBytes, firstPartLength + skipped, restPartLength);

            assertEquals(in.skip(10), 0);
            assertEquals(in.read(), -1);
        }
    }

    @Test
    public void testLargeData()
    {
        int length = 123456789;
        Random random = new Random(0);
        byte[] expected = new byte[length];

        // randomly generate the bytes
        random.nextBytes(expected);
        List<byte[]> buffers = new ArrayList<>();
        int copyBytes = 0;
        while (copyBytes < length) {
            int currentLength = min(length - copyBytes, random.nextInt(1 << 20) + 64);
            buffers.add(Arrays.copyOfRange(expected, copyBytes, copyBytes + currentLength));
            copyBytes += currentLength;
        }
        try (GatheringByteArrayInputStream in = new GatheringByteArrayInputStream(buffers, length)) {
            byte[] actual = new byte[length];
            copyBytes = 0;
            while (copyBytes < length) {
                int currentLength = min(length - copyBytes, random.nextInt(1 << 20) + 64);
                assertEquals(in.read(actual, copyBytes, currentLength), currentLength);
                assertByteArrayEquals(actual, copyBytes, expected, copyBytes, currentLength);
                copyBytes += currentLength;
            }
            assertEquals(in.skip(100), 0);
            assertEquals(in.read(), -1);
        }
    }

    private static void assertByteArrayEquals(
            byte[] actual,
            int actualStart,
            byte[] expected,
            int expectedStart,
            int length)
    {
        for (int i = 0; i < length; i++) {
            assertEquals(actual[i + actualStart], expected[i + expectedStart]);
        }
    }
}
