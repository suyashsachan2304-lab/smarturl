package com.suyash.smarturl.util;

import java.security.SecureRandom;

public class ShortCodeGenerator {

    private static final String CHARACTERS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    private ShortCodeGenerator(){}

    public static String generate(int length){

        StringBuilder builder = new StringBuilder();

        for(int i=0;i<length;i++){

            builder.append(
                    CHARACTERS.charAt(
                            RANDOM.nextInt(CHARACTERS.length())
                    )
            );
        }

        return builder.toString();

    }

}