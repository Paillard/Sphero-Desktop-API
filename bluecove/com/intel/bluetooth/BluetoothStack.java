/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2009 Vlad Skarzhevskyy
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
 *  @version $Id: BluetoothStack.java 3045 2010-07-06 16:16:28Z skarzhevskyy $
 */
package com.intel.bluetooth;

import javax.bluetooth.*;
import java.io.IOException;

/**
 * New native stack support should ONLY implement this interface. No other classes should
 * ideally be changed except BlueCoveImpl where the instance of new class should be
 * created.
 *
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 */
public interface BluetoothStack {

    int FEATURE_L2CAP = 1;

    int FEATURE_SERVICE_ATTRIBUTES = 1 << 1;

    int FEATURE_SET_DEVICE_SERVICE_CLASSES = 1 << 2;

    int FEATURE_RSSI = 1 << 3;
    
    int FEATURE_ASSIGN_SERVER_PSM = 1 << 4;

    class LibraryInformation {

        public final String libraryName;

        /**
         * Class ClassLoader of which to use for loading library as resource. May be null.
         */
        public Class stackClass;

        public final boolean required;

        public LibraryInformation(String libraryName) {
            this(libraryName, true);
        }

        public LibraryInformation(String libraryName, boolean required) {
            this.libraryName = libraryName;
            this.required = required;
        }

        public static BluetoothStack.LibraryInformation[] library(String libraryName) {
            return new BluetoothStack.LibraryInformation[] { new BluetoothStack.LibraryInformation(libraryName) };
        }

        @Override
        public String toString() {
            return "LibraryInformation{" +
                    "libraryName='" + libraryName + '\'' +
                    ", stackClass=" + stackClass +
                    ", required=" + required +
                    '}';
        }
    }

    // ---------------------- Library initialization

    /**
     * Used by library initialization to detect if shared library already loaded. The
     * caller with catch UnsatisfiedLinkError and will load libraries returned by
     * requireNativeLibraries().
     */
    boolean isNativeCodeLoaded();

    /**
     * List the native libraries that need to be loaded.
     *
     * @see java.lang.System#loadLibrary(java.lang.String)
     * @return array of library names used by implementation.
     */
    BluetoothStack.LibraryInformation[] requireNativeLibraries();

    /**
     * Used to verify native library version. versionMajor1 * 1000000 + versionMajor2 *
     * 10000 + versionMinor * 100 + versionBuild
     *
     * @return Version number in decimal presentation. e.g. 2030407 for version 2.3.4
     *         build 7
     */
    int getLibraryVersion() throws BluetoothStateException;

    /**
     * Used if OS Supports multiple Bluetooth stacks 0x01 winsock; 0x02 widcomm; 0x04
     * bluesoleil; 0x08 BlueZ; 0x10 OS X stack;
     *
     * @return stackID
     */
    int detectBluetoothStack();

    /**
     *
     * @param nativeDebugCallback
     *            DebugLog.class
     * @param on
     */
    void enableNativeDebug(Class nativeDebugCallback, boolean on);

    /**
     * Call is made when we want to use this stack.
     */
    void initialize() throws BluetoothStateException;

    void destroy();

    String getStackID();

    /**
     * Called from long running native code to see if thread interrupted. If yes
     * InterruptedIOException would be thrown.
     *
     * @return true if interrupted
     */
    boolean isCurrentThreadInterruptedCallback();

    /**
     * @return implemented features, see FEATURE_* constants
     */
    int getFeatureSet();

    // ---------------------- LocalDevice

    /**
     * Retrieves the Bluetooth address of the local device.
     *
     * @see javax.bluetooth.LocalDevice#getBluetoothAddress()
     */
    String getLocalDeviceBluetoothAddress() throws BluetoothStateException;

    /**
     * Retrieves the name of the local device.
     *
     * @see javax.bluetooth.LocalDevice#getFriendlyName()
     */
    String getLocalDeviceName();

    /**
     * Retrieves the class of the local device.
     *
     * @see javax.bluetooth.LocalDevice#getDeviceClass()
     */
    DeviceClass getLocalDeviceClass();

    /**
     * Implementation for local device service class
     *
     * @see javax.bluetooth.ServiceRecord#setDeviceServiceClasses(int) and
     * @see javax.bluetooth.LocalDevice#updateRecord(javax.bluetooth.ServiceRecord)
     * @param classOfDevice
     */
    void setLocalDeviceServiceClasses(int classOfDevice);

    /**
     * @see javax.bluetooth.LocalDevice#setDiscoverable(int)
     */
    boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException;

    /**
     * @see javax.bluetooth.LocalDevice#getDiscoverable()
     */
    int getLocalDeviceDiscoverable();

    /**
     * @see javax.bluetooth.LocalDevice#isPowerOn()
     */
    boolean isLocalDevicePowerOn();

    /**
     * @see javax.bluetooth.LocalDevice#getProperty(String)
     */
    String getLocalDeviceProperty(String property);

    // ---------------------- Remote Device authentication

    /**
     * Attempts to authenticate RemoteDevice. Return <code>false</code> if the stack does
     * not support authentication.
     *
     * @see javax.bluetooth.RemoteDevice#authenticate()
     */
    boolean authenticateRemoteDevice(long address) throws IOException;

    /**
     * Sends an authentication request to a remote Bluetooth device. Non JSR-82,
     *
     * @param address
     *            Remote Device address
     * @param passkey
     *            A Personal Identification Number (PIN) to be used for device
     *            authentication.
     * @return <code>true</code> if authentication is successful; otherwise
     *         <code>false</code>
     * @throws IOException
     *             if there are error during authentication.
     */
    boolean authenticateRemoteDevice(long address, String passkey) throws IOException;

    /**
     * Removes authentication between local and remote bluetooth devices. Non JSR-82,
     *
     * @param address
     *            Remote Device address authentication.
     * @throws IOException
     *             if there are error during authentication.
     */

    void removeAuthenticationWithRemoteDevice(long address) throws IOException;

    // ---------------------- Device Inquiry

    /**
     * called by JSR-82 code Device Inquiry
     *
     * @see javax.bluetooth.DiscoveryAgent#startInquiry(int,
     *      javax.bluetooth.DiscoveryListener)
     */
    boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException;

    /**
     * called by JSR-82 code Device Inquiry
     *
     * @see javax.bluetooth.DiscoveryAgent#cancelInquiry(javax.bluetooth.DiscoveryListener)
     */
    boolean cancelInquiry(DiscoveryListener listener);

    /**
     * called by implementation when device name is unknown or <code>alwaysAsk</code> is
     * <code>true</code>
     *
     * Returns <code>null</code> if the Bluetooth system does not support this feature; If
     * the remote device does not have a name then an empty string.
     *
     * @see javax.bluetooth.RemoteDevice#getFriendlyName(boolean)
     */
    String getRemoteDeviceFriendlyName(long address) throws IOException;

    /**
     * @see javax.bluetooth.DiscoveryAgent#retrieveDevices(int)
     * @return null if not implemented
     */
    RemoteDevice[] retrieveDevices(int option);

    /**
     * @see javax.bluetooth.RemoteDevice#isTrustedDevice()
     * @return null if not implemented
     */
    Boolean isRemoteDeviceTrusted(long address);

    /**
     * @see javax.bluetooth.RemoteDevice#isAuthenticated()
     * @return null if not implemented
     */
    Boolean isRemoteDeviceAuthenticated(long address);

    // ---------------------- Service search

    /**
     * called by JSR-82 code Service search
     *
     * @see javax.bluetooth.DiscoveryAgent#searchServices(int[],UUID[],javax.bluetooth.RemoteDevice,
     *      javax.bluetooth.DiscoveryListener)
     */
    int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException;

    /**
     * called by JSR-82 code Service search
     *
     * @see javax.bluetooth.DiscoveryAgent#cancelServiceSearch(int)
     */
    boolean cancelServiceSearch(int transID);

    /**
     * Called by ServiceRecord.populateRecord(int[] attrIDs) during Service search.
     *
     * @see javax.bluetooth.ServiceRecord#populateRecord(int[])
     */
    boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int... attrIDs) throws IOException;

    // ---------------------- Client and Server RFCOMM connections

    /**
     * Used to create handle for
     * {@link com.intel.bluetooth.BluetoothRFCommClientConnection}
     *
     * @see javax.microedition.io.Connector#open(String, int, boolean)
     */
    long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException;

    /**
     * @param handle
     * @param expected
     *            Value specified when connection was open
     *            ServiceRecord.xxAUTHENTICATE_xxENCRYPT
     * @return expected if not implemented by stack
     * @throws IOException
     *
     * @see javax.bluetooth.RemoteDevice#isAuthenticated()
     * @see javax.bluetooth.RemoteDevice#isEncrypted()
     */
    int rfGetSecurityOpt(long handle, int expected) throws IOException;

    /**
     * @see com.intel.bluetooth.BluetoothRFCommClientConnection
     * @see com.intel.bluetooth.BluetoothRFCommConnection#close()
     * @see com.intel.bluetooth.BluetoothRFCommConnection#closeConnectionHandle(long)
     */
    void connectionRfCloseClientConnection(long handle) throws IOException;

    /**
     * @see com.intel.bluetooth.BluetoothRFCommServerConnection
     * @see #connectionRfCloseClientConnection(long)
     * @see javax.microedition.io.Connection#close()
     */
    void connectionRfCloseServerConnection(long handle) throws IOException;

    /**
     * Used to create handle for
     * {@link com.intel.bluetooth.BluetoothRFCommConnectionNotifier}
     *
     * @see javax.microedition.io.Connector#open(String, int, boolean)
     */
    long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord) throws IOException;

    /**
     * @see javax.bluetooth.LocalDevice#updateRecord(javax.bluetooth.ServiceRecord)
     */
    void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException;

    /**
     * Used to create handle for
     * {@link com.intel.bluetooth.BluetoothRFCommServerConnection}
     *
     * @see com.intel.bluetooth.BluetoothRFCommConnectionNotifier#acceptAndOpen()
     * @see javax.microedition.io.StreamConnectionNotifier#acceptAndOpen()
     */
    long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException;

    /**
     * @see com.intel.bluetooth.BluetoothConnectionNotifierBase#close()
     * @see javax.microedition.io.Connection#close()
     */
    void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException;

    /**
     * @see javax.bluetooth.RemoteDevice#getRemoteDevice(javax.microedition.io.Connection)
     */
    long getConnectionRfRemoteAddress(long handle) throws IOException;

    /**
     * @see javax.bluetooth.RemoteDevice#encrypt(javax.microedition.io.Connection,
     *      boolean)
     */
    boolean rfEncrypt(long address, long handle, boolean on);

    /**
     * @see java.io.InputStream#read()
     * @see com.intel.bluetooth.BluetoothRFCommInputStream#read()
     */
    int connectionRfRead(long handle) throws IOException;

    /**
     * @see java.io.InputStream#read(byte[],int,int)
     * @see com.intel.bluetooth.BluetoothRFCommInputStream#read(byte[],int,int)
     */
    int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;

    /**
     * @see java.io.InputStream#available()
     * @see com.intel.bluetooth.BluetoothRFCommInputStream#available()
     */
    int connectionRfReadAvailable(long handle) throws IOException;

    /**
     * @see com.intel.bluetooth.BluetoothRFCommOutputStream#write(int)
     */
    void connectionRfWrite(long handle, int b) throws IOException;

    /**
     * @see com.intel.bluetooth.BluetoothRFCommOutputStream#write(byte[], int, int)
     */
    void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException;

    /**
     * @see com.intel.bluetooth.BluetoothRFCommOutputStream#flush()
     */
    void connectionRfFlush(long handle) throws IOException;

    // ---------------------- Client and Server L2CAP connections

    /**
     * Used to create handle for
     * {@link com.intel.bluetooth.BluetoothL2CAPClientConnection}
     */
    long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU) throws IOException;

    /**
     * Closing {@link com.intel.bluetooth.BluetoothL2CAPClientConnection}
     *
     * @see javax.microedition.io.Connection#close()
     */
    void l2CloseClientConnection(long handle) throws IOException;

    /**
     * Used to create handle for
     * {@link com.intel.bluetooth.BluetoothL2CAPConnectionNotifier}
     *
     * @see javax.microedition.io.Connector#open(String, int, boolean)
     */
    long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU, ServiceRecordImpl serviceRecord) throws IOException;

    /**
     * @see javax.bluetooth.LocalDevice#updateRecord(javax.bluetooth.ServiceRecord)
     */
    void l2ServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException;

    /**
     * Used to create handle for
     * {@link com.intel.bluetooth.BluetoothL2CAPServerConnection}
     *
     * @see com.intel.bluetooth.BluetoothL2CAPConnectionNotifier#acceptAndOpen()
     * @see javax.bluetooth.L2CAPConnectionNotifier#acceptAndOpen()
     */
    long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException;

    /**
     * Closing {@link com.intel.bluetooth.BluetoothL2CAPServerConnection}
     *
     * @see #l2CloseClientConnection(long)
     */
    void l2CloseServerConnection(long handle) throws IOException;

    /**
     * @see com.intel.bluetooth.BluetoothConnectionNotifierBase#close()
     * @see javax.microedition.io.Connection#close()
     */
    void l2ServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException;

    /**
     * @see #rfGetSecurityOpt(long, int)
     */
    int l2GetSecurityOpt(long handle, int expected) throws IOException;

    /**
     * @see javax.bluetooth.L2CAPConnection#getTransmitMTU()
     */
    int l2GetTransmitMTU(long handle) throws IOException;

    /**
     * @see javax.bluetooth.L2CAPConnection#getReceiveMTU()
     */
    int l2GetReceiveMTU(long handle) throws IOException;

    /**
     * @see javax.bluetooth.L2CAPConnection#ready()
     */
    boolean l2Ready(long handle) throws IOException;

    /**
     * @see javax.bluetooth.L2CAPConnection#receive(byte[])
     */
    int l2Receive(long handle, byte... inBuf) throws IOException;

    /**
     * @see javax.bluetooth.L2CAPConnection#send(byte[])
     */
    void l2Send(long handle, byte[] data, int transmitMTU) throws IOException;

    /**
     * @see javax.bluetooth.RemoteDevice#getRemoteDevice(javax.microedition.io.Connection)
     */
    long l2RemoteAddress(long handle) throws IOException;

    /**
     * @see javax.bluetooth.RemoteDevice#encrypt(javax.microedition.io.Connection,
     *      boolean)
     */
    boolean l2Encrypt(long address, long handle, boolean on) throws IOException;
}
