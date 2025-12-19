package com.kw.parserProject.statements;

public record Assignment(String writeVariable, Expression expression, int lineNumber) implements Statement {
    public Assignment(String writeVariable, Expression expression) {
        this(writeVariable, expression, -1);
    }

    @Override
    public String toString() {
        return print();
    }

    String print() {
        return writeVariable + " = " + expression.print();
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }
}
