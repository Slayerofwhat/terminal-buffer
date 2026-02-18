import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TerminalBuffer}.
 * Tests document expected behavior: dimensions, cursor clamping, wrap/scroll, scrollback cap, and content access.
 */
class TerminalBufferTest {
    TerminalBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new TerminalBuffer(10, 4, 5);
    }

    @Nested
    @DisplayName("Setup and initial state")
    class Setup {
        @Test
        @DisplayName("Constructor creates screen with exact width and height; each line has width spaces")
        void initialDimensions() {
            int width = 12;
            int height = 6;
            TerminalBuffer b = new TerminalBuffer(width, height, 10);
            String[] screenLines = b.getScreenText().split("\n");
            assertEquals(height, screenLines.length, "Screen must have exactly height lines");
            for (int i = 0; i < height; i++) {
                assertEquals(width, screenLines[i].length(), "Each line must have exactly width characters");
                assertEquals(" ".repeat(width), screenLines[i], "Initial content is spaces");
            }
        }

        @Test
        @DisplayName("Initially scrollback is empty so getFullText equals getScreenText")
        void initialScrollbackEmpty() {
            assertEquals(buffer.getScreenText(), buffer.getFullText());
        }
    }

    @Nested
    @DisplayName("Cursor position and movement")
    class CursorTest {
        @Test
        @DisplayName("Cursor starts at (0, 0)")
        void initialPosition() {
            assertEquals(0, buffer.getCursorColumn());
            assertEquals(0, buffer.getCursorRow());
        }

        @Test
        @DisplayName("setCursor clamps to valid range [0, width-1] x [0, height-1]")
        void setCursorClampsToScreenBounds() {
            buffer.setCursor(100, 100);
            assertEquals(9, buffer.getCursorColumn());
            assertEquals(3, buffer.getCursorRow());

            buffer.setCursor(-1, -1);
            assertEquals(0, buffer.getCursorColumn());
            assertEquals(0, buffer.getCursorRow());
        }

        @Test
        void moveCursorUp() {
            buffer.setCursor(2, 2);
            buffer.moveCursorUp(1);
            assertEquals(2, buffer.getCursorColumn());
            assertEquals(1, buffer.getCursorRow());
            buffer.moveCursorUp(10);
            assertEquals(0, buffer.getCursorRow());
        }

        @Test
        void moveCursorDown() {
            buffer.moveCursorDown(2);
            assertEquals(0, buffer.getCursorColumn());
            assertEquals(2, buffer.getCursorRow());
            buffer.moveCursorDown(10);
            assertEquals(3, buffer.getCursorRow());
        }

        @Test
        void moveCursorLeft() {
            buffer.setCursor(5, 0);
            buffer.moveCursorLeft(2);
            assertEquals(3, buffer.getCursorColumn());
            buffer.moveCursorLeft(10);
            assertEquals(0, buffer.getCursorColumn());
        }

        @Test
        void moveCursorRight() {
            buffer.moveCursorRight(3);
            assertEquals(3, buffer.getCursorColumn());
            buffer.moveCursorRight(20);
            assertEquals(9, buffer.getCursorColumn());
        }
    }

    @Nested
    @DisplayName("Current attributes applied to writes")
    class AttributesTest {
        @Test
        @DisplayName("Each cell stores a snapshot of attributes at write time; later changes do not affect it")
        void attributesUsedForWritesAndSnapshot() {
            Attributes attrs = new Attributes();
            attrs.setForegroundColor(Color.RED);
            attrs.setBold(true);
            buffer.setCurrentAttributes(attrs);
            buffer.writeText("X");
            attrs.setForegroundColor(Color.GREEN);
            attrs.setBold(false);
            buffer.writeText("Y");
            assertEquals(Color.RED, buffer.getAttributesAt(0, 0).getForegroundColor());
            assertTrue(buffer.getAttributesAt(0, 0).isBold());
            assertEquals(Color.GREEN, buffer.getAttributesAt(0, 1).getForegroundColor());
            assertFalse(buffer.getAttributesAt(0, 1).isBold());
        }

        @Test
        @DisplayName("fillLine uses current attributes for every cell")
        void fillLineUsesCurrentAttributes() {
            Attributes attrs = new Attributes();
            attrs.setUnderline(true);
            buffer.setCurrentAttributes(attrs);
            buffer.fillLine(1, 'x');
            assertTrue(buffer.getAttributesAt(1, 0).isUnderline());
            assertEquals('x', buffer.getCharAt(1, 0));
        }
    }

    @Nested
    @DisplayName("writeText: overwrite at cursor, wrap and scroll")
    class WriteTextTest {
        @Test
        @DisplayName("Overwrites at cursor and advances cursor")
        void overwritesAndMovesCursor() {
            buffer.writeText("Hi");
            assertEquals("Hi        ", buffer.getLine(0));
            assertEquals(2, buffer.getCursorColumn());
            assertEquals(0, buffer.getCursorRow());
        }

        @Test
        @DisplayName("When reaching end of line, cursor wraps to start of next line")
        void wrapsToNextLine() {
            buffer.writeText("0123456789A");
            assertEquals("0123456789", buffer.getLine(0));
            assertEquals("A         ", buffer.getLine(1));
            assertEquals(1, buffer.getCursorColumn());
            assertEquals(1, buffer.getCursorRow());
        }

        @Test
        @DisplayName("When cursor would go past bottom, screen scrolls up and top line enters scrollback")
        void scrollsWhenPastBottom() {
            buffer.writeText("Line0!!!!!");
            buffer.writeText("Line1!!!!!");
            buffer.writeText("Line2!!!!!");
            buffer.writeText("Line3!!!!!");
            buffer.writeText("X");
            assertEquals("Line0!!!!!", buffer.getLine(0));
            assertEquals("Line1!!!!!", buffer.getLine(1));
            assertEquals("Line2!!!!!", buffer.getLine(2));
            assertEquals("Line3!!!!!", buffer.getLine(3));
            assertEquals("X         ", buffer.getLine(4));
            assertEquals(1, buffer.getCursorColumn());
            assertEquals(3, buffer.getCursorRow());
        }

        @Test
        @DisplayName("Empty string is a no-op: cursor unchanged")
        void emptyStringSafe() {
            buffer.writeText("");
            assertEquals(0, buffer.getCursorColumn());
            assertEquals(0, buffer.getCursorRow());
        }

        @Test
        @DisplayName("Newline character is written as literal; no special line break")
        void newlineWrittenAsLiteralCharacter() {
            buffer.writeText("a\nb");
            assertEquals("a\nb       ", buffer.getLine(0));
            assertEquals(3, buffer.getCursorColumn());
        }
    }

    @Nested
    @DisplayName("insertText: insert and shift; wrap when inserting at start of full line")
    class InsertTextTest {
        @Test
        @DisplayName("Inserts at cursor and shifts existing content right")
        void insertsAndShifts() {
            buffer.writeText("abc");
            buffer.setCursor(1, 0);
            buffer.insertText("X");
            assertEquals("aXbc      ", buffer.getLine(0));
            assertEquals(2, buffer.getCursorColumn());
        }

        @Test
        @DisplayName("Insert at column 0 on full line: first width chars go to next line, rest stays on current")
        void insertWrapsLine() {
            buffer.writeText("1234567890");
            buffer.setCursor(0, 0);
            buffer.insertText("AB");
            assertEquals("90        ", buffer.getLine(0));
            assertEquals("AB12345678", buffer.getLine(1));
            assertEquals(2, buffer.getCursorColumn());
            assertEquals(1, buffer.getCursorRow());
        }

        @Test
        @DisplayName("Empty string is a no-op: buffer and cursor unchanged")
        void insertEmptyStringIsNoOp() {
            buffer.writeText("ab");
            buffer.setCursor(1, 0);
            buffer.insertText("");
            assertEquals("ab        ", buffer.getLine(0));
            assertEquals(1, buffer.getCursorColumn());
            assertEquals(0, buffer.getCursorRow());
        }
    }

    @Nested
    @DisplayName("fillLine")
    class FillLineTest {
        @Test
        @DisplayName("Fills entire line with given character using current attributes")
        void fillsEntireLineWithCharacter() {
            buffer.fillLine(0, '.');
            assertEquals("..........", buffer.getLine(0));
        }

        @Test
        @DisplayName("Out-of-range row is ignored (no change)")
        void fillLineIgnoresOutOfBoundsRow() {
            buffer.writeText("original");
            buffer.fillLine(-1, 'x');
            buffer.fillLine(4, 'x');
            assertEquals("original  ", buffer.getLine(0));
            assertEquals("          ", buffer.getLine(1));
        }
    }

    @Nested
    @DisplayName("insertEmptyLine")
    class InsertEmptyLineTest {
        @Test
        @DisplayName("Top screen line moves to scrollback; new empty line added at bottom")
        void movesTopToScrollbackAddsEmptyAtBottom() {
            buffer.writeText("first");
            buffer.setCursor(0, 1);
            buffer.writeText("second");
            buffer.insertEmptyLine();
            assertEquals("first     ", buffer.getLine(0));
            assertEquals("second    ", buffer.getLine(1));
            assertEquals("          ", buffer.getLine(2));
            assertEquals("          ", buffer.getLine(3));
            assertEquals("          ", buffer.getLine(4));
            assertEquals(5, buffer.getFullText().split("\n").length);
        }

        @Test
        @DisplayName("Scrollback size never exceeds scrollbackMax; oldest lines dropped")
        void scrollbackCappedAtMax() {
            for (int i = 0; i < 8; i++) {
                buffer.writeText("Line" + i + "!!!!");
                buffer.setCursor(0, 3);
            }
            for (int i = 0; i < 6; i++) {
                buffer.insertEmptyLine();
            }
            String[] lines = buffer.getFullText().split("\n");
            int scrollbackCount = lines.length - 4;
            assertTrue(scrollbackCount <= 5, "Scrollback must not exceed scrollbackMax(5)");
            assertEquals(9, lines.length, "When cap is active: scrollbackMax(5) + height(4)");
        }
    }

    @Nested
    @DisplayName("clearScreen and clearAll")
    class ClearOperationsTest {
        @Test
        @DisplayName("clearScreen leaves scrollback intact; screen becomes empty; cursor (0,0)")
        void clearScreenKeepsScrollbackResetsCursor() {
            buffer.writeText("hello");
            buffer.insertEmptyLine();
            buffer.insertEmptyLine();
            buffer.clearScreen();
            assertEquals("hello     ", buffer.getLine(0));
            assertEquals("          ", buffer.getLine(1));
            assertEquals(0, buffer.getCursorColumn());
            assertEquals(0, buffer.getCursorRow());
        }

        @Test
        @DisplayName("clearAll clears scrollback and screen; cursor (0,0)")
        void clearAllClearsScrollbackAndScreen() {
            buffer.writeText("x");
            buffer.insertEmptyLine();
            buffer.clearAll();
            assertEquals("          ", buffer.getLine(0));
            assertEquals(4, buffer.getFullText().split("\n").length);
            assertEquals(0, buffer.getCursorColumn());
            assertEquals(0, buffer.getCursorRow());
        }
    }

    @Nested
    @DisplayName("Content access: getCell, getCharAt, getLine, getScreenText, getFullText")
    class ContentAccessTest {
        @Test
        @DisplayName("getCell: row 0..scrollbackSize-1 is scrollback; then screen")
        void getCellScreenAndScrollback() {
            buffer.writeText("A");
            buffer.insertEmptyLine();
            Cell scrollbackCell = buffer.getCell(0, 0);
            Cell screenCell = buffer.getCell(1, 0);
            assertNotNull(scrollbackCell);
            assertNotNull(screenCell);
            assertEquals('A', scrollbackCell.getCh());
            assertEquals(' ', screenCell.getCh());
        }

        @Test
        @DisplayName("getCell returns null for negative or out-of-range row/column")
        void getCellOutOfBoundsReturnsNull() {
            assertNull(buffer.getCell(-1, 0));
            assertNull(buffer.getCell(0, -1));
            assertNull(buffer.getCell(0, 10));
            buffer.writeText("x");
            assertNull(buffer.getCell(4, 0));
        }

        @Test
        @DisplayName("getCharAt returns space for out-of-bounds; otherwise cell character")
        void getCharAtReturnsSpaceWhenOutOfBounds() {
            buffer.writeText("a");
            assertEquals('a', buffer.getCharAt(0, 0));
            assertEquals(' ', buffer.getCharAt(0, 5));
            assertEquals(' ', buffer.getCharAt(-1, 0));
            assertEquals(' ', buffer.getCharAt(0, 100));
        }

        @Test
        @DisplayName("getAttributesAt returns null when out of bounds")
        void getAttributesAtOutOfBoundsReturnsNull() {
            buffer.writeText("x");
            assertNotNull(buffer.getAttributesAt(0, 0));
            assertNull(buffer.getAttributesAt(-1, 0));
            assertNull(buffer.getAttributesAt(0, 10));
        }

        @Test
        @DisplayName("getLine returns null for row < 0 or row >= total line count")
        void getLineOutOfBoundsReturnsNull() {
            assertNull(buffer.getLine(-1));
            assertNull(buffer.getLine(100));
            buffer.writeText("a");
            buffer.insertEmptyLine();
            int totalRows = buffer.getFullText().split("\n").length;
            assertNull(buffer.getLine(totalRows));
        }

        @Test
        @DisplayName("getScreenText returns only visible screen lines, newline-separated")
        void getScreenTextFormat() {
            buffer.writeText("ab");
            String screen = buffer.getScreenText();
            String[] lines = screen.split("\n");
            assertEquals(4, lines.length);
            assertTrue(lines[0].startsWith("ab"));
        }

        @Test
        @DisplayName("getFullText returns scrollback lines then screen lines")
        void getFullTextScrollbackThenScreen() {
            buffer.writeText("one");
            buffer.insertEmptyLine();
            buffer.writeText("two");
            String full = buffer.getFullText();
            assertTrue(full.startsWith("one"));
            assertTrue(full.contains("two"));
        }
    }

    @Nested
    @DisplayName("Edge cases and boundary conditions")
    class EdgeCasesTest {
        @Test
        @DisplayName("Width 1: each character on its own line; cursor wraps after every char")
        void narrowBuffer() {
            TerminalBuffer narrow = new TerminalBuffer(1, 2, 2);
            narrow.writeText("AB");
            assertEquals("A", narrow.getLine(0));
            assertEquals("B", narrow.getLine(1));
            assertEquals(0, narrow.getCursorColumn());
            assertEquals(1, narrow.getCursorRow());
        }

        @Test
        @DisplayName("Height 1: writing past first line scrolls immediately; only one screen line")
        void singleRowBuffer() {
            TerminalBuffer single = new TerminalBuffer(3, 1, 5);
            single.writeText("ABC");
            assertEquals("ABC", single.getLine(0));
            single.writeText("D");
            assertEquals("ABC", single.getLine(0));
            assertEquals("D  ", single.getLine(1));
            assertEquals(1, single.getCursorColumn(), "Cursor advanced after writing 'D'");
            assertEquals(0, single.getCursorRow());
        }

        @Test
        @DisplayName("scrollbackMax 0: no scrollback retained; getFullText has only screen lines")
        void zeroScrollbackMax() {
            TerminalBuffer noScroll = new TerminalBuffer(5, 2, 0);
            noScroll.writeText("aaaaa");
            noScroll.writeText("b");
            noScroll.insertEmptyLine();
            noScroll.insertEmptyLine();
            String[] lines = noScroll.getFullText().split("\n");
            assertEquals(2, lines.length);
        }

        @Test
        @DisplayName("fillLine with space overwrites line with spaces")
        void fillLineWithSpace() {
            buffer.writeText("hello");
            buffer.fillLine(0, ' ');
            assertEquals("          ", buffer.getLine(0));
        }

        @Test
        @DisplayName("Multiple scrolls: scrollback order and screen content correct")
        void multipleScrolls() {
            TerminalBuffer buf = new TerminalBuffer(10, 4, 15);
            for (int i = 0; i < 10; i++) {
                buf.writeText("L" + i + "!!!!!!!!");
                buf.setCursor(0, 3);
            }
            buf.writeText("X");
            assertEquals("L0!!!!!!!!", buf.getLine(0));
            assertEquals("L7!!!!!!!!", buf.getLine(9));
            assertEquals("X         ", buf.getLine(12));
            String full = buf.getFullText();
            assertTrue(full.contains("L0!!!!!!!!"));
            assertTrue(full.contains("X         "));
        }
    }
}
