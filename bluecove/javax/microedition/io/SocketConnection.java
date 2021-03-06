/**
 *  BlueCove - Java library for Bluetooth
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
 *  @version $Id: SocketConnection.java 2471 2008-12-01 03:44:20Z skarzhevskyy $
 */
package javax.microedition.io;

import java.io.IOException;

public interface SocketConnection extends StreamConnection {

	byte DELAY = 0;

	byte LINGER = 1;

	byte KEEPALIVE = 2;

	byte RCVBUF = 3;

	byte SNDBUF = 4;

	void setSocketOption(byte option, int value) throws IllegalArgumentException, IOException;

	int getSocketOption(byte option) throws IllegalArgumentException, IOException;

	String getLocalAddress() throws IOException;

	int getLocalPort() throws IOException;

	String getAddress() throws IOException;

	int getPort() throws IOException;
}
