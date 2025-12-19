package com.kw.parserProject;

import com.kw.parserProject.statements.*;
import com.kw.parserProject.tokens.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    Parser parser;

    @BeforeEach
    void setUp() {
        parser = new Parser();
    }


    @Test
    void shouldRecognizeAssignment() {
        // when
        List<Statement> statements = parser.parse(List.of(new VariableToken("x"), new AssignmentToken(), new ConstantToken("25")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());
        assertEquals(0, actualStatement.expression().readVariables().size());
    }

    @Test
    void shouldRecognizeAssignmentWithOperation() {
        // when
        List<Statement> statements = parser.parse(List.of(new VariableToken("x"), new AssignmentToken(),
                        new ConstantToken("25"), new OperatorToken("+"), new VariableToken("x")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());
        assertIterableEquals(List.of("x"), actualStatement.expression().readVariables());
    }

    @Test
    void shouldRecognizeAssignmentWithOperationAndNegativeNumber() {
        // when
        List<Statement> statements = parser.parse(List.of(new VariableToken("x"), new AssignmentToken(),
                        new OperatorToken("-"), new ConstantToken("25"), new OperatorToken("+"), new VariableToken("x")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());
        assertIterableEquals(List.of("x"), actualStatement.expression().readVariables());
        ValueExpression leftExpression = (ValueExpression) ((OperatorExpression) actualStatement.expression()).leftExpression();
        assertEquals(-25f, leftExpression.value());
    }

    @Test
    void shouldRecognizeAssignmentWithOperationAndNegativeNumberCase2() {
        // when
        List<Statement> statements = parser.parse(List.of(new VariableToken("x"), new AssignmentToken(),
                        new VariableToken("x"), new OperatorToken("*"), new OperatorToken("-"), new ConstantToken("25")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());
        assertIterableEquals(List.of("x"), actualStatement.expression().readVariables());
        ValueExpression leftExpression = (ValueExpression) ((OperatorExpression) actualStatement.expression()).rightExpression();
        assertEquals(-25f, leftExpression.value());
    }

    @Test
    void shouldRecognizeAssignmentWithOperationAndNegativeNumberCase3() {
        // when
        List<Statement> statements = parser.parse(List.of(new VariableToken("x"), new AssignmentToken(),
                        new VariableToken("x"), new OperatorToken("*"), new OperatorToken("-"), new ConstantToken("25.178")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());
        assertIterableEquals(List.of("x"), actualStatement.expression().readVariables());
        ValueExpression leftExpression = (ValueExpression) ((OperatorExpression) actualStatement.expression()).rightExpression();
        assertEquals(-25.178f, leftExpression.value());
    }

    @Test
    void shouldThrowWithWrongOperatorWhenConstantIsExpected() {
        // when
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> parser.parse(List.of(new VariableToken("x"), new AssignmentToken(),
                new VariableToken("x"), new OperatorToken("+"), new OperatorToken("/"), new ConstantToken("25"))));
        // then
        assertEquals("Expecting an expression, did not encounter valid one", illegalArgumentException.getMessage());
    }

    @Test
    void shouldRecognizeAssignmentWithChainOperation() {
        // when
        // x = y - x * 2
        List<Statement> statements = parser.parse(List.of(
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("y"),
                        new OperatorToken("-"), new VariableToken("x"), new OperatorToken("*"),
                        new ConstantToken("2")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        // y - rightExpression
        OperatorExpression topTierExpression = assertOperator(actualStatement.expression(), "-");
        assertVariableExpression(topTierExpression.leftExpression(), "y");

        // rightExpression -> x * 2
        OperatorExpression rightExpression = assertOperator(topTierExpression.rightExpression(), "*");
        assertVariableExpression(rightExpression.leftExpression(), "x");
        assertValueExpression(rightExpression.rightExpression(), 2);

        assertIterableEquals(List.of("x", "y"), actualStatement.expression().readVariables());

        assertIterableEquals(List.of("x", "y"), actualStatement.expression().readVariables());
    }

    @Test
    void shouldRecognizeAssignmentWithChainOperationWithOperatorBalancing() {
        // when
        // operator balancing not yet implemented fully
        // x = x * 2 - y
        List<Statement> statements = parser.parse(List.of(
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("x"),
                        new OperatorToken("*"), new ConstantToken("2"),
                        new OperatorToken("-"), new VariableToken("y")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        // leftExpression - 2
        OperatorExpression topTierExpression = assertOperator(actualStatement.expression(), "-");
        assertVariableExpression(topTierExpression.rightExpression(), "y");

        // leftExpression -> x * 2
        OperatorExpression leftExpression = assertOperator(topTierExpression.leftExpression(), "*");
        assertVariableExpression(leftExpression.leftExpression(), "x");
        assertValueExpression(leftExpression.rightExpression(), 2);

        assertIterableEquals(List.of("x", "y"), actualStatement.expression().readVariables());
    }

    @Test
    void shouldRecognizeAssignmentWithChainOperationAndBracketsWithOperatorBalancing() {
        // when
        // operator balancing not yet implemented fully
        // x = x * (2 - y)
        List<Statement> statements = parser.parse(List.of(
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("x"),
                        new OperatorToken("*"), new OpeningBracketToken(), new ConstantToken("2"),
                        new OperatorToken("-"), new VariableToken("y"), new ClosingBracketToken()))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        OperatorExpression topTierExpression = assertOperator(actualStatement.expression(), "*");
        // x * rightExpression
        assertVariableExpression(topTierExpression.leftExpression(), "x");
        BracketExpression bracketExpression = assertBracketExpression(topTierExpression.rightExpression());

        // rightExpression -> expressionInBracket -> ( 2 - y )
        OperatorExpression expressionInBrackets = assertOperator(bracketExpression.expressionInBrackets(), "-");
        // 2 - y
        assertValueExpression(expressionInBrackets.leftExpression(), 2);
        assertVariableExpression(expressionInBrackets.rightExpression(), "y");

        assertIterableEquals(List.of("x", "y"), actualStatement.expression().readVariables());
    }

    @Test
    void shouldRecognizeAssignmentWithMultipleMultiplicationsWithOperatorBalancing() {
        // when
        // operator balancing not yet implemented fully
        // x = 2 * x + y / 5
        List<Statement> statements = parser.parse(List.of(
                        new VariableToken("x"), new AssignmentToken(), new ConstantToken("2"),
                        new OperatorToken("*"), new VariableToken("x"), new OperatorToken("+"),
                        new VariableToken("y"), new OperatorToken("/"), new ConstantToken("5")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        OperatorExpression topTierExpression = assertOperator(actualStatement.expression(), "+");
        // leftExpression * rightExpression

        // leftExpression -> 2 * x
        OperatorExpression leftExpression = assertOperator(topTierExpression.leftExpression(), "*");

        assertValueExpression(leftExpression.leftExpression(), 2);
        assertVariableExpression(leftExpression.rightExpression(), "x");

        // rightExpression -> y / 5
        OperatorExpression rightExpression = assertOperator(topTierExpression.rightExpression(), "/");

        assertVariableExpression(rightExpression.leftExpression(), "y");
        assertValueExpression(rightExpression.rightExpression(), 5);

        assertIterableEquals(List.of("x", "y"), actualStatement.expression().readVariables());
    }

    @Test
    void shouldHandleMultipleMultiplicationsWithOperatorBalancing() {
        // when
        // operator balancing not yet implemented fully
        // x = x * y * z / a
        List<Statement> statements = parser.parse(List.of(
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("x"),
                        new OperatorToken("*"), new VariableToken("y"), new OperatorToken("*"),
                        new VariableToken("z"), new OperatorToken("/"), new VariableToken("a")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        OperatorExpression topTierExpression = assertOperator(actualStatement.expression(), "/");
        // leftExpression / a
        Expression rightExpression = topTierExpression.rightExpression();
        assertVariableExpression(rightExpression, "a");

        // leftExpression -> x * y * z
        // x -> left, y * z -> right, due to rotation
        OperatorExpression secondLevelLeftExpression = assertOperator(topTierExpression.leftExpression(), "*");

        assertVariableExpression(secondLevelLeftExpression.leftExpression(), "x");
        OperatorExpression thirdTierExpression = assertOperator(secondLevelLeftExpression.rightExpression(), "*");
        assertVariableExpression(thirdTierExpression.leftExpression(), "y");
        assertVariableExpression(thirdTierExpression.rightExpression(), "z");

        assertIterableEquals(List.of("a", "x", "y", "z"), actualStatement.expression().readVariables());
    }

    @Test
    void shouldHandleBracketsAndMultipleOperatorsWithOperatorBalancing() {
        // when
        // operator balancing not yet implemented fully
        // x = x * ( y + z ) / a
        List<Statement> statements = parser.parse(List.of(
                        new VariableToken("x"), new AssignmentToken(),
                        new VariableToken("x"), new OperatorToken("*"),
                        new OpeningBracketToken(), new VariableToken("y"), new OperatorToken("+"), new VariableToken("z"),
                        new ClosingBracketToken(), new OperatorToken("/"), new VariableToken("a")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        OperatorExpression topTierExpression = assertOperator(actualStatement.expression(), "/");
        // leftExpression / a

        Expression rightExpression = topTierExpression.rightExpression();
        assertVariableExpression(rightExpression, "a");

        // x * ( y + z )
        OperatorExpression secondLevelLeftExpression = assertOperator(topTierExpression.leftExpression(), "*");

        assertVariableExpression(secondLevelLeftExpression.leftExpression(), "x");
        BracketExpression thirdTierExpression = assertBracketExpression(secondLevelLeftExpression.rightExpression());

        OperatorExpression expressionInBracket = assertOperator(thirdTierExpression.expressionInBrackets(), "+");

        assertVariableExpression(expressionInBracket.leftExpression(), "y");
        assertVariableExpression(expressionInBracket.rightExpression(), "z");

        assertIterableEquals(List.of("a", "x", "y", "z"), actualStatement.expression().readVariables());
    }


    @Test
    void shouldRecognizeAssignmentReadingVariable() {
        // when
        List<Statement> statements = parser.parse(List.of(
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("y")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());
        assertEquals(1, actualStatement.expression().readVariables().size());

        assertEquals("y", actualStatement.expression().readVariables().getFirst());
    }

    @Test
    void shouldHandleIfStatementWithoutElseClause() {
        // when
        List<Statement> statements = parser.parse(List.of(new KeywordToken("if"),
                        new VariableToken("a"), new OperatorToken("<"), new ConstantToken("3"),
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("y"),
                        new KeywordToken("end")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(IfStatement.class, statements.getFirst());

        IfStatement actualStatement = (IfStatement) statements.getFirst();

        // if-clause - one assignment
        assertEquals(1, actualStatement.ifClauseStatements().size());

        // else clause - empty
        assertEquals(0, actualStatement.elseClauseStatements().size());

        // read variables from condition
        assertEquals(1, actualStatement.condition().readVariables().size());
        assertEquals("a", actualStatement.condition().readVariables().getFirst());
    }

    @Test
    void shouldHandleIfStatementWithElseClause() {
        // when
        List<Statement> statements = parser.parse(List.of(new KeywordToken("if"),
                        new VariableToken("a"), new OperatorToken("<"), new ConstantToken("3"),
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("y"),
                        new KeywordToken("else"),
                        new VariableToken("y"), new AssignmentToken(), new VariableToken("x"),
                        new KeywordToken("end")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(IfStatement.class, statements.getFirst());

        IfStatement actualStatement = (IfStatement) statements.getFirst();

        // if-clause - one assignment
        assertEquals(1, actualStatement.ifClauseStatements().size());

        // else clause - one assignment
        assertEquals(1, actualStatement.elseClauseStatements().size());

        // read variables from condition
        assertEquals(1, actualStatement.condition().readVariables().size());
        assertEquals("a", actualStatement.condition().readVariables().getFirst());
    }

    @Test
    void shouldHandleWhileStatement() {
        // when
        List<Statement> statements = parser.parse(List.of(new KeywordToken("while"),
                        new VariableToken("a"), new OperatorToken("<"), new ConstantToken("3"),
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("y"),
                        new KeywordToken("end")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(WhileStatement.class, statements.getFirst());

        WhileStatement actualStatement = (WhileStatement) statements.getFirst();

        assertEquals(1, actualStatement.statements().size());

        // read variables from condition
        assertEquals(1, actualStatement.condition().readVariables().size());
        assertEquals("a", actualStatement.condition().readVariables().getFirst());
    }

    @Test
    void shouldHandleLackOfEndKeywordInWhileStatement() {
        // when
        assertThrows(IllegalArgumentException.class, () -> parser.parse(List.of(new KeywordToken("while"), new VariableToken("a"), new OperatorToken("<"), new ConstantToken("3"), new VariableToken("x"), new AssignmentToken(), new VariableToken("y"))));
    }

    @Test
    void shouldHandleDifferentKeywordTokenInWhileStatement() {
        // when
        assertThrows(IllegalArgumentException.class, () -> parser.parse(List.of(new KeywordToken("while"), new VariableToken("a"), new OperatorToken("<"), new ConstantToken("3"), new VariableToken("x"), new AssignmentToken(), new VariableToken("y"), new KeywordToken("else"), new ConstantToken("2"), new KeywordToken("end"))));
    }

    @Test
    void shouldHandleDifferentTypeTokenInWhileStatement() {
        // something reasonable, but not parsable to expression
        // when
        assertThrows(IllegalArgumentException.class, () -> parser.parse(List.of(new KeywordToken("while"), new VariableToken("a"), new OperatorToken("<"), new ConstantToken("3"), new VariableToken("x"), new AssignmentToken(), new VariableToken("y"), new ConstantToken("2"), new KeywordToken("end"))));
    }

    @Test
    void shouldHandleBogusTokenAfterWhileStatement() {
        // something unreasonable, like closing bracket
        // when
        assertThrows(IllegalArgumentException.class, () -> parser.parse(List.of(new KeywordToken("while"), new ClosingBracketToken(), new AssignmentToken(), new VariableToken("y"))));
    }

    @Test
    void shouldHandleDeadSilenceAfterWhileStatement() {
        // when
        assertThrows(IllegalArgumentException.class, () -> parser.parse(List.of(new VariableToken("x"), new AssignmentToken(), new VariableToken("y"), new KeywordToken("while"))));
    }

    @Test
    void shouldHandleNonExpressionAfterWhileStatement() {
        // when
        assertThrows(IllegalArgumentException.class, () -> parser.parse(List.of(new KeywordToken("while"), new VariableToken("x"), new AssignmentToken(), new VariableToken("y"))));
    }

    @Test
    void shouldHandleEndingStatementListRandomly() {
        // fails due to not consuming all tokens; any non-statement can be used
        // when
        assertThrows(IllegalArgumentException.class, () -> parser.parse(List.of(new VariableToken("x"), new AssignmentToken(), new VariableToken("y"), new KeywordToken("end"))));
    }

    private static OperatorExpression assertOperator(Expression operatorExpression, String expected) {
        assertInstanceOf(OperatorExpression.class, operatorExpression);
        OperatorExpression expression = (OperatorExpression) operatorExpression;
        assertEquals(expected, expression.operator());
        return expression;
    }

    private static BracketExpression assertBracketExpression(Expression bracketExpression) {
        assertInstanceOf(BracketExpression.class, bracketExpression);
        return (BracketExpression) bracketExpression;
    }

    private static void assertValueExpression(Expression valueExpression, float expected) {
        assertInstanceOf(ValueExpression.class, valueExpression);
        assertEquals(expected, ((ValueExpression) valueExpression).value());
    }

    private static void assertVariableExpression(Expression variableExpression, String expected) {
        assertInstanceOf(VariableExpression.class, variableExpression);
        assertEquals(expected, ((VariableExpression) variableExpression).variable());
    }

}