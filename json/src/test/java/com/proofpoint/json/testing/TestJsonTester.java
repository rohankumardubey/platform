/*
 * Copyright 2013 Proofpoint, Inc.
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
package com.proofpoint.json.testing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.proofpoint.json.JsonCodec.listJsonCodec;
import static com.proofpoint.json.testing.JsonTester.assertJsonEncode;
import static com.proofpoint.json.testing.JsonTester.decodeJson;
import static org.testng.Assert.assertEquals;

public class TestJsonTester
{

    private final SimpleEncoder simpleEncoder = new SimpleEncoder();
    private Map<String, Object> simpleExpected;
    private final ComplexEncoder complexEncoder = new ComplexEncoder();
    private Map<String, Object> complexExpected;

    @BeforeMethod
    public void setup()
    {
        simpleExpected = new HashMap<>();
        simpleExpected.put("s", "fred");
        simpleExpected.put("i", 3);
        simpleExpected.put("b", true);

        complexExpected = new HashMap<>();
        complexExpected.put("list", List.of("a", "b", "a"));
        complexExpected.put("obj", simpleExpected);
    }

    @Test
    public void testEncodeSimple()
    {
        assertJsonEncode(simpleEncoder, simpleExpected);
    }

    @Test
    public void testEncodeDateTime()
    {
        assertJsonEncode(Instant.ofEpochMilli(1376344694123L), "2013-08-12T21:58:14.123Z");
    }

    @Test(expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = "JSON encoding \\{\r?\n" +
            "  \"s\" : \"fred\",\r?\n" +
            "  \"i\" : 3,\r?\n" +
            "  \"b\" : true\r?\n" +
            "} expected \\[\\{b=true, s=fred, extra=field, i=3}] but found \\[\\{s=fred, i=3, b=true}]")
    public void testMissingField()
    {
       simpleExpected.put("extra", "field");
       assertJsonEncode(simpleEncoder, simpleExpected);
    }

    @Test(expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = "testing message JSON encoding \\{\r?\n" +
            "  \"s\" : \"fred\",\r?\n" +
            "  \"i\" : 3,\r?\n" +
            "  \"b\" : true\r?\n" +
            "} expected \\[\\{s=fred, i=3}] but found \\[\\{s=fred, i=3, b=true}]")
    public void testExtraField()
    {
       simpleExpected.remove("b");
       assertJsonEncode(simpleEncoder, simpleExpected, "testing message");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testStringAsNumber()
    {
       simpleExpected.put("i", "3");
       assertJsonEncode(simpleEncoder, simpleExpected);
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testStringAsBoolean()
    {
       simpleExpected.put("b", "true");
       assertJsonEncode(simpleEncoder, simpleExpected);
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testStringAsNull()
    {
       simpleExpected.put("nul", "null");
       assertJsonEncode(simpleEncoder, simpleExpected);
    }
    
    @Test
    public void testEncodeComplex()
    {
        assertJsonEncode(complexEncoder, complexExpected);
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testWrongListMember()
    {
        complexExpected.put("list", List.of("a", "b", "b"));
        assertJsonEncode(complexEncoder, complexExpected);
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testWrongListOrder()
    {
        complexExpected.put("list", List.of("a", "a", "b"));
        assertJsonEncode(complexEncoder, complexExpected);
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testMissingListMember()
    {
        complexExpected.put("list", List.of("a", "b"));
        assertJsonEncode(complexEncoder, complexExpected);
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testSubObjWithWrongValue()
    {
        simpleExpected.put("s", "wrong");
        assertJsonEncode(complexEncoder, complexExpected);
    }

    @Test
    public void testDecodeClass()
    {
        SimpleDecoder simpleDecoder = decodeJson(SimpleDecoder.class, ImmutableMap.of("intValue", 3, "extra", "value"));
        assertEquals(simpleDecoder.intValue, 3);
    }

    @Test
    public void testDecodeCodec()
    {
        List<SimpleDecoder> simpleDecoder = decodeJson(listJsonCodec(SimpleDecoder.class), List.of(ImmutableMap.of("intValue", 3, "extra", "value")));
        assertEquals(simpleDecoder.size(), 1);
        assertEquals(simpleDecoder.get(0).intValue, 3);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Invalid JSON string for \\[simple type, class com\\.proofpoint\\.json\\.testing\\.TestJsonTester\\$SimpleDecoder\\].*")
    public void testDecodeConstructionException()
    {
        decodeJson(SimpleDecoder.class, ImmutableMap.of());
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Invalid JSON string for \\[collection type; class java\\.util\\.List, contains \\[simple type, class com\\.proofpoint\\.json\\.testing\\.TestJsonTester\\$SimpleDecoder\\]\\].*")
    public void testDecodeWrongType()
    {
        decodeJson(listJsonCodec(SimpleDecoder.class), ImmutableMap.of("intValue", 3));
    }

    private static class SimpleEncoder
    {
        @JsonProperty
        private final String s = "fred";
        @JsonProperty
        private final int i = 3;
        @JsonProperty
        private final boolean b = true;
        @JsonProperty
        private final Integer nul = null;
    }

    private static class ComplexEncoder
    {
        @JsonProperty
        private final List<String> list = List.of("a", "b", "a");
        @JsonProperty
        private final SimpleEncoder obj = new SimpleEncoder();
    }

    private static class SimpleDecoder
    {
        final int intValue;

        private SimpleDecoder(int intValue)
        {
            this.intValue = intValue;
        }

        @JsonCreator
        private static SimpleDecoder simpleDecoder(@JsonProperty("intValue") Integer intValue){
            return new SimpleDecoder(intValue);
        }
    }
}
