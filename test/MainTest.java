import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

import edu.cvtc.bigram.*;

@SuppressWarnings({"SpellCheckingInspection"})
class MainTest {
  @Test
  void createConnection() {
    assertDoesNotThrow(
        () -> {
          Connection db = Main.createConnection();
          assertNotNull(db);
          assertFalse(db.isClosed());
          db.close();
          assertTrue(db.isClosed());
        }, "Failed to create and close connection."
    );
  }

  @Test
  void reset() {
    Main.reset();
    assertFalse(Files.exists(Path.of(Main.DATABASE_PATH)));
  }

  @Test
  void mainArgs() {
    assertAll(
        () -> {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          System.setOut(new PrintStream(out));
          Main.main(new String[]{"--version"});
          String output = out.toString();
          assertTrue(output.startsWith("Version "));
        },
        () -> {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          System.setOut(new PrintStream(out));
          Main.main(new String[]{"--help"});
          String output = out.toString();
          assertTrue(output.startsWith("Add bigrams"));
        },
        () -> assertDoesNotThrow(() -> {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          System.setErr(new PrintStream(out));
          Main.main(new String[]{"--reset"});
          String output = out.toString();
          assertTrue(output.startsWith("Expected"));
        }),
        () -> assertDoesNotThrow(() -> Main.main(new String[]{"./sample-texts/non-existant-file.txt"})),
        () -> assertDoesNotThrow(() -> Main.main(new String[]{"./sample-texts/empty.txt"}))
    );
  }

  //Test Valid input. Should add to the bigram count.
    @Test
    void testAddBigramWithValidInput() throws SQLException {
        try (Connection db = Main.createConnection()) {
            int word1 = Main.getId(db, "cat");
            int word2 = Main.getId(db, "the");

            int initialBigrams = getBigramCount(db);
            System.out.println("Initial count: " + initialBigrams);

            Main.addBigram(db, word1, word2);

            int newBigrams = getBigramCount(db);
            System.out.println("Count after valid input: " + newBigrams);

            assertEquals(initialBigrams + 1, newBigrams,
                    String.format("The bigram count should increase by 1."));
        }
    }

    // Test negative ID. Should not change the bigram count.
    @Test
    void testAddInvalidInputNegative() throws SQLException {
        try (Connection db = Main.createConnection()) {
            int initialBigrams = getBigramCount(db);
            System.out.println("Initial count: " + initialBigrams);

            // Test invalid input with negative IDs
            System.out.println("Test invalid input (-1, -1)");
            assertDoesNotThrow(() -> Main.addBigram(db, -1, -1),
                    "Negative ID should not throw exception");

            int currentBigrams = getBigramCount(db);

            assertEquals(initialBigrams, currentBigrams, "The bigram count should remain unchanged for invalid input.");
        }
    }

    // Test 0 ID. Should not change the bigram count.
    @Test
    void testAddInvalidInputZero() throws SQLException {
        try (Connection db = Main.createConnection()) {
            int initialBigrams = getBigramCount(db);
            System.out.println("Initial count: " + initialBigrams);

            // Test invalid input with zero ID
            assertDoesNotThrow(() -> Main.addBigram(db, 0, 0),
                    "Zero ID should not throw exception");

            int currentBigrams = getBigramCount(db);
            System.out.println("Count after input 0: " + currentBigrams);

            assertEquals(initialBigrams, currentBigrams,
                    "The bigram count should remain unchanged for invalid input.");
        }
    }

    // Find size of database
    private int getBigramCount(Connection conn) throws SQLException {
        Statement command = conn.createStatement();
        ResultSet rows = command.executeQuery("SELECT COUNT(*) AS count FROM bigrams");
        rows.next();
        return rows.getInt(1);
    }
}