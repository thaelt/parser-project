package com.kw.parserProject.statements;

import java.util.List;

public class WhileStatement extends Statement {
    Expression condition;
    List<Statement> statements;
    public WhileStatement(Expression condition, List<Statement> statements, String content) {
        super(content);
        this.condition = condition;
        this.statements = statements;
    }

    @Override
    public String writeVariable() {
        return null;
    }

    @Override
    public List<String> postVisitReadVariable() {
        return condition.readVariables();
    }

    @Override
    public List<String> readVariables() {
        return List.of();
    }

    @Override
    public List<Statement> subStatements() {
        return statements;
    }
}