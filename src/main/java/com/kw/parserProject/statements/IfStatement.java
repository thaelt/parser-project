package com.kw.parserProject.statements;

import java.util.List;

public record IfStatement(Expression condition, List<Statement> ifClauseStatements,
                          List<Statement> elseClauseStatements, int lineNumber) implements Statement {
    public IfStatement(Expression condition, List<Statement> ifClauseStatements, List<Statement> elseClauseStatements) {
        this(condition, ifClauseStatements, elseClauseStatements, -1);
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }
}
