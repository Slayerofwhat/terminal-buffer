public class Attributes {
    private Color backgroundColor = Color.DEFAULT;
    private Color foregroundColor = Color.DEFAULT;
    private boolean isBold = false;
    private boolean isItalic = false;
    private boolean isUnderline = false;

    public Attributes() {}

    public Attributes(Color backgroundColor, Color foregroundColor, boolean isBold, boolean isItalic, boolean isUnderline) {
        this.backgroundColor = backgroundColor;
        this.foregroundColor = foregroundColor;
        this.isBold = isBold;
        this.isItalic = isItalic;
        this.isUnderline = isUnderline;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public Color getForegroundColor() {
        return foregroundColor;
    }

    public void setForegroundColor(Color foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    public boolean isBold() {
        return isBold;
    }

    public void setBold(boolean bold) {
        isBold = bold;
    }

    public boolean isItalic() {
        return isItalic;
    }

    public void setItalic(boolean italic) {
        isItalic = italic;
    }

    public boolean isUnderline() {
        return isUnderline;
    }

    public void setUnderline(boolean underline) {
        isUnderline = underline;
    }

    public Attributes copy(){
        return new Attributes(backgroundColor, foregroundColor, isBold, isItalic, isUnderline);
    }
}
