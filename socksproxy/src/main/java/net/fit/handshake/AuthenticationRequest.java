package net.fit.handshake;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthenticationRequest {
    @RequiredArgsConstructor
    public enum AuthenticationMethod {
        NO_AUTH ((byte) 0x00),
        GSSAPI ((byte) 0x01),
        USERNAME_PASSWORD ((byte) 0x02),
        NO_ACCEPTABLE ((byte) 0xff);

        @Getter private final byte code;
        private static AuthenticationMethod valueOfCode(byte code) {
            for (AuthenticationMethod method : values()) {
                if (method.code == code)
                    return method;
            }
            return null;
        }
    }

    @Getter private final List<AuthenticationMethod> methodsList;

    public Byte[] toByteArray() {
        List<Byte> authBytes = new ArrayList<>();
        authBytes.add((byte) 0x05);
        authBytes.add((byte) methodsList.size());
        for (AuthenticationMethod method : methodsList) {
            byte code = method.getCode();
            authBytes.add(code);
        }
        return authBytes.toArray(new Byte[methodsList.size() + 2]);
    }

    public static AuthenticationRequest parseFromBytes(byte[] input) {
        if (input.length < 2)
            return null;
        if (input[0] != 0x05)
            return null;
        if (input.length != input[1] + 2)
            return null;
        List<AuthenticationMethod> methodList = new ArrayList<>();
        for (int i = 2; i < input.length; i++) {
            AuthenticationMethod method = AuthenticationMethod.valueOfCode(input[i]);
            methodList.add(method);
        }
        return new AuthenticationRequest(methodList);
    }
}
