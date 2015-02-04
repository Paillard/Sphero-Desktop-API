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
 *  @version $Id: BlueZAPIV4.java 3066 2011-01-06 18:57:17Z skarzhevskyy $
 */
package org.bluez.v4;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.bluetooth.DiscoveryAgent;

import com.intel.bluetooth.BluetoothConsts.DeviceClassConsts;
import org.bluez.BlueZAPI;
import org.bluez.Error;
import org.bluez.Error.Failed;
import org.bluez.dbus.DBusProperties;
import org.bluez.v4.Adapter.DeviceFound;
import org.bluez.v4.Device.Properties;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;

import com.intel.bluetooth.DebugLog;

/**
 * Access BlueZ v4 over D-Bus
 */
public class BlueZAPIV4 implements BlueZAPI {

    private DBusConnection dbusConn;

    private Manager dbusManager;

    private Adapter adapter;

    private Path adapterPath;

    public BlueZAPIV4(DBusConnection dbusConn, Manager dbusManager) {
        this.dbusConn = dbusConn;
        this.dbusManager = dbusManager;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#findAdapter(java.lang.String)
     */
    public Path findAdapter(String pattern) throws Error.InvalidArguments {
        try {
            return dbusManager.FindAdapter(pattern);
        } catch (Error.NoSuchAdapter e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#defaultAdapter()
     */
    public Path defaultAdapter() throws Error.InvalidArguments {
        try {
            return dbusManager.DefaultAdapter();
        } catch (Error.NoSuchAdapter e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#getAdapter(int)
     */
    public Path getAdapter(int number) {
        Path[] adapters = dbusManager.ListAdapters();
        if (adapters == null) {
            throw null;
        }
        if (number < 0 || number >= adapters.length) {
            throw null;
        }
        return adapters[number];
    }

    private String hciID(String adapterPath) {
        String bluezPath = "/org/bluez/";
        String path;
        if (adapterPath.startsWith(bluezPath)) {
            path = adapterPath.substring(bluezPath.length());
        } else {
            path = adapterPath;
        }
        int lastpart = path.lastIndexOf('/');
        if (lastpart != -1 && lastpart != path.length() -1) {
            return path.substring(lastpart + 1);
        } else {
            return path;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#listAdapters()
     */
    public List<String> listAdapters() {
        List<String> v = new Vector<>();
        Path[] adapters = dbusManager.ListAdapters();
        if (adapters != null) {
            for (Path adapter1 : adapters) {
                v.add(hciID(adapter1.getPath()));
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
        return DBusProperties.getStringValue(adapter, Adapter.Properties.Address);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#getAdapterID()
     */
    public String getAdapterID() {
        return hciID(adapterPath.getPath());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#getAdapterDeviceClass()
     */
    public int getAdapterDeviceClass() {
        // Since BlueZ 4.34
        Integer deviceClass = DBusProperties.getIntValue(adapter, Adapter.Properties.Class);
        if (deviceClass == null) {
            return DeviceClassConsts.MAJOR_COMPUTER;
        } else {
            return deviceClass;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#getAdapterName()
     */
    public String getAdapterName() {
        return DBusProperties.getStringValue(adapter, Adapter.Properties.Name);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#isAdapterDiscoverable()
     */
    public boolean isAdapterDiscoverable() {
        return DBusProperties.getBooleanValue(adapter, Adapter.Properties.Discoverable);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#getAdapterDiscoverableTimeout()
     */
    public int getAdapterDiscoverableTimeout() {
        return DBusProperties.getIntValue(adapter, Adapter.Properties.DiscoverableTimeout);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#setAdapterDiscoverable(int)
     */
    public boolean setAdapterDiscoverable(int mode) {
        switch (mode) {
        case DiscoveryAgent.NOT_DISCOVERABLE:
            adapter.SetProperty(DBusProperties.getPropertyName(Adapter.Properties.Discoverable), new Variant<>(Boolean.FALSE));
            break;
        case DiscoveryAgent.GIAC:
            adapter.SetProperty(DBusProperties.getPropertyName(Adapter.Properties.DiscoverableTimeout), new Variant<>(new UInt32(0)));
            adapter.SetProperty(DBusProperties.getPropertyName(Adapter.Properties.Discoverable), new Variant<>(Boolean.TRUE));
            break;
        case DiscoveryAgent.LIAC:
            adapter.SetProperty(DBusProperties.getPropertyName(Adapter.Properties.DiscoverableTimeout), new Variant<>(new UInt32(180)));
            adapter.SetProperty(DBusProperties.getPropertyName(Adapter.Properties.Discoverable), new Variant<>(Boolean.TRUE));
            break;
        default:
            if (0x9E8B00 <= mode && mode <= 0x9E8B3F) {
                // system does not support the access mode specified
                return false;
            }
            throw new IllegalArgumentException("Invalid discoverable mode");
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#getAdapterManufacturer()
     */
    public String getAdapterManufacturer() {
        // TODO How do I get this in BlueZ 4?
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#getAdapterRevision()
     */
    public String getAdapterRevision() {
        // TODO How do I get this in BlueZ 4?
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#getAdapterVersion()
     */
    public String getAdapterVersion() {
        // TODO How do I get this in BlueZ 4?
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#isAdapterPowerOn()
     */
    public boolean isAdapterPowerOn() {
        return DBusProperties.getBooleanValue(adapter, Adapter.Properties.Powered);
    }

    private <T extends DBusSignal> void quietRemoveSigHandler(Class<T> type, DBusSigHandler<T> handler) {
        try {
            dbusConn.removeSigHandler(type, handler);
        } catch (DBusException ignore) {
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#deviceInquiry(org.bluez.BlueZAPI.DeviceInquiryListener)
     */
    public void deviceInquiry(BlueZAPI.DeviceInquiryListener listener) throws DBusException, InterruptedException {
        DBusSigHandler<DeviceFound> remoteDeviceFound = s -> {
            String deviceName = null;
            int deviceClass = -1;
            boolean paired = false;
            Map<String, Variant<?>> properties = s.getDeviceProperties();
            if (properties != null) {
                deviceName = DBusProperties.getStringValue(properties, Properties.Name);
                deviceClass = DBusProperties.getIntValue(properties, Properties.Class);
                //TODO verify that this ever present
                paired = DBusProperties.getBooleanValue(properties, Properties.Paired, false);
            }
            listener.deviceDiscovered(s.getDeviceAddress(), deviceName, deviceClass, paired);
        };
        try {
            dbusConn.addSigHandler(DeviceFound.class, remoteDeviceFound);

            adapter.StartDiscovery();

            // Verify that discovery actually started to avoid race condition
            int tick = 0;
            boolean discovering = false;
            while (tick < 5 && !(discovering = DBusProperties.getBooleanValue(adapter, Adapter.Properties.Discovering))) {
                Thread.sleep(200);
                tick ++;
            }
            if (!discovering) {
                throw new Failed("Unable to confirm discovering state");
            }
            
            listener.deviceInquiryStarted();

            while (DBusProperties.getBooleanValue(adapter, Adapter.Properties.Discovering)) {
                Thread.sleep(200);
            }

            adapter.StopDiscovery();

        } finally {
            quietRemoveSigHandler(DeviceFound.class, remoteDeviceFound);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#deviceInquiryCancel()
     */
    public void deviceInquiryCancel() {
        adapter.StopDiscovery();
    }

    private Device getDevice(String deviceAddress) throws DBusException {
        Path devicePath;
        try {
            devicePath = adapter.FindDevice(deviceAddress);
        } catch (Error.DoesNotExist e) {
            DebugLog.debug("can't get device", e);
            devicePath = adapter.CreateDevice(deviceAddress);
        }
        return dbusConn.getRemoteObject("org.bluez", devicePath.getPath(), Device.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#getRemoteDeviceFriendlyName(java.lang.String)
     */
    public String getRemoteDeviceFriendlyName(String deviceAddress) throws DBusException, IOException {
        return DBusProperties.getStringValue(getDevice(deviceAddress), Properties.Name);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#retrieveDevices(boolean)
     */
    public List<String> retrieveDevices(boolean preKnown) {
        Path[] devices = adapter.ListDevices();
        List<String> addresses = new Vector<>();
        if (devices != null) {
            for (Path devicePath : devices) {
                try {
                    Device device = dbusConn.getRemoteObject("org.bluez", devicePath.getPath(), Device.class);
                    Map<String, Variant<?>> properties = device.GetProperties();
                    if (properties != null) {
                        String address = DBusProperties.getStringValue(properties, Properties.Address);
                        boolean paired = DBusProperties.getBooleanValue(properties, Properties.Paired, false);
                        boolean trusted = DBusProperties.getBooleanValue(properties, Properties.Trusted, false);
                        if (!preKnown || paired || trusted) {
                            addresses.add(address);
                        }
                    }
                } catch (DBusException e) {
                    DebugLog.debug("can't get device " + devicePath, e);
                }
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
        return DBusProperties.getBooleanValue(getDevice(deviceAddress), Properties.Connected);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#isRemoteDeviceTrusted(java.lang.String)
     */
    public Boolean isRemoteDeviceTrusted(String deviceAddress) throws DBusException {
        return DBusProperties.getBooleanValue(getDevice(deviceAddress), Properties.Paired);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#authenticateRemoteDevice(java.lang.String)
     */
    public void authenticateRemoteDevice(String deviceAddress) throws DBusException {
        throw new DBusException("TODO: How to implement this using Agent?");
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
        }
        Agent agent = new Agent() {

            public void Authorize(Path device, String uuid) throws Error.Rejected, Error.Canceled {
            }

            public void ConfirmModeChange(String mode) throws Error.Rejected, Error.Canceled {
            }

            public void DisplayPasskey(Path device, UInt32 passkey, byte entered) {
            }

            public void RequestConfirmation(Path device, UInt32 passkey) throws Error.Rejected, Error.Canceled {
            }

            public UInt32 RequestPasskey(Path device) throws Error.Rejected, Error.Canceled {
                return null;
            }

            public String RequestPinCode(Path device) throws Error.Rejected, Error.Canceled {
                return passkey;
            }

            public void Cancel() {
            }

            public void Release() {
            }

            public boolean isRemote() {
                return false;
            }

        };

        String agentPath = "/org/bluecove/authenticate/" + getAdapterID() + "/" + deviceAddress.replace(':', '_');

        DebugLog.debug("export Agent", agentPath);
        dbusConn.exportObject(agentPath, agent);

        try {
            adapter.CreatePairedDevice(deviceAddress, new Path(agentPath), "");
            return true;
        } finally {
            dbusConn.unExportObject(agentPath);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#removeAuthenticationWithRemoteDevice(java.lang.String)
     */
    public void removeAuthenticationWithRemoteDevice(String deviceAddress) {
        Path devicePath = adapter.FindDevice(deviceAddress);
        adapter.RemoveDevice(devicePath);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#getRemoteDeviceServices(java.lang.String)
     */
    public Map<Integer, String> getRemoteDeviceServices(String deviceAddress) throws DBusException {
        Path devicePath;
        try {
            devicePath = adapter.FindDevice(deviceAddress);
        } catch (Error.DoesNotExist e) {
            devicePath = adapter.CreateDevice(deviceAddress);
        }
        Device device = dbusConn.getRemoteObject("org.bluez", devicePath.getPath(), Device.class);

        Map<UInt32, String> xmlMap = device.DiscoverServices("");
        Map<Integer, String> xmlRecords = new HashMap<>();
        for (Entry<UInt32, String> record : xmlMap.entrySet()) {
            xmlRecords.put(record.getKey().intValue(), record.getValue());
        }
        return xmlRecords;
    }

    private Service getSDPService() throws DBusException {
        return dbusConn.getRemoteObject("org.bluez", adapterPath.getPath(), Service.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#registerSDPRecord(java.lang.String)
     */
    public long registerSDPRecord(String sdpXML) throws DBusException {
        DebugLog.debug("AddRecord", sdpXML);
        UInt32 handle = getSDPService().AddRecord(sdpXML);
        return handle.longValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#updateSDPRecord(long, java.lang.String)
     */
    public void updateSDPRecord(long handle, String sdpXML) throws DBusException {
        DebugLog.debug("UpdateRecord", sdpXML);
        getSDPService().UpdateRecord(new UInt32(handle), sdpXML);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.bluez.BlueZAPI#unregisterSDPRecord(long)
     */
    public void unregisterSDPRecord(long handle) throws DBusException {
        DebugLog.debug("RemoveRecord", handle);
        getSDPService().RemoveRecord(new UInt32(handle));

    }

}
