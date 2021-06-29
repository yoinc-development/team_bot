import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HCMessageTest {

    static HCMessage underTest;

    @Mock
    static Properties properties;

    @BeforeAll
    public static void setup() {
        underTest = new HCMessage(properties);

    }

    @Test
    public void testCorrectClaimNoAdmin() {
        int result = underTest.handleMessage("/claim", false);
        assertEquals(1, result);
    }

    @Test
    public void testCorrectSetupNoAdmin() {
        int result = underTest.handleMessage("/setup", false);
        assertEquals(0, result);
    }

    @Test
    public void testCorrectSetupAsAdmin() {
        int result = underTest.handleMessage("/setup", true);
        assertEquals(9, result);
    }

    @Test
    public void testNoNumberInClaim() {
        int result = (int) underTest.returnNumber("/claim", 0);
        assertEquals(-2, result);
    }

    @Test
    public void testNoRealNumberInClaim() {
        int result = (int) underTest.returnNumber("/claim fail", 0);
        assertEquals(-1, result);
    }

    @Test
    public void testCorrectClaim() {
        int result = (int) underTest.returnNumber("/claim 5", 0);
        assertEquals(5, result);
    }
}
