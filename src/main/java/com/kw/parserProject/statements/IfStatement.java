package com.kw.parserProject.statements;

import java.util.List;

public record IfStatement(Expression condition, List<Statement> ifClauseStatements,
                          List<Statement> elseClauseStatements) implements Statement {

}
