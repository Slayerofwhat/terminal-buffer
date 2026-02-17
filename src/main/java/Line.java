import java.util.ArrayList;
import java.util.List;

public class Line {
    private List<Cell> cells = new ArrayList<>();

    public Line(int width, Attributes currentAttributes){
        for (int i = 0; i < width; i++) {
            cells.add(new Cell(' ', currentAttributes));
        }
    }
}
