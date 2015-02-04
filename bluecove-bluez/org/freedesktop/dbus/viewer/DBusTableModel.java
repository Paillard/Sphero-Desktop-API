/*
   D-Bus Java Viewer
   Copyright (c) 2006 Peter Cox

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.viewer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
class DBusTableModel extends AbstractTableModel
{
	private static final String INTROSPECTABLE = "introspectable?";

	private static final String OWNER = "owner";

	private static final String USER = "user";

	private static final String NAME = "name";
	
	private static final String PATH = "path";

	final String[] columns = {NAME, PATH, USER, OWNER, INTROSPECTABLE};

	private List<DBusEntry> entries = new ArrayList<>();

	/** {@inheritDoc} */
	public int getRowCount()
	{
		return entries.size();
	}
	/** Add a row to the table model
	 * 
	 * @param entry The dbus entry to add
	 */
	public void add(DBusEntry entry)
	{
        entries.add(entry);
	}

	/** {@inheritDoc} */
	public int getColumnCount()
	{
		return columns.length;
	}

	/** {@inheritDoc} */
	@Override
	public String getColumnName(int column)
	{
		return columns[column];
	}
	
	/** Get a row of the table
	 * @param row The row index
	 * @return The table row
	 */
	public DBusEntry getEntry(int row)
	{
		return entries.get(row);
	}
	
	/** {@inheritDoc} */
	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		String columnName = getColumnName(columnIndex);
		if (columnName.equals(NAME))
		{
			return String.class;
		}
        switch (columnName) {
            case PATH:
                return String.class;
            case USER:
                return Object.class;
            case OWNER:
                return String.class;
            case INTROSPECTABLE:
                return Boolean.class;
        }
		return super.getColumnClass(columnIndex);
	}

	/** {@inheritDoc} */
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		DBusEntry entry = getEntry(rowIndex);
		String columnName = getColumnName(columnIndex);
		if (columnName.equals(NAME))
		{
			return entry.getName();
		}
        switch (columnName) {
            case PATH:
                return entry.getPath();
            case USER:
                return entry.getUser();
            case OWNER:
                return entry.getOwner();
            case INTROSPECTABLE:
                return entry.getIntrospectable() != null;
        }
		return null;
	}

}
