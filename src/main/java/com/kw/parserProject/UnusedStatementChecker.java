package com.kw.parserProject;

import com.kw.parserProject.statements.Statement;

import java.util.*;
import java.util.stream.Stream;

public class UnusedStatementChecker {

    List<Statement> getUnusedStatements(List<Statement> program) {
        List<Statement> unusedOverwrittenAssignmentsFromStatements = new LinkedList<>();
        Map<String, Statement> recentAssignments = new HashMap<>();

        for (Statement parsedAssignment : program) {
            updateUnusedStatements(unusedOverwrittenAssignmentsFromStatements, recentAssignments, parsedAssignment);
        }

        return Stream.concat(
                        unusedOverwrittenAssignmentsFromStatements.stream(),
                        recentAssignments.values().stream()
                ).toList();
    }

    private void updateUnusedStatements(List<Statement> unusedStatements, Map<String, Statement> recentAssignments, Statement statement) {
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
                unusedStatements.add(previousStatement);
            }
            // let's populate recent assignments with the freshest entry
            recentAssignments.put(writeVariable, statement);
        }

        // and post visit options like while loops
        statement.postVisitReadVariable().forEach(recentAssignments::remove);
    }
}
