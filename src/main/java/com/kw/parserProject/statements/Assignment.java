package com.kw.parserProject.statements;

public record Assignment(String writeVariable, Expression expression) implements Statement {

    @Override
    public String toString() {
        return print();
    }

    String print() {
        return writeVariable + " = " + expression.print();
    }
}
