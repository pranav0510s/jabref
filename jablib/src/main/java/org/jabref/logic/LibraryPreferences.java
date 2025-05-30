package org.jabref.logic;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.database.BibDatabaseMode;

public class LibraryPreferences {

    private final ObjectProperty<BibDatabaseMode> defaultBibDatabaseMode;
    private final BooleanProperty alwaysReformatOnSave;
    private final BooleanProperty autoSave;
    private final BooleanProperty addImportedEntries;
    private final StringProperty addImportedEntriesGroupName;

    public LibraryPreferences(BibDatabaseMode defaultBibDatabaseMode,
                              boolean alwaysReformatOnSave,
                              boolean autoSave,
                              boolean addImportedEntries,
                              String addImportedEntriesGroupName) {
        this.defaultBibDatabaseMode = new SimpleObjectProperty<>(defaultBibDatabaseMode);
        this.alwaysReformatOnSave = new SimpleBooleanProperty(alwaysReformatOnSave);
        this.autoSave = new SimpleBooleanProperty(autoSave);
        this.addImportedEntries = new SimpleBooleanProperty(addImportedEntries);
        this.addImportedEntriesGroupName = new SimpleStringProperty(addImportedEntriesGroupName);
    }

    public BibDatabaseMode getDefaultBibDatabaseMode() {
        return defaultBibDatabaseMode.get();
    }

    public ObjectProperty<BibDatabaseMode> defaultBibDatabaseModeProperty() {
        return defaultBibDatabaseMode;
    }

    public void setDefaultBibDatabaseMode(BibDatabaseMode defaultBibDatabaseMode) {
        this.defaultBibDatabaseMode.set(defaultBibDatabaseMode);
    }

    public boolean shouldAlwaysReformatOnSave() {
        return alwaysReformatOnSave.get();
    }

    public BooleanProperty alwaysReformatOnSaveProperty() {
        return alwaysReformatOnSave;
    }

    public void setAlwaysReformatOnSave(boolean alwaysReformatOnSave) {
        this.alwaysReformatOnSave.set(alwaysReformatOnSave);
    }

    public boolean shouldAutoSave() {
        return autoSave.get();
    }

    public BooleanProperty autoSaveProperty() {
        return autoSave;
    }

    public void setAutoSave(boolean shouldAutoSave) {
        this.autoSave.set(shouldAutoSave);
    }

    public boolean isAddImportedEntriesEnabled() {
        return addImportedEntries.get();
    }

    public BooleanProperty addImportedEntriesProperty() {
        return addImportedEntries;
    }

    public void setAddImportedEntries(boolean addImportedEntries) {
        this.addImportedEntries.set(addImportedEntries);
    }

    public String getAddImportedEntriesGroupName() {
        return addImportedEntriesGroupName.get();
    }

    public StringProperty addImportedEntriesGroupNameProperty() {
        return addImportedEntriesGroupName;
    }

    public void setAddImportedEntriesGroupName(String addImportedEntriesGroupName) {
        this.addImportedEntriesGroupName.set(addImportedEntriesGroupName);
    }
}
