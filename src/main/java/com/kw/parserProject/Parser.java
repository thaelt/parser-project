package com.kw.parserProject;

import com.kw.parserProject.statements.*;
import com.kw.parserProject.tokens.*;
import com.kw.parserProject.utility.ReadResults;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Parser {

    private static final List<Character> RECOGNIZED_OPERATORS = List.of('+', '-', '*', '/', '<', '>');
    private static final List<String> RECOGNIZED_KEYWORDS = List.of("while", "else", "end", "if");

    List<Statement> parse(String programCode) {
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
        // variable = expression
        // if expression statementList end
        // if expression statementList else statementList end
        // while expression statementList end

        Token token = tryReadingToken(tokens, startIndex);
        if (token instanceof KeywordToken) {
            if ("if".equals(token.data)) {
                ReadResults<Integer, Expression> tryReadingIfCondition = readExpressionWithBrackets(tokens, startIndex + 1);
                if (tryReadingIfCondition.nextIndex() == -1) {
                    throw new IllegalStateException("Bad IF statement");
                }
                ReadResults<Integer, List<Statement>> readStatementListIfClauseResults = readStatementList(tokens, tryReadingIfCondition.nextIndex());
                int endStatementIndex = readStatementListIfClauseResults.nextIndex();
                // expect a keyword
                if (endStatementIndex == -1) {
                    throw new IllegalStateException("Couldn't read statements");
                }
                Token secondToken = tokens.get(endStatementIndex);
                if (!(secondToken instanceof KeywordToken)) {
                    throw new IllegalStateException("Bad IF statement");
                }

                if ("else".equals(secondToken.data)) {
                    ReadResults<Integer, List<Statement>> readStatementListResults = readStatementList(tokens, endStatementIndex + 1);
                    int elseIfStatementIndex = readStatementListResults.nextIndex();
                    if (elseIfStatementIndex == -1) {
                        throw new IllegalStateException("BAD ELSEIF");
                    }
                    Token endKeyword = tokens.get(elseIfStatementIndex);
                    if (!("end".equals(endKeyword.data))) {
                        throw new IllegalStateException("No end keyword after if-else");
                    }
                    Statement statement = new IfStatement(tryReadingIfCondition.value(), readStatementListIfClauseResults.value(), readStatementListResults.value(), tokens.subList(startIndex, elseIfStatementIndex + 1).toString());
                    return new ReadResults<>(elseIfStatementIndex + 1, statement);
                } else if (("end".equals(secondToken.data))) {
                    Statement ifStatement = new IfStatement(tryReadingIfCondition.value(), readStatementListIfClauseResults.value(), List.of(), tokens.subList(startIndex, endStatementIndex + 1).toString());
                    return new ReadResults<>(endStatementIndex + 1, ifStatement);
                }
            } else if ("while".equals(token.data)) {
                ReadResults<Integer, Expression> i = readExpressionWithBrackets(tokens, startIndex + 1);
                if (i.nextIndex() == -1) {
                    throw new IllegalStateException("Bad while statement");
                }
                //statementList

                ReadResults<Integer, List<Statement>> readStatementListResults = readStatementList(tokens, i.nextIndex());
                int elseIfStatementIndex = readStatementListResults.nextIndex();
                if (elseIfStatementIndex == -1) {
                    throw new IllegalStateException("BAD ELSEIF");
                }
                // end keyword
                Token endKeyword = tokens.get(elseIfStatementIndex);
                if (!("end".equals(endKeyword.data))) {
                    throw new IllegalStateException("No end keyword after while");
                }

                Statement whileStatement = new WhileStatement(i.value(), readStatementListResults.value(), tokens.subList(startIndex, elseIfStatementIndex + 1).toString());
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
        // variable = expression
        // if expression statementList end
        // if expression statementList else statementList end
        // while expression statementList end

        Token token = tokens.get(startIndex);
        if (token instanceof VariableToken) {
            Token assignmentToken = tokens.get(startIndex + 1);
            if (!(assignmentToken instanceof AssignmentToken)) {
                throw new IllegalStateException("Expecting = sign");
            }

            ReadResults<Integer, Expression> cursor = readExpressionWithBrackets(tokens, startIndex + 2);
            if (cursor.nextIndex() == -1) {
                throw new IllegalStateException("Couldn't read the expression to assign");
            }

            Statement statement = new Assignment(token.data, cursor.value(), tokens.subList(startIndex, cursor.nextIndex()).stream().map(Token::toString).collect(Collectors.joining()));
            return new ReadResults<>(cursor.nextIndex(), statement);
        }
        return new ReadResults<>(-1, null);
    }

    private List<String> findVariableTokensUsed(List<Token> tokens) {
        return tokens.stream()
                .filter(token -> token instanceof VariableToken)
                .map(token -> token.data)
                .toList();
    }

    private ReadResults<Integer, Expression> readExpressionWithBrackets(List<Token> tokens, int startIndex) {
        Token token = tokens.get(startIndex);
        if (token instanceof OpeningBracketToken) {
            ReadResults<Integer, Expression> cursor = readExpressionWithBrackets(tokens, startIndex + 1);
            if (!(tokens.get(cursor.nextIndex()) instanceof ClosingBracketToken)) {
                throw new IllegalStateException("No closing brackets");
            }
            List<String> variableTokensUsed = findVariableTokensUsed(tokens.subList(startIndex, cursor.nextIndex()));
            Expression e = new Expression(variableTokensUsed, tokens.subList(startIndex, cursor.nextIndex()).toString());
            return new ReadResults<>(cursor.nextIndex() + 1, e);
        }
        return readExpression(tokens, startIndex);
    }

    private ReadResults<Integer, Expression> readExpression(List<Token> tokens, int startIndex) {
        Token token = tryReadingToken(tokens, startIndex);
        if (token instanceof VariableToken || token instanceof ConstantToken) {
            // we're good

            Token nextToken = tryReadingToken(tokens, startIndex + 1);
            if (nextToken instanceof OperatorToken) {
                // call readExpressionWithBrackets
                ReadResults<Integer, Expression> endIndex = readExpressionWithBrackets(tokens, startIndex + 2);
                // merge
                // rethink BODMAS rules here when merging - it doesn't matter for recognizing usages, but might be required to help with dead code pruning
                List<String> variableTokensUsed = findVariableTokensUsed(tokens.subList(startIndex, endIndex.nextIndex()));
                Expression e = new Expression(variableTokensUsed, tokens.subList(startIndex, endIndex.nextIndex()).toString());
                return new ReadResults<>(endIndex.nextIndex(), e);
            } else {
                List<String> variableTokensUsed = findVariableTokensUsed(tokens.subList(startIndex, startIndex + 1));
                Expression e = new Expression(variableTokensUsed, tokens.subList(startIndex, startIndex + 1).toString());
                return new ReadResults<>(startIndex + 1, e);
            }
        }
        return new ReadResults<>(-1, null);
    }

    private Token tryReadingToken(List<Token> tokens, int index) {
        if (index >= tokens.size()) {
            return null;
        }
        return tokens.get(index);
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
}
