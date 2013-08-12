package org.hustcse.wifirobot;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;

//auto set bond bluetooth
class ClsUtils{
	//Debugging
	private static final String TAG = "clsutils";
	
	static public boolean createBond(Class btClass, BluetoothDevice btDevice)
			throws Exception{
		Method method = btClass.getMethod("createBond");
		Boolean returnValue = (Boolean) method.invoke(btDevice);
		return returnValue;
	}
    static public boolean removeBond(Class btClass, BluetoothDevice btDevice)  
            throws Exception {  
        Method removeBondMethod = btClass.getMethod("removeBond");  
        Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice);  
        return returnValue.booleanValue();  
    }  
    static public boolean cancelPairingUserInput(Class btClass,BluetoothDevice device)		 
    		throws Exception{
    		
		Method createBondMethod = btClass.getMethod("cancelPairingUserInput");
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        return returnValue.booleanValue();
    }
   static  public boolean cancelBondProcess(Class btClass,BluetoothDevice device) 
		   throws Exception{
		  
        Method createBondMethod = btClass.getMethod("cancelBondProcess");
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
	    return returnValue.booleanValue();
   }  	    
   static public  boolean setPin(Class btClass, BluetoothDevice btDevice, String str)
		   throws Exception{
		try {
			Method method = btClass.getDeclaredMethod("setPin", new Class[]{byte[].class});
			Boolean value = (Boolean) method.invoke(btDevice, new Object[] {str.getBytes()});
			Log.e(TAG, ""+value);
		} catch (Exception e) {
			// TODO: handle exception
			Log.e(TAG, "setPin error++");
			return false;
		}
		return true;
   }
	
   static public boolean printAllInform(Class clsShow)
   {
		try {
			Method[] methods = clsShow.getMethods();
			int i = 0;
			for(; i < methods.length; i++)
			{
				Log.e(TAG, methods[i].getName() + ";and the i is:" +i);
			}
			Field[] allFields = clsShow.getFields();
			for(i = 0; i < allFields.length; i++){
				Log.e(TAG, allFields[i].getName());
			}
		} catch (Exception e) {
			// TODO: handle exception
			return false;
		}
		return true;
	}
}
