public class Cell {
    private char ch;
    private Attributes attributes;

    public Cell(char ch, Attributes attributes) {
        this.ch = ch;
        this.attributes = attributes;
    }

    public char getCh() {
        return ch;
    }

    public void setCh(char ch) {
        this.ch = ch;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }
}
