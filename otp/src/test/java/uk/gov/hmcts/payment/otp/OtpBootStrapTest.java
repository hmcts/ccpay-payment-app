package uk.gov.hmcts.payment.otp;

import com.amdelamar.jotp.OTP;
import com.amdelamar.jotp.type.Type;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class OtpBootStrapTest {

//    @Mock
//    OTP otp;

//    @InjectMocks
//    OtpBootstrap otpBootstrap;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @Test
    public void testMain() throws Exception {
//        String[] input = {"secret"};
//        OtpBootstrap.main(input);
//        assertEquals("hello", outContent.toString());

    }

}
