package com.properties.prop.widget;

import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

public class EditCell3<T, E> extends TableCell<T, String> {

private TextField textField;

@Override
public void startEdit() {
    if (!isEmpty()) {
        super.startEdit();
        createTextField();
        setText(null);
        setGraphic(textField);
        textField.selectAll();
    }
}

@Override
public void cancelEdit() {
    super.cancelEdit();
    
    setText((String) getItem());
    setGraphic(null);
}

@Override
public void updateItem(String item, boolean empty) {
super.updateItem(item, empty);

if (empty) {
    setText(null);
    setGraphic(null);
    } else {
        if (isEditing()) {
            if (textField != null) {
            textField.setText(getString());
            }
            setText(null);
            setGraphic(textField);
        } else {
            setText(getString());
            setGraphic(null);
        }
    }
}

private void createTextField() {
    textField = new TextField(getString());
    textField.setOnAction(evt -> { // enable ENTER commit
        commitEdit(textField.getText());
    });

    textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
    
    ChangeListener<? super Boolean> changeListener = (observable, oldSelection, newSelection) ->
    {
        if (! newSelection) {
            commitEdit(textField.getText());
        }
    };
    textField.focusedProperty().addListener(changeListener);
    
    textField.setOnKeyPressed((ke) -> {
        if (ke.getCode().equals(KeyCode.ESCAPE)) {
            textField.focusedProperty().removeListener(changeListener);
            cancelEdit();
        }
    });
}

private String getString() {
    return getItem() == null ? "" : getItem().toString();
}


@Override
public void commitEdit(String item) {

if (isEditing()) {
    super.commitEdit(item);
} else {
    final TableView table = getTableView();
    if (table != null) {
        TablePosition position = new TablePosition(getTableView(), getTableRow().getIndex(), getTableColumn());
        TableColumn.CellEditEvent editEvent = new TableColumn.CellEditEvent(table, position, TableColumn.editCommitEvent(), item);
        Event.fireEvent(getTableColumn(), editEvent);
    }
        updateItem(item, false);
        if (table != null) {
            table.edit(-1, null);
        }

    }
}

} 