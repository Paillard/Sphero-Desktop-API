/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2009 Vlad Skarzhevskyy
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *  @author vlads
 *  @version $Id: OBEXOperationCodes.java 2915 2009-03-13 17:07:26Z skarzhevskyy $
 */
package com.intel.bluetooth.obex;

import javax.obex.ResponseCodes;

/**
 * See <a
 * href="http://bluetooth.com/Bluetooth/Learn/Technology/Specifications/">Bluetooth
 * Specification Documents</A> for details.
 *
 */
interface OBEXOperationCodes {

	byte OBEX_VERSION = 0x10; /* OBEX Protocol Version 1.1 */

	short OBEX_DEFAULT_MTU = 0x400;

	short OBEX_MINIMUM_MTU = 0xFF;

	short OBEX_MTU_HEADER_RESERVE = 3 + 5 + 3;

	int OBEX_MAX_PACKET_LEN = 0xFFFF;

	char FINAL_BIT = 0x80;

	char CONNECT = FINAL_BIT;

	char DISCONNECT = 0x01 | FINAL_BIT;

	char PUT = 0x02;

	char PUT_FINAL = PUT | FINAL_BIT;

	char GET = 0x03;

	char GET_FINAL = GET | FINAL_BIT;

	char SETPATH = 0x05;

	char SETPATH_FINAL = SETPATH | FINAL_BIT;
	
	char SESSION = 0x07;

	char ABORT = 0xFF;

	int OBEX_RESPONSE_CONTINUE = 0x90;

	int OBEX_RESPONSE_SUCCESS = ResponseCodes.OBEX_HTTP_OK;

}
