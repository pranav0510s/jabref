package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.importer.ImportException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MedlineImporterFilesTest {

    private static final String FILE_ENDING = ".xml";

    private static final String MALFORMED_KEY_WORD = "Malformed";

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("MedlineImporterTest") && name.endsWith(FILE_ENDING)
                && !name.contains(MALFORMED_KEY_WORD);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    private static Stream<String> invalidFileNames() throws IOException {
        Predicate<String> fileName = name -> !name.startsWith("MedlineImporterTest");
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void isRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(new MedlineImporter(), fileName);
    }

    @ParameterizedTest
    @MethodSource("invalidFileNames")
    void isNotRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsNotRecognizedFormat(new MedlineImporter(), fileName);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void importEntries(String fileName) throws ImportException, IOException {
        ImporterTestEngine.testImportEntries(new MedlineImporter(), fileName, FILE_ENDING);
    }

    private static Stream<String> malformedFileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("MedlineImporterTest" + MALFORMED_KEY_WORD)
                && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @ParameterizedTest
    @MethodSource("malformedFileNames")
    void importMalfomedFiles(String fileName) throws IOException {
        ImporterTestEngine.testImportMalformedFiles(new MedlineImporter(), fileName);
    }
}
