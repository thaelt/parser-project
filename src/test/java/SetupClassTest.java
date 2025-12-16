import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SetupClassTest {

    @Test
    void shouldCheckIfWorks() {
        // given
        SetupClass setupClass = new SetupClass();

        // when
        int results = setupClass.returnSum(1, 3);

        // then
        assertEquals(4, results);
    }

}