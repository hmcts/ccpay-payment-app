package uk.gov.hmcts.payment;

import com.amdelamar.jotp.OTP;
import com.amdelamar.jotp.type.Type;

/**
 * Utility jar used by azure webjob to read onetime password from console.
 */
public class OtpBootstrap {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Proper Usage is: java -jar otp.jar <base32-secret>");
            System.exit(0);
        }
        String secret = args[0];
        // Generate a Time-based OTP from the secret, using Unix-time rounded down to the nearest 30 seconds.
        String otp = OTP.create(secret, OTP.timeInHex(), 6, Type.TOTP);
        System.out.println(otp);
    }
}
