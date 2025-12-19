package com.kw.parserProject;

import com.kw.parserProject.tokens.*;

import java.util.LinkedList;
import java.util.List;

public class Lexer {
    private static final List<String> RECOGNIZED_KEYWORDS = List.of("while", "else", "end", "if");

    public List<Token> extractTokens(String programCode) {
        List<String> lines = programCode.lines().toList();
        List<Token> results = new LinkedList<>();
        for (int i = 0; i < lines.size(); i++) {
            List<Token> tokens = parseLine(lines.get(i));
            int lineNumber = i + 1;
            tokens.forEach(token -> token.addLineNumber(lineNumber));
            results.addAll(tokens);
        }
        return results;
    }

    List<Token> parseLine(String line) {
        List<Token> tokens = new LinkedList<>();

        char[] charArray = line.toCharArray();
        int startingPos = 0;

        while (startingPos != -1) {
            startingPos = readNextToken(charArray, startingPos, tokens);
        }

        return tokens;
    }

    private int readNextToken(char[] input, int startingPos, List<Token> tokens) {
        if (startingPos >= input.length) return -1;
        char character = input[startingPos];

        if (Character.isWhitespace(character)) {
            return startingPos + 1;
        }

        if (Character.isDigit(character)) {
            return readNumeric(input, startingPos, tokens);
        }
        if (character >= 'a' && character <= 'z') {
            // check for reserved keywords
            int readKeywordResults = readReservedKeyword(input, startingPos, tokens);
            if (readKeywordResults != -1) return readKeywordResults;
            String variableName = readIdentifier(input, startingPos);
            tokens.add(new VariableToken(variableName));
            return startingPos + 1;
        }
        Operator resolvedOperator = Operator.resolve(character);
        if (resolvedOperator != null) {
            tokens.add(new OperatorToken(resolvedOperator));
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
        throw new IllegalArgumentException("Cannot recognize token at position: " + startingPos);
    }

    private int readNumeric(char[] input, int startingPos, List<Token> tokens) {
        StringBuilder stringBuilder = new StringBuilder();

        boolean encounteredDecimalSeparator = false;
        int i = startingPos;
        while (i < input.length) {
            char current = input[i];

            if (current == '.') {
                if (encounteredDecimalSeparator) {
                    throw new IllegalArgumentException("Multiple decimal separators found when attempting to parse a number");
                }
                encounteredDecimalSeparator = true;
            } else if (!Character.isDigit(current)) {
                break;
            }

            stringBuilder.append(current);
            i++;
        }

        String readNumber = stringBuilder.toString();
        if (readNumber.endsWith(".")) {
            throw new IllegalArgumentException("Encountering decimal number with separator, but without any numbers after it");
        }

        tokens.add(new ConstantToken(stringBuilder.toString()));
        return i;
    }

    private String readIdentifier(char[] input, int startingPos) {
        int identifierLength = 0;
        int readTokensLimit = 5; // to avoid reading extremely long identifier which we'll fail anyway
        int lastIndex = startingPos;
        while (identifierLength < readTokensLimit &&
                input.length > lastIndex
                && input[lastIndex] >= 'a' && input[lastIndex] <= 'z') {
            lastIndex++;
            identifierLength++;
        }

        String identifier = new String(input, startingPos, identifierLength);
        if (identifierLength != 1) {
            throw new IllegalArgumentException("Identifiers are expected to be one-characters only, " +
                    "encountered identifier starting with: " + identifier);
        }
        return identifier;
    }

    private int readReservedKeyword(char[] input, int startingPos, List<Token> tokens) {
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
