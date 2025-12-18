package com.kw.parserProject;

import com.kw.parserProject.statements.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnusedStatementCheckerTest {

    UnusedStatementChecker unusedStatementChecker;

    // commonly defined statements/conditions:
    Assignment xIsFive = new Assignment("x", new Expression(5), "x = 5");
    Assignment xIsSix = new Assignment("x", new Expression(6), "x = 6");
    Assignment yIsXPlusTwo = new Assignment("y", new Expression(new Expression("x"), "+", new Expression(2)), "y = x + 2");
    Assignment zIsXPlusThree = new Assignment("z", new Expression(new Expression("x"), "+", new Expression(3)), "z = x + 3");
    Expression xLessThanFive = new Expression(new Expression("x"), "<", new Expression(5));
    Expression zLessThanFive = new Expression(new Expression("z"), "<", new Expression(5));

    @BeforeEach
    void setUp() {
        unusedStatementChecker = new UnusedStatementChecker();
    }

    @Test
    void shouldHandleSingleAssignment() {
        // given
        List<Statement> statements = List.of(
                xIsFive
        );

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(statements);

        // then
        assertEquals(1, results.size());
        assertEquals(xIsFive, results.getFirst());
    }

    @Test
    void shouldPickAllAssignmentsForSameVariable() {
        // given
        List<Statement> statements = List.of(
                xIsFive, xIsSix
        );

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(statements);

        // then
        assertEquals(2, results.size());
        assertEquals(xIsFive, results.getFirst());
        assertEquals(xIsSix, results.get(1));
    }

    @Test
    void shouldPickOnlyLastUnusedAssignmentIfVariableIsReadSeveralTimes() {
        // given
        List<Statement> statements = List.of(
                xIsFive, xIsSix, yIsXPlusTwo, zIsXPlusThree
        );

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(statements);

        // then
        assertEquals(3, results.size());
        assertEquals(xIsFive, results.getFirst());
        assertEquals(yIsXPlusTwo, results.get(1));
        assertEquals(zIsXPlusThree, results.get(2));
    }

    @Test
    void shouldHandleStatementDefinedTwiceIfNeverRead() {
        // given
        List<Statement> statements = List.of(
                xIsFive, xIsFive
        );

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(statements);

        // then
        assertEquals(2, results.size());
        assertEquals(xIsFive, results.getFirst());
        assertEquals(xIsFive, results.get(1));
    }

    @Test
    void shouldHandleStatementDefinedTwiceIfRead() {
        // given
        List<Statement> statements = List.of(
                xIsFive, xIsFive, yIsXPlusTwo
        );

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(statements);

        // then
        assertEquals(2, results.size());
        assertEquals(xIsFive, results.getFirst());
        assertEquals(yIsXPlusTwo, results.get(1));
        // adding line numbers or other metadata might be a good next step to find exactly which one is unused in this case
    }

    @Test
    void shouldMarkVariableUsedInWhileConditionAsRead() {
        // given
        Statement whileStatement = new WhileStatement(xLessThanFive, List.of(xIsFive), "while (x < 5) x=5 end");

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(List.of(whileStatement));

        // then
        assertEquals(0, results.size());
    }

    @Test
    void shouldMarkUnusedAssignmentFromWhileLoop() {
        // given
        Statement whileStatement = new WhileStatement(zLessThanFive, List.of(xIsFive), "while (z < 5) x=5 end");

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(List.of(whileStatement));

        // then
        assertEquals(1, results.size());
        assertEquals(xIsFive, results.getFirst());
    }

    @Test
    void shouldHandleIfStatement() {
        // given
        Statement ifStatement = new IfStatement(zLessThanFive, List.of(xIsFive), List.of(), "if (z<5) x=5 end");

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(List.of(ifStatement));

        // then
        assertEquals(1, results.size());
        assertEquals(xIsFive, results.getFirst());
    }

    @Test
    void shouldHandleIfElseStatement() {
        // given
        Statement ifStatement = new IfStatement(zLessThanFive, List.of(xIsFive), List.of(xIsSix), "if (z<5) x=5 else x=6 end");

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(List.of(ifStatement));

        // then
        assertEquals(2, results.size());
        assertEquals(xIsFive, results.getFirst());
        assertEquals(xIsSix, results.get(1));
    }

    @Test
    void shouldHandleUsingVariableFromIfElseStatement() {
        // given
        Statement ifStatement = new IfStatement(zLessThanFive, List.of(xIsFive), List.of(xIsSix), "if (z<5) x=5 else x=6 end");

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(List.of(ifStatement, yIsXPlusTwo));

        // then
        assertEquals(1, results.size());
        assertEquals(yIsXPlusTwo, results.getFirst());
    }


}