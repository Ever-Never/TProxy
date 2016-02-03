package sii.uniroma2.HonorineCevallos.TProxy.exceptions;

/*
 * $Header: /home/cvs/jakarta-commons/httpclient/src/java/org/apache/commons/httpclient/URIException.java,v 1.8.2.1 2004/02/22 18:21:13 olegk Exp $
 * $Revision: 1.8.2.1 $
 * $Date: 2004/02/22 18:21:13 $
 *
 * ====================================================================
 *
 *  Copyright 2002-2004 The Apache Software Foundation
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



        import java.io.IOException;

/**
 * The URI parsing and escape encoding exception.
 *

 * Why is it from IOException?
 * To simplify the programming style for the inherited exception instances.
 *


 *
 * @author Sung-Gu
 * @version $Revision: 1.8.2.1 $ $Date: 2002/03/14 15:14:01
 */
public class URIException extends IOException {

    // ----------------------------------------------------------- constructors

    /**
     * Default constructor.
     */
    public URIException() {
    }


    /**
     * The constructor with a reason code argument.
     *
     * @param reasonCode the reason code
     */
    public URIException(int reasonCode) {
        setReasonCode(reasonCode);
    }


    /**
     * The constructor with a reason string and its code arguments.
     *
     * @param reasonCode the reason code
     * @param reason the reason
     */
    public URIException(int reasonCode, String reason) {
        super(reason); // for backward compatibility of Throwable
        this.reason = reason;
        setReasonCode(reasonCode);
    }


    /**
     * The constructor with a reason string argument.
     *
     * @param reason the reason
     */
    public URIException(String reason) {
        super(reason); // for backward compatibility of Throwable
        this.reason = reason;
        setReasonCode(UNKNOWN);
    }

    // -------------------------------------------------------------- constants

    /**
     * No specified reason code.
     */
    public static final int UNKNOWN = 0;


    /**
     * The URI parsing error.
     */
    public static final int PARSING = 1;


    /**
     * The unsupported character encoding.
     */
    public static final int UNSUPPORTED_ENCODING = 2;


    /**
     * The URI escape encoding and decoding error.
     */
    public static final int ESCAPING = 3;


    /**
     * The DNS punycode encoding or decoding error.
     */
    public static final int PUNYCODE = 4;

    // ------------------------------------------------------------- properties

    /**
     * The reason code.
     */
    protected int reasonCode;


    /**
     * The reason message.
     */
    protected String reason;

    // ---------------------------------------------------------------- methods

    /**
     * Get the reason code.
     *
     * @return the reason code
     */
    public int getReasonCode() {
        return reasonCode;
    }


    /**
     * Set the reason code.
     *
     * @param reasonCode the reason code
     */
    public void setReasonCode(int reasonCode) {
        this.reasonCode = reasonCode;
    }


    /**
     * Get the reason message.
     *
     * @return the reason message
     */
    public String getReason() {
        return reason;
    }


    /**
     * Set the reason message.
     *
     * @param reason the reason message
     */
    public void setReason(String reason) {
        this.reason = reason;
    }


}