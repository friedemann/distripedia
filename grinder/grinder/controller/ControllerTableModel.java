/**
 *
 */
package grinder.controller;

import javax.swing.table.AbstractTableModel;

/**
 * Table model controlling the table structure of the client table on the GUI.
 *
 * @author Friedemann
 */
@SuppressWarnings("serial")
public class ControllerTableModel extends AbstractTableModel {

	private final int columnCount;
	private final int rowCount;

	private String[] columnNames;
	private String[][] data;

	/**
	 * Instantiate a new controller specific table model
	 */
	public ControllerTableModel(final int rows, final int cols) {
		rowCount = rows;
		columnCount = cols;
		data = new String[rows][cols];

	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	@Override
	public int getColumnCount() {
		return columnCount;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	@Override
	public int getRowCount() {
		return rowCount;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	@Override
	public String getValueAt(final int row, final int col) {
		return data[row][col];
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
	 */
	@Override
	public String getColumnName(final int col) {
		return columnNames[col].toString();
	}

	/**
	 * Sets a cell value at the specified "coordinate"
	 *
	 * @param value the value
	 * @param row the row
	 * @param col the col
	 */
	public void setValueAt(final String value, final int row, final int col) {
		data[row][col] = value;
		fireTableCellUpdated(row, col);
	}

	/**
	 * Sets the column names.
	 *
	 * @param tableColumnNames array with column names
	 */
	public void setColumnNames(final String[] tableColumnNames) {
		columnNames = tableColumnNames;
		fireTableStructureChanged();
	}

	/**
	 * Sets the complete table data. <b><i>The array dimensions must match the
	 * table layout!</i><b>
	 *
	 * @param data the data to set
	 */
	public void setData(final String[][] data) {
		this.data = data;
		fireTableDataChanged();
	}

}
