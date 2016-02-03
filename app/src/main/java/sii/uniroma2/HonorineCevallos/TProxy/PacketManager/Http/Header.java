package sii.uniroma2.HonorineCevallos.TProxy.PacketManager.Http;
/*
 * $Header: /home/cvs/jakarta-commons/httpclient/src/java/org/apache/commons/httpclient/Header.java,v 1.10.2.3 2004/02/22 18:21:13 olegk Exp $
 * $Revision: 1.10.2.3 $
 * $Date: 2004/02/22 18:21:13 $
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


import sii.uniroma2.HonorineCevallos.TProxy.exceptions.HttpException;

/**
 *
 An HTTP header.


 *
 * @author Remy Maucherat
 * @author Mike Bowler
 * @version $Revision: 1.10.2.3 $ $Date: 2004/02/22 18:21:13 $
 */
public class Header extends NameValuePair {

    // ----------------------------------------------------------- Constructors

    /**
     * Default constructor.
     */
    public Header() {
        this(null, null);
    }

    /**
     * Constructor with name and value
     *
     * @param name the header name
     * @param value the header value
     */
    public Header(String name, String value) {
        super(name, value);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Returns a {@link String} representation of the header in the form:
     *
     * Name: valueCRLF
     *

     * The string is terminated by CRLF.
     *
     * @return stringHEAD
     */
    public String toExternalForm() {
        return ((null == getName() ? "" : getName())
                + ": "
                + (null == getValue() ? "" : getValue())
                + "\r\n");
    }

    /**
     * Returns a {@link String} representation of the header.
     *
     * @return stringHEAD
     */
    public String toString() {
        return toExternalForm();
    }

    /**
     * Returns an array of {@link HeaderElement}s
     * constructed from my value.
     *
     * @see HeaderElement#parse
     * @throws HttpException if the header cannot be parsed
     * @return an array of header elements
     */

    public HeaderElement[] getValues() throws HttpException {
        return HeaderElement.parse(getValue());
    }

}