package com.kw.parserProject;

import com.kw.parserProject.tokens.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LexerTest {

    Lexer lexer;

    @BeforeEach
    void setUp() {
        lexer = new Lexer();
    }

    @ParameterizedTest
    @MethodSource("basicTokenTestCases")
    void shouldRecognizeBasicTokens(String input, Class<? extends Token> expectedTokenType, String expectedData) {
        // when
        List<Token> tokens = lexer.parseLine(input);

        // then
        assertEquals(1, tokens.size());
        Token actualToken = tokens.getFirst();

        assertInstanceOf(expectedTokenType, actualToken);
        assertEquals(expectedData, actualToken.data);
    }

    @Test
    void shouldIgnoreWhitespacesAndLineBreaks() {
        // when
        // we're likely not encountering newlines here, but we might as well check if we skip them
        List<Token> tokens = lexer.parseLine(" x    \ty \nz");

        // then
        assertEquals(3, tokens.size());
        Token xToken = tokens.getFirst();
        Token yToken = tokens.get(1);
        Token zToken = tokens.get(2);

        assertInstanceOf(VariableToken.class, xToken);
        assertEquals("x", xToken.data);
        assertInstanceOf(VariableToken.class, yToken);
        assertEquals("y", yToken.data);
        assertInstanceOf(VariableToken.class, zToken);
        assertEquals("z", zToken.data);
    }

    @ParameterizedTest
    @ValueSource(strings = {"whil", "abc", "fi"})
    void shouldLimitVariablesToOneCharacter(String illegalVariable) {
        // expect
        assertThrows(IllegalArgumentException.class, () -> lexer.parseLine(illegalVariable));
    }

    public static Stream<Arguments> basicTokenTestCases() {
        return Stream.of(
                Arguments.of("x", VariableToken.class, "x"),
                Arguments.of("y", VariableToken.class, "y"),
                Arguments.of("a", VariableToken.class, "a"),
                Arguments.of("=", AssignmentToken.class, "="),
                Arguments.of("+", OperatorToken.class, "+"),
                Arguments.of("-", OperatorToken.class, "-"),
                Arguments.of("/", OperatorToken.class, "/"),
                Arguments.of("*", OperatorToken.class, "*"),
                Arguments.of("<", OperatorToken.class, "<"),
                Arguments.of(">", OperatorToken.class, ">"),
                Arguments.of("5", ConstantToken.class, "5"),
                Arguments.of("123", ConstantToken.class, "123"),
//                Arguments.of("123.21", ConstantToken.class, "123.21"),
//                Arguments.of("-923", ConstantToken.class, "923")
//                Arguments.of("-241.55", ConstantToken.class, "-241.55")
                Arguments.of("0", ConstantToken.class, "0"),
                Arguments.of("(", OpeningBracketToken.class, "("),
                Arguments.of(")", ClosingBracketToken.class, ")"),
                Arguments.of("if", KeywordToken.class, "if"),
                Arguments.of("else", KeywordToken.class, "else"),
                Arguments.of("while", KeywordToken.class, "while"),
                Arguments.of("end", KeywordToken.class, "end")
        );
    }

}