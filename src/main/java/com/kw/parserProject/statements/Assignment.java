package com.kw.parserProject.statements;

import java.util.List;

public class Assignment extends Statement {
    String writeVariable;
    Expression expression;
    public Assignment(String writeVariable, Expression expression, String content) {
        super(content);
        this.writeVariable = writeVariable;
        this.expression = expression;
    }

    @Override
    public String writeVariable() {
        return writeVariable;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public List<String> postVisitReadVariable() {
        return List.of();
    }

    @Override
    public List<String> readVariables() {
        return expression.readVariables();
    }

    @Override
    public List<Statement> subStatements() {
        return List.of();
    }
}
