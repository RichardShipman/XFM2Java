/*

   Richard Shipman's utility program to access XFM2 over serial
   from Java.
  
   Very little error checking or anything else going on here...

   Still need to find out how to bulk read and write to EEPROM.
   
   Uses jSerialComm from https://fazecast.github.io/jSerialComm/
   Compile: javac -cp jSerialComm-2.6.2.jar XFM2.java
   Run:     java -cp jSerialComm-2.6.2.jar:. XFM2

   Copyright © 2020 Richard Shipman 
   richard@shipman.me.uk
   Version 1.0 - 23rd July 2020
   
*/

import com.fazecast.jSerialComm.*;
import java.io.*;
import java.util.Scanner;




public class XFM2 {

   BufferedReader br;
   SerialPort comPort;

   public XFM2 () {
      br=new BufferedReader(new InputStreamReader(System.in));
   }


   public void printSerialPorts() {
      SerialPort[] ports = SerialPort.getCommPorts();
      for (int i=0; i<ports.length; i++) {
         System.out.println("["+i+"]"+" "+ports[i].toString());
      }
   }


   public void setUpPort() {

      this.printSerialPorts();
      System.out.print("Enter device index:");
      String inLine="";
      try {
         inLine = br.readLine();
      } catch (IOException e) {}
//  Check it is a valid int in input
      int selected = Integer.parseInt(inLine);
// Check selected is in correct range
      comPort = SerialPort.getCommPorts()[selected];
      comPort.setBaudRate(500000);
      comPort.setNumDataBits(8);
      comPort.setParity(SerialPort.NO_PARITY);
      comPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
   }


   public void getCommandLoop() {
      char inch=' ';
   	  while (inch != 'x') {
		  System.out.print("Enter a command:");
		  String line="";
		  try {
			 line = br.readLine();
		  } catch (IOException e) {}
//		  System.out.println(line);
	
		  inch = line.charAt(0);
		  
		  switch (inch) {
			 case 'd':
				dump(); 
				break;
			 case 'i':
			 case '1':
			 case '2':
			 case '$':
			 	 int ret= simpleCommand(inch);
			 	 System.out.println("Returned: "+ret);
			 	 break;		 	 
			 case 'r':
			 case 'w':
			 	 eepromPrompt(inch);
			 	 break;
			 case 'g':
			 	getVal();
			 	break;
			 case 's':
			 	setVal();
			 	break;
			 case '*':
			 	 midiUtil();
			 	 break;
			 case '?':
			 	printHelp();
			 	break; 
		   }
      }
   }
   
   public void printHelp() {
		char[] command={'?','d','i','r','w','g','s','1','2','$','f','*','x'};
		String[] hlptxt={"Print out this help",
							  "Dump complete parameter set",
							  "Initialize active program",
							  "Load a program from EEPROM",
							  "Write active program to EEPROM",
							  "Get a single value",
							  "Set a single value",
							  "Activate first unit",
							  "Activate second unit",
							  "Initialize EEPROM. WARNING: All programs will be lost",
							  "*Load a file and send it to the device",
							  "Set MIDI channels or LAYER mode",
		"Exit this program"};
		for (int i=0; i<command.length; i++) {
				System.out.println(command[i]+" : "+hlptxt[i]);
		}
   }
   
   /* 
   Menu call methods 
   */
   
   public void midiUtil() {
      int unit = getNumber("Unit 1 = 10, Unit 2 = 11, Layer Mode = 12");
      int channel = getNumber("Channel (0-16) or Layer mode Off(0)/On(1)");
      midiUtil(unit,channel);
   }
   
   
   public void eepromPrompt(int cmd) {
	   int location = getNumber("Enter a program number (0-127):");
      System.out.println("Location value = "+String.format("0x%02X",location));
   	eepromCmd(cmd,location);
   }

   	
   public void getVal() {
      int location= getNumber("Enter a location to load:");

      int val = getVal(location);
      System.out.println("Location = "+String.format("0x%02X",location));

      System.out.print("Contents :");
      System.out.print(val);
      System.out.println(" :"+String.format("0x%02X",val));

   }      
   
   public void setVal() {
      int location= getNumber("Enter a location to load:");
      int value = getNumber("Enter the value to set it to:");
	  
      System.out.println("Location value = "+String.format("0x%02X",location));
      setVal(location,value);
   }
   
   
   /*
   Communication methods
   */
   
   
   public int midiUtil(int unit, int channel) {
      int retval=-1;
   	
      if (!comPort.isOpen()) {
//         System.out.println("Com port is closed");
         comPort.openPort();
      } else {
//         System.out.println("Com port is already open");
      }
      byte[] buffer = new byte[3];
      buffer[0]='*';
      buffer[1]=(byte)unit;
      buffer[2]=(byte)channel;
//      System.out.println("Location value = "+String.format("0x%02X",buffer[1]));

      comPort.writeBytes(buffer,3);
      retval=readSingleValue();

      comPort.closePort();
      System.out.println();
      return retval;
    
   }
   

   public int eepromCmd(int cmd, int location) {
      int retval=-1;
   	
      if (!comPort.isOpen()) {
//         System.out.println("Com port is closed");
         comPort.openPort();
      } else {
//         System.out.println("Com port is already open");
      }
      byte[] buffer = new byte[2];
      buffer[0]=(byte)cmd;
      buffer[1]=(byte)location;
//      System.out.println("Location value = "+String.format("0x%02X",buffer[1]));

      comPort.writeBytes(buffer,2);
      retval=readSingleValue();

      comPort.closePort();
      System.out.println();
      return retval;
   }
      
   
   public int simpleCommand(int cmd) {
   	int retval=-1;
      if (!comPort.isOpen()) {
//         System.out.println("Com port is closed");
         comPort.openPort();
      } else {
//         System.out.println("Com port is already open");
      }
      byte[] buffer = new byte[1];
      buffer[0]=(byte)cmd;

      System.out.println("Command = "+String.format("0x%02X",buffer[0]));

      comPort.writeBytes(buffer,1);

      System.out.print("Contents :");

      retval=readSingleValue();
      
      comPort.closePort();
      System.out.println();
      
      return retval;
   
   }
   	

   
   public int getVal(int location) {
      int retval=-1;
   	if (!comPort.isOpen()) {
//         System.out.println("Com port is closed");
         comPort.openPort();
      } else {
//         System.out.println("Com port is already open");
      }
      byte[] buffer = new byte[3];
      buffer[0]='g';
      buffer[1]=convertIntToByte(location);
      buffer[2]=getHiByte(location);
      comPort.writeBytes(buffer,3);
      retval=readSingleValue();
      comPort.closePort();
      return retval;
   }
   
   
   public void setVal(int location, int value) {
      if (!comPort.isOpen()) {
//         System.out.println("Com port is closed");
         comPort.openPort();
      } else {
//         System.out.println("Com port is already open");
      }
      byte[] buffer = new byte[4];
      buffer[0]='s'; 
      buffer[1]=convertIntToByte(location);
      if (location>255) {
         buffer[2]=getHiByte(location);
         buffer[3]=(byte)value;
      } else {
      	buffer[2]=(byte)value;
         buffer[3]=0;
      }
//      System.out.println("Location value = "+String.format("0x%02X ",buffer[1])
//      												  +String.format("0x%02X ",buffer[2])
//      												  +String.format("0x%02X ",buffer[3]));
      comPort.writeBytes(buffer,4);
      comPort.closePort();
      System.out.println();
   }
   

   public void dump() {
      if (!comPort.isOpen()) {
//         System.out.println("Com port is closed");
         comPort.openPort();
      } else {
//         System.out.println("Com port is already open");
      }
      byte[] buffer = new byte[2];
      buffer[0]='d';
      comPort.writeBytes(buffer,1);

      while (comPort.bytesAvailable() <1) {}

      int i=0;
      int val=0;
      while (comPort.bytesAvailable() >0) {
         try {
            Thread.sleep(1);
         } catch (InterruptedException ex) {}
//         comPort.readBytes(buffer,1);
//         System.out.print(String.format(" %02X",buffer[0]));
         System.out.print(String.format(" %02X",readSingleValue()));
         if ((i+1)%8==0) {System.out.print(" ");};
         if ((i+1)%16==0) {System.out.println("- "+String.format("0x%02X (",i)+i+")");};
         i++;
      } 
      comPort.closePort();
   	   
   }
   
   
/*
   Utility Stuff
*/


   public static byte convertIntToByte(int inp) {
   	byte b = (byte) (inp);
   	if (inp>255) b=(byte)255;
   	return b;
   }
   
   public static byte getHiByte(int inp) {
   	byte b = convertIntToByte(inp-255);
   	return b;
   }
   
   public int getNumber(String prompt) {
   int retval;
	  System.out.print(prompt);
	  String line="";
	  try {
		 line = br.readLine();
	  } catch (IOException e) {}
	  retval = Integer.parseInt(line);
	  // do some checking stuff....?
	  return retval;
   }
   
   
	public int readSingleValue() {
		int retval=-1;
		byte[] buffer = new byte[1];
		while (comPort.bytesAvailable() <1) {}
	
		comPort.readBytes(buffer,1);
		retval = Byte.toUnsignedInt(buffer[0]);
		return retval;
	}
   
   
/*
   Main method 
*/
   public static void main(String args[]) {
      System.out.println("Make sure that your XFM2 is connected!");
      System.out.println("Select 'Digilent Adept USB Device'");
      XFM2 thing=new XFM2();
      thing.setUpPort();
      thing.getCommandLoop();

      System.exit(0);
   }
}
