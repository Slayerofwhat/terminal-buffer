import java.util.ArrayList;
import java.util.List;

public class Line {
    private List<Cell> cells = new ArrayList<>();

    public Line(int width, Attributes currentAttributes){
        for (int i = 0; i < width; i++) {
            cells.add(new Cell(' ', currentAttributes.copy()));
        }
    }

    public int length() {
        return cells.size();
    }

    public Cell get(int index) {
        return cells.get(index);
    }

    public void set(int index, Cell c) {
        cells.set(index, c);
    }

    public void insertChar(int index, Cell c, int maxWidth) {
        cells.add(index, c);
        if (cells.size() > maxWidth) {
            cells.removeLast();
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(cells.size());
        for (Cell c : cells) sb.append(c.getCh());
        return sb.toString();
    }
}
