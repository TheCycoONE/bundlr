package com.properties.prop.widget;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

public class EditCell < S, T > extends TextFieldTableCell< S, T > {

    private TextField textField;

    private boolean escapePressed = false;

    private TablePosition< S,

        ? > tablePos = null;

    public EditCell(final StringConverter< T > converter) {

        super(converter);

    }

    public static < S > Callback<TableColumn< S,String >,TableCell<S,String >> forTableColumn() {
        return forTableColumn(new DefaultStringConverter());

    }

    public static <S,T> Callback < TableColumn <S,T> , TableCell <S,T>> forTableColumn(final StringConverter < T > converter) {
        return list -> new EditCell < S, T > (converter);
    }

    @Override

    public void startEdit() {

        if (!isEditable() || !getTableView().isEditable() ||

            !getTableColumn().isEditable()) {

            return;

        }

        super.startEdit();
        if (textField == null) {

            textField = getTextField();
            textField.setText(getItemText());
            textField.positionCaret(getItemText().length());
        }

        if (isEditing()) {

            escapePressed = false;

            startEdit(textField);

            final TableView< S > table = getTableView();

            tablePos = table.getEditingCell();

        }

    }

    /** {@inheritDoc} */

    @Override

    public void commitEdit(T newValue) {

        if (!isEditing())

            return;

        final TableView < S > table = getTableView();

        if (table != null) {

            // Inform the TableView of the edit being ready to be committed.

            TableColumn.CellEditEvent editEvent = new TableColumn.CellEditEvent(table, tablePos,

                TableColumn.editCommitEvent(), newValue);

            Event.fireEvent(getTableColumn(), editEvent);

        }

        // we need to setEditing(false):

        super.cancelEdit(); // this fires an invalid EditCancelEvent.

        // update the item within this cell, so that it represents the new value

        updateItem(newValue, false);

        if (table != null) {

            // reset the editing cell on the TableView

            table.edit(-1, null);

        }

    }

    /** {@inheritDoc} */

    @Override

    public void cancelEdit() {

        if (escapePressed) {

            // this is a cancel event after escape key

            super.cancelEdit();

            setText(getItemText()); // restore the original text in the view

        } else {

            // this is not a cancel event after escape key

            // we interpret it as commit.

            String newText = textField.getText();

            // commit the new text to the model

            this.commitEdit(getConverter().fromString(newText));

        }
        setGraphic(null); // stop editing with TextField

    }

    /** {@inheritDoc} */

    @Override

    public void updateItem(T item, boolean empty) {

        super.updateItem(item, empty);

        updateItem();

    }

    private TextField getTextField() {

        final TextField textField = new TextField(getItemText());

        // Use onAction here rather than onKeyReleased (with check for Enter),

        textField.setOnAction(event -> {

            if (getConverter() == null) {

                throw new IllegalStateException("StringConverter is null.");

            }

            this.commitEdit(getConverter().fromString(textField.getText()));

            event.consume();

        });

        textField.focusedProperty().addListener(new ChangeListener< Boolean >() {

            @Override

            public void changed(ObservableValue<? extends Boolean> observable,Boolean oldValue, Boolean newValue) {

                if (!newValue) {

                    commitEdit(getConverter().fromString(textField.getText()));

                }

            }

        });

        textField.setOnKeyPressed(t -> {

            if (t.getCode() == KeyCode.ESCAPE)

                escapePressed = true;

            else

                escapePressed = false;

        });

        textField.setOnKeyReleased(t -> {

            if (t.getCode() == KeyCode.ESCAPE) {

                throw new IllegalArgumentException(

                    "did not expect esc key releases here.");

            }

        });

        textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {

            if (event.getCode() == KeyCode.ESCAPE) {

                textField.setText(getConverter().toString(getItem()));

                cancelEdit();

                event.consume();

            } else if ((event.getCode() == KeyCode.RIGHT ||

                event.getCode() == KeyCode.TAB)) {
                if(!isEditing()) {
                    getTableView().getSelectionModel().selectNext();
                    getTableView().getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                    event.consume();
                }

            } else if (event.getCode() == KeyCode.LEFT ) {

                if(!isEditing()) {
                    getTableView().getSelectionModel().selectPrevious();
                    getTableView().getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

                    event.consume();
                }

            } else if (event.getCode() == KeyCode.UP) {
                if(!isEditing()) {
                    getTableView().getSelectionModel().selectAboveCell();
                    getTableView().getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

                    event.consume();
                }

            } else if (event.getCode() == KeyCode.DOWN) {
                if(!isEditing()) {
                    getTableView().getSelectionModel().selectBelowCell();
                    getTableView().getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

                    event.consume();
                }

            }

        });

        return textField;

    }

    private String getItemText() {

        return getConverter() == null ?

            getItem() == null ? "" : getItem().toString() :

            getConverter().toString(getItem());

    }

    private void updateItem() {

        if (isEmpty()) {

            setText(null);

            setGraphic(null);

        } else {

            if (isEditing()) {

                if (textField != null) {

                    textField.setText(getItemText());
                    textField.positionCaret(getItemText().length());

                }

                setText(null);

                setGraphic(textField);

            } else {

                setText(getItemText());

                setGraphic(null);

            }

        }

    }

    private void startEdit(final TextField textField) {

        /*if (textField != null) {

            textField.setText(getItemText());
            textField.positionCaret(getItemText().length());

        }*/

        setText(null);

        setGraphic(textField);

        // requesting focus so that key input can immediately go into the

        // TextField

        textField.requestFocus();

    }

}