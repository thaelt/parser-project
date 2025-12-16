package com.kw.parserProject;

import com.kw.parserProject.statements.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnusedStatementCheckerTest {

    UnusedStatementChecker unusedStatementChecker;

    @BeforeEach
    void setUp() {
        unusedStatementChecker = new UnusedStatementChecker();
    }

    @Test
    void shouldHandleSingleAssignment() {
        // given
        Assignment xIsFive = new Assignment("x", new Expression(List.of(), "5"), "x = 5");
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
        Assignment xIsFive = new Assignment("x", new Expression(List.of(), "5"), "x = 5");
        Assignment xIsSix = new Assignment("x", new Expression(List.of(), "6"), "x = 6");
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
        Assignment xIsFive = new Assignment("x", new Expression(List.of(), "5"), "x = 5");
        Assignment xIsSix = new Assignment("x", new Expression(List.of(), "6"), "x = 6");
        Assignment yIsXPlusTwo = new Assignment("y", new Expression(List.of("x"), "x + 2"), "y = x + 2");
        Assignment zIsXPlusThree = new Assignment("z", new Expression(List.of("x"), "x + 3"), "z = x + 3");
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
        Assignment xIsFive = new Assignment("x", new Expression(List.of(), "5"), "x = 5");
        Assignment xIsFiveAgain = new Assignment("x", new Expression(List.of(), "5"), "x = 5");
        List<Statement> statements = List.of(
                xIsFive, xIsFiveAgain
        );

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(statements);

        // then
        assertEquals(2, results.size());
        assertEquals(xIsFive, results.getFirst());
        assertEquals(xIsFiveAgain, results.get(1));
    }

    @Test
    void shouldHandleStatementDefinedTwiceIfRead() {
        // given
        Assignment xIsFive = new Assignment("x", new Expression(List.of(), "5"), "x = 5");
        Assignment xIsFiveAgain = new Assignment("x", new Expression(List.of(), "5"), "x = 5");
        Assignment yIsXPlusTwo = new Assignment("y", new Expression(List.of("x"), "x + 2"), "y = x + 2");
        List<Statement> statements = List.of(
                xIsFive, xIsFiveAgain, yIsXPlusTwo
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
        Expression condition = new Expression(List.of("x"), "(x < 5)");
        Assignment xIsFive = new Assignment("x", new Expression(List.of(), "5"), "x = 5");

        Statement whileStatement = new WhileStatement(condition, List.of(xIsFive), "while (x < 5) x=5 end");

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(List.of(whileStatement));

        // then
        assertEquals(0, results.size());
    }

    @Test
    void shouldMarkUnusedAssignmentFromWhileLoop() {
        // given
        Expression condition = new Expression(List.of("z"), "(z < 5)");
        Assignment xIsFive = new Assignment("x", new Expression(List.of(), "5"), "x = 5");

        Statement whileStatement = new WhileStatement(condition, List.of(xIsFive), "while (x < 5) x=5 end");

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(List.of(whileStatement));

        // then
        assertEquals(1, results.size());
        assertEquals(xIsFive, results.getFirst());
    }

    @Test
    void shouldHandleIfStatement() {
        // given
        Expression condition = new Expression(List.of("z"), "(z < 5)");
        Assignment xIsFive = new Assignment("x", new Expression(List.of(), "5"), "x = 5");

        Statement ifStatement = new IfStatement(condition, List.of(xIsFive), List.of(), "if (z<5) x=5 end");

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(List.of(ifStatement));

        // then
        assertEquals(1, results.size());
        assertEquals(xIsFive, results.getFirst());
    }

    @Test
    void shouldHandleIfElseStatement() {
        // given
        Expression condition = new Expression(List.of("z"), "(z < 5)");
        Assignment xIsFive = new Assignment("x", new Expression(List.of(), "5"), "x = 5");
        Assignment xIsSix = new Assignment("x", new Expression(List.of(), "6"), "x = 6");

        Statement ifStatement = new IfStatement(condition, List.of(xIsFive), List.of(xIsSix), "if (z<5) x=5 else x=6 end");

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(List.of(ifStatement));

        // then
        assertEquals(2, results.size());
        assertEquals(xIsFive, results.getFirst());
        assertEquals(xIsSix, results.get(1));
    }


    @Disabled("Not yet implemented - requires better if statement handling or branch eval, possibly both")
    @Test
    void shouldHandleUsingVariableFromIfElseStatement() {
        // given
        Expression condition = new Expression(List.of("z"), "(z < 5)");
        Assignment xIsFive = new Assignment("x", new Expression(List.of(), "5"), "x = 5");
        Assignment xIsSix = new Assignment("x", new Expression(List.of(), "6"), "x = 6");
        Assignment yIsXPlusTwo = new Assignment("y", new Expression(List.of("x"), "x + 2"), "y = x + 2");

        Statement ifStatement = new IfStatement(condition, List.of(xIsFive), List.of(xIsSix), "if (z<5) x=5 else x=6 end");

        // when
        List<Statement> results = unusedStatementChecker.getUnusedStatements(List.of(ifStatement, yIsXPlusTwo));

        // then
        assertEquals(1, results.size());
        assertEquals(yIsXPlusTwo, results.getFirst());
    }


}