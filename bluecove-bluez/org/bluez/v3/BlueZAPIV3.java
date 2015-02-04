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
 *  @version $Id: BlueZAPIV3.java 2988 2009-04-22 22:15:55Z skarzhevskyy $
 */
package org.bluez.v3;

import com.intel.bluetooth.BlueCoveImpl;
import com.intel.bluetooth.BluetoothConsts;
import com.intel.bluetooth.BluetoothConsts.DeviceClassConsts;
import com.intel.bluetooth.DebugLog;
import org.bluez.BlueZAPI;
import org.bluez.Error;
import org.bluez.v3.Adapter.DiscoveryCompleted;
import org.bluez.v3.Adapter.DiscoveryStarted;
import org.bluez.v3.Adapter.RemoteClassUpdated;
import org.bluez.v3.Adapter.RemoteDeviceFound;
import org.bluez.v3.Adapter.RemoteNameUpdated;
import org.freedesktop.DBus;
import org.freedesktop.DBus.Error.NoReply;
import org.freedesktop.dbus.*;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

import javax.bluetooth.DiscoveryAgent;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * 
 * Access BlueZ v3 over D-Bus
 * 
 */
public class BlueZAPIV3 implements BlueZAPI {

    private DBusConnection dbusConn;

    private Manager dbusManager;

    private Adapter adapter;

    private Path adapterPath;

    private long lastDeviceDiscoveryTime;

    public BlueZAPIV3(DBusConnection dbusConn, Manager dbusManager) {
        this.dbusConn = dbusConn;
        this.dbusManager = dbusManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#findAdapter(java.lang.String)
     */
    public Path findAdapter(String pattern) throws Error.InvalidArguments {
        String path;
        try {
            path = dbusManager.FindAdapter(pattern);
        } catch (Error.NoSuchAdapter e) {
            return null;
        }
        if (path == null) {
            return null;
        } else {
            return new Path(path);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#defaultAdapter()
     */
    public Path defaultAdapter() throws Error.InvalidArguments {
        String path;
        try {
            path = dbusManager.DefaultAdapter();
        } catch (Error.NoSuchAdapter e) {
            return null;
        }
        if (path == null) {
            return null;
        } else {
            return new Path(path);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapter(int)
     */
    public Path getAdapter(int number) {
        String[] adapters = dbusManager.ListAdapters();
        if (adapters == null) {
            throw null;
        }
        if (number < 0 || number >= adapters.length) {
            throw null;
        }
        return new Path(String.valueOf(adapters[number]));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#listAdapters()
     */
    public List<String> listAdapters() {
        List<String> v = new Vector<>();
        String[] adapters = dbusManager.ListAdapters();
        if (adapters != null) {
            for (String adapter1 : adapters) {
                String adapterId = String.valueOf(adapter1);
                String bluezPath = "/org/bluez/";
                if (adapterId.startsWith(bluezPath)) {
                    adapterId = adapterId.substring(bluezPath.length());
                }
                v.add(adapterId);
            }
        }
        return v;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#selectAdapter(org.freedesktop.dbus.Path)
     */
    public void selectAdapter(Path adapterPath) throws DBusException {
        DebugLog.debug("selectAdapter", adapterPath.getPath());
        adapter = dbusConn.getRemoteObject("org.bluez", adapterPath.getPath(), Adapter.class);
        this.adapterPath = adapterPath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapterAddress()
     */
    public String getAdapterAddress() {
        return adapter.GetAddress();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapterID()
     */
    public String getAdapterID() {
        String bluezPath = "/org/bluez/";
        if (adapterPath.getPath().startsWith(bluezPath)) {
            return adapterPath.getPath().substring(bluezPath.length());
        } else {
            return adapterPath.getPath();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapterDeviceClass()
     */
    public int getAdapterDeviceClass() {
        int record = 0;
        String major = adapter.GetMajorClass();

        if ("computer".equals(major)) {
            record |= DeviceClassConsts.MAJOR_COMPUTER;
        } else {
            DebugLog.debug("Unknown MajorClass", major);
        }

        String minor = adapter.GetMinorClass();
        switch (minor) {
            case "uncategorized":
                record |= DeviceClassConsts.COMPUTER_MINOR_UNCLASSIFIED;
                break;
            case "desktop":
                record |= DeviceClassConsts.COMPUTER_MINOR_DESKTOP;
                break;
            case "server":
                record |= DeviceClassConsts.COMPUTER_MINOR_SERVER;
                break;
            case "laptop":
                record |= DeviceClassConsts.COMPUTER_MINOR_LAPTOP;
                break;
            case "handheld":
                record |= DeviceClassConsts.COMPUTER_MINOR_HANDHELD;
                break;
            case "palm":
                record |= DeviceClassConsts.COMPUTER_MINOR_PALM;
                break;
            case "wearable":
                record |= DeviceClassConsts.COMPUTER_MINOR_WEARABLE;
                break;
            default:
                DebugLog.debug("Unknown MinorClass", minor);
                record |= DeviceClassConsts.COMPUTER_MINOR_UNCLASSIFIED;
                break;
        }

        String[] srvc = adapter.GetServiceClasses();
        if (srvc != null) {
            for (String serviceClass : srvc) {
                switch (serviceClass) {
                    case "positioning":
                        record |= DeviceClassConsts.POSITIONING_SERVICE;
                        break;
                    case "networking":
                        record |= DeviceClassConsts.NETWORKING_SERVICE;
                        break;
                    case "rendering":
                        record |= DeviceClassConsts.RENDERING_SERVICE;
                        break;
                    case "capturing":
                        record |= DeviceClassConsts.CAPTURING_SERVICE;
                        break;
                    case "object transfer":
                        record |= DeviceClassConsts.OBJECT_TRANSFER_SERVICE;
                        break;
                    case "audio":
                        record |= DeviceClassConsts.AUDIO_SERVICE;
                        break;
                    case "telephony":
                        record |= DeviceClassConsts.TELEPHONY_SERVICE;
                        break;
                    case "information":
                        record |= DeviceClassConsts.INFORMATION_SERVICE;
                        break;
                    default:
                        DebugLog.debug("Unknown ServiceClasses", serviceClass);
                        break;
                }
            }
        }

        return record;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapterName()
     */
    public String getAdapterName() {
        try {
            return adapter.GetName();
        } catch (Error.NotReady e) {
            return null;
        } catch (Error.Failed e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#isAdapterDiscoverable()
     */
    public boolean isAdapterDiscoverable() {
        return adapter.IsDiscoverable();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapterDiscoverableTimeout()
     */
    public int getAdapterDiscoverableTimeout() {
        return adapter.GetDiscoverableTimeout().intValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#setAdapterDiscoverable(int)
     */
    public boolean setAdapterDiscoverable(int mode) throws DBusException {
        String modeStr;
        switch (mode) {
        case DiscoveryAgent.NOT_DISCOVERABLE:
            modeStr = "connectable";
            break;
        case DiscoveryAgent.GIAC:
            modeStr = "discoverable";
            break;
        case DiscoveryAgent.LIAC:
            modeStr = "limited";
            break;
        default:
            if (0x9E8B00 <= mode && mode <= 0x9E8B3F) {
                // system does not support the access mode specified
                return false;
            }
            throw new IllegalArgumentException("Invalid discoverable mode");
        }
        adapter.SetMode(modeStr);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapterManufacturer()
     */
    public String getAdapterManufacturer() {
        return adapter.GetManufacturer();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapterRevision()
     */
    public String getAdapterRevision() {
        return adapter.GetRevision();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapterVersion()
     */
    public String getAdapterVersion() {
        return adapter.GetVersion();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#isAdapterPowerOn()
     */
    public boolean isAdapterPowerOn() {
        return !"off".equals(adapter.GetMode());
    }

    private <T extends DBusSignal> void quietRemoveSigHandler(Class<T> type, DBusSigHandler<T> handler) {
        try {
            dbusConn.removeSigHandler(type, handler);
        } catch (DBusException ignore) {
        }
    }

    private boolean hasBonding(String deviceAddress) {
        try {
            return adapter.HasBonding(deviceAddress);
        } catch (Throwable e) {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#deviceInquiry(org.bluez.BlueZAPI.DeviceInquiryListener )
     */
    public void deviceInquiry(BlueZAPI.DeviceInquiryListener listener) throws DBusException, InterruptedException {

        Object discoveryCompletedEvent = new Object();

        DBusSigHandler<DiscoveryCompleted> discoveryCompleted = s -> {
            DebugLog.debug("discoveryCompleted.handle()");
            synchronized (discoveryCompletedEvent) {
                discoveryCompletedEvent.notifyAll();
            }
        };

        DBusSigHandler<DiscoveryStarted> discoveryStarted = s -> {
            DebugLog.debug("device discovery procedure has been started.");
            //TODO
        };

        DBusSigHandler<RemoteDeviceFound> remoteDeviceFound = s -> listener.deviceDiscovered(s.getDeviceAddress(), null, s.getDeviceClass().intValue(), hasBonding(s.getDeviceAddress()));

        DBusSigHandler<RemoteNameUpdated> remoteNameUpdated = s -> listener.deviceDiscovered(s.getDeviceAddress(), s.getDeviceName(), -1, false);

        DBusSigHandler<RemoteClassUpdated> remoteClassUpdated = s -> listener.deviceDiscovered(s.getDeviceAddress(), null, s.getDeviceClass().intValue(), hasBonding(s.getDeviceAddress()));

        try {
            dbusConn.addSigHandler(DiscoveryCompleted.class, discoveryCompleted);
            dbusConn.addSigHandler(DiscoveryStarted.class, discoveryStarted);
            dbusConn.addSigHandler(RemoteDeviceFound.class, remoteDeviceFound);
            dbusConn.addSigHandler(RemoteNameUpdated.class, remoteNameUpdated);
            dbusConn.addSigHandler(RemoteClassUpdated.class, remoteClassUpdated);

            // Inquiries are throttled if they are called too quickly in succession.
            // e.g. JSR-82 TCK
            long sinceDiscoveryLast = System.currentTimeMillis() - lastDeviceDiscoveryTime;
            long acceptableInterval = 5 * 1000;
            if (sinceDiscoveryLast < acceptableInterval) {
                Thread.sleep(acceptableInterval - sinceDiscoveryLast);
            }

            synchronized (discoveryCompletedEvent) {
                adapter.DiscoverDevices();
                listener.deviceInquiryStarted();
                DebugLog.debug("wait for device inquiry to complete...");
                discoveryCompletedEvent.wait();
                //adapter.CancelDiscovery();
            }

        } finally {
            quietRemoveSigHandler(RemoteClassUpdated.class, remoteClassUpdated);
            quietRemoveSigHandler(RemoteNameUpdated.class, remoteNameUpdated);
            quietRemoveSigHandler(RemoteDeviceFound.class, remoteDeviceFound);
            quietRemoveSigHandler(DiscoveryStarted.class, discoveryStarted);
            quietRemoveSigHandler(DiscoveryCompleted.class, discoveryCompleted);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#deviceInquiryCancel()
     */
    public void deviceInquiryCancel() throws DBusException {
        adapter.CancelDiscovery();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getRemoteDeviceFriendlyName(java.lang.String)
     */
    public String getRemoteDeviceFriendlyName(String deviceAddress) throws DBusException, IOException {
        Object discoveryCompletedEvent = new Object();
        Vector<String> namesFound = new Vector<>();

        DBusSigHandler<DiscoveryCompleted> discoveryCompleted = s -> {
            DebugLog.debug("discoveryCompleted.handle()");
            synchronized (discoveryCompletedEvent) {
                discoveryCompletedEvent.notifyAll();
            }
        };

        DBusSigHandler<RemoteNameUpdated> remoteNameUpdated = s -> {
            if (deviceAddress.equals(s.getDeviceAddress())) {
                if (s.getDeviceName() != null) {
                    namesFound.add(s.getDeviceName());
                    synchronized (discoveryCompletedEvent) {
                        discoveryCompletedEvent.notifyAll();
                    }
                } else {
                    DebugLog.debug("device name is null");
                }
            } else {
                DebugLog.debug("ignore device name " + s.getDeviceAddress() + " " + s.getDeviceName());
            }
        };

        try {
            dbusConn.addSigHandler(DiscoveryCompleted.class, discoveryCompleted);
            dbusConn.addSigHandler(RemoteNameUpdated.class, remoteNameUpdated);

            synchronized (discoveryCompletedEvent) {
                adapter.DiscoverDevices();
                DebugLog.debug("wait for device inquiry to complete...");
                try {
                    discoveryCompletedEvent.wait();
                    DebugLog.debug(namesFound.size() + " device name(s) found");
                    if (namesFound.isEmpty()) {
                        throw new IOException("Can't retrive device name");
                    }
                    // return the last name found
                    return namesFound.get(namesFound.size() - 1);
                } catch (InterruptedException e) {
                    throw new InterruptedIOException();
                }
            }
        } finally {
            quietRemoveSigHandler(RemoteNameUpdated.class, remoteNameUpdated);
            quietRemoveSigHandler(DiscoveryCompleted.class, discoveryCompleted);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#retrieveDevices(boolean)
     */
    public List<String> retrieveDevices(boolean preKnown) {
        if (!preKnown) {
            return null;
        }
        List<String> addresses = new Vector<>();
        String[] bonded = adapter.ListBondings();
        if (bonded != null) {
            for (String aBonded : bonded) {
                addresses.add(aBonded);
            }
        }
        String[] trusted = adapter.ListTrusts();
        if (trusted != null) {
            for (String aTrusted : trusted) {
                addresses.add(aTrusted);
            }
        }
        return addresses;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#isRemoteDeviceConnected(java.lang.String)
     */
    public boolean isRemoteDeviceConnected(String deviceAddress) throws DBusException {
        return adapter.IsConnected(deviceAddress);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#isRemoteDeviceTrusted(java.lang.String)
     */
    public Boolean isRemoteDeviceTrusted(String deviceAddress) throws DBusException {
        return adapter.HasBonding(deviceAddress);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#authenticateRemoteDevice(java.lang.String)
     */
    public void authenticateRemoteDevice(String deviceAddress) throws DBusException {
        adapter.CreateBonding(deviceAddress);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#authenticateRemoteDevice(java.lang.String,
     * java.lang.String)
     */
    public boolean authenticateRemoteDevice(String deviceAddress, String passkey) throws DBusException {
        if (passkey == null) {
            authenticateRemoteDevice(deviceAddress);
            return true;
        } else {

            PasskeyAgent passkeyAgent = new PasskeyAgent() {

                public String Request(String path, String address) throws Error.Rejected, Error.Canceled {
                    if (deviceAddress.equals(address)) {
                        DebugLog.debug("PasskeyAgent.Request");
                        return passkey;
                    } else {
                        return "";
                    }
                }

                public boolean isRemote() {
                    return false;
                }

                public void Cancel(String path, String address) {
                }

                public void Release() {
                }
            };

            //            final Object completedEvent = new Object();
            //            DBusSigHandler<Adapter.BondingCreated> bondingCreated = new DBusSigHandler<Adapter.BondingCreated>() {
            //                public void handle(Adapter.BondingCreated s) {
            //                    DebugLog.debug("BondingCreated.handle");
            //                    synchronized (completedEvent) {
            //                        completedEvent.notifyAll();
            //                    }
            //                }
            //            };

            DebugLog.debug("get security on path", adapterPath.getPath());
            Security security = dbusConn.getRemoteObject("org.bluez", adapterPath.getPath(), Security.class);

            String passkeyAgentPath = "/org/bluecove/authenticate/" + getAdapterID() + "/" + deviceAddress.replace(':', '_');

            DebugLog.debug("export passkeyAgent", passkeyAgentPath);
            dbusConn.exportObject(passkeyAgentPath, passkeyAgent);

            // see http://bugs.debian.org/cgi-bin/bugreport.cgi?bug=501222
            boolean useDefaultPasskeyAgentBug = BlueCoveImpl.getConfigProperty("bluecove.bluez.registerDefaultPasskeyAgent", false);
            try {
                if (useDefaultPasskeyAgentBug) {
                    security.RegisterDefaultPasskeyAgent(passkeyAgentPath);
                } else {
                    security.RegisterPasskeyAgent(passkeyAgentPath, deviceAddress);
                }
                adapter.CreateBonding(deviceAddress);
                return true;
            } finally {
                //quietRemoveSigHandler(Adapter.BondingCreated.class, bondingCreated);
                try {
                    if (useDefaultPasskeyAgentBug) {
                        security.UnregisterDefaultPasskeyAgent(passkeyAgentPath);
                    } else {
                        security.UnregisterPasskeyAgent(passkeyAgentPath, deviceAddress);
                    }
                } catch (DBusExecutionException ignore) {
                }
                dbusConn.unExportObject(passkeyAgentPath);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#removeAuthenticationWithRemoteDevice(java.lang.String)
     */
    public void removeAuthenticationWithRemoteDevice(String deviceAddress) throws DBusException {
        adapter.RemoveBonding(deviceAddress);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getRemoteDeviceServices(java.lang.String)
     */
    public Map<Integer, String> getRemoteDeviceServices(String deviceAddress) throws DBusException {
        String match = "";
        UInt32[] serviceHandles;
        try {
            serviceHandles = adapter.GetRemoteServiceHandles(deviceAddress, match);
        } catch (NoReply e) {
            return null;
        }
        if (serviceHandles == null) {
            throw new DBusException("Recived no records");
        }
        Map<Integer, String> xmlRecords = new HashMap<>();
        for (UInt32 serviceHandle : serviceHandles) {
            xmlRecords.put(serviceHandle.intValue(), adapter.GetRemoteServiceRecordAsXML(deviceAddress, serviceHandle));
        }
        return xmlRecords;
    }

    private Database getSDPService() throws DBusException {
        //return dbusConn.getRemoteObject("org.bluez", adapterPath.getPath(), Database.class);
        return dbusConn.getRemoteObject("org.bluez", "/org/bluez", Database.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#registerSDPRecord(java.lang.String)
     */
    public long registerSDPRecord(String sdpXML) throws DBusException {
        DebugLog.debug("AddServiceRecordFromXML", sdpXML);
        UInt32 handle = getSDPService().AddServiceRecordFromXML(sdpXML);
        return handle.longValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#updateSDPRecord(long, java.lang.String)
     */
    public void updateSDPRecord(long handle, String sdpXML) throws DBusException {
        DebugLog.debug("UpdateServiceRecordFromXML", sdpXML);
        getSDPService().UpdateServiceRecordFromXML(new UInt32(handle), sdpXML);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#unregisterSDPRecord(long)
     */
    public void unregisterSDPRecord(long handle) throws DBusException {
        DebugLog.debug("RemoveServiceRecord", handle);
        getSDPService().RemoveServiceRecord(new UInt32(handle));
    }
}
