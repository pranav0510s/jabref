package org.jabref.logic.integrity;

import java.util.List;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AbbreviationCheckerTest {

    private JournalAbbreviationRepository abbreviationRepository;
    private AbbreviationChecker checker;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        abbreviationRepository = JournalAbbreviationLoader.loadBuiltInRepository();
        abbreviationRepository.addCustomAbbreviation(new Abbreviation("Test Journal", "T. J."));
        entry = new BibEntry(StandardEntryType.InProceedings);
        checker = new AbbreviationChecker(abbreviationRepository);
    }

    @Test
    void checkEntryComplainsAboutAbbreviatedJournalName() {
        entry.setField(StandardField.BOOKTITLE, "T. J.");
        assertNotEquals(List.of(), checker.check(entry));
    }

    @Test
    void checkEntryDoesNotComplainAboutJournalNameThatHasSameAbbreviation() {
        entry.setField(StandardField.BOOKTITLE, "Journal");
        abbreviationRepository.addCustomAbbreviation(new Abbreviation("Journal", "Journal"));
        assertEquals(List.of(), checker.check(entry));
    }

    @Test
    void checkEntryDoesNotComplainAboutJournalNameThatHasNoAbbreviation() {
        entry.setField(StandardField.BOOKTITLE, "Non-Abbreviated-Title");
        assertEquals(List.of(), checker.check(entry));
    }

    @Test
    void checkEntryDoesNotComplainAboutJournalNameThatHasNoInput() {
        assertEquals(List.of(), checker.check(entry));
    }

    @Test
    void checkEntryWorksForLaTeXField() {
        entry.setField(StandardField.BOOKTITLE, "Reducing Complexity and Power of Digital Multibit Error-Feedback $\\Delta$$\\Sigma$ Modulators");
        assertEquals(List.of(), checker.check(entry));
    }

    @Test
    void checkEntryWorksForLaTeXFieldStilContainingIllegalChars() {
        entry.setField(StandardField.BOOKTITLE, "Proceedings of the 5\\({}^{\\mbox{th}}\\) Central-European Workshop on Services and their Composition, Rostock, Germany, February 21-22, 2013");
        assertEquals(List.of(), checker.check(entry));
    }
}
