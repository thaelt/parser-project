package com.kw.parserProject;

import com.kw.parserProject.statements.Statement;

import java.util.*;
import java.util.stream.Stream;

public class UnusedStatementChecker {

    List<Statement> getUnusedStatements(List<Statement> program) {
        Map<String, Stack<Statement>> unusedOverwrittenAssignmentsFromStatements = new HashMap<>();
        Map<String, Statement> recentAssignments = new HashMap<>();

        for (Statement parsedAssignment : program) {
            updateUnusedStatements(unusedOverwrittenAssignmentsFromStatements, recentAssignments, parsedAssignment);
        }

        return Stream.concat(unusedOverwrittenAssignmentsFromStatements.values().stream().filter(stack -> !stack.isEmpty()).flatMap(Collection::stream),
                        recentAssignments.values().stream())
                .toList();
    }

    void updateUnusedStatements(Map<String, Stack<Statement>> unusedStatements, Map<String, Statement> recentAssignments, Statement statement) {
        //TODO can't use stack alone, as smth like:
        // b = 1
        // b = 2
        // a = b + 1
        // c = b + 2
        // will clear two writes of b, and it should only one. Write test against it

        // let's start pre-traversal first
        // if variable not in recent assignments, nothing to do, if it's there - let's clear the flag
        statement.readVariables().forEach(recentAssignments::remove);

        // then, any substatements - handle recursively
        statement.subStatements().forEach(subStatement -> updateUnusedStatements(unusedStatements, recentAssignments, subStatement));

        // if variable is written to, mark it
        String writeVariable = statement.writeVariable();
        if (Objects.nonNull(writeVariable)) {
            Statement previousStatement = recentAssignments.get(writeVariable);
            if (previousStatement != null) {
                // value was written to, but never read up to this point, let's store it
                unusedStatements.computeIfAbsent(statement.writeVariable(), (_) -> new Stack<>()).push(previousStatement);
            }
            // let's populate recent assignments with the freshest entry
            recentAssignments.put(writeVariable, statement);
        }

        // and post visit options like while loops
        statement.postVisitReadVariable().forEach(recentAssignments::remove);
    }
}
