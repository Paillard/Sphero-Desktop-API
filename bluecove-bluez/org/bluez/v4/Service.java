/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2009 Vlad Skarzhevskyy
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
 *  =======================================================================================
 *
 *  BlueZ docs licensed under GNU Free Documentation License, Version 1.1 http://www.fsf.org
 *  Copyright (C) 2004-2008  Marcel Holtmann <marcel@holtmann.org>
 *  Copyright (C) 2005-2006  Johan Hedberg <johan.hedberg@nokia.com>
 *  Copyright (C) 2005-2006  Claudio Takahasi <claudio.takahasi@indt.org.br>
 *  Copyright (C) 2006-2007  Luiz von Dentz <luiz.dentz@indt.org.br> 
 *  
 *  @author vlads
 *  @version $Id: Service.java 3001 2009-07-27 14:29:31Z skarzhevskyy $
 */
package org.bluez.v4;

import org.bluez.Error.DoesNotExist;
import org.bluez.Error.Failed;
import org.bluez.Error.InvalidArguments;
import org.bluez.Error.NotAuthorized;
import org.bluez.Error.NotAvailable;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.UInt32;

/**
 * BlueZ V4 D-Bus Service API
 * 
 * Service hierarchy
 * <p>
 * Service org.bluez
 * <p>
 * Interface org.bluez.Service
 * <p>
 * Object path [variable prefix]/{hci0,hci1,...}
 * <p>
 * 
 * Created base on D-Bus API description for BlueZ. bluez-4.32/doc/service-api.txt
 */
@DBusInterfaceName("org.bluez.Service")
public interface Service extends org.bluez.v3.Service {

    /**
     * Adds a new service record from the XML description and returns the
     * assigned record handle.
     */
    UInt32 AddRecord(String record) throws InvalidArguments, Failed;

    /**
     * Updates a given service record provided in the XML format.
     */
    void UpdateRecord(UInt32 handle, String record) throws InvalidArguments, NotAvailable, Failed;

    /**
     * Remove a service record identified by its handle.
     * 
     * It is only possible to remove service records that where added by the
     * current connection.
     */
    void RemoveRecord(UInt32 handle) throws InvalidArguments, NotAuthorized, DoesNotExist,
            Failed;

    /**
     * Request an authorization for an incoming connection for a specific
     * service record. The service record needs to be registered via AddRecord
     * first.
     */
    void RequestAuthorization(String address, UInt32 handle) throws InvalidArguments, NotAuthorized,
            DoesNotExist, Failed;

    void CancelAuthorization() throws InvalidArguments, NotAuthorized, DoesNotExist, Failed;

}
