package demo;/*
 * Copyright (c) 2010, Apigee Corporation.  All rights reserved.
 *  Apigee(TM) and the Apigee logo are trademarks or
 *  registered trademarks of Apigee Corp. or its subsidiaries.  All other
 *  trademarks are the property of their respective owners.
 */

import javax.xml.bind.DatatypeConverter;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Author : asribalaji
 */
public class CommonUtil {
    private static CommonUtil instance = new CommonUtil();

    public static CommonUtil getInstance(){
        return instance;
    }

    public static String generateRandText(int len){
        byte[] secretBytes = new byte[len*2];
        Random random = new SecureRandom();
        random.nextBytes(secretBytes);
        return DatatypeConverter.printBase64Binary(secretBytes).substring(0, len)
                .replace('/', 'A')
                .replace('\\', 'B')
                .replace('\'', 'C')
                .replace('"', 'D')
                .replace('?', 'E')
                .replace('%', 'F')
                .replace('+', 'G');
    }

    public static long generateRandId() {
        Random random = new SecureRandom();
        Long randNo = random.nextLong();
        return Math.abs(randNo);
    }

}
