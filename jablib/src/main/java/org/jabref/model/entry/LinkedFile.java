package org.jabref.model.entry;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.strings.StringUtil;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents the link to an external file (e.g. associated PDF file).
 * This class is {@link Serializable} which is needed for drag and drop in gui
 */
@AllowedToUseLogic("Uses FileUtil from logic")
@NullMarked
public class LinkedFile implements Serializable {

    private static final String REGEX_URL = "^((?:https?\\:\\/\\/|www\\.)(?:[-a-z0-9]+\\.)*[-a-z0-9]+.*)";
    private static final Pattern URL_PATTERN = Pattern.compile(REGEX_URL);

    private static final LinkedFile NULL_OBJECT = new LinkedFile("", Path.of(""), "");

    // We have to mark these properties as transient because they can't be serialized directly
    private transient StringProperty description = new SimpleStringProperty();
    private transient StringProperty link = new SimpleStringProperty();
    // This field is a {@link StringProperty}, and not an {@link ObjectProperty<FileType>}, as {@link LinkedFile} might
    // be a URI, where a file type might not be present.
    private transient StringProperty fileType = new SimpleStringProperty();
    private transient StringProperty sourceURL = new SimpleStringProperty();

    public LinkedFile(String description, Path link, String fileType) {
        this(Objects.requireNonNull(description), Objects.requireNonNull(link).toString(), Objects.requireNonNull(fileType));
    }

    public LinkedFile(String description, Path link, String fileType, String sourceUrl) {
        this(Objects.requireNonNull(description), Objects.requireNonNull(link).toString(), Objects.requireNonNull(fileType), Objects.requireNonNull(sourceUrl));
    }

    public LinkedFile(String description, String link, FileType fileType) {
        this(description, link, fileType.getName());
    }

    /**
     * Constructor can also be used for non-valid paths. We need to parse them, because the GUI needs to render it.
     */
    public LinkedFile(String description, String link, String fileType, String sourceUrl) {
        this.description.setValue(Objects.requireNonNull(description));
        setLink(link);
        this.fileType.setValue(Objects.requireNonNull(fileType));
        this.sourceURL.setValue(sourceUrl);
    }

    public LinkedFile(String description, String link, String fileType) {
        this(description, link, fileType, "");
    }

    public LinkedFile(URL link, String fileType) {
        this("", Objects.requireNonNull(link).toString(), Objects.requireNonNull(fileType));
    }

    public LinkedFile(String description, URL link, String fileType) {
        this(description, Objects.requireNonNull(link).toString(), Objects.requireNonNull(fileType));
    }

    public LinkedFile(String description, URL link, String fileType, String sourceUrl) {
        this(description, Objects.requireNonNull(link).toString(), Objects.requireNonNull(fileType), Objects.requireNonNull(sourceUrl));
    }

    /**
     * Constructs a new LinkedFile with an empty file type and an empty description
     */
    public LinkedFile(Path link) {
        this("", Objects.requireNonNull(link), "");
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public StringProperty linkProperty() {
        return link;
    }

    public StringProperty fileTypeProperty() {
        return fileType;
    }

    public StringProperty sourceUrlProperty() {
        return sourceURL;
    }

    public String getFileType() {
        return fileType.get();
    }

    public void setFileType(String fileType) {
        this.fileType.setValue(fileType);
    }

    public void setFileType(FileType fileType) {
        this.setFileType(fileType.getName());
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        this.description.setValue(description);
    }

    public String getLink() {
        return link.get();
    }

    public void setLink(String link) {
        if (!isOnlineLink(link)) {
            this.link.setValue(link.replace("\\", "/"));
        } else {
            this.link.setValue(link);
        }
    }

    public String getSourceUrl() {
        return sourceURL.get();
    }

    public void setSourceURL(String url) {
        this.sourceURL.setValue(url);
    }

    public Observable[] getObservables() {
        return new Observable[] {this.link, this.description, this.fileType, this.sourceURL};
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof LinkedFile that) {
            return Objects.equals(description.get(), that.description.get())
                    && Objects.equals(link.get(), that.link.get())
                    && Objects.equals(fileType.get(), that.fileType.get())
                    && Objects.equals(sourceURL.get(), that.sourceURL.get());
        }
        return false;
    }

    /**
     * Writes serialized object to ObjectOutputStream, automatically called
     */
    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(getFileType());
        out.writeUTF(getLink());
        out.writeUTF(getDescription());
        out.writeUTF(getSourceUrl());
        out.flush();
    }

    /**
     * Reads serialized object from {@link ObjectInputStream}, automatically called
     */
    @Serial
    private void readObject(ObjectInputStream in) throws IOException {
        fileType = new SimpleStringProperty(in.readUTF());
        link = new SimpleStringProperty(in.readUTF());
        description = new SimpleStringProperty(in.readUTF());
        sourceURL = new SimpleStringProperty(in.readUTF());
    }

    /**
     * Checks if the given String is an online link
     *
     * @param toCheck The String to check
     * @return <code>true</code>, if it starts with "http://", "https://" or contains "www."; <code>false</code> otherwise
     */
    public static boolean isOnlineLink(String toCheck) {
        String normalizedFilePath = toCheck.trim().toLowerCase();
        return URL_PATTERN.matcher(normalizedFilePath).matches();
    }

    @Override
    public int hashCode() {
        return Objects.hash(description.get(), link.get(), fileType.get(), sourceURL.get());
    }

    @Override
    public String toString() {
        return "ParsedFileField{" +
                "description='" + description.get() + '\'' +
                ", link='" + link.get() + '\'' +
                ", fileType='" + fileType.get() + '\'' +
                (StringUtil.isNullOrEmpty(sourceURL.get()) ? "" : (", sourceUrl='" + sourceURL.get() + '\'')) +
                '}';
    }

    public boolean isEmpty() {
        return NULL_OBJECT.equals(this);
    }

    public boolean isOnlineLink() {
        return isOnlineLink(link.get());
    }

    public Optional<Path> findIn(BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        List<Path> dirs = databaseContext.getFileDirectories(filePreferences);
        return findIn(dirs);
    }

    /// Tries to locate the file.
    /// In case the path is absolute, the path is checked.
    /// In case the path is relative, the given directories are used as base directories.
    ///
    /// @return absolute path if found.
    public Optional<Path> findIn(List<Path> directories) {
        try {
            if (link.get().isEmpty()) {
                // We do not want to match empty paths (which could be any file or none ?!)
                return Optional.empty();
            }

            Path file = Path.of(link.get());
            if (file.isAbsolute() || directories.isEmpty()) {
                if (Files.exists(file)) {
                    return Optional.of(file);
                } else {
                    return Optional.empty();
                }
            } else {
                return FileUtil.find(link.get(), directories);
            }
        } catch (InvalidPathException ex) {
            return Optional.empty();
        }
    }
}
