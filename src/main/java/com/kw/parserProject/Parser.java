package com.kw.parserProject;

import com.kw.parserProject.statements.*;
import com.kw.parserProject.tokens.*;
import com.kw.parserProject.utility.ReadResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Parser {

    public static final ReadResults<Integer, Expression> EXPRESSION_NOT_FOUND = new ReadResults<>(-1, null);
    public static final ReadResults<Integer, Statement> STATEMENT_NOT_FOUND = new ReadResults<>(-1, null);

    public Program parse(List<Token> tokens) {
        ReadResults<Integer, List<Statement>> program = readStatementList(new ArrayList<>(tokens), 0);
        if (program.nextIndex() != tokens.size()) {
            throw new IllegalArgumentException("Did not manage to consume all tokens");
        }

        return new Program(program.value());
    }

    private ReadResults<Integer, List<Statement>> readStatementList(List<Token> tokens, int startIndex) {
        List<Statement> statements = new ArrayList<>();

        int previousSuccessIndex = -1;
        var res = readStatement(tokens, startIndex);
        while (res.nextIndex() != -1) {
            statements.add(res.value());
            previousSuccessIndex = res.nextIndex();
            res = readStatement(tokens, res.nextIndex());
        }
        if (statements.isEmpty()) {
            throw new IllegalArgumentException("Expecting at least one statement");
        }
        return new ReadResults<>(previousSuccessIndex, statements);
    }

    private ReadResults<Integer, Statement> readStatement(List<Token> tokens, int startIndex) {
        Token token = tryReadingToken(tokens, startIndex);

        return switch (token) {
            case KeywordToken keywordToken -> handleKeywordToken(tokens, startIndex, keywordToken);
            case VariableToken variableToken -> handleAssignmentStatement(tokens, startIndex, variableToken);
            case null, default -> STATEMENT_NOT_FOUND;
        };
    }

    private ReadResults<Integer, Statement> handleKeywordToken(List<Token> tokens, int startIndex, KeywordToken token) {
        return switch (token.data) {
            case "if" -> handleIfStatement(tokens, startIndex, token);
            case "while" -> handleWhileStatement(tokens, startIndex, token);
            case null, default -> STATEMENT_NOT_FOUND;
        };
    }

    private ReadResults<Integer, Statement> handleWhileStatement(List<Token> tokens, int startIndex, KeywordToken token) {
        ReadResults<Integer, Expression> whileExpressionReadResults = readExpressionWithBrackets(tokens, startIndex + 1);
        assertReadSuccess(whileExpressionReadResults, "Expecting expression after 'while' keyword, did not encounter one");

        //statementList
        ReadResults<Integer, List<Statement>> readStatementListResults = readStatementList(tokens, whileExpressionReadResults.nextIndex());
        assertReadSuccess(readStatementListResults, "Expecting statements in 'while' loop, did not encounter one");

        int endStatementIndex = readStatementListResults.nextIndex();
        // end keyword
        KeywordToken endKeyword = tryReadingToken(tokens, endStatementIndex, KeywordToken.class);
        assertTokenIsEndKeyword(endKeyword);

        Statement whileStatement = new WhileStatement(whileExpressionReadResults.value(), readStatementListResults.value(), token.lineNumber);
        return new ReadResults<>(endStatementIndex + 1, whileStatement);
    }

    private ReadResults<Integer, Statement> handleIfStatement(List<Token> tokens, int startIndex, KeywordToken token) {
        ReadResults<Integer, Expression> tryReadingIfCondition = readExpressionWithBrackets(tokens, startIndex + 1);
        assertReadSuccess(tryReadingIfCondition, "Expecting condition in 'if' statement, did not encounter one");

        ReadResults<Integer, List<Statement>> readStatementListIfClauseResults = readStatementList(tokens, tryReadingIfCondition.nextIndex());
        assertReadSuccess(readStatementListIfClauseResults, "Expecting statements in 'if' section, did not encounter one");

        int potentialEndKeywordIndex = readStatementListIfClauseResults.nextIndex();
        KeywordToken nextKeywordToken = tryReadingToken(tokens, potentialEndKeywordIndex, KeywordToken.class);

        List<Statement> ifClauseStatements = readStatementListIfClauseResults.value();
        List<Statement> elseIfClauseStatements = List.of(); // default else clause is empty if undefined

        if ("else".equals(nextKeywordToken.data)) {
            ReadResults<Integer, List<Statement>> elseSectionReadResults = readStatementList(tokens, potentialEndKeywordIndex + 1);
            assertReadSuccess(elseSectionReadResults, "Expecting statements in 'else' section, did not encounter one");

            elseIfClauseStatements = elseSectionReadResults.value();
            potentialEndKeywordIndex = elseSectionReadResults.nextIndex();
        }
        KeywordToken _ = tryReadingToken(tokens, potentialEndKeywordIndex, KeywordToken.class);

        Statement statement = new IfStatement(tryReadingIfCondition.value(), ifClauseStatements, elseIfClauseStatements, token.lineNumber);
        return new ReadResults<>(potentialEndKeywordIndex + 1, statement);
    }

    private ReadResults<Integer, Statement> handleAssignmentStatement(List<Token> tokens, int startIndex, VariableToken variableToken) {
        AssignmentToken _ = tryReadingToken(tokens, startIndex + 1, AssignmentToken.class);

        ReadResults<Integer, Expression> assignmentReadResults = readExpressionWithBrackets(tokens, startIndex + 2);
        assertReadSuccess(assignmentReadResults, "Expecting expression to assign, did not encounter one");

        Statement statement = new Assignment(variableToken.data, assignmentReadResults.value(), variableToken.lineNumber);
        return new ReadResults<>(assignmentReadResults.nextIndex(), statement);
    }

    private ReadResults<Integer, Expression> readExpressionWithBrackets(List<Token> tokens, int startIndex) {
        Token token = tryReadingToken(tokens, startIndex);
        return switch (token) {
            case OpeningBracketToken _ -> handleOpeningBracket(tokens, startIndex);
            case VariableToken variableToken -> chainIfPossible(tokens, startIndex, wrapInExpression(variableToken));
            case ConstantToken constantToken -> chainIfPossible(tokens, startIndex, wrapInExpression(constantToken));
            case OperatorToken operatorToken -> handleOperatorToken(tokens, startIndex, operatorToken);
            case null, default -> EXPRESSION_NOT_FOUND;
        };
    }

    private ReadResults<Integer, Expression> handleOperatorToken(List<Token> tokens, int startIndex, OperatorToken token) {
        if (!Operator.MINUS.equals(token.getOperator())) {
            return EXPRESSION_NOT_FOUND;
        }

        ConstantToken constantToNegate = tryReadingToken(tokens, startIndex + 1, ConstantToken.class);
        ValueExpression valueExpression = new ValueExpression(Operator.MINUS.character + constantToNegate.data);
        return chainIfPossible(tokens, startIndex + 1, valueExpression);
    }

    private ReadResults<Integer, Expression> handleOpeningBracket(List<Token> tokens, int startIndex) {
        ReadResults<Integer, Expression> expressionReadResults = readExpressionWithBrackets(tokens, startIndex + 1);
        assertReadSuccess(expressionReadResults, "Expecting an expression in brackets, did not encounter one");

        ClosingBracketToken _ = tryReadingToken(tokens, expressionReadResults.nextIndex(), ClosingBracketToken.class);

        Expression expression = new BracketExpression(expressionReadResults.value());
        return chainIfPossible(tokens, expressionReadResults.nextIndex(), expression);
    }

    private ReadResults<Integer, Expression> chainIfPossible(List<Token> tokens, int startIndex, Expression expression) {
        int potentialLastTokenIndex = startIndex + 1;
        Token nextToken = tryReadingToken(tokens, potentialLastTokenIndex);
        if (nextToken instanceof OperatorToken operatorToken) {
            return chainExpressions(tokens, expression, potentialLastTokenIndex, operatorToken);
        }

        return new ReadResults<>(potentialLastTokenIndex, expression);
    }

    private ReadResults<Integer, Expression> chainExpressions(List<Token> tokens, Expression expression, int potentialLastTokenIndex, OperatorToken operatorToken) {
        ReadResults<Integer, Expression> endIndex = readExpressionWithBrackets(tokens, potentialLastTokenIndex + 1);
        assertReadSuccess(endIndex, "Expecting an expression, did not encounter valid one");

        // we're collecting chained assignments from right to left, due to recursion call.
        // it is not a problem with basic unused variable usage analysis as order of operation is not important for it.
        // however let's try fixing operator order
        Operator operator = operatorToken.getOperator();
        Expression rightSideExpression = endIndex.value();
        if (rightSideExpression instanceof OperatorExpression operatorExpression) {
            Expression rotatedExpression = rotateExpressions(expression, operator, operatorExpression);
            return new ReadResults<>(endIndex.nextIndex(), rotatedExpression);
        }

        Expression chainedExpression = new OperatorExpression(expression, operator, rightSideExpression);
        return new ReadResults<>(endIndex.nextIndex(), chainedExpression);
    }

    private static Expression rotateExpressions(Expression leftSideExpression, Operator operator, OperatorExpression rightSideExpression) {
        // push operator and left input argument as deep into the operator tree as possible and connect it with rightSideExpression's left predecessor
        // this will transform "left * (right + right2)" into "(left * right) + right2"
        // and should respect operator precedence and multiple chain levels (hopefully)

        Expression leftPointer = rightSideExpression;
        OperatorExpression parentPointer = null;

        while (leftPointer instanceof OperatorExpression ex
                && shouldRotateDueToOperatorPrecedence(operator, ex.operator())) {
            parentPointer = ex;
            leftPointer = ex.leftExpression();
        }

        // create new leaf node - attach left part of expression to the operator as a right node
        OperatorExpression newExpression = new OperatorExpression(leftSideExpression, operator, leftPointer);

        if (parentPointer != null) {
            parentPointer.setLeftExpression(newExpression);
            return rightSideExpression;
        }

        return newExpression;
    }

    private static boolean shouldRotateDueToOperatorPrecedence(Operator left, Operator right) {
        return left.precedence >= right.precedence;
    }

    private Expression wrapInExpression(VariableToken variableToken) {
        return new VariableExpression(variableToken.data);
    }

    private Expression wrapInExpression(ConstantToken constantToken) {
        return new ValueExpression(constantToken.data);
    }

    private Token tryReadingToken(List<Token> tokens, int index) {
        if (index >= tokens.size()) {
            return null;
        }
        return tokens.get(index);
    }

    private <T extends Token> T tryReadingToken(List<Token> tokens, int index, Class<T> tokenClass) {
        if (index >= tokens.size()) {
            throw new IllegalArgumentException("All tokens already consumed");
        }
        Token token = tokens.get(index);
        if (!tokenClass.isInstance(token)) {
            throw new IllegalArgumentException("Expected " + tokenClass.getName() + ", got " + (token == null ? "null" : token.getClass().toString()));
        }
        return tokenClass.cast(token);
    }

    private static void assertReadSuccess(ReadResults<Integer, ?> readingResult, String errorMessage) {
        if (readingResult.nextIndex() == -1) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static void assertTokenIsEndKeyword(KeywordToken tokenToCheck) {
        if (!Objects.equals(tokenToCheck.data, "end")) {
            throw new IllegalArgumentException("Expected a token with data: " + "end" + ", got " + tokenToCheck.data);
        }
    }
}
