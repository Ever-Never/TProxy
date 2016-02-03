package sii.uniroma2.HonorineCevallos.TProxy.exceptions;
/*
 * $Header: /home/cvs/jakarta-commons/httpclient/src/java/org/apache/commons/httpclient/HttpException.java,v 1.13.2.1 2004/02/22 18:21:13 olegk Exp $
 * $Revision: 1.13.2.1 $
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


/**
 * Signals that an HTTP or HttpClient exception has occurred.
 *

 * Why is it from URIException?
 * To simplify the programming style for the inherited exception instances.
 *
 *


 * The usage of the reserved status and reason codes
 *


 *
 -x: Internal use
 *
 0: Unknown reason
 *
 x: Basic ill-prepared reason
 *
 1xx: Informational status
 *
 2xx: Success status
 *
 3xx: Redirection status
 *
 4xx: Client error
 *
 5xx: Server error
 *

 *
 * @author Unascribed
 * @version $Revision: 1.13.2.1 $ $Date: 2004/02/22 18:21:13 $
 */
public class HttpException extends URIException {

    /**
     * Creates a new HttpException.
     */
    public HttpException() {
        super();
    }

    /**
     * Creates a new HttpException with the specified message.
     *
     * @param message exception message
     */
    public HttpException(String message) {
        super(message);
    }
}