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
 *  @author vlads
 *  @version $Id: BlueZAPI.java 2892 2009-03-11 20:20:07Z skarzhevskyy $
 */
package org.bluez;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.bluez.Error.InvalidArguments;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Abstraction interface to access BlueZ over D-Bus.
 * 
 * Only methods required for BlueCove JSR-82 implementation are declared.
 */
public interface BlueZAPI {

    /**
     * Receive device discovery events.
     */
    interface DeviceInquiryListener {

        void deviceInquiryStarted();

        void deviceDiscovered(String deviceAddr, String deviceName, int deviceClass, boolean paired);

    }

    List<String> listAdapters();

    Path getAdapter(int number);

    Path findAdapter(String pattern) throws InvalidArguments;

    Path defaultAdapter() throws InvalidArguments;

    void selectAdapter(Path adapterPath) throws DBusException;

    String getAdapterID();

    String getAdapterAddress();

    int getAdapterDeviceClass();

    String getAdapterName();

    boolean isAdapterDiscoverable();

    int getAdapterDiscoverableTimeout();

    String getAdapterVersion();

    String getAdapterRevision();

    String getAdapterManufacturer();

    boolean isAdapterPowerOn();

    boolean setAdapterDiscoverable(int mode);

    void deviceInquiry(BlueZAPI.DeviceInquiryListener listener) throws DBusException, InterruptedException;

    void deviceInquiryCancel();

    String getRemoteDeviceFriendlyName(String deviceAddress) throws DBusException, IOException;

    List<String> retrieveDevices(boolean preKnown);

    boolean isRemoteDeviceConnected(String deviceAddress) throws DBusException;

    Boolean isRemoteDeviceTrusted(String deviceAddress) throws DBusException;

    /**
     * If device could not be reached returns {@code null}
     */
    Map<Integer, String> getRemoteDeviceServices(String deviceAddress) throws DBusException;

    void authenticateRemoteDevice(String deviceAddress) throws DBusException;

    boolean authenticateRemoteDevice(String deviceAddress, String passkey) throws DBusException;

    void removeAuthenticationWithRemoteDevice(String deviceAddress);

    long registerSDPRecord(String sdpXML) throws DBusExecutionException, DBusException;

    void updateSDPRecord(long handle, String sdpXML) throws DBusExecutionException, DBusException;

    void unregisterSDPRecord(long handle) throws DBusExecutionException, DBusException;
}
