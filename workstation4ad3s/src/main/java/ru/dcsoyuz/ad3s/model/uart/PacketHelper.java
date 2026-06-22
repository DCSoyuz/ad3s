package ru.dcsoyuz.ad3s.model.uart;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yuri.filatov on 05.08.2016.
 */
public class PacketHelper {


    private final byte CEB_START_ADDRESS  = 0x46;
    private final byte BOX_START_BYTE_TO_IMITATOR = 0x0B;
    private final byte BOX_START_BYTE_TO_CEB = 0x0A;
    static boolean debug = false;

    public static byte[] createBoxPacketToImitator(byte[] data){
        byte[] res = new byte[data.length + 4];
        res[0] = 0x1B;
        res[1] = 0x1B;
        res[2] = (byte)data.length;
        res[3] = (byte)0xFF;
        System.arraycopy(data, 0, res, 4, data.length);
        res = createBoxCheckSum(res);
        return res;
    }

    public static byte[] extractDataFromPacket(byte [] data){
        int size = (int)data[1];
        debug = ! debug;
        byte []res = new byte [size-1];
        System.arraycopy(data, 2, res, 0, size-1);
        return res;
    }

    public static String byteToInt6Bit(byte low_byte, byte high_byte){
        Integer i = (((int)high_byte & 0x3F) << 6) +  ((int)low_byte & 0x3F);
        i = i << 4+16;
        i = i >> 4+16;
        i = -i;
        return i.toString();
    }


    public static byte[] createBoxPacketToCeb(byte [] data){
        byte[] res = new byte[data.length + 4];
        res[0] = 0x1A;
        res[1] = 0x1A;
        res[2] = (byte)data.length;
        res[3] = (byte)0xFF;
        System.arraycopy(data, 0, res, 4, data.length);
        res = createBoxCheckSum(res);
        return res;
    }

    public static byte[]createCebPacket(byte [] data){
        byte[] res = new byte[data.length + 2];
        res[1] = (byte)(data.length + 1);
        System.arraycopy(data, 0, res, 2, data.length);
        res[0] = 0x55;
        res = createCheckSum(res);
        res[0] = 0x46;
        return res;
    }



    public static byte [] getDataFromPacket(byte []packet){
        Integer size = (int)packet[5] - 1;
        byte []data = new byte[size];
        System.arraycopy(packet, 6, data, 0, size);
        return data;
    }

    public static int getReadSdiWord(int address){
        int v1 = ((address << 2) | 0xC000);
        int sum = 0;
        for(int i = 0; i<=15; i++){
            sum = sum + ((v1 >> i)  & 1);
        }
        int v2 = v1 | (sum & 1);
        int hb = v2 &0xFF;
        int lb = (v2 >> 8) &0xFF;
        int v3 = (hb << 8) | lb;
        return v3;
    }


    private static byte [] createCheckSum(byte[] data){
        byte [] res = new byte[data.length + 1];
        System.arraycopy(data, 0, res, 0, data.length);
        int sum = calcCheckSum(data);
        //sum  = sum + 0x55;
        res[res.length - 1] = (byte) (sum & 0xFF); // Lowes
        return res;
    }

    public static String convPacketToHexString(byte[] bytes, String separator) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]).toUpperCase();
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
            hexString.append(separator);
        }
        return hexString.toString();
    }

    public static String convPacketToHexString(List<Byte> bytes, String separator) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < bytes.size(); i++) {
            String hex = Integer.toHexString(0xFF & bytes.get(i)).toUpperCase();
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
            hexString.append(separator);
        }
        return hexString.toString();
    }

    public static List<String> convPacketToListString(byte[] bytes) {
        List<String> res = new ArrayList<>();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hex = "0" + hex;
            }
            res.add(hex);
        }
        return res;
    }


    private static byte [] createBoxCheckSum(byte[] data){
        byte [] res = new byte[data.length + 2];
        System.arraycopy(data, 0, res, 0, data.length);
        int sum = calcCheckSum(data);
        res[res.length - 2] = (byte) (sum & 0xFF); // Lowes
        res[res.length - 1] = (byte) ((sum >> 8) & 0xFF); // Higest
        return res;
    }

    public  static int calcCheckSum(byte [] data){
        Integer sum = 0;
        for(byte word : data){
            sum = sum + b2i(word);
        }
        return sum;
    }

    public static int b2i(byte word){
        return word & 0xFF;
    }

    public  static byte[]  addDataToPacket(byte[]packet, byte[]data){
        byte [] res = new byte[packet.length +data.length];
        System.arraycopy(packet, 0, res, 0, packet.length);
        System.arraycopy(data, 0, res, packet.length, data.length);

        return res;
    }


    public  static byte[]  clone(byte[]data){
        byte [] res = new byte[data.length];
        System.arraycopy(data, 0, res, 0, data.length);
        return res;
    }


    public static byte[]  extractCebPacket(byte [] data){
        int size = (int)data[5];
        debug = ! debug;
        byte []res = new byte [size+2];
        if(size < 58){
            System.arraycopy(data, 4, res, 0, size+2);
        } else {
            System.arraycopy(data, 4, res, 0, size-6);
            System.arraycopy(data, 64, res, 58, 8);
        }


        return res;
    }


    public static Boolean getBitFromByte(byte b, int num){
       int mask =  (int) Math.pow(2, num);
       int res = ( b & mask ) >> num;
       return res == 1;
    }




    public  static byte[]  addDataToPacket(byte[]packet, Integer data){
        return addDataToPacket(packet, i2b(data));
    }




    public  static byte[]  addDataToPacket(byte[]packet, byte b){
        byte [] res = new byte[packet.length + 1];
        System.arraycopy(packet, 0, res, 0, packet.length);
        res [packet.length ] = b;
        return res;
    }
    public  static byte[]  addDataToPacket(byte[]packet, List<Integer> data){
        for(Integer word : data){
            packet = addDataToPacket(packet, word);
        }
        return packet;
    }

    public static List<Integer> getIntList(byte [] bytes){
        List<Integer> res = new ArrayList<>();
        for(int i = 0; i < bytes.length/2; i++){
            res.add (((bytes[2*i+1] & 0xFF ) << 8) | (bytes[2*i] & 0xFF ));
        }
        return res;
    }


    public static List<String> createHex(List<Integer> arr, int mask){
        List<String> res = new ArrayList<>();
        for(Integer v : arr){
            Integer vMask = v | mask;
            String str = Integer.toHexString(vMask);
            str = str.substring(1);
            res.add(str);
        }
        return res;
    }

    public static byte[] s2b(String str){
        Integer i  = Integer.parseInt(str);
        return i2b(i);
    }

    public static byte[] i2b(Integer arg){
        byte[] res = new byte[2];
        res[0] =(byte)(arg & 0xFF);
        res[1]= (byte)((arg>> 8) & 0xFF);
        return res;
    }

    public static byte bool2byte(boolean [] arg){
        int res = 0;
        for(int i = 0; i <= arg.length-1; i++ ){
            int mask = arg[i] ? 1 << i : 0;
            res = res | mask;
        }
        return (byte)res;
    }

    public static String getSensor(byte low_byte, byte high_byte){
        int i =  ((low_byte & 0xff) | (high_byte << 8)) << 20 >> 20;
        return Integer.toString(i);
    }

    public static double getSensorDouble(byte low_byte, byte high_byte){
        double i =  ((low_byte & 0xff) | (high_byte << 8)) << 20 >> 20;
        return i;
    }

    public static String getSignedValue16bit(byte low_byte, byte high_byte){
        int i =  ((low_byte & 0xff) | (high_byte << 8)) << 16 >> 16;
        return Integer.toString(i);
    }

    public static Integer getSignedValue16bitInt(byte low_byte, byte high_byte){
        int i =  ((low_byte & 0xff) | (high_byte << 8)) << 16 >> 16;
        return i;
    }



    public static String getUnsignedWord16bit(byte low_byte, byte high_byte){
        int i =  ((low_byte & 0xff) | ((high_byte & 0xFF) << 8));
        return Integer.toString(i);
    }
    public static Integer getUnsignedWord16bitInt(byte low_byte, byte high_byte){
        int i =  ((low_byte & 0xff) | ((high_byte & 0xFF) << 8));
        return i;
    }





    public static String getUnsignedWord2bit(byte low_byte, byte high_byte){
        int i =  ((low_byte & 0x0f) | ((high_byte & 0xFF) << 8));
        return Integer.toString(i);
    }

    public static String getUnsignedWord12bit(byte low_byte, byte high_byte){
        int i =  ((low_byte & 0xff) | ((high_byte & 0x0F) << 8));
        return Integer.toString(i);
    }

    public static String getAngle22(byte low_byte, byte high_byte, byte high_bit6){
        Integer angle = (((int)high_bit6) << 16) & 0xFF0000 | ((((int)high_byte) << 8 ) & 0xFF00)|((int) low_byte & 0xFF);
        return Integer.toHexString(angle/8 | 0x100000).substring(1);
    }



    public static String getAngle19(byte low_byte, byte high_byte, byte high_bit6){
        Integer angle = (((int)high_bit6) << 16) & 0x070000| (((int)high_byte) << 8 ) & 0xFF00|((int) low_byte & 0xFF);
        return Integer.toHexString(angle | 0x100000).substring(1);
    }




}
