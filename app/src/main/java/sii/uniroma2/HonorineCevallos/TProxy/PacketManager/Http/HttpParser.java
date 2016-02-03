package sii.uniroma2.HonorineCevallos.TProxy.PacketManager.Http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import sii.uniroma2.HonorineCevallos.TProxy.exceptions.HttpException;

/**
 *
 * ====================================================================
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 *
 * A utility class for parsing http header values.
 *
 * @author Michael Becke
 * @author Oleg Kalnichevski
 *
 * @since 2.0beta1
 */
public class HttpParser {



    /**
     * Constructor for HttpParser.
     */
     public HttpParser() { }

    /**
     * Return byte array from an (unchunked) input stream.
     * Stop reading when "\n" terminator encountered
     * If the stream ends before the line terminator is found,
     * the last part of the string will still be returned.
     * If no input data available, null is returned
     *
     * @param inputStream the stream to read from
     *
     * @throws IOException if an I/O problem occurs
     * @return a byte array from the stream
     */
    public static byte[] readRawLine(InputStream inputStream) throws IOException {

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int ch;
        while ((ch = inputStream.read()) >= 0) {
            buf.write(ch);
            if (ch == '\n') {
                break;
            }
        }
        if (buf.size() == 0) {
            return null;
        }
        return buf.toByteArray();
    }

    /**
     * Read up to "\n" from an (unchunked) input stream.
     * If the stream ends before the line terminator is found,
     * the last part of the string will still be returned.
     * If no input data available, null is returned
     *
     * @param buffer the ByteBuffer to read from
     *
     * @throws IOException if an I/O problem occurs
     * @return a line from the stream
     */
    public static String readLine(ByteBuffer buffer) throws IOException {
        byte[] rawdata = buffer.array();
        if (rawdata == null) {
            return null;
        }
        int len = rawdata.length;
        int offset = 0;
        if (len > 0) {
            if (rawdata[len - 1] == '\n') {
                offset++;
                if (len > 1) {
                    if (rawdata[len - 2] == '\r') {
                        offset++;
                    }
                }
            }
        }
        return HttpConstants.getString(rawdata, 0, len - offset);
    }

    /**
     * Parses headers from the given stream.  Headers with the same name are not
     * combined.
     *
     * @param buffer the ByteBuffer to read headers from
     *
     * @return an array of headers in the order in which they were parsed
     *
     * @throws IOException if an IO error occurs while reading from the stream
     * @throws HttpException if there is an error parsing a header value
     */
    public static Header[] parseHeaders(ByteBuffer buffer) throws IOException, HttpException {

        ArrayList headers = new ArrayList();
        String name = null;
        StringBuffer value = null;

        for (; ;) {
            String line = HttpParser.readLine(buffer);
            if ((line == null) || (line.length() < 1)) {
                break;
            }

            // Parse the header name and value
            // Check for folded headers first
            // Detect LWS-char see HTTP/1.0 or HTTP/1.1 Section 2.2
            // discussion on folded headers
            if ((line.charAt(0) == ' ') || (line.charAt(0) == '\t')) {
                // we have continuation folded header
                // so append value
                if (value != null) {
                    value.append(' ');
                    value.append(line.trim());
                }
            } else {
                // make sure we save the previous name,value pair if present
                if (name != null) {
                    headers.add(new Header(name, value.toString()));
                }

                // Otherwise we should have normal HTTP header line
                // Parse the header name and value
                int colon = line.indexOf(":");
                if (colon < 0) {
                    throw new HttpException("Unable to parse header: " + line);
                }
                name = line.substring(0, colon).trim();
                value = new StringBuffer(line.substring(colon + 1).trim());
            }

        }

        // make sure we save the last name,value pair if present
        if (name != null) {
            headers.add(new Header(name, value.toString()));
        }

        return (Header[]) headers.toArray(new Header[headers.size()]);
    }
}