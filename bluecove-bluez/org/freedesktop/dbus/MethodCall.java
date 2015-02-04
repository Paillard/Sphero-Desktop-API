/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import cx.ath.matthew.debug.Debug;
import cx.ath.matthew.utils.Hexdump;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.MessageFormatException;

import java.util.Arrays;
import java.util.Vector;

import static org.freedesktop.dbus.Gettext.getResource;

public class MethodCall extends Message
{
   MethodCall() { }
   public MethodCall(String dest, String path, String iface, String member, byte flags, String sig, Object... args) throws DBusException
   {
      this(null, dest, path, iface, member, flags, sig, args);
   }
   public MethodCall(String source, String dest, String path, String iface, String member, byte flags, String sig, Object... args) throws DBusException
   {
      super(Endian.BIG, MessageType.METHOD_CALL, flags);

      if (null == member || null == path)
         throw new MessageFormatException(getResource("Must specify destination, path and function name to MethodCalls."));
       headers.put(HeaderField.PATH, path);
       headers.put(HeaderField.MEMBER, member);

      Vector<Object> hargs = new Vector<>();

      hargs.add(new Object[] { HeaderField.PATH, new Object[] { ArgumentType.OBJECT_PATH_STRING, path } });
      
      if (null != source) {
          headers.put(HeaderField.SENDER, source);
         hargs.add(new Object[] { HeaderField.SENDER, new Object[] { ArgumentType.STRING_STRING, source } });
      }
      
      if (null != dest) {
          headers.put(HeaderField.DESTINATION, dest);
         hargs.add(new Object[] { HeaderField.DESTINATION, new Object[] { ArgumentType.STRING_STRING, dest } });
      }
      
      if (null != iface) {
         hargs.add(new Object[] { HeaderField.INTERFACE, new Object[] { ArgumentType.STRING_STRING, iface } });
          headers.put(HeaderField.INTERFACE, iface);
      }
      
      hargs.add(new Object[] { HeaderField.MEMBER, new Object[] { ArgumentType. STRING_STRING, member } });

      if (null != sig) {
         if (Debug.debug) Debug.print(Debug.DEBUG, "Appending arguments with signature: "+sig);
         hargs.add(new Object[] { HeaderField.SIGNATURE, new Object[] { ArgumentType.SIGNATURE_STRING, sig } });
          headers.put(HeaderField.SIGNATURE, sig);
          setArgs(args);
      }

      byte[] blen = new byte[4];
       appendBytes(blen);
       append("ua(yv)", serial, hargs.toArray());
       pad((byte)8);

      long c = bytecounter;
      if (null != sig) append(sig, args);
      if (Debug.debug) Debug.print(Debug.DEBUG, "Appended body, type: "+sig+" start: "+c+" end: "+ bytecounter +" size: "+(bytecounter -c));
       marshallint(bytecounter -c, blen, 0, 4);
      if (Debug.debug) Debug.print("marshalled size ("+ Arrays.toString(blen) +"): "+Hexdump.format(blen));
   }
   private static long REPLY_WAIT_TIMEOUT = 20000;
   /**
    * Set the default timeout for method calls.
    * Default is 20s.
    * @param timeout New timeout in ms.
    */
   public static void setDefaultTimeout(long timeout)
   {
       REPLY_WAIT_TIMEOUT = timeout;
   }
   Message reply;
   public synchronized boolean hasReply()
   {
      return null != reply;
   }
   /**
    * Block (if neccessary) for a reply.
    * @return The reply to this MethodCall, or null if a timeout happens.
    * @param timeout The length of time to block before timing out (ms).
    */
   public synchronized Message getReply(long timeout)
   {
      if (Debug.debug) Debug.print(Debug.VERBOSE, "Blocking on "+this);
      if (null != reply) return reply;
      try {
          wait(timeout);
         return reply;
      } catch (InterruptedException Ie) { return reply; }
   }
   /**
    * Block (if neccessary) for a reply.
    * Default timeout is 20s, or can be configured with setDefaultTimeout()
    * @return The reply to this MethodCall, or null if a timeout happens.
    */
   public synchronized Message getReply()
   {
      if (Debug.debug) Debug.print(Debug.VERBOSE, "Blocking on "+this);
      if (null != reply) return reply;
      try {
          wait(REPLY_WAIT_TIMEOUT);
         return reply;
      } catch (InterruptedException Ie) { return reply; }
   }
   protected synchronized void setReply(Message reply)
   {
      if (Debug.debug) Debug.print(Debug.VERBOSE, "Setting reply to "+this+" to "+reply);
      this.reply = reply;
       notifyAll();
   }

}
