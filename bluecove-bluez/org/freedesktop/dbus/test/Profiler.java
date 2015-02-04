/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.exceptions.DBusException;
import java.util.List;
import java.util.Map;

public interface Profiler extends DBusInterface
{
   class ProfileSignal extends DBusSignal
   {
      public ProfileSignal(String path) throws DBusException
      {
         super(path);
      }
   }
   void array(int[] v);
   void stringarray(String[] v);
   void map(Map<String, String> m);
   void list(List<String> l);
   void bytes(byte[] b);
   void struct(ProfileStruct ps);
   void string(String s);
	void NoReply();
	void Pong();
}


