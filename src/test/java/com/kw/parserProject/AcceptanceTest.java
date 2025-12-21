package com.kw.parserProject;

import com.kw.parserProject.statements.Statement;
import com.kw.parserProject.tokens.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AcceptanceTest {

    Lexer lexer;
    Parser parser;
    UnusedStatementChecker unusedStatementChecker;

    @BeforeEach
    void setUp() {
        lexer = new Lexer();
        parser = new Parser();
        unusedStatementChecker = new UnusedStatementChecker();
    }

    @ParameterizedTest(name = "Expecting {1}")
    @MethodSource("basicTestCases")
    void shouldDetectUnusedStatements(String input, String expectedOutput) {
        // when
        List<Token> tokens = lexer.extractTokens(input);
        Program parsedProgram = parser.parse(tokens);
        List<Statement> actualOutput = unusedStatementChecker.getUnusedStatements(parsedProgram);

        // then
        assertEquals(expectedOutput, actualOutput.toString());
    }

    private static Stream<Arguments> basicTestCases() {
        return Stream.of(
                Arguments.of("""
                        a = 1
                        b = a
                        x = 3
                        y = 4
                        while (b < 5)
                          z = x
                          b = b + 1
                          x = 9
                          y = 10
                        end""", "[y = 4, z = x, y = 10]"),
                Arguments.of("""
                        a = 1
                        b = 2
                        """, "[a = 1, b = 2]"),
                Arguments.of("""
                        a = -21
                        b = 2
                        """, "[a = -21, b = 2]"),
                Arguments.of("""
                        a = 1
                        if a < 5
                           a = a + 1
                        end
                        b = a
                        """, "[b = a]"),
                Arguments.of("""
                        c = 5 + 2
                        a = 4 < 3
                        if c < 6
                           d = 12
                        else
                           while c < 10
                              c = c + 10
                              d = 2
                           end
                           d = -5.2145
                        end
                        b = c + a
                        """, "[d = 12, d = 2, d = -5.2145, b = c + a]"),
                Arguments.of("""
                        c = 5 + 2
                        a = 4 < 3
                        if c < 6
                           d = 12
                        else
                           while c < 10
                              c = c + 10
                              d = 2
                           end
                           d = -5.2145
                        end
                        b = c + a
                        e = 2 + (d + b)
                        """, "[d = 2, e = 2 + (d + b)]"),
                Arguments.of("""
                        c = 5 + (- 2 + 5)
                        a = 4 < 3
                        d = 2
                        if c < 6
                           d = 12
                        else
                           while c < 10
                              c = c * 2 * ( d + a ) + 10
                              z = 4
                              d = 2
                           end
                           d = 5
                        end
                        b = c + a
                        e = 2 + d + b
                        """, "[d = 2, z = 4, e = 2 + d + b]"), // this one reports 'd=2' from 3rd line
                Arguments.of("""
                        c = 5 + 2
                        a = 4 < 3
                        d = 2
                        if c < 6
                           d = 12
                        else
                           while c < 10
                              c = c * 2 * ( d + a ) + 10
                           end
                           d = 5
                        end
                        b = c + a
                        e = 2 + d + b
                        """, "[d = 2, e = 2 + d + b]")
        );
    }

}
