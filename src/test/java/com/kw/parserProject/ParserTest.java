package com.kw.parserProject;

import com.kw.parserProject.statements.*;
import com.kw.parserProject.tokens.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.kw.parserProject.Operator.*;
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
        List<Statement> statements = parser.parse(List.of(
                new VariableToken("x"), new AssignmentToken(), new ConstantToken("25")))
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
                        new ConstantToken("25"), new OperatorToken(PLUS), new VariableToken("x")))
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
                        new OperatorToken(MINUS), new ConstantToken("25"), new OperatorToken(PLUS), new VariableToken("x")))
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
                        new VariableToken("x"), new OperatorToken(MULTIPLY), new OperatorToken(MINUS), new ConstantToken("25")))
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
                        new VariableToken("x"), new OperatorToken(MULTIPLY), new OperatorToken(MINUS), new ConstantToken("25.178")))
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
                new VariableToken("x"), new OperatorToken(PLUS), new OperatorToken(DIVIDE), new ConstantToken("25"))));
        // then
        assertEquals("Expecting an expression, did not encounter valid one", illegalArgumentException.getMessage());
    }

    @Test
    void shouldRecognizeAssignmentWithChainOperation() {
        // when
        // x = y - x * 2
        List<Statement> statements = parser.parse(List.of(
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("y"),
                        new OperatorToken(MINUS), new VariableToken("x"), new OperatorToken(MULTIPLY),
                        new ConstantToken("2")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        OperatorExpression topTierExpression = assertOperator(actualStatement.expression(), MINUS);
        assertVariableExpression(topTierExpression.leftExpression(), "y");

        OperatorExpression rightExpression = assertOperator(topTierExpression.rightExpression(), MULTIPLY);
        assertVariableExpression(rightExpression.leftExpression(), "x");
        assertValueExpression(rightExpression.rightExpression(), 2);

        assertIterableEquals(List.of("x", "y"), actualStatement.expression().readVariables());
    }

    @Test
    void shouldRecognizeAssignmentWithChainOperationWithOperatorBalancing() {
        // when
        // x = x * 2 - y
        List<Statement> statements = parser.parse(List.of(
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("x"),
                        new OperatorToken(MULTIPLY), new ConstantToken("2"),
                        new OperatorToken(MINUS), new VariableToken("y")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        OperatorExpression topTierExpression = assertOperator(actualStatement.expression(), MINUS);
        assertVariableExpression(topTierExpression.rightExpression(), "y");

        OperatorExpression leftExpression = assertOperator(topTierExpression.leftExpression(), MULTIPLY);
        assertVariableExpression(leftExpression.leftExpression(), "x");
        assertValueExpression(leftExpression.rightExpression(), 2);

        assertIterableEquals(List.of("x", "y"), actualStatement.expression().readVariables());
    }

    @Test
    void shouldRecognizeAssignmentWithChainOperationAndBracketsWithOperatorBalancing() {
        // when
        // x = x * (2 - y)
        List<Statement> statements = parser.parse(List.of(
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("x"),
                        new OperatorToken(MULTIPLY), new OpeningBracketToken(), new ConstantToken("2"),
                        new OperatorToken(MINUS), new VariableToken("y"), new ClosingBracketToken()))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        OperatorExpression topTierExpression = assertOperator(actualStatement.expression(), MULTIPLY);
        assertVariableExpression(topTierExpression.leftExpression(), "x");
        BracketExpression bracketExpression = assertBracketExpression(topTierExpression.rightExpression());

        OperatorExpression expressionInBrackets = assertOperator(bracketExpression.expressionInBrackets(), MINUS);
        assertValueExpression(expressionInBrackets.leftExpression(), 2);
        assertVariableExpression(expressionInBrackets.rightExpression(), "y");

        assertIterableEquals(List.of("x", "y"), actualStatement.expression().readVariables());
    }

    @Test
    void shouldRecognizeAssignmentWithMultipleMultiplicationsWithOperatorBalancing() {
        // when
        // x = 2 * x + y / 5
        List<Statement> statements = parser.parse(List.of(
                        new VariableToken("x"), new AssignmentToken(), new ConstantToken("2"),
                        new OperatorToken(MULTIPLY), new VariableToken("x"), new OperatorToken(PLUS),
                        new VariableToken("y"), new OperatorToken(DIVIDE), new ConstantToken("5")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        OperatorExpression topTierExpression = assertOperator(actualStatement.expression(), PLUS);
        OperatorExpression leftExpression = assertOperator(topTierExpression.leftExpression(), MULTIPLY);

        assertValueExpression(leftExpression.leftExpression(), 2);
        assertVariableExpression(leftExpression.rightExpression(), "x");

        OperatorExpression rightExpression = assertOperator(topTierExpression.rightExpression(), DIVIDE);

        assertVariableExpression(rightExpression.leftExpression(), "y");
        assertValueExpression(rightExpression.rightExpression(), 5);

        assertIterableEquals(List.of("x", "y"), actualStatement.expression().readVariables());
    }

    @Test
    void shouldHandleMultipleMultiplicationsWithOperatorBalancing() {
        // when
        // x = x * y * z / a
        List<Statement> statements = parser.parse(List.of(
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("x"),
                        new OperatorToken(MULTIPLY), new VariableToken("y"), new OperatorToken(MULTIPLY),
                        new VariableToken("z"), new OperatorToken(DIVIDE), new VariableToken("a")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        OperatorExpression topTierExpression = assertOperator(actualStatement.expression(), DIVIDE);
        Expression rightExpression = topTierExpression.rightExpression();
        assertVariableExpression(rightExpression, "a");

        OperatorExpression secondLevelLeftExpression = assertOperator(topTierExpression.leftExpression(), MULTIPLY);
        assertVariableExpression(secondLevelLeftExpression.rightExpression(), "z");
        OperatorExpression thirdTierExpression = assertOperator(secondLevelLeftExpression.leftExpression(), MULTIPLY);
        assertVariableExpression(thirdTierExpression.leftExpression(), "x");
        assertVariableExpression(thirdTierExpression.rightExpression(), "y");

        assertIterableEquals(List.of("a", "x", "y", "z"), actualStatement.expression().readVariables());
    }

    @Test
    void shouldHandleOperatorBalancingWithComparisonOperators() {
        // when
        // x = x < y * z > a
        List<Statement> statements = parser.parse(List.of(
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("x"),
                        new OperatorToken(LESS_THAN), new VariableToken("y"), new OperatorToken(MULTIPLY),
                        new VariableToken("z"), new OperatorToken(GREATER_THAN), new VariableToken("a")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        OperatorExpression topTierExpression = assertOperator(actualStatement.expression(), MULTIPLY);

        OperatorExpression secondLevelLeftExpression = assertOperator(topTierExpression.leftExpression(), LESS_THAN);
        assertVariableExpression(secondLevelLeftExpression.leftExpression(), "x");
        assertVariableExpression(secondLevelLeftExpression.rightExpression(), "y");

        OperatorExpression secondLevelRightExpression = assertOperator(topTierExpression.rightExpression(), GREATER_THAN);
        assertVariableExpression(secondLevelRightExpression.leftExpression(), "z");
        assertVariableExpression(secondLevelRightExpression.rightExpression(), "a");

        assertIterableEquals(List.of("a", "x", "y", "z"), actualStatement.expression().readVariables());
    }

    @Test
    void shouldHandleOperatorBalancingWithComparisonOperatorsCase2() {
        // when
        // x = x < y * z > a + 1
        List<Statement> statements = parser.parse(List.of(
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("x"),
                        new OperatorToken(LESS_THAN), new VariableToken("y"), new OperatorToken(MULTIPLY),
                        new VariableToken("z"), new OperatorToken(GREATER_THAN), new VariableToken("a"),
                        new OperatorToken(PLUS), new ConstantToken("1")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        OperatorExpression topTierExpression = assertOperator(actualStatement.expression(), PLUS);
        assertValueExpression(topTierExpression.rightExpression(), 1f);

        OperatorExpression secondLevelLeftExpression = assertOperator(topTierExpression.leftExpression(), MULTIPLY);
        OperatorExpression xLessThanY = assertOperator(secondLevelLeftExpression.leftExpression(), LESS_THAN);
        OperatorExpression zGreaterThanA = assertOperator(secondLevelLeftExpression.rightExpression(), GREATER_THAN);

        assertVariableExpression(xLessThanY.leftExpression(), "x");
        assertVariableExpression(xLessThanY.rightExpression(), "y");

        assertVariableExpression(zGreaterThanA.leftExpression(), "z");
        assertVariableExpression(zGreaterThanA.rightExpression(), "a");

        assertIterableEquals(List.of("a", "x", "y", "z"), actualStatement.expression().readVariables());
    }

    @Test
    void shouldHandleBracketsAndMultipleOperatorsWithOperatorBalancing() {
        // when
        // x = x * ( y + z ) / a
        List<Statement> statements = parser.parse(List.of(
                        new VariableToken("x"), new AssignmentToken(),
                        new VariableToken("x"), new OperatorToken(MULTIPLY),
                        new OpeningBracketToken(), new VariableToken("y"), new OperatorToken(PLUS), new VariableToken("z"),
                        new ClosingBracketToken(), new OperatorToken(DIVIDE), new VariableToken("a")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(Assignment.class, statements.getFirst());

        Assignment actualStatement = (Assignment) statements.getFirst();
        assertEquals("x", actualStatement.writeVariable());

        OperatorExpression topTierExpression = assertOperator(actualStatement.expression(), DIVIDE);

        Expression rightExpression = topTierExpression.rightExpression();
        assertVariableExpression(rightExpression, "a");

        OperatorExpression secondLevelLeftExpression = assertOperator(topTierExpression.leftExpression(), MULTIPLY);

        assertVariableExpression(secondLevelLeftExpression.leftExpression(), "x");
        BracketExpression thirdTierExpression = assertBracketExpression(secondLevelLeftExpression.rightExpression());

        OperatorExpression expressionInBracket = assertOperator(thirdTierExpression.expressionInBrackets(), PLUS);

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
                        new VariableToken("a"), new OperatorToken(LESS_THAN), new ConstantToken("3"),
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("y"),
                        new KeywordToken("end")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(IfStatement.class, statements.getFirst());

        IfStatement actualStatement = (IfStatement) statements.getFirst();

        assertEquals(1, actualStatement.ifClauseStatements().size());
        assertEquals(0, actualStatement.elseClauseStatements().size());

        assertEquals(1, actualStatement.condition().readVariables().size());
        assertEquals("a", actualStatement.condition().readVariables().getFirst());
    }

    @Test
    void shouldHandleIfStatementWithElseClause() {
        // when
        List<Statement> statements = parser.parse(List.of(new KeywordToken("if"),
                        new VariableToken("a"), new OperatorToken(LESS_THAN), new ConstantToken("3"),
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("y"),
                        new KeywordToken("else"),
                        new VariableToken("y"), new AssignmentToken(), new VariableToken("x"),
                        new KeywordToken("end")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(IfStatement.class, statements.getFirst());

        IfStatement actualStatement = (IfStatement) statements.getFirst();

        assertEquals(1, actualStatement.ifClauseStatements().size());
        assertEquals(1, actualStatement.elseClauseStatements().size());

        assertEquals("x", ((Assignment) actualStatement.ifClauseStatements().getFirst()).writeVariable());
        assertEquals("y", ((Assignment) actualStatement.ifClauseStatements().getFirst())
                .expression().readVariables().getFirst());
        assertEquals("y", ((Assignment) actualStatement.elseClauseStatements().getFirst()).writeVariable());
        assertEquals("x", ((Assignment) actualStatement.elseClauseStatements().getFirst())
                .expression().readVariables().getFirst());

        assertEquals(1, actualStatement.condition().readVariables().size());
        assertEquals("a", actualStatement.condition().readVariables().getFirst());
    }

    @Test
    void shouldHandleWhileStatement() {
        // when
        List<Statement> statements = parser.parse(List.of(new KeywordToken("while"),
                        new VariableToken("a"), new OperatorToken(LESS_THAN), new ConstantToken("3"),
                        new VariableToken("x"), new AssignmentToken(), new VariableToken("y"),
                        new KeywordToken("end")))
                .statements();

        // then
        assertEquals(1, statements.size());
        assertInstanceOf(WhileStatement.class, statements.getFirst());

        WhileStatement actualStatement = (WhileStatement) statements.getFirst();

        assertEquals(1, actualStatement.statements().size());

        assertEquals(1, actualStatement.condition().readVariables().size());
        assertEquals("a", actualStatement.condition().readVariables().getFirst());
    }

    @Test
    void shouldHandleLackOfEndKeywordInWhileStatement() {
        // when
        assertThrows(IllegalArgumentException.class, () -> parser.parse(List.of(new KeywordToken("while"), new VariableToken("a"), new OperatorToken(LESS_THAN), new ConstantToken("3"), new VariableToken("x"), new AssignmentToken(), new VariableToken("y"))));
    }

    @Test
    void shouldHandleDifferentKeywordTokenInWhileStatement() {
        // when
        assertThrows(IllegalArgumentException.class, () -> parser.parse(List.of(new KeywordToken("while"), new VariableToken("a"), new OperatorToken(LESS_THAN), new ConstantToken("3"), new VariableToken("x"), new AssignmentToken(), new VariableToken("y"), new KeywordToken("else"), new ConstantToken("2"), new KeywordToken("end"))));
    }

    @Test
    void shouldHandleDifferentTypeTokenInWhileStatement() {
        // something reasonable, but not parsable to expression
        // when
        assertThrows(IllegalArgumentException.class, () -> parser.parse(List.of(new KeywordToken("while"), new VariableToken("a"), new OperatorToken(LESS_THAN), new ConstantToken("3"), new VariableToken("x"), new AssignmentToken(), new VariableToken("y"), new ConstantToken("2"), new KeywordToken("end"))));
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
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> parser.parse(List.of(new VariableToken("x"), new AssignmentToken(), new VariableToken("y"), new KeywordToken("end"))));
        assertEquals("Did not manage to consume all tokens", exception.getMessage());
    }

    private static OperatorExpression assertOperator(Expression operatorExpression, Operator expected) {
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