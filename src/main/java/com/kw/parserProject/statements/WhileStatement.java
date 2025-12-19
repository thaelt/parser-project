package com.kw.parserProject.statements;

import java.util.List;

public record WhileStatement(Expression condition, List<Statement> statements, int lineNumber) implements Statement {
    public WhileStatement(Expression condition, List<Statement> statements) {
        this(condition, statements, -1);
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }
}