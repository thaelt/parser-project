package com.kw.parserProject;

import com.kw.parserProject.utility.Printable;

public enum Operator implements Printable {
    PLUS("+", 1),
    MINUS("-", 1),
    MULTIPLY("*", 2),
    DIVIDE("/", 2),
    LESS_THAN("<", 3),
    GREATER_THAN(">", 3);

    final String character;
    final int precedence; // higher - the better

    Operator(String printableCharacter, int precedence) {
        this.character = printableCharacter;
        this.precedence = precedence;
    }

    @Override
    public String print() {
        return character;
    }

    static Operator resolve(char sign) {
        return switch (sign) {
            case '+' -> PLUS;
            case '-' -> MINUS;
            case '*' -> MULTIPLY;
            case '/' -> DIVIDE;
            case '<' -> LESS_THAN;
            case '>' -> GREATER_THAN;
            default -> null;
        };
    }
}
