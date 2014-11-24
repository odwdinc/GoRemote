package systems.movingdata.goremote;

/**
 * Created by ap on 11/23/14.
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class WolMagicPacket
{
    private final String mIp;
    private final String mMacAddress;
    private final int mPort;

    public WolMagicPacket(String MacAddress, String Ip, int Port)
    {
        this.mMacAddress = MacAddress;
        this.mIp = Ip;
        this.mPort = Port;
    }

    public String send()
            throws UnknownHostException, SocketException, IOException, IllegalArgumentException
    {
        String[] arrayOfString = this.mMacAddress.split(":");
        byte[] arrayOfByte1 = new byte[6];
        for (int i = 0; i < 6; i++)
            arrayOfByte1[i] = ((byte)Integer.parseInt(arrayOfString[i], 16));
        byte[] arrayOfByte2 = new byte[102];
        for (int j = 0; j < 6; j++)
            arrayOfByte2[j] = -1;
        int k = 6;
        while (k < arrayOfByte2.length)
        {
            System.arraycopy(arrayOfByte1, 0, arrayOfByte2, k, arrayOfByte1.length);
            k += arrayOfByte1.length;
        }
        InetAddress localInetAddress = InetAddress.getByName(this.mIp);
        DatagramPacket localDatagramPacket = new DatagramPacket(arrayOfByte2, arrayOfByte2.length, localInetAddress, this.mPort);
        DatagramSocket localDatagramSocket = new DatagramSocket();
        try
        {
            localDatagramSocket.send(localDatagramPacket);
            return Arrays.asList(arrayOfString).toString();
        }
        finally
        {
            if (localDatagramSocket != null)
                localDatagramSocket.close();
        }
    }


}