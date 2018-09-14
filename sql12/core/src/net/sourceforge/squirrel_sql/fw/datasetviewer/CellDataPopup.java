package net.sourceforge.squirrel_sql.fw.datasetviewer;
/*
 * Copyright (C) 2001 Colin Bell
 * colbell@users.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.lgc.bulkdata.BulkData;
import com.lgc.bulkdata.BulkDataException;
import com.lgc.bulkdata.BulkDataFactory;
import com.lgc.bulkdata.BulkDataSerialType;
import com.lgc.bulkdata.DataTypeDef;
import com.lgc.bulkdata.NullValueDef;
import com.lgc.bulkdata.ProtoBufBulkData;

//import net.sourceforge.squirrel_sql.client.gui.AboutBoxDialog;
import net.sourceforge.squirrel_sql.client.Main;
import net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.BlobDescriptor;
import net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.CellComponentFactory;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;

/**
 * Generate a popup window to display and manipulate the
 * complete contents of a cell.
 */
public class CellDataPopup
{
	public static final String PREF_KEY_POPUPEDITABLEIOPANEL_WIDTH = "Squirrel.popupEditableIOPanelWidth";
	public static final String PREF_KEY_POPUPEDITABLEIOPANEL_HEIGHT = "Squirrel.popupEditableIOPanelHeight";


	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(CellDataPopup.class);


	/**
	 * function to create the popup display when called from JTable
	 */
	public static void showDialog(JTable table,
		ColumnDisplayDefinition colDef,
		MouseEvent evt,
		boolean isModelEditable)
	{
		CellDataPopup popup = new CellDataPopup();
		popup.createAndShowDialog(table, evt, colDef, isModelEditable);
	}

	private void createAndShowDialog(JTable table, MouseEvent evt,
		ColumnDisplayDefinition colDef, boolean isModelEditable)
	{
      Point pt = evt.getPoint();
      int row = table.rowAtPoint(pt);
      int col = table.columnAtPoint(pt);

      Object obj = table.getValueAt(row, col);

      // since user is now using popup, stop editing
      // using the in-cell editor, if any
      CellEditor editor = table.getCellEditor(row, col);
      if (editor != null)
         editor.cancelCellEditing();

      Component parent = SwingUtilities.windowForComponent(table);

      final JDialog dialog = getDialog(table, table.getColumnName(col), colDef, obj,
            row, col, isModelEditable, table);


      dialog.pack();

		Dimension dim;
		if (Main.getApplication().getSquirrelPreferences().isRememberValueOfPopup())
		{
			int width = Preferences.userRoot().getInt(PREF_KEY_POPUPEDITABLEIOPANEL_WIDTH, 600);
			int height = Preferences.userRoot().getInt(PREF_KEY_POPUPEDITABLEIOPANEL_HEIGHT, 300);
			dim = new Dimension(width, height);
		}
		else
		{
			dim = dialog.getSize();
			if (dim.width < 300)
			{
				dim.width = 300;
			}
			if (dim.height < 300)
			{
				dim.height = 300;
			}
			if (dim.width > 600)
			{
				dim.width = 600;
			}
			if (dim.height > 500)
			{
				dim.height = 500;
			}
		}

		Point parentBounds = parent.getLocation();

      parentBounds.x += SwingUtilities.convertPoint((Component) evt.getSource(), pt, parent).x;
      parentBounds.y += SwingUtilities.convertPoint((Component) evt.getSource(), pt, parent).y;

		dialog.setLocation(parentBounds);
      dialog.setSize(dim);

      dialog.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				Preferences.userRoot().putInt(PREF_KEY_POPUPEDITABLEIOPANEL_WIDTH, dialog.getSize().width);
				Preferences.userRoot().putInt(PREF_KEY_POPUPEDITABLEIOPANEL_HEIGHT, dialog.getSize().height);
			}
		});

		dialog.setVisible(true);
	}


	private JDialog getDialog(JTable table, String columnName, ColumnDisplayDefinition colDef, Object obj, int row,
			int col, boolean isModelEditable, JTable table2) {
		
		JDialog dialog = null;
		
		if (!(obj instanceof BlobDescriptor)) {
	        dialog = new TextAreaDialog(table, table.getColumnName(col), colDef, obj,
	                row, col, isModelEditable, table);
		} else {
	        dialog = new TableDialog(table, table.getColumnName(col), colDef, obj,
	                row, col, isModelEditable, table);
		}
        
        return dialog;
	}


	//
	// inner class for the data display pane
	//
	private static class ColumnDataPopupPanel extends JPanel {

      private final PopupEditableIOPanel ioPanel;
		private JDialog _parentFrame = null;
		private int _row;
		private int _col;
		private JTable _table;

		ColumnDataPopupPanel(Object cellContents,
			ColumnDisplayDefinition colDef,
			boolean tableIsEditable)
		{
			super(new BorderLayout());

			if (tableIsEditable &&
				CellComponentFactory.isEditableInPopup(colDef, cellContents)) {

				// data is editable in popup
				ioPanel = new PopupEditableIOPanel(colDef, cellContents, true);

				// Since data is editable, we need to add control panel
				// to manage user requests for DB update, file IO, etc.
				JPanel editingControls = createPopupEditingControls();
				add(editingControls, BorderLayout.SOUTH);
			}
			else {
				// data is not editable in popup
				ioPanel = new PopupEditableIOPanel(colDef, cellContents, false);
			}

			add(ioPanel, BorderLayout.CENTER);

		}

		/**
		 * Set up user controls to stop editing and update DB.
		 */
		private JPanel createPopupEditingControls() {

			JPanel panel = new JPanel(new BorderLayout());

			// create update/cancel controls using default layout
			JPanel updateControls = new JPanel();

			// set up Update button
			// i18n[cellDataPopUp.updateData=Update Data]
			JButton updateButton = new JButton(s_stringMgr.getString("cellDataPopUp.updateData"));
			updateButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {

					// try to convert the text in the popup into a valid
					// instance of type of data object being held in the table cell
					StringBuffer messageBuffer = new StringBuffer();
					Object newValue = ColumnDataPopupPanel.this.ioPanel.getObject(messageBuffer);
					if (messageBuffer.length() > 0) {
						// handle an error in conversion of text to object

						// i18n[cellDataPopUp.cannnotBGeConverted=The given text cannot be converted into the internal object.\n
						//Please change the data or cancel editing.\n
						//The conversion error was:\n{0}]
						String msg = s_stringMgr.getString("cellDataPopUp.cannnotBGeConverted", messageBuffer);

						JOptionPane.showMessageDialog(
							ColumnDataPopupPanel.this,
							msg,
							// i18n[cellDataPopUp.conversionError=Conversion Error]
							s_stringMgr.getString("cellDataPopUp.conversionError"),
							JOptionPane.ERROR_MESSAGE);

						ColumnDataPopupPanel.this.ioPanel.requestFocus();

					}
					else
					{
						_table.setValueAt(newValue, _row, _col);
						ColumnDataPopupPanel.this._parentFrame.setVisible(false);
						ColumnDataPopupPanel.this._parentFrame.dispose();
					}
				}
			});

			// set up Cancel button
			// i18n[cellDataPopup.cancel=Cancel]
			JButton cancelButton = new JButton(s_stringMgr.getString("cellDataPopup.cancel"));
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					ColumnDataPopupPanel.this._parentFrame.setVisible(false);
					ColumnDataPopupPanel.this._parentFrame.dispose();
				}
			});

			// add buttons to button panel
			updateControls.add(updateButton);
			updateControls.add(cancelButton);

			// add button panel to main panel
			panel.add(updateControls, BorderLayout.SOUTH);

			return panel;
		}

		/*
		 * Save various information which is needed to do Update & Cancel.
		 */
		 public void setUserActionInfo(JDialog parent, int row, int col,
		 	JTable table) {
		 	_parentFrame = parent;
		 	_row = row;
		 	_col = col;
		 	_table = table;
		 }


	}



	// The following is only useable for a root type of InternalFrame. If the
	// root type is Dialog or Frame, then other code must be used.
	class TextAreaDialog extends JDialog
	{

        public TextAreaDialog(Component comp, String columnName, ColumnDisplayDefinition colDef,
										Object value, int row, int col,
										boolean isModelEditable, JTable table)
		{

         // i18n[cellDataPopup.valueofColumn=Value of column {0}]
			super(SwingUtilities.windowForComponent(comp), s_stringMgr.getString("cellDataPopup.valueofColumn", columnName));
			ColumnDataPopupPanel popup =
				new ColumnDataPopupPanel(value, colDef, isModelEditable);
			popup.setUserActionInfo(this, row, col, table);
			setContentPane(popup);

         GUIUtils.enableCloseByEscape(this);
		}

   }
	
   class TableDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public TableDialog(Component comp, String columnName, ColumnDisplayDefinition colDef,
			Object value, int row, int col,
			boolean isModelEditable, JTable table) {
		
		
		super(SwingUtilities.windowForComponent(comp), s_stringMgr.getString("cellDataPopup.valueofColumn", columnName));
		
		try {
			
			BlobDescriptor blobDescriptor = (BlobDescriptor) value;
			BulkData dataObjIn = BulkDataFactory.createWrapper(BulkDataSerialType.PROTOBUF, new NullValueDef(), blobDescriptor.getBlob());
			
			String [] columns = ((ProtoBufBulkData)dataObjIn).getColumnNames().toArray(new String[dataObjIn.getColumnCount() + 1]);

			for (int l = columns.length-1; l >= 1; l--) {
				columns[l] = columns[l-1]; 
			}
			
			columns[0] = "";
			
			Object[][] data = new Object[dataObjIn.getRowCount() + 3][dataObjIn.getColumnCount() + 1];

			data[0][0] = "Unit Type";
			data[1][0] = "Unit";
			data[2][0] = "Data Type";
			
			int k = 1;
			for(int j = 1; j < columns.length; j++, k++) {
				
				data[0][k] = dataObjIn.getColumnUnitTypeName(columns[j]);
				data[1][k] = dataObjIn.getColumnUnitName(columns[j]);
				data[2][k] = dataObjIn.getColumnType(columns[j]).name();
				
				DataTypeDef type = dataObjIn.getColumnType(columns[j]);
				Object values = (Object) dataObjIn.getColumnArray(columns[j]);
				
				if (DataTypeDef.STRING.equals(type)) {
					String [] strValues = (String []) values;
					
					for (int s = 0; s < strValues.length; s++) {
						data[s + 3][k] = strValues[s];
					}
					
				} else if (DataTypeDef.FLOAT.equals(type)) {
					float [] fValues = (float []) values;
					
					for (int s = 0; s < fValues.length; s++) {
						data[s + 3][k] = fValues[s];
					}
					
				} else if (DataTypeDef.DOUBLE.equals(type)) {
					double [] dValues = (double []) values;
					
					for (int s = 0; s < dValues.length; s++) {
						data[s + 3][k] = dValues[s];
					}
					
				} else if (DataTypeDef.INTEGER.equals(type)) {
					int [] iValues = (int []) values;
					
					for (int s = 0; s < iValues.length; s++) {
						data[s + 3][k] = iValues[s];
					}
				}				
			}
			
			
			
/*			Iterator<Iterator<Object>> rows = dataObjIn.getRows();
			int i = 3;
			while(rows.hasNext()) {
				
				// Setting row number
				data[i][0] = i - 2;
				
				Iterator<Object> rowData = rows.next();
				int j = 1;
				while(rowData.hasNext()) {
					data[i][j++] = rowData.next();
				}
				
				i++;
			}*/
			
			JTable dataTable = new JTable(data, columns);
			dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
/*			Enumeration<TableColumn> modelEnum = dataTable.getColumnModel().getColumns();
				
				while (modelEnum.hasMoreElements()) {
					((TableColumn) modelEnum.nextElement()).setCellRenderer(new DefaultTableCellRenderer() {

						private static final long serialVersionUID = 1L;

						@Override
						public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
								boolean hasFocus, int row, int column) {
							Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
									column);
							if (column == 0 || row > 3) {
								c.setBackground(Color.LIGHT_GRAY);
							}
							return c;
						}
					});
				}*/

/*			dataTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
		        Color backgroundColor = getBackground();

		        @Override
		        public Component getTableCellRendererComponent(
		            JTable table, Object value, boolean isSelected,
		            boolean hasFocus, int row, int column) {
		            Component c = super.getTableCellRendererComponent(
		                table, value, isSelected, hasFocus, row, column);
		            c.setBackground(Color.LIGHT_GRAY);
		            return c;
		        }
			});*/

//			((DefaultTableCellRenderer)dataTable.getColumnModel().getColumn(0).()).setBackground(Color.LIGHT_GRAY);
//			dataTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellHeaderRenderer());
					
	        setContentPane(new JScrollPane(dataTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)); 

		} catch (BulkDataException e) {
			e.printStackTrace();
		}
		
//		ColumnDataPopupPanel popup =
//			new ColumnDataPopupPanel(value, colDef, isModelEditable);
//		popup.setUserActionInfo(this, row, col, table);
//		setContentPane(popup);
		
		GUIUtils.enableCloseByEscape(this);
	}
	
	   
   }

}
