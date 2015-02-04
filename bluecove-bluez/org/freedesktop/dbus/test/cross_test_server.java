/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Set;
import java.util.Vector;

import org.freedesktop.DBus;
import org.freedesktop.DBus.Binding;
import org.freedesktop.DBus.Binding.SingleTests;
import org.freedesktop.DBus.Binding.TestClient;
import org.freedesktop.DBus.Binding.TestClient.Trigger;
import org.freedesktop.DBus.Binding.TestSignals.Triggered;
import org.freedesktop.DBus.Binding.Tests;
import org.freedesktop.DBus.Binding.Triplet;
import org.freedesktop.DBus.Description;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.UInt16;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.UInt64;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

public class cross_test_server implements Tests, SingleTests, DBusSigHandler<Trigger>
{
   private DBusConnection conn;
   boolean run = true;
   private Set<String> done = new TreeSet<>();
   private Set<String> notdone = new TreeSet<>();
   {
       notdone.add("org.freedesktop.DBus.Binding.Tests.Identity");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityByte");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityBool");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt16");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt16");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt32");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt32");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt64");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt64");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityDouble");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityString");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityArray");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityByteArray");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityBoolArray");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt16Array");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt16Array");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt32Array");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt32Array");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt64Array");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt64Array");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityDoubleArray");
       notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityStringArray");
       notdone.add("org.freedesktop.DBus.Binding.Tests.Sum");
       notdone.add("org.freedesktop.DBus.Binding.SingleTests.Sum");
       notdone.add("org.freedesktop.DBus.Binding.Tests.InvertMapping");
       notdone.add("org.freedesktop.DBus.Binding.Tests.DeStruct");
       notdone.add("org.freedesktop.DBus.Binding.Tests.Primitize");
       notdone.add("org.freedesktop.DBus.Binding.Tests.Invert");
       notdone.add("org.freedesktop.DBus.Binding.Tests.Trigger");
       notdone.add("org.freedesktop.DBus.Binding.Tests.Exit");
       notdone.add("org.freedesktop.DBus.Binding.TestClient.Trigger");
   }
   
   public cross_test_server(DBusConnection conn)
   {
      this.conn = conn;
   }
   public boolean isRemote() { return false; }
   @SuppressWarnings("unchecked")
   @Description("Returns whatever it is passed")
      public <T> Variant<T> Identity(Variant<T> input)
      {
          done.add("org.freedesktop.DBus.Binding.Tests.Identity");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.Identity");
         return new Variant(input.getValue());
      }
   @Description("Returns whatever it is passed")
      public byte IdentityByte(byte input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityByte");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityByte");
         return input;
      }
   @Description("Returns whatever it is passed")
      public boolean IdentityBool(boolean input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityBool");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityBool");
         return input;
      }
   @Description("Returns whatever it is passed")
      public short IdentityInt16(short input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt16");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt16");
         return input;
      }
   @Description("Returns whatever it is passed")
      public UInt16 IdentityUInt16(UInt16 input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt16");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt16");
         return input;
      }
   @Description("Returns whatever it is passed")
      public int IdentityInt32(int input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt32");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt32");
         return input;
      }
   @Description("Returns whatever it is passed")
      public UInt32 IdentityUInt32(UInt32 input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt32");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt32");
         return input;
      }
   @Description("Returns whatever it is passed")
      public long IdentityInt64(long input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt64");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt64");
         return input;
      }
   @Description("Returns whatever it is passed")
      public UInt64 IdentityUInt64(UInt64 input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt64");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt64");
         return input;
      }
   @Description("Returns whatever it is passed")
      public double IdentityDouble(double input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityDouble");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityDouble");
         return input;
      }
   @Description("Returns whatever it is passed")
      public String IdentityString(String input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityString");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityString");
         return input;
      }
   @Description("Returns whatever it is passed")
      public <T> Variant<T>[] IdentityArray(Variant<T>[] input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityArray");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityArray");
         return input;
      }
   @Description("Returns whatever it is passed")
      public byte[] IdentityByteArray(byte[] input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityByteArray");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityByteArray");
         return input;
      }
   @Description("Returns whatever it is passed")
      public boolean[] IdentityBoolArray(boolean[] input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityBoolArray");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityBoolArray");
         return input;
      }
   @Description("Returns whatever it is passed")
      public short[] IdentityInt16Array(short[] input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt16Array");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt16Array");
         return input;
      }
   @Description("Returns whatever it is passed")
      public UInt16[] IdentityUInt16Array(UInt16[] input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt16Array");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt16Array");
         return input;
      }
   @Description("Returns whatever it is passed")
      public int[] IdentityInt32Array(int[] input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt32Array");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt32Array");
         return input;
      }
   @Description("Returns whatever it is passed")
      public UInt32[] IdentityUInt32Array(UInt32[] input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt32Array");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt32Array");
         return input;
      }
   @Description("Returns whatever it is passed")
      public long[] IdentityInt64Array(long[] input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt64Array");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt64Array");
         return input;
      }
   @Description("Returns whatever it is passed")
      public UInt64[] IdentityUInt64Array(UInt64[] input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt64Array");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt64Array");
         return input;
      }
   @Description("Returns whatever it is passed")
      public double[] IdentityDoubleArray(double[] input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityDoubleArray");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityDoubleArray");
         return input;
      }
   @Description("Returns whatever it is passed")
      public String[] IdentityStringArray(String[] input) 
      {
          done.add("org.freedesktop.DBus.Binding.Tests.IdentityStringArray");
          notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityStringArray");
         return input;
      }
   @Description("Returns the sum of the values in the input list")
   public long Sum(int[] a)
   {
       done.add("org.freedesktop.DBus.Binding.Tests.Sum");
       notdone.remove("org.freedesktop.DBus.Binding.Tests.Sum");
      long sum = 0;
      for (int b: a) sum += b;
      return sum;
   }
   @Description("Returns the sum of the values in the input list")
   public UInt32 Sum(byte[] a)
   {
       done.add("org.freedesktop.DBus.Binding.SingleTests.Sum");
       notdone.remove("org.freedesktop.DBus.Binding.SingleTests.Sum");
      int sum = 0;
      for (byte b: a) sum += b < 0 ? b+256 : b;
      return new UInt32(sum % (UInt32.MAX_VALUE+1));
   }
   @Description("Given a map of A => B, should return a map of B => a list of all the As which mapped to B")
   public Map<String, List<String>> InvertMapping(Map<String, String> a)
   {
       done.add("org.freedesktop.DBus.Binding.Tests.InvertMapping");
       notdone.remove("org.freedesktop.DBus.Binding.Tests.InvertMapping");
      HashMap<String, List<String>> m = new HashMap<>();
      for (String s: a.keySet()) {
         String b = a.get(s);
         List<String> l = m.get(b);
         if (null == l) {
            l = new Vector<>();
            m.put(b, l);
         }
         l.add(s);
      }
      return m;
   }
   @Description("This method returns the contents of a struct as separate values")
   public Triplet<String,UInt32,Short> DeStruct(Binding.TestStruct a)
   {
       done.add("org.freedesktop.DBus.Binding.Tests.DeStruct");
       notdone.remove("org.freedesktop.DBus.Binding.Tests.DeStruct");
      return new Triplet<>(a.a, a.b, a.c);
   }
   @Description("Given any compound type as a variant, return all the primitive types recursively contained within as an array of variants")
   @SuppressWarnings("unchecked")
   public List<Variant<Object>> Primitize(Variant<Object> a)
   {
       done.add("org.freedesktop.DBus.Binding.Tests.Primitize");
       notdone.remove("org.freedesktop.DBus.Binding.Tests.Primitize");
      return cross_test_client.PrimitizeRecurse(a.getValue(), a.getType());
   }
   @Description("inverts it's input")
   public boolean Invert(boolean a)
   {
       done.add("org.freedesktop.DBus.Binding.Tests.Invert");
       notdone.remove("org.freedesktop.DBus.Binding.Tests.Invert");
      return !a;
   }
   @Description("triggers sending of a signal from the supplied object with the given parameter")
   public void Trigger(String a, UInt64 b)
   {
       done.add("org.freedesktop.DBus.Binding.Tests.Trigger");
       notdone.remove("org.freedesktop.DBus.Binding.Tests.Trigger");
      try {
          conn.sendSignal(new Triggered(a, b));
      } catch (DBusException DBe) {
         throw new DBusExecutionException(DBe.getMessage());
      }
   }
   public void Exit()
   {
       done.add("org.freedesktop.DBus.Binding.Tests.Exit");
       notdone.remove("org.freedesktop.DBus.Binding.Tests.Exit");
       run = false;
      synchronized (this) {
          notifyAll();
      }
   }
   public void handle(Trigger t)
   {
       done.add("org.freedesktop.DBus.Binding.TestClient.Trigger");
       notdone.remove("org.freedesktop.DBus.Binding.TestClient.Trigger");
      try {
         TestClient cb = conn.getRemoteObject(t.getSource(), "/Test", TestClient.class);
         cb.Response(t.a, t.b);
      } catch (DBusException DBe) {
         throw new DBusExecutionException(DBe.getMessage());
      }
   }

   public static void main(String[] args)
   { try {
      DBusConnection conn = DBusConnection.getConnection(DBusConnection.SESSION);
      conn.requestBusName("org.freedesktop.DBus.Binding.TestServer");
      cross_test_server cts = new cross_test_server(conn);
      conn.addSigHandler(Trigger.class, cts);
      conn.exportObject("/Test", cts);
      synchronized (cts) {
         while (cts.run) {
            try {
               cts.wait();
            } catch (InterruptedException Ie) {}
         }
      }
      for (String s: cts.done)
         System.out.println(s+" ok");
      for (String s: cts.notdone)
         System.out.println(s+" untested");
      conn.disconnect();
      System.exit(0);
   } catch (DBusException DBe) {
      DBe.printStackTrace();
      System.exit(1);
   }}
}

