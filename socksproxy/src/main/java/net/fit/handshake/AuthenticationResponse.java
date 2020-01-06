package net.fit.handshake;

public class AuthenticationResponse {
    public static byte[] generateResponse(AuthenticationRequest.AuthenticationMethod method) {
        return new byte[]{0x05, method.getCode()};
    }
}
