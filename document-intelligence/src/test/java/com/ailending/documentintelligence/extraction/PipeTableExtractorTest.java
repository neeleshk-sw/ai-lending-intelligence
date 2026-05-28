package com.ailending.documentintelligence.extraction;

import com.ailending.documentintelligence.model.ExtractedTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PipeTableExtractorTest {

    private PipeTableExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new PipeTableExtractor();
    }

    @Test
    void validPipeTable_parsedCorrectly() {
        String text = "| Date       | Description    | Amount |\n" +
                      "| ---------- | -------------- | ------ |\n" +
                      "| 2024-01-01 | Salary credit  | 75000  |\n" +
                      "| 2024-01-05 | Rent debit     | 20000  |";

        List<ExtractedTable> tables = extractor.extract(text);

        assertEquals(1, tables.size());
        ExtractedTable table = tables.get(0);
        assertEquals(3, table.getHeaders().size());
        assertEquals("Date",        table.getHeaders().get(0));
        assertEquals("Description", table.getHeaders().get(1));
        assertEquals("Amount",      table.getHeaders().get(2));
        assertEquals(2, table.getRows().size());
        assertEquals("Salary credit", table.getRows().get(0).get(1));
    }

    @Test
    void rowWithMismatchedColumnCount_isSkipped() {
        String text = "| Name   | Score |\n" +
                      "| ------ | ----- |\n" +
                      "| Alice  | 720   |\n" +   // correct
                      "| Bob    |       |  extra  |\n" +  // mismatched — skipped
                      "| Carol  | 680   |";       // correct

        List<ExtractedTable> tables = extractor.extract(text);

        assertEquals(1, tables.size());
        // Alice and Carol included; Bob row skipped (3 cols vs 2 header cols)
        assertEquals(2, tables.get(0).getRows().size());
        assertEquals("Alice", tables.get(0).getRows().get(0).get(0));
        assertEquals("Carol", tables.get(0).getRows().get(1).get(0));
    }

    @Test
    void textWithNoTable_returnsEmptyList() {
        List<ExtractedTable> tables = extractor.extract("This is plain text. No tables here.");
        assertTrue(tables.isEmpty());
    }

    @Test
    void blankText_returnsEmptyList() {
        assertTrue(extractor.extract("").isEmpty());
        assertTrue(extractor.extract(null).isEmpty());
    }

    @Test
    void multipleTables_bothExtracted() {
        String text = "Bank Statement\n" +
                      "| Month | Balance |\n" +
                      "| ----- | ------- |\n" +
                      "| Jan   | 100000  |\n" +
                      "\n" +
                      "Salary Slips\n" +
                      "| Month | Gross  | Net   |\n" +
                      "| ----- | ------ | ----- |\n" +
                      "| Jan   | 80000  | 70000 |";

        List<ExtractedTable> tables = extractor.extract(text);

        assertEquals(2, tables.size());
        assertEquals(2, tables.get(0).getHeaders().size());
        assertEquals(3, tables.get(1).getHeaders().size());
    }
}
