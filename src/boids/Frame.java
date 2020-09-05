package boids;

public class Frame {
    private final Border leftBorder;
    private final Border rightBorder;
    private final Border upBorder;
    private final Border downBorder;

    public Frame(Border leftBorder, Border rightBorder, Border upBorder, Border downBorder) {
        this.leftBorder = leftBorder;
        this.rightBorder = rightBorder;
        this.upBorder = upBorder;
        this.downBorder = downBorder;
    }

    public Border getLeftBorder() {
        return leftBorder;
    }

    public Border getRightBorder() {
        return rightBorder;
    }

    public Border getUpBorder() {
        return upBorder;
    }

    public Border getDownBorder() {
        return downBorder;
    }

}
