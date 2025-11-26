package com.vscode.updater.gui;

import com.vscode.updater.discovery.VSCodeInstance;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model for managing VS Code instances in the settings window.
 */
public class InstanceTableModel extends AbstractTableModel {
    private static final String[] COLUMN_NAMES = {
        "Enabled", "Edition", "Version", "Path", "Last Update", "Status"
    };
    
    private List<VSCodeInstance> instances = new ArrayList<>();
    
    @Override
    public int getRowCount() {
        return instances.size();
    }
    
    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }
    
    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> Boolean.class;
            default -> String.class;
        };
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0; // Only enabled column is editable
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex >= instances.size()) return null;
        
        VSCodeInstance instance = instances.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> instance.enabled();
            case 1 -> instance.edition().getDisplayName();
            case 2 -> instance.version();
            case 3 -> instance.getShortPath();
            case 4 -> instance.lastUpdateTime();
            case 5 -> instance.lastUpdateStatus();
            default -> null;
        };
    }
    
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 0 && rowIndex < instances.size()) {
            VSCodeInstance instance = instances.get(rowIndex);
            VSCodeInstance updated = instance.withEnabled((Boolean) aValue);
            instances.set(rowIndex, updated);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
    
    public void setInstances(List<VSCodeInstance> newInstances) {
        this.instances = new ArrayList<>(newInstances);
        fireTableDataChanged();
    }
    
    public List<VSCodeInstance> getInstances() {
        return new ArrayList<>(instances);
    }
    
    public void setAllEnabled(boolean enabled) {
        for (int i = 0; i < instances.size(); i++) {
            VSCodeInstance instance = instances.get(i);
            instances.set(i, instance.withEnabled(enabled));
        }
        fireTableDataChanged();
    }
}