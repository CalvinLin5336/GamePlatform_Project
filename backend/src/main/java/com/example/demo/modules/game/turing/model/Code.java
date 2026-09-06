package com.example.demo.modules.game.turing.model;

import java.util.Objects;

public class Code {
    private Integer blue;   
    private Integer yellow; 
    private Integer purple; 

    public Code() {
    }

    public Code(Integer blue, Integer yellow, Integer purple) {
        setBlue(blue);
        setYellow(yellow);
        setPurple(purple);
    }

    public Integer getBlue() { return blue; }
    public void setBlue(Integer blue) {
        if (blue != null && (blue < 1 || blue > 5)) {
            throw new IllegalArgumentException("Blue digit must be between 1 and 5");
        }
        this.blue = blue;
    }

    public Integer getYellow() { return yellow; }
    public void setYellow(Integer yellow) {
        if (yellow != null && (yellow < 1 || yellow > 5)) {
            throw new IllegalArgumentException("Yellow digit must be between 1 and 5");
        }
        this.yellow = yellow;
    }

    public Integer getPurple() { return purple; }
    public void setPurple(Integer purple) {
        if (purple != null && (purple < 1 || purple > 5)) {
            throw new IllegalArgumentException("Purple digit must be between 1 and 5");
        }
        this.purple = purple;
    }

    public int getDigit(char color) {
        int b = (blue != null) ? blue : 0;
        int y = (yellow != null) ? yellow : 0;
        int p = (purple != null) ? purple : 0;
        switch (Character.toUpperCase(color)) {
            case 'B': case 'T': return b;
            case 'Y': case 'S': return y;
            case 'P': case 'C': return p;
            default: throw new IllegalArgumentException("Invalid digit color reference: " + color);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Code code = (Code) o;
        return Objects.equals(blue, code.blue) &&
               Objects.equals(yellow, code.yellow) &&
               Objects.equals(purple, code.purple);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blue, yellow, purple);
    }

    @Override
    public String toString() {
        return "" + (blue != null ? blue : "") + (yellow != null ? yellow : "") + (purple != null ? purple : "");
    }
}