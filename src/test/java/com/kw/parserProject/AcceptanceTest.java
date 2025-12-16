package com.kw.parserProject;

import com.kw.parserProject.statements.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AcceptanceTest {

    Parser parser;
    UnusedStatementChecker unusedStatementChecker;

    @BeforeEach
    void setUp() {
        parser = new Parser();
        unusedStatementChecker = new UnusedStatementChecker();
    }

    @ParameterizedTest
    @MethodSource("basicTestCases")
    void shouldDetectUnusedStatements(String input, String expectedOutput) {
        // when
        List<Statement> statements = parser.parse(input);
        List<Statement> actualOutput = unusedStatementChecker.getUnusedStatements(statements);

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
                        end""", "[y=4, x=9, y=10, z=x]"),
                Arguments.of("""
                        a = 1
                        b = 2
                        """, "[a=1, b=2]"),
                Arguments.of("""
                        a = 1
                        if a < 5
                           a = a + 1
                        end
                        b = a
                        """, "[b=a]"),
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
                           d = 5
                        end
                        b = c + a
                        """, "[d=12, d=2, b=c+a, d=5]")
        );
    }

}
