package com.kw.parserProject;

import com.kw.parserProject.statements.Statement;

import java.util.List;

public record Program(List<Statement> statements) {
}
