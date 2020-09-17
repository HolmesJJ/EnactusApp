package com.hc.bluetoothlibrary.bleBluetooth;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class ParseLeAdvData {
	private final static String TAG = "ParseLeAdvData";

	//BLE 广播包数据类型
	/**< Flags for discoverability. */
	public static final short BLE_GAP_AD_TYPE_FLAGS = 0x01; 
	/**< Partial list of 16 bit service UUIDs. */
	public static final short BLE_GAP_AD_TYPE_16BIT_SERVICE_UUID_MORE_AVAILABLE = 0x02;
	/**< Complete list of 16 bit service UUIDs. */
	public static final short BLE_GAP_AD_TYPE_16BIT_SERVICE_UUID_COMPLETE = 0x03; 
	/**< Partial list of 32 bit service UUIDs. */
	public static final short BLE_GAP_AD_TYPE_32BIT_SERVICE_UUID_MORE_AVAILABLE = 0x04; 
	/**< Complete list of 32 bit service UUIDs. */
	public static final short BLE_GAP_AD_TYPE_32BIT_SERVICE_UUID_COMPLETE = 0x05; 
	/**< Partial list of 128 bit service UUIDs. */
	public static final short BLE_GAP_AD_TYPE_128BIT_SERVICE_UUID_MORE_AVAILABLE = 0x06; 
	/**< Complete list of 128 bit service UUIDs. */
	public static final short BLE_GAP_AD_TYPE_128BIT_SERVICE_UUID_COMPLETE = 0x07; 
	/**< Short local device name. */
	public static final short BLE_GAP_AD_TYPE_SHORT_LOCAL_NAME = 0x08; 
	/**< Complete local device name. */
	public static final short BLE_GAP_AD_TYPE_COMPLETE_LOCAL_NAME = 0x09;
	/**< Transmit power level. */
	public static final short BLE_GAP_AD_TYPE_TX_POWER_LEVEL = 0x0A; 
	/**< Class of device. */
	public static final short BLE_GAP_AD_TYPE_CLASS_OF_DEVICE = 0x0D; 
	/**< Simple Pairing Hash C. */
	public static final short BLE_GAP_AD_TYPE_SIMPLE_PAIRING_HASH_C = 0x0E; 
	/**< Simple Pairing Randomizer R. */
	public static final short BLE_GAP_AD_TYPE_SIMPLE_PAIRING_RANDOMIZER_R = 0x0F; 
	/**< Security Manager TK Value. */
	public static final short BLE_GAP_AD_TYPE_SECURITY_MANAGER_TK_VALUE = 0x10; 
	/**< Security Manager Out Of Band Flags. */
	public static final short BLE_GAP_AD_TYPE_SECURITY_MANAGER_OOB_FLAGS = 0x11; 
	/**< Slave Connection Interval Range. */
	public static final short BLE_GAP_AD_TYPE_SLAVE_CONNECTION_INTERVAL_RANGE = 0x12; 
	/**< List of 16-bit Service Solicitation UUIDs. */
	public static final short BLE_GAP_AD_TYPE_SOLICITED_SERVICE_UUIDS_16BIT = 0x14; 
	/**< List of 128-bit Service Solicitation UUIDs. */
	public static final short BLE_GAP_AD_TYPE_SOLICITED_SERVICE_UUIDS_128BIT = 0x15; 
	/**< Service Data. */
	public static final short BLE_GAP_AD_TYPE_SERVICE_DATA = 0x16; 
	/**< Public Target Address. */
	public static final short BLE_GAP_AD_TYPE_PUBLIC_TARGET_ADDRESS = 0x17; 
	/**< Random Target Address. */
	public static final short BLE_GAP_AD_TYPE_RANDOM_TARGET_ADDRESS = 0x18; 
	/**< Appearance. */
	public static final short BLE_GAP_AD_TYPE_APPEARANCE = 0x19; 
	/**< Manufacturer Specific Data. */
	public static final short BLE_GAP_AD_TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF; 

	/**
	 * 解析广播数据
	 * @param type 类型：查看上面常量
	 * @param adv_data 解析包数据
	 * @return 指定类型的数据
	 */
	public static byte[] adv_report_parse(short type, byte[] adv_data)

	{

		int index = 0;

		int length;

		byte[] data;

		byte field_type = 0;

		byte field_length = 0;

		length = adv_data.length;

		while (index < length)

		{

			try

			{

				field_length = adv_data[index];

				field_type = adv_data[index+1];

			}

			catch(Exception e)

			{


				return null;

			}

			if (field_type == (byte)type)

			{

				data = new byte[field_length-1];

				byte i;

				for(i = 0;i < field_length-1;i++)

				{

					data[i] = adv_data[index+2+i];

				}

				return data;

			}

			index += field_length+1;

			if(index >= adv_data.length)

			{

				return null;

			}

		}

		return null;

	}

	/**
	 * 获得本地名称
	 * @param adv_data 广播数据
	 * @return
	 */
	public static String getLocalName(byte[] adv_data){
		byte[] data = adv_report_parse(BLE_GAP_AD_TYPE_COMPLETE_LOCAL_NAME,adv_data);
		if(data != null){
			return byteArrayToGbkString(data,0,data.length);
		}
		return null;
	}
	
	/**
	 * 获得16bit数据包
	 * @param adv_data
	 * @return
	 */
	public static List<String> get16BitServiceUuids(byte[] adv_data){
		List<String> list = new ArrayList<String>();
		/**128bit解析方法*/
//		byte[] data = adv_report_parse(BLE_GAP_AD_TYPE_128BIT_SERVICE_UUID_COMPLETE,adv_data);
//		if(data != null){
//			int size = data.length / 16;
//			for (int i = 0; i < size; i++) {
//				UUID uuid = decodeUuid128(data,i * 16);
//				list.add(uuid.toString());
//			}
//			return list;
//		}
		byte[] data = adv_report_parse(BLE_GAP_AD_TYPE_16BIT_SERVICE_UUID_COMPLETE,adv_data);
		if(data != null){
			for (int i = 0; i < data.length / 2; i++) {
				byte[] by = {data[i * 2 + 1],data[i * 2]};
				String str = bytesToHexString(by,true);
				list.add(str);
			}
			return list;
		}
		return list;
	}

	public static String getShort16(byte[] adv_data){
        byte[] data = adv_report_parse(BLE_GAP_AD_TYPE_16BIT_SERVICE_UUID_COMPLETE,adv_data);
        String dataStr = "";
        if(data != null){
            for (int i = 0; i < data.length / 2; i++) {
                byte[] by = {data[i * 2 + 1],data[i * 2]};
                dataStr += bytesToHexString(by,true);
            }
        }else {
            data = adv_report_parse(BLE_GAP_AD_TYPE_16BIT_SERVICE_UUID_MORE_AVAILABLE,adv_data);
            if(data != null){
                for (int i = 0; i < data.length / 2; i++) {
                    byte[] by = {data[i * 2 + 1],data[i * 2]};
                    dataStr += bytesToHexString(by,true);
                }
            }
        }


        if (dataStr.equals("")){
            return null;
        }else {
            return dataStr;
        }
    }
	
	/**
	 * byte to Hex String
	 * 
	 * @param barray
	 *            byte array
	 * @return hex string
	 */
	private static String bytesToHexString(byte[] barray,boolean flag) {
		if (barray == null) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		String stemp;
		for (int i = 0; i < barray.length; i++) {
			stemp = Integer.toHexString(0xFF & barray[i]);
			if (stemp.length() < 2) {
				sb.append(0);
			}
			sb.append(stemp.toUpperCase());
			// sb.append("  ");
		}
		if(flag){
			return "0x" + sb.toString();
		}
		return sb.toString();
	}
	
	
	public static UUID decodeUuid128(byte[] adv_data,int i){
		int j = decodeUuid32(adv_data, i + 12);
		int k = decodeUuid32(adv_data, i + 8);
		int l = decodeUuid32(adv_data, i + 4);
		int il = decodeUuid32(adv_data, i + 0);
		return new UUID(((long)j << 32) + (0xffffffffL & (long)k),((long)l << 32) + (0xffffffffL & (long)il));
	}
	
	public static int decodeUuid32(byte[] adv_data,int i){
		int j = 0xff & adv_data[i];
		int k = 0xff & adv_data[i + 1];
		int l = 0xff & adv_data[i + 2];
		return j | ((0xff & adv_data[i + 3]) << 24 | l << 16 | k << 8);
	}
	private static String byteArrayToGbkString(byte[] inarray, int offset, int len) {

		String gbkstr = "";
		int idx = 0;
		if (inarray != null) {
			for (idx = 0; idx < len; idx++) {
				if (inarray[idx + offset] == 0x00) {
					break;
				}
			}
			try {
				gbkstr = new String(inarray, offset, idx, "UTF-8");
			} catch (UnsupportedEncodingException e) {
			}
		}

		return gbkstr;
	}
}
