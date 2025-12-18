package com.kw.parserProject.statements;

import java.util.List;

public record WhileStatement(Expression condition, List<Statement> statements) implements Statement {
}