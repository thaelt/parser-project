package com.kw.parserProject;

import com.kw.parserProject.statements.*;
import com.kw.parserProject.tokens.*;
import com.kw.parserProject.utility.ReadResults;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Parser {

    private static final List<Character> RECOGNIZED_OPERATORS = List.of('+', '-', '*', '/', '<', '>');
    private static final List<String> RECOGNIZED_KEYWORDS = List.of("while", "else", "end", "if");

    public List<Statement> parse(String programCode) {
        List<Token> tokens = extractTokens(programCode);
        ReadResults<Integer, List<Statement>> program = readStatementList(new LinkedList<>(tokens), 0);

        return program.value();
    }

    private List<Token> extractTokens(String programCode) {
        List<Token> tokens = new LinkedList<>();
        char[] charArray = programCode.toCharArray();
        int startingPos = 0;

        while (startingPos != -1) {
            startingPos = readNextToken(charArray, startingPos, tokens);
        }

        return tokens;
    }

    private ReadResults<Integer, List<Statement>> readStatementList(List<Token> tokens, int startIndex) {
        List<Statement> statements = new ArrayList<>();
        ReadResults<Integer, Statement> resultIndex = readConditionStatement(tokens, startIndex);
        int previousSuccessIndex = -1;
        while (resultIndex.nextIndex() != -1) {
            previousSuccessIndex = resultIndex.nextIndex();
            statements.add(resultIndex.value());
            resultIndex = readConditionStatement(tokens, resultIndex.nextIndex());
        }
        return new ReadResults<>(previousSuccessIndex, statements);
    }

    private ReadResults<Integer, Statement> readConditionStatement(List<Token> tokens, int startIndex) {
        Token token = tryReadingToken(tokens, startIndex);
        if (token instanceof KeywordToken) {
            if ("if".equals(token.data)) {
                ReadResults<Integer, Expression> tryReadingIfCondition = readExpressionWithBrackets(tokens, startIndex + 1);
                assertTokenIsPresent(tryReadingIfCondition, "Expecting condition in 'if' statement, did not encounter one");

                ReadResults<Integer, List<Statement>> readStatementListIfClauseResults = readStatementList(tokens, tryReadingIfCondition.nextIndex());
                assertTokenIsPresent(readStatementListIfClauseResults, "Expecting statements in 'if' section, did not encounter one");

                int potentialEndKeywordIndex = readStatementListIfClauseResults.nextIndex();
                Token secondToken = tokens.get(potentialEndKeywordIndex);
                assertTokenIsOfType(secondToken, KeywordToken.class);

                List<Statement> ifClauseStatements = readStatementListIfClauseResults.value();
                List<Statement> elseIfClauseStatements = List.of();

                if ("else".equals(secondToken.data)) {
                    ReadResults<Integer, List<Statement>> elseSectionReadResults = readStatementList(tokens, potentialEndKeywordIndex + 1);
                    assertTokenIsPresent(elseSectionReadResults, "Expecting statements in 'else' section, did not encounter one");

                    elseIfClauseStatements = elseSectionReadResults.value();
                    potentialEndKeywordIndex = elseSectionReadResults.nextIndex();
                }
                Token endKeyword = tokens.get(potentialEndKeywordIndex);
                assertTokenIsOfType(endKeyword, KeywordToken.class);
                assertTokenHasData(endKeyword, "end");

                Statement statement = new IfStatement(tryReadingIfCondition.value(), ifClauseStatements, elseIfClauseStatements, tokens.subList(startIndex, potentialEndKeywordIndex + 1).toString());
                return new ReadResults<>(potentialEndKeywordIndex + 1, statement);
            } else if ("while".equals(token.data)) {
                ReadResults<Integer, Expression> whileExpressionReadResults = readExpressionWithBrackets(tokens, startIndex + 1);
                assertTokenIsPresent(whileExpressionReadResults, "Expecting expression after 'while' keyword, did not encounter one");

                //statementList
                ReadResults<Integer, List<Statement>> readStatementListResults = readStatementList(tokens, whileExpressionReadResults.nextIndex());
                assertTokenIsPresent(readStatementListResults, "Expecting statements in 'while' loop, did not encounter one");

                int elseIfStatementIndex = readStatementListResults.nextIndex();
                // end keyword
                Token endKeyword = tokens.get(elseIfStatementIndex);
                assertTokenHasData(endKeyword, "end");

                Statement whileStatement = new WhileStatement(whileExpressionReadResults.value(), readStatementListResults.value(), tokens.subList(startIndex, elseIfStatementIndex + 1).toString());
                return new ReadResults<>(elseIfStatementIndex + 1, whileStatement);
            }
        } else if (token instanceof VariableToken) {
            // might be a simple assignment
            return readAssignmentStatement(tokens, startIndex);
        }
        // no token found or token of different type
        return new ReadResults<>(-1, null);
    }

    private ReadResults<Integer, Statement> readAssignmentStatement(List<Token> tokens, int startIndex) {
        Token token = tokens.get(startIndex);
        if (token instanceof VariableToken) {
            Token assignmentToken = tokens.get(startIndex + 1);
            assertTokenIsOfType(assignmentToken, AssignmentToken.class);

            ReadResults<Integer, Expression> assignmentReadResults = readExpressionWithBrackets(tokens, startIndex + 2);
            assertTokenIsPresent(assignmentReadResults, "Expecting expression to assign, did not encounter one");

            Statement statement = new Assignment(token.data, assignmentReadResults.value(), tokens.subList(startIndex, assignmentReadResults.nextIndex()).stream().map(Token::toString).collect(Collectors.joining()));
            return new ReadResults<>(assignmentReadResults.nextIndex(), statement);
        }
        return new ReadResults<>(-1, null);
    }

    private ReadResults<Integer, Expression> readExpressionWithBrackets(List<Token> tokens, int startIndex) {
        Token token = tokens.get(startIndex);
        if (token instanceof OpeningBracketToken) {
            ReadResults<Integer, Expression> expressionReadResults = readExpressionWithBrackets(tokens, startIndex + 1);
            assertTokenIsPresent(expressionReadResults, "Expecting an expression in brackets, did not encounter one");

            Token closingBracket = tokens.get(expressionReadResults.nextIndex());
            assertTokenIsOfType(closingBracket, ClosingBracketToken.class);

            List<String> variableTokensUsed = findVariableTokensUsed(tokens.subList(startIndex, expressionReadResults.nextIndex()));
            Expression e = new Expression(variableTokensUsed, tokens.subList(startIndex, expressionReadResults.nextIndex()).toString());
            return new ReadResults<>(expressionReadResults.nextIndex() + 1, e);
        }
        return readExpression(tokens, startIndex);
    }

    private ReadResults<Integer, Expression> readExpression(List<Token> tokens, int startIndex) {
        Token token = tryReadingToken(tokens, startIndex);
        if (token instanceof VariableToken || token instanceof ConstantToken) {
            int potentialLastTokenIndex = startIndex + 1;
            Token nextToken = tryReadingToken(tokens, startIndex + 1);
            if (nextToken instanceof OperatorToken) {
                ReadResults<Integer, Expression> endIndex = readExpressionWithBrackets(tokens, startIndex + 2);
                // to do merge, rethink BODMAS rules here - it doesn't matter for recognizing usages, but might be required to help with dead code pruning
                assertTokenIsPresent(endIndex, "Expecting an expression, did not encounter one");
                potentialLastTokenIndex = endIndex.nextIndex();
            }
            List<String> variableTokensUsed = findVariableTokensUsed(tokens.subList(startIndex, potentialLastTokenIndex));
            Expression e = new Expression(variableTokensUsed, tokens.subList(startIndex, potentialLastTokenIndex).toString());
            return new ReadResults<>(potentialLastTokenIndex, e);
        }
        return new ReadResults<>(-1, null);
    }

    private Token tryReadingToken(List<Token> tokens, int index) {
        if (index >= tokens.size()) {
            return null;
        }
        return tokens.get(index);
    }

    private List<String> findVariableTokensUsed(List<Token> tokens) {
        return tokens.stream()
                .filter(token -> token instanceof VariableToken)
                .map(token -> token.data)
                .toList();
    }

    private int readNextToken(char[] input, int startingPos, List<Token> tokens) {
        if (startingPos >= input.length) return -1;
        char character = input[startingPos];

        if (Character.isWhitespace(character)) {
            return startingPos + 1;
        }

        if (Character.isDigit(character)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(character);

            int i = startingPos + 1;
            while (i < input.length && Character.isDigit(input[i])) {
                stringBuilder.append(input[i]);
                i++;
            }

            tokens.add(new ConstantToken(stringBuilder.toString()));
            return i;
        }
        if (character >= 'a' && character <= 'z') {
            // check for reserved keywords
            int readKeywordResults = readReservedKeyword(input, startingPos, tokens);
            if (readKeywordResults != -1) return readKeywordResults;
            tokens.add(new VariableToken(Character.toString(character)));
            return startingPos + 1;
        }
        if (RECOGNIZED_OPERATORS.contains(character)) {
            tokens.add(new OperatorToken(Character.toString(character)));
            return startingPos + 1;
        }
        if (character == '=') {
            tokens.add(new AssignmentToken());
            return startingPos + 1;
        }
        if (character == '(') {
            tokens.add(new OpeningBracketToken());
            return startingPos + 1;
        }
        if (character == ')') {
            tokens.add(new ClosingBracketToken());
            return startingPos + 1;
        }
        return -1;
    }

    private int readReservedKeyword(char[] input, int startingPos, List<Token> tokens) {
        if (startingPos >= input.length) return -1;
        for (String recognizedKeyword : RECOGNIZED_KEYWORDS) {
            int keywordLength = recognizedKeyword.length();

            if (startingPos + keywordLength <= input.length) {
                String keyword = new String(input, startingPos, keywordLength);
                if (recognizedKeyword.equals(keyword)) {
                    tokens.add(new KeywordToken(recognizedKeyword));
                    return startingPos + keywordLength;
                }
            }
        }
        return -1;
    }

    private static void assertTokenIsPresent(ReadResults<Integer, ?> readingResult, String errorMessage) {
        if (readingResult.nextIndex() == -1) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static void assertTokenIsOfType(Token tokenToCheck, Class<? extends Token> tokenClass) {
        if (!tokenClass.isInstance(tokenToCheck)) {
            throw new IllegalArgumentException("Expected " + tokenClass.getName() + ", got " + tokenToCheck.getClass());
        }
    }

    private static void assertTokenHasData(Token tokenToCheck, String expectedData) {
        if (!Objects.equals(tokenToCheck.data, expectedData)) {
            throw new IllegalArgumentException("Expected a token with data: " + expectedData + ", got " + tokenToCheck.data);
        }
    }
}
