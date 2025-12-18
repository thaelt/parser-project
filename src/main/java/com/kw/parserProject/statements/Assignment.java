package com.kw.parserProject.statements;

public final class Assignment implements Statement {
    private final String writeVariable;
    private final Expression expression;
    private final String content;

    public Assignment(String writeVariable, Expression expression, String content) {
        this.content = content;
        this.writeVariable = writeVariable;
        this.expression = expression;
    }

    public String getWriteVariable() {
        return writeVariable;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return content;
    }
}
