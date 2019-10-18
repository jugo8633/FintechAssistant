package org.iii.chihlee.datatransfer;

import org.iii.more.common.Logs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;


public abstract class Controller
{
    static private String mstrLastError = null;
    /*
     * CMP body data length
     */
    static public final int MAX_DATA_LEN = 4096;
    
    /*
     * this define socket packet for CMP
     */
    static public class CMP_HEADER
    {
        public int command_length;
        public int command_id;
        public int command_status;
        public int sequence_number;
    }
    
    ;
    
    static private int CMP_HEADER_SIZE = 16;
    
    static public class CMP_PACKET
    {
        public CMP_HEADER cmpHeader = new CMP_HEADER();
        public String cmpBody;
    }
    
    ;
    
    /*
     * CMP Command set
     */
    static public final int generic_nack = 0x80000000;
    static public final int bind_request = 0x00000001;
    static public final int bind_response = 0x80000001;
    static public final int authentication_request = 0x00000002;
    static public final int authentication_response = 0x80000002;
    static public final int access_log_request = 0x00000003;
    static public final int access_log_response = 0x80000003;
    static public final int initial_request = 0x00000004;
    static public final int initial_response = 0x80000004;
    static public final int sign_up_request = 0x00000005;
    static public final int sign_up_response = 0x80000005;
    static public final int unbind_request = 0x00000006;
    static public final int unbind_response = 0x80000006;
    static public final int update_request = 0x00000007;
    static public final int update_response = 0x80000007;
    static public final int reboot_request = 0x00000010;
    static public final int reboot_response = 0x80000010;
    static public final int config_request = 0x00000011;
    static public final int config_response = 0x80000011;
    static public final int wheelpies_request = 0x00000012;
    static public final int wheelpies_response = 0x80000012;
    static public final int power_port_state_request = 0x00000013;
    static public final int power_port_state_response = 0x80000013;
    static public final int ser_api_signin_request = 0x00000014;
    static public final int ser_api_signin_response = 0x80000014;
    static public final int enquire_link_request = 0x00000015;
    static public final int enquire_link_response = 0x80000015;
    static public final int rdm_login_request = 0x00000016;
    static public final int rdm_login_response = 0x80000016;
    static public final int rdm_operate_request = 0x00000017;
    static public final int rdm_operate_response = 0x80000017;
    static public final int rdm_logout_request = 0x00000018;
    static public final int rdm_logout_response = 0x80000018;
    static public final int device_control_request = 0x00000019;
    static public final int device_control_response = 0x80000019;
    static public final int device_state_request = 0x00000020;
    static public final int device_state_response = 0x80000020;
    static public final int semantic_request = 0x00000030;
    static public final int semantic_response = 0x80000030;
    static public final int semantic_word_request = 0x00000057;
    static public final int semantic_word_response = 0x80000057;
    static public final int amx_control_command_request = 0x00000040;
    static public final int amx_control_command_response = 0x80000040;
    static public final int amx_status_command_request = 0x00000041;
    static public final int amx_status_command_response = 0x80000041;
    static public final int amx_broadcast_status_command_request = 0x00000042;
    static public final int amx_broadcast_status_command_response = 0x80000042;
    static public final int assistant_request = 0x00000062;
    static public final int assistant_response = 0x80000062;
    
    
    /*
     * CMP status set
     */
    static public final int STATUS_ROK = 0x00000000; // No Error
    static public final int STATUS_RINVMSGLEN = 0x00000001; // Message Length is
    // invalid
    static public final int STATUS_RINVCMDLEN = 0x00000002; // Command Length is
    // invalid
    static public final int STATUS_RINVCMDID = 0x00000003; // Invalid Command ID
    static public final int STATUS_RINVBNDSTS = 0x00000004; // Incorrect BIND
    // Status for given
    // command
    static public final int STATUS_RALYBND = 0x00000005; // Already in Bound
    // State
    static public final int STATUS_RSYSERR = 0x00000008; // System Error
    static public final int STATUS_RBINDFAIL = 0x00000010; // Bind Failed
    static public final int STATUS_RINVBODY = 0x00000040; // Invalid Packet Body
    // Data
    static public final int STATUS_RINVCTRLID = 0x00000041; // Invalid
    // Controller ID
    static public final int STATUS_RINVJSON = 0x00000042; // Invalid JSON Data
    
    private static int msnSequence = 0;
    private static final int nConnectTimeOut = 3000; // Socket Connect Timeout
    private static final int nReceiveTimeOut = 3000; // Socket Read IO Timeout
    
    public static final int ERR_CMP = -1000;
    public static final int ERR_PACKET_LENGTH = -6 + ERR_CMP;
    public static final int ERR_PACKET_SEQUENCE = -7 + ERR_CMP;
    public static final int ERR_REQUEST_FAIL = -8 + ERR_CMP;
    public static final int ERR_SOCKET_INVALID = -9 + ERR_CMP;
    public static final int ERR_INVALID_PARAM = -10 + ERR_CMP;
    public static final int ERR_LOG_DATA_LENGTH = -11 + ERR_CMP;
    public static final int ERR_EXCEPTION = -12 + ERR_CMP;
    public static final int ERR_IOEXCEPTION = -13 + ERR_CMP;
    public static final int ERR_PACKET_CONVERT = -14 + ERR_CMP;
    
    private static final String CODE_TYPE = "UTF-8"; // CMP Body data type
    
    public static boolean validSocket(Socket msocket)
    {
        if (null == msocket || msocket.isClosed())
        {
            return false;
        }
        return true;
    }
    
    private static int getSequence()
    {
        if (0x7FFFFFFF <= ++msnSequence)
        {
            msnSequence = 0x00000001;
        }
        return msnSequence;
    }
    
    public static String getLastError()
    {
        return mstrLastError;
    }
    
    /**
     * Controller Message Request Protocol
     *
     * @param nCommand   : CMP Command. Ref. CMP Document.
     * @param strBody    : CMP Request Body, If packet is no body that will set to
     *                   null.
     * @param respPacket : CMP response packet.
     * @param msocket    : Valid socket require
     * @return : CMP Status, Ref.CMP Document.
     */
    public static int cmpRequest(final int nCommand, final String strBody, CMP_PACKET respPacket,
            Socket msocket)
    {
        if (null == respPacket)
        {
            System.out.println("Parameter CMP_PACKET invalid");
            return ERR_INVALID_PARAM;
        }
        int nCmpStatus = STATUS_ROK;
        
        try
        {
            if (!validSocket(msocket))
            {
                return ERR_SOCKET_INVALID;
            }
            
            final int nSequence = getSequence();
            
            OutputStream outSocket = msocket.getOutputStream();
            InputStream inSocket = msocket.getInputStream();
            // header + body + endChar
            int nLength = CMP_HEADER_SIZE;
            if (null != strBody && 0 < strBody.length())
            {
                nLength += strBody.getBytes(CODE_TYPE).length + 1;
            }
            ByteBuffer buf = ByteBuffer.allocate(nLength);
            buf.putInt(nLength);
            buf.putInt(nCommand);
            buf.putInt(STATUS_ROK);
            buf.putInt(nSequence);
            
            // debug using start
            Logs.showTrace("@@Request Command@@ ");
            Logs.showTrace("Command ID: " + String.valueOf(nCommand));
            Logs.showTrace("Command Length: " + String.valueOf(nLength));
            Logs.showTrace("Command Status: " + String.valueOf(STATUS_ROK));
            Logs.showTrace("Command Sequence: " + String.valueOf(nSequence));
            Logs.showTrace("Command Body: " + strBody);
            
            
            if (null != strBody && 0 < strBody.length())
            {
                buf.put(strBody.getBytes(CODE_TYPE));
                // add endChar
                buf.put((byte) 0);
            }
            
            buf.flip();
            // Send Request
            outSocket.write(buf.array());
            
            buf.clear();
            buf = ByteBuffer.allocate(CMP_HEADER_SIZE);
            
            // Receive Response
            nLength = inSocket.read(buf.array(), 0, CMP_HEADER_SIZE);
            buf.rewind();
            
            if (CMP_HEADER_SIZE <= nLength)
            {
                buf.order(ByteOrder.BIG_ENDIAN);
                
                respPacket.cmpHeader.command_length = buf.getInt(0); // offset
                respPacket.cmpHeader.command_id = buf.getInt(4) & 0x00ffffff;
                respPacket.cmpHeader.command_status = buf.getInt(8);
                respPacket.cmpHeader.sequence_number = buf.getInt(12);
                
                if (nSequence != respPacket.cmpHeader.sequence_number)
                {
                    nCmpStatus = ERR_PACKET_SEQUENCE;
                }
                else
                {
                    
                    nCmpStatus = respPacket.cmpHeader.command_status;
                    int nBodySize = respPacket.cmpHeader.command_length - CMP_HEADER_SIZE;
                    
                    if (0 < nBodySize)
                    {
                        buf.clear();
                        buf = ByteBuffer.allocate(nBodySize);
                        nLength = inSocket.read(buf.array(), 0, --nBodySize); // not
                        // read
                        // end-char
                        if (nLength == nBodySize)
                        {
                            byte[] bytes = new byte[nBodySize];
                            buf.get(bytes);
                            respPacket.cmpBody = new String(bytes, Charset.forName(CODE_TYPE));
                        }
                    }
                }
            }
            else
            {
                nCmpStatus = ERR_PACKET_LENGTH;
            }
        }
        catch (SocketException e)
        {
            mstrLastError = e.toString();
            Logs.showError("CMP Request Exception:" + e.toString());
            nCmpStatus = ERR_IOEXCEPTION;
            
        }
        catch (Exception e)
        {
            mstrLastError = e.toString();
            Logs.showError("CMP Request Exception:" + e.toString());
            nCmpStatus = ERR_EXCEPTION;
        }
        
        // for debugging use Start
		/*Logs.showTrace("@@Response Command@@ ");
		Logs.showTrace("Command ID: " + String.valueOf(respPacket.cmpHeader.command_id));
		Logs.showTrace("Command Length: " + String.valueOf(respPacket.cmpHeader.command_length));
		Logs.showTrace("Command Status: " + String.valueOf(respPacket.cmpHeader.command_status));
		Logs.showTrace("Sequence Number: " + String.valueOf(respPacket.cmpHeader.sequence_number)
		);*/
        if (null != respPacket.cmpBody)
        {
            Logs.showTrace("Response Message: " + respPacket.cmpBody);
        }
        // for debugging use End
        
        return nCmpStatus;
    }
    
    /**
     * Controller Message Request Protocol
     *
     * @param strIP      : Socket Connect IP.
     * @param nPort      : Socket Connect Port.
     * @param nCommand   : CMP Command. Ref. CMP Document.
     * @param strBody    : CMP Request Body, If packet is no body that will set to
     *                   null.
     * @param respPacket : CMP response packet.
     * @return : CMP Status, Ref.CMP Document.
     */
    public static int cmpRequest(final String strIP, final int nPort, final int nCommand,
            final String strBody, CMP_PACKET respPacket)
    {
        int returnStatus = 0;
        try
        {
            Socket msocket = new Socket();
            msocket.connect(new InetSocketAddress(strIP, nPort), nConnectTimeOut);
            msocket.setSoTimeout(nReceiveTimeOut);
            returnStatus = cmpRequest(nCommand, strBody, respPacket, msocket);
            msocket.close();
            msocket = null;
            
            return returnStatus;
        }
        catch (SocketException e)
        {
            return ERR_SOCKET_INVALID;
        }
        catch (IOException e)
        {
            return ERR_IOEXCEPTION;
        }
        
    }
    
    /*
     * public static int cmpRequestNew(final int nCommand, final String strBody,
     * CMP_PACKET respPacket, Socket msocket) { CMP_PACKET sendPacket = new
     * CMP_PACKET();
     *
     * int sendStatus = cmpSend(nCommand, strBody, sendPacket, msocket); if
     * (sendStatus == STATUS_ROK) { cmpReceive(respPacket, msocket,
     * sendPacket.cmpHeader.sequence_number);
     *
     * } else { return sendStatus; }
     *
     * }
     */
    
    public static int cmpSend(final int nCommand, final String strBody, CMP_PACKET sendPacket,
            Socket msocket, final int nSequence)
    {
        int nCmpStatus = STATUS_ROK;
        int nLength = 0;
        try
        {
            if (!validSocket(msocket))
            {
                return ERR_SOCKET_INVALID;
            }
            
            OutputStream outSocket = msocket.getOutputStream();
            // header + body + endChar
            nLength = CMP_HEADER_SIZE;
            if (null != strBody && 0 < strBody.length())
            {
                nLength += strBody.getBytes(CODE_TYPE).length + 1;
                sendPacket.cmpBody = strBody;
            }
            ByteBuffer buf = ByteBuffer.allocate(nLength);
            buf.putInt(nLength);
            buf.putInt(nCommand);
            buf.putInt(nCmpStatus);
            buf.putInt(nSequence);
            
            sendPacket.cmpHeader.command_id = nCommand;
            sendPacket.cmpHeader.command_length = nLength;
            sendPacket.cmpHeader.command_status = nCmpStatus;
            sendPacket.cmpHeader.sequence_number = nSequence;
            
            /*
             * // debug using start Logs.showTrace("@@Request Command@@ ");
             * Logs.showTrace("Command ID: " + String.valueOf(nCommand));
             * Logs.showTrace("Command Length: " + String.valueOf(nLength));
             * Logs.showTrace("Command Status: " + String.valueOf(nCmpStatus));
             * Logs.showTrace("Command Sequence: " + String.valueOf(nSequence));
             * Logs.showTrace("Command Body: " + strBody); // debug using end
             */
            if (null != strBody && 0 < strBody.length())
            {
                buf.put(strBody.getBytes(CODE_TYPE));
                // add endChar
                buf.put((byte) 0);
            }
            
            buf.flip();
            // Send Request
            outSocket.write(buf.array());
            
            buf.clear();
            buf = null;
            
        }
        catch (IOException e)
        {
            // debug using start
            Logs.showTrace("@@Request Command@@ ");
            Logs.showTrace("Command ID: " + String.valueOf(nCommand));
            Logs.showTrace("Command Length: " + String.valueOf(nLength));
            Logs.showTrace("Command Status: " + String.valueOf(nCmpStatus));
            Logs.showTrace("Command Sequence: " + String.valueOf(nSequence));
            Logs.showTrace("Command Body: " + strBody);
            // debug using end
            
            Logs.showError(e.toString());
            nCmpStatus = ERR_IOEXCEPTION;
        }
        
        return nCmpStatus;
    }
    
    public static int cmpSend(final int nCommand, final String strBody, CMP_PACKET sendPacket,
            Socket msocket)
    {
        
        final int nSequence = getSequence();
        return cmpSend(nCommand, strBody, sendPacket, msocket, nSequence);
        
        
    }
    
    public static int cmpReceive(CMP_PACKET receivePacket, Socket msocket, final int nSequence)
    {
        int nCmpStatus = STATUS_ROK;
        if (null == receivePacket)
        {
            System.out.println("Parameter CMP_PACKET invalid");
            return ERR_INVALID_PARAM;
        }
        
        if (!validSocket(msocket))
        {
            return ERR_SOCKET_INVALID;
        }
        
        try
        {
            InputStream inSocket = msocket.getInputStream();
            
            ByteBuffer buf = ByteBuffer.allocate(CMP_HEADER_SIZE);
            
            // Receive Response
            int nLength = inSocket.read(buf.array(), 0, CMP_HEADER_SIZE);
            buf.rewind();
            
            if (CMP_HEADER_SIZE <= nLength)
            {
                buf.order(ByteOrder.BIG_ENDIAN);
                
                receivePacket.cmpHeader.command_length = buf.getInt(0); // offset
                receivePacket.cmpHeader.command_id = buf.getInt(4) & 0x00ffffff;
                receivePacket.cmpHeader.command_status = buf.getInt(8);
                receivePacket.cmpHeader.sequence_number = buf.getInt(12);
                
                if (nSequence >= 0)
                {
                    if (receivePacket.cmpHeader.sequence_number != nSequence)
                    {
                        return ERR_PACKET_SEQUENCE;
                        
                    }
                }
                nCmpStatus = receivePacket.cmpHeader.command_status;
                int nBodySize = receivePacket.cmpHeader.command_length - CMP_HEADER_SIZE;
                
                if (0 < nBodySize)
                {
                    buf.clear();
                    buf = ByteBuffer.allocate(nBodySize);
                    nLength = inSocket.read(buf.array(), 0, --nBodySize); // not
                    // read
                    // end-char
                    if (nLength == nBodySize)
                    {
                        byte[] bytes = new byte[nBodySize];
                        buf.get(bytes);
                        receivePacket.cmpBody = new String(bytes, Charset.forName(CODE_TYPE));
                    }
                    buf.clear();
                }
                
                // for debugging use Start
				/*Logs.showTrace("@@Response Command@@ ");
				Logs.showTrace("Command ID: " + String.valueOf(receivePacket.cmpHeader
				.command_id));
				Logs.showTrace("Command Length: " + String.valueOf(receivePacket.cmpHeader
				.command_length));
				Logs.showTrace("Command Status: " + String.valueOf(receivePacket.cmpHeader
				.command_status));
				Logs.showTrace("Sequence Number: " + String.valueOf(receivePacket.cmpHeader
				.sequence_number));
				
				if (null != receivePacket.cmpBody)
				{
					Logs.showTrace("Response Message: " + receivePacket.cmpBody);
				}*/
                // for debugging use End
            }
            else
            {
                nCmpStatus = ERR_PACKET_LENGTH;
            }
            buf = null;
        }
        catch (IOException e)
        {
            nCmpStatus = ERR_IOEXCEPTION;
            Logs.showError(e.toString());
        }
        catch (IndexOutOfBoundsException e)
        {
            nCmpStatus = ERR_PACKET_CONVERT;
            Logs.showError(e.toString());
        }
        catch (Exception e)
        {
            nCmpStatus = ERR_EXCEPTION;
            Logs.showError(e.toString());
            
        }
        
        return nCmpStatus;
        
    }
    
}
