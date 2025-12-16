package com.kw.parserProject.statements;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class IfStatement extends Statement {
    Expression condition;
    List<Statement> ifClauseStatements;
    List<Statement> elseClauseStatements;
    public IfStatement(Expression condition, List<Statement> ifClauseStatements, List<Statement> elseClauseStatements, String content) {
        super(content);
        this.condition = condition;
        this.ifClauseStatements = ifClauseStatements;
        this.elseClauseStatements = elseClauseStatements;
    }


    @Override
    public String writeVariable() {
        return null;
    }

    @Override
    public List<String> postVisitReadVariable() {
        return List.of();
    }

    @Override
    public List<String> readVariables() {
        return condition.readVariables();
    }

    @Override
    public List<Statement> subStatements() {
        return Stream.of(ifClauseStatements, elseClauseStatements).flatMap(Collection::stream).toList();
    }
}
