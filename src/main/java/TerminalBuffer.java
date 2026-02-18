import java.util.ArrayList;
import java.util.List;

public class TerminalBuffer {
    private final int width;
    private final int height;
    private final int scrollbackMax;

    private final List<Line> screen = new ArrayList<>();
    private final List<Line> scrollback = new ArrayList<>();

    private int cursorRow = 0;
    private int cursorColumn = 0;

    private Attributes currentAttributes = new Attributes();

    public TerminalBuffer(int width, int height, int scrollbackMax) {
        this.width = width;
        this.height = height;
        this.scrollbackMax = scrollbackMax;

        for (int i = 0; i < height; i++) {
            screen.add(new Line(width, currentAttributes));
        }
    }

    public Attributes getCurrentAttributes() {
        return currentAttributes;
    }

    public void setCurrentAttributes(Attributes currentAttributes) {
        this.currentAttributes = currentAttributes;
    }

    public int getCursorRow() {
        return cursorRow;
    }

    public int getCursorColumn() {
        return cursorColumn;
    }

    public void setCursor(int col, int row) {
        cursorColumn = Math.max(0, Math.min(col, width - 1));
        cursorRow = Math.max(0, Math.min(row, height - 1));
    }

    public void moveCursorUp(int n) {
        setCursor(cursorColumn, cursorRow - n);
    }

    public void moveCursorDown(int n) {
        setCursor(cursorColumn, cursorRow + n);
    }

    public void moveCursorLeft(int n) {
        setCursor(cursorColumn - n, cursorRow);
    }

    public void moveCursorRight(int n) {
        setCursor(cursorColumn + n, cursorRow);
    }

    public void writeText(String text) {
        for (char ch : text.toCharArray()) {
            if (cursorColumn >= width) {
                cursorColumn = 0;
                cursorRow++;
                if (cursorRow >= height) {
                    scrollUp();
                    cursorRow = height - 1;
                }
            }

            Line line = screen.get(cursorRow);
            line.set(cursorColumn, new Cell(ch, currentAttributes.copy()));

            cursorColumn++;
        }

        if (cursorColumn >= width) {
            cursorColumn = 0;
            cursorRow++;
            if (cursorRow >= height) {
                scrollUp();
                cursorRow = height - 1;
            }
        }
    }

    public void insertText(String text) {
        if (text.isEmpty()) return;

        Line line = screen.get(cursorRow);

        if (cursorColumn == 0 && line.length() == width) {
            String lineContent = line.toString();
            String combined = text + lineContent;
            if (combined.length() > width) {
                String forNextLine = combined.substring(0, width);
                String forCurrentLine = combined.substring(width);
                setLineContent(cursorRow, forCurrentLine);
                cursorRow++;
                if (cursorRow >= height) {
                    scrollUp();
                    cursorRow = height - 1;
                }
                setLineContent(cursorRow, forNextLine);
                cursorColumn = text.length();
                return;
            }
        }

        for (char ch : text.toCharArray()) {
            line = screen.get(cursorRow);
            line.insertChar(cursorColumn, new Cell(ch, currentAttributes.copy()), width);

            cursorColumn++;
            if (cursorColumn >= width) {
                cursorColumn = 0;
                cursorRow++;
                if (cursorRow >= height) {
                    scrollUp();
                    cursorRow = height - 1;
                }
            }
        }

        if (cursorColumn >= width) {
            cursorColumn = 0;
            cursorRow++;
            if (cursorRow >= height) {
                scrollUp();
                cursorRow = height - 1;
            }
        }
    }

    private void setLineContent(int row, String s) {
        String padded = s.length() >= width ? s.substring(0, width) : s + " ".repeat(width - s.length());
        Line line = screen.get(row);
        for (int i = 0; i < width; i++) {
            line.set(i, new Cell(padded.charAt(i), currentAttributes.copy()));
        }
    }

    public void fillLine(int row, char ch) {
        if (row < 0 || row >= height) return;
        Line line = screen.get(row);
        for (int i = 0; i < width; i++) {
            line.set(i, new Cell(ch, currentAttributes.copy()));
        }
    }

    public void insertEmptyLine() {
        if (!screen.isEmpty()) {
            scrollback.add(screen.removeFirst());
            if (scrollback.size() > scrollbackMax) {
                scrollback.removeFirst();
            }
        }

        screen.add(new Line(width, currentAttributes));
    }

    public void clearScreen() {
        screen.clear();
        for (int i = 0; i < height; i++)
            screen.add(new Line(width, currentAttributes));

        setCursor(0, 0);
    }

    public void clearAll() {
        scrollback.clear();
        clearScreen();
    }

    public Cell getCell(int row, int col) {
        int totalRows = scrollback.size() + screen.size();
        if (row < 0 || row >= totalRows || col < 0 || col >= width) {
            return null;
        }
        if (row < scrollback.size()) {
            return scrollback.get(row).get(col);
        }
        return screen.get(row - scrollback.size()).get(col);
    }

    public char getCharAt(int row, int col) {
        Cell cell = getCell(row, col);
        return cell != null ? cell.getCh() : ' ';
    }

    public Attributes getAttributesAt(int row, int col) {
        Cell cell = getCell(row, col);
        return cell != null ? cell.getAttributes() : null;
    }

    public String getLine(int row) {
        int totalRows = scrollback.size() + screen.size();
        if (row < 0 || row >= totalRows) {
            return null;
        }
        if (row < scrollback.size()) return scrollback.get(row).toString();
        return screen.get(row - scrollback.size()).toString();
    }

    public String getScreenText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < screen.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(screen.get(i).toString());
        }
        return sb.toString();
    }

    public String getFullText() {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Line sl : scrollback) {
            if (n++ > 0) sb.append("\n");
            sb.append(sl.toString());
        }
        for (Line sl : screen) {
            if (n++ > 0) sb.append("\n");
            sb.append(sl.toString());
        }
        return sb.toString();
    }

    private void scrollUp() {
        if (!screen.isEmpty()) {
            scrollback.add(screen.removeFirst());
            if (scrollback.size() > scrollbackMax) {
                scrollback.removeFirst();
            }
        }

        screen.add(new Line(width, currentAttributes));
    }
}
