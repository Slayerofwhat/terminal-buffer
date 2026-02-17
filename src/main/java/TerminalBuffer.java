import java.util.ArrayList;
import java.util.List;

public class TerminalBuffer {
    private int width;
    private int height;
    private int scrollbackMax;

    private List<Line> screen = new ArrayList<>();
    private List<Line> scrollback = new ArrayList<>();

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

    public void setCursorRow(int cursorRow) {
        this.cursorRow = cursorRow;
    }

    public int getCursorColumn() {
        return cursorColumn;
    }

    public void setCursorColumn(int cursorColumn) {
        this.cursorColumn = cursorColumn;
    }
}
