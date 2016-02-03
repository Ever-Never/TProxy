package sii.uniroma2.HonorineCevallos.TProxy.PacketManager.Http;
/*
 * $Header: /home/cvs/jakarta-commons/httpclient/src/java/org/apache/commons/httpclient/HttpConstants.java,v 1.10.2.2 2004/02/27 19:11:10 olegk Exp $
 * $Revision: 1.10.2.2 $
 * $Date: 2004/02/27 19:11:10 $
 *
 * ====================================================================
 *
 *  Copyright 1999-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * .
 *
 * [Additional notices, if required by prior licensing conditions]
 *
 */

import java.io.UnsupportedEncodingException;

/**
 * HTTP content conversion routines.
 *
 * @author Oleg Kalnichevski
 * @author Mike Bowler
 */
public class HttpConstants {

    /** Character set used to encode HTTP protocol elements */
    public static final String HTTP_ELEMENT_CHARSET = "US-ASCII";

    /** Default content encoding chatset */
    public static final String DEFAULT_CONTENT_CHARSET = "ISO-8859-1";


    /**
     * Converts the specified string to a byte array of HTTP element characters.
     * This method is to be used when encoding content of HTTP elements (such as
     * request headers)
     *
     * @param data the string to be encoded
     * @return The resulting byte array.
     */
    public static byte[] getBytes(final String data) {
        if (data == null) {
            throw new IllegalArgumentException("Parameter may not be null");
        }

        try {
            return data.getBytes(HTTP_ELEMENT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return data.getBytes();
        }
    }

    /**
     * Converts the byte array of HTTP element characters to a string This
     * method is to be used when decoding content of HTTP elements (such as
     * response headers)
     *
     * @param data the byte array to be encoded
     * @param offset the index of the first byte to encode
     * @param length the number of bytes to encode
     * @return The resulting string.
     */
    public static String getString(final byte[] data, int offset, int length) {
        StringBuilder sb= new StringBuilder();
        for(int i=0; i<data.length;i++)
        sb.append(data[i]);
        return sb.toString();
    }

    /**
     * Converts the byte array of HTTP element characters to a string This
     * method is to be used when decoding content of HTTP elements (such as
     * response headers)
     *
     * @param data the byte array to be encoded
     * @return The resulting string.
     */
    public static String getString(final byte[] data) {
        return getString(data, 0, data.length);
    }

    /**
     * Converts the specified string to a byte array of HTTP content characters
     * This method is to be used when encoding content of HTTP request/response
     * If the specified charset is not supported, default HTTP content encoding
     * (ISO-8859-1) is applied
     *
     * @param data the string to be encoded
     * @param charset the desired character encoding
     * @return The resulting byte array.
     */
    public static byte[] getContentBytes(final String data, String charset) {

        if (data == null) {
            throw new IllegalArgumentException("Parameter may not be null");
        }

        if ((charset == null) || (charset.equals(""))) {
            charset = DEFAULT_CONTENT_CHARSET;
        }

        try {
            return data.getBytes(charset);
        } catch (UnsupportedEncodingException e) {

            try {
                return data.getBytes(DEFAULT_CONTENT_CHARSET);
            } catch (UnsupportedEncodingException e2) {
                e2.printStackTrace();
                return data.getBytes();
            }
        }
    }

    /**
     * Converts the byte array of HTTP content characters to a string This
     * method is to be used when decoding content of HTTP request/response If
     * the specified charset is not supported, default HTTP content encoding
     * (ISO-8859-1) is applied
     *
     * @param data the byte array to be encoded
     * @param offset the index of the first byte to encode
     * @param length the number of bytes to encode
     * @param charset the desired character encoding
     * @return The result of the conversion.
     */
    public static String getContentString(
            final byte[] data,
            int offset,
            int length,
            String charset
    ) {

        if (data == null) {
            throw new IllegalArgumentException("Parameter may not be null");
        }

        if ((charset == null) || (charset.equals(""))) {
            charset = DEFAULT_CONTENT_CHARSET;
        }

        try {
            return new String(data, offset, length, charset);
        } catch (UnsupportedEncodingException e) {


            try {
                return new String(data, offset, length, DEFAULT_CONTENT_CHARSET);
            } catch (UnsupportedEncodingException e2) {
                e2.printStackTrace();
                return new String(data, offset, length);
            }
        }
    }


    /**
     * Converts the byte array of HTTP content characters to a string This
     * method is to be used when decoding content of HTTP request/response If
     * the specified charset is not supported, default HTTP content encoding
     * (ISO-8859-1) is applied
     *
     * @param data the byte array to be encoded
     * @param charset the desired character encoding
     * @return The result of the conversion.
     */
    public static String getContentString(final byte[] data, String charset) {
        return getContentString(data, 0, data.length, charset);
    }

    /**
     * Converts the specified string to a byte array of HTTP content characters
     * using default HTTP content encoding (ISO-8859-1) This method is to be
     * used when encoding content of HTTP request/response
     *
     * @param data the string to be encoded
     * @return The byte array as above.
     */
    public static byte[] getContentBytes(final String data) {
        return getContentBytes(data, null);
    }

    /**
     * Converts the byte array of HTTP content characters to a string using
     * default HTTP content encoding (ISO-8859-1) This method is to be used when
     * decoding content of HTTP request/response
     *
     * @param data the byte array to be encoded
     * @param offset the index of the first byte to encode
     * @param length the number of bytes to encode
     * @return The string representation of the byte array.
     */
    public static String getContentString(final byte[] data, int offset, int length) {
        return getContentString(data, offset, length, null);
    }

    /**
     * Converts the byte array of HTTP content characters to a string using
     * default HTTP content encoding (ISO-8859-1) This method is to be used when
     * decoding content of HTTP request/response
     *
     * @param data the byte array to be encoded
     * @return The string representation of the byte array.
     */
    public static String getContentString(final byte[] data) {
        return getContentString(data, null);
    }

    /**
     * Converts the specified string to byte array of ASCII characters.
     *
     * @param data the string to be encoded
     * @return The string as a byte array.
     */
    public static byte[] getAsciiBytes(final String data) {

        if (data == null) {
            throw new IllegalArgumentException("Parameter may not be null");
        }

        try {
            return data.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("HttpClient requires ASCII support");
        }
    }

    /**
     * Converts the byte array of ASCII characters to a string. This method is
     * to be used when decoding content of HTTP elements (such as response
     * headers)
     *
     * @param data the byte array to be encoded
     * @param offset the index of the first byte to encode
     * @param length the number of bytes to encode
     * @return The string representation of the byte array
     */
    public static String getAsciiString(final byte[] data, int offset, int length) {

        if (data == null) {
            throw new IllegalArgumentException("Parameter may not be null");
        }

        try {
            return new String(data, offset, length, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("HttpClient requires ASCII support");
        }
    }

    /**
     * Converts the byte array of ASCII characters to a string. This method is
     * to be used when decoding content of HTTP elements (such as response
     * headers)
     *
     * @param data the byte array to be encoded
     * @return The string representation of the byte array
     */
    public static String getAsciiString(final byte[] data) {
        return getAsciiString(data, 0, data.length);
    }
}