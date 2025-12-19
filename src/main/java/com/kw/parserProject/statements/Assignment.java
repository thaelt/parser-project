package com.kw.parserProject.statements;

import com.kw.parserProject.utility.Printable;

public record Assignment(String writeVariable, Expression expression, int lineNumber) implements Statement, Printable {
    public Assignment(String writeVariable, Expression expression) {
        this(writeVariable, expression, -1);
    }

    @Override
    public String toString() {
        return print();
    }

    @Override
    public String print() {
        return writeVariable + " = " + expression.print();
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }
}
