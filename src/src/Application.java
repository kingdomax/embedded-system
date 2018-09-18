/************************************************************************/
/* Author: Pramoch Viriyathomrongul (pramoch.vir@student.mahidol.ac.th) */
/*    	   Patboke Jitthamma		(patboke.jit@student.mahidol.ac.th) */
/************************************************************************/
/*  File Description:                                                   */
/*                                                                      */
/*    This project contains 3 important files	  						*/
/*    Application.java			:	custom application					*/
/*    ImplementApiFunction.c 	:	JNI file implements API function	*/
/*    makefile					:	for creating shared library			*/
/*																		*/
/*    Note that, please defines the external jar path to this project 	*/
/*    directory.(org.apache.commons.io.jar)								*/
/*    When the program starts, it also create GUI thread and run 		*/
/* 	  readInput() consistency. If user triggers action listener on any	*/ 
/* 	  element of GUI. Those elements will call their function as defined*/
/*	  																	*/
/* 	  For compiled this project, using these arguments:					*/
/* 	  -Djava.library.path=jni  -Xss65536k -Xmx128m						*/
/*																		*/
/*    For details of API function from dwf.h refer to:             		*/
/*    The WaveForms SDK User's Manual (available in the WaveForms SDK)  */
/*                                                                      */
/************************************************************************/
/*  Revision History:                                                   */
/*                                                                      */
/*  08/11/2014		 : Created                                          */
/*                                                                      */
/************************************************************************/



import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.border.*;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.WindowConstants;

import org.apache.commons.io.FileUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class Application
{
	static { System.loadLibrary("Application"); } 

	// Declare native method
	private native static void sayTschus();							// close all instrument and device					
	private native static void autoConfig();						// 	auto default config all instrument parameter					
	private native static void toggleSupply(int val3);				//	toggle power pin(VCC FIXED SUPPLY), always invoke this function with parameter 1 before call setVoltageSupply(), parameter0 mean disable power pin
	private native static void setVoltageSupply(double val2);		//  set voltage of power pin, parameter is 3.3 and 5 volt
	private native static void setOutputValue(int val);				//	set the output value on all output pins
	private native static boolean sayHallo();						// 	enumerate all connected devices and open the first discovered device, return true if device connected successful otherwise return false
	private native static int getInputValue();						//	read and return input states of all I/O pins
	private native static int getOutputSetValue();					//	returns the currently set output values across all output pins.
	private native static double getVoltage();						//	reads the status of ANALOG IO and return currently voltage
	private native static double getCurrent();						//	reads the status of ANALOG IO and return currently current
	private native static String getNameSN();						//	return name and series number of device
	
	
	// Declare current value of I/O pin
	private static StringBuilder inputPinValue = new StringBuilder("0000000000000000");		// represent currently input value in string pin, initialized by readInput() function
	private static StringBuilder outputPinValue = new StringBuilder("0000000000000000");	// represent currently output value in string pin, accessed by setOutput() function
	
	
	// Declare variable for logic
	private static boolean hasLog = false;
	private static boolean flagRunAll = false;										// boolean for recognize program that ever run hwpAll() function once
	private static boolean stopPower = false;										// boolean for trigger stopped display currently voltage and current 
	private static boolean intermidiateStop = false;								// boolean for trigger stopped processing hwpAll() function
	private static boolean disableButton = false;									// boolean for enable/disable button of GUI
	private static int correctQuery = 0;											// represent line number of executed query
	private static int indicatorTempRead = 0;										// currently line in tempRead arrayList
	private static int logNumber = 0;												// represent number of log
	private static ArrayList<String> tempRead = new ArrayList<String>();			// arrayList<String> is stored text from read file
	private static ArrayList<String> tempWrite = new ArrayList<String>();			// arrayList<String> is stored result for writing text file
	private static ArrayList<String> tagOutput = new ArrayList<String>();			// arrayList<String> is stored output for writing html file
	private static ArrayList<String> tagInput = new ArrayList<String>();			// arrayList<String> is stored input for writing html file
	private static ArrayList<String> tagExpectedInput = new ArrayList<String>();	// arrayList<String> is stored expected input for writing html file	
	private static ArrayList<String> tagLog = new ArrayList<String>();				// arrayList<String> is stored log for writing html file		
	private static int runMode = 0;													// indicated currently state of program (0=quick mode, 1=full mode) 
	
	
	// Declare variable for GUI main Window
	private static ImageIcon iconOff = new ImageIcon("OFF.png");
	private static ImageIcon iconOn = new ImageIcon("ON.png");
	private static JLabel[] pinLabel = {new JLabel(iconOff),new JLabel(iconOff),new JLabel(iconOff),new JLabel(iconOff)
											,new JLabel(iconOff),new JLabel(iconOff),new JLabel(iconOff),new JLabel(iconOff)
											,new JLabel(iconOff),new JLabel(iconOff),new JLabel(iconOff),new JLabel(iconOff)
											,new JLabel(iconOff),new JLabel(iconOff),new JLabel(iconOff),new JLabel(iconOff)};
	private static final ArrayList<JToggleButton> button = new ArrayList<JToggleButton>();
	private static final ArrayList<String> on = new ArrayList<String>();
	private static final ArrayList<String> off = new ArrayList<String>();
	private static final JTextField filename = new JTextField(30);
	private static final JButton open = new JButton("Open");
	private static final JFrame window = new JFrame("Digital I/O Test");	
	private static final JFileChooser fileChooser = new JFileChooser();
	private static final JEditorPane document = new JEditorPane();
	private static final JFileChooser c = new JFileChooser();
	
	
	// Declare variable for GUI sub Window
	private static final JTextArea data = new JTextArea();
	private static final ImageIcon save = new ImageIcon("save.png");
	private static final ImageIcon clear = new ImageIcon("clear.png");
	private static final JButton clearBut = new JButton("Clear",clear);
	private static final JButton saveTXTBut = new JButton("   TXT",save);
	private static final JButton saveHTMLBut = new JButton("HTML",save);
	private static final JScrollPane scroll = new JScrollPane(data,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	private static final JRadioButton quick = new JRadioButton("Quick Run");
	private static final JRadioButton full = new JRadioButton("Full Run");
	private static final ImageIcon run = new ImageIcon("runAll.png");
	private static final ImageIcon next = new ImageIcon("next.png");
	private static final ImageIcon previous = new ImageIcon("previous.png");
	private static final JButton runBut = new JButton(run);
	private static final JButton nextBut = new JButton("Next",next);
	private static final JButton previousBut = new JButton("Pre -",previous);
	private static JList<String> list = new JList<String>();
	private static final JScrollPane scroll2 = new JScrollPane(list,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	private static int indicatorList = 0;
	
	
	/*
	 * 	Starting point of program
	 */
	public static void main(String[] args)
	{   
	   try
	   {
		   if(!Application.sayHallo())		// open device
		   {	
			   JOptionPane.showMessageDialog(null,"Device is not connected !","Warning",JOptionPane.WARNING_MESSAGE);		   
		   }
		   else
		   {
			   Application.autoConfig();	// auto configure hardware first
			
			   Application.Window(); 		// start GUI
	
			   Application.readInput();		// start read input
		   }  
	   }	   
	   catch(Exception e){System.out.println(e.getMessage());}
	}
 
	
	
	/*
	 *	This main thread continuously read input state of allI/O pins
	 *	Parameter	:	none
	 *	Return		:	none
	 *	Logic		:	1.	Repeat looping every 20 millisecond, if there was any modification value
	 *					2.	Extract subset of bit
	 *					3.	Toggle light bulb
	 */
	private static void readInput() throws InterruptedException
	{
		int previousValue = -1;
		int currentValue = 0;
		int temp,i;
  
	   while(true)	// loop read input
	   {
		   currentValue = Application.getInputValue();		// invoke input value
		   if(currentValue != previousValue)				// if there was any modification value
		   {
			   // extract subset of bit
			   for(i=0; i<=15; i++)						
			   {
				   temp = currentValue & ( 1 << 16+i );
				   temp = temp >> 16+i;
				   if(temp>=0&&temp<=9)	{	inputPinValue.setCharAt(15-i, Character.forDigit(temp, 10));	}
				   if(temp==-1)			{	inputPinValue.setCharAt(15-i, '1');							}
			   }
			  
			   // Turn Light ON/OFF 
			   for(i=0; i<=15; i++)
			   {
				   if(inputPinValue.charAt(i)=='1')
				   {
					   pinLabel[15-i].setIcon(iconOn);
				   }
				   else
				   { 
					   pinLabel[15-i].setIcon(iconOff);			
				   }
			   }		   		   			   
			  		   
				previousValue = currentValue;		// update new input value
		   }
		   else
		   {
			   TimeUnit.MILLISECONDS.sleep(20);		// protect memory run 100%
		   }
	   }// end loop
   }
	
	
	
	/*	
	 *	GUI MAIN WINDOW 
	 *	
	 *	Important elements
	 *	1.	Menu bar		:	eMenuItem variable 
	 *							invoke sayTschus() native function when action is performed
	 *	
	 *	2.	Power button	:	power variable 
	 *							invoke toggleSupply(), getVoltage(), getCurrent() native function when action is performed
	 *
	 *	3. 	Voltage button	:	first and second variable 	
	 *							invoke setVoltageSupply() native function, parameters are 3 and 5.5 respectively.
	 *
	 *	4.	Bulb16-31		:	pinLabel[] variable
	 *							is accessed by readInput() method.
	 *
	 * 	5.	Button0-15		:	button variable
	 * 							call setOutputByPin() function, parameters are their own number.
	 * 
	 * 	6.	Open file button:	open variable
	 * 							call readFile(), clearStorage() and OpenNewWindow() method when action is performed.	
	 */
	private static void Window()
	{
	   final JMenuBar menubar = new JMenuBar();
		
		// Menu Bar ------------------------------------------------------------------------------------------------
		JMenu file = new JMenu("File");
		file.setMnemonic(KeyEvent.VK_F);

		JMenuItem eMenuItem = new JMenuItem("Exit");
		eMenuItem.setMnemonic(KeyEvent.VK_E);
		eMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event) 
			{
				Application.sayTschus();
				System.exit(0);
			}
		});

		file.add(eMenuItem);

		menubar.add(file);
		//---------------------------------------------------------------------------------------------------------
		
		//Digital Supply -------------------------------------------------------------------------------------------------
		JPanel panelNorth = new JPanel(new FlowLayout(3,100,3));
		Border borderNorth = BorderFactory.createTitledBorder("Digital Supply");
		panelNorth.setBorder(borderNorth);
		final Box box1 = Box.createVerticalBox();
		final Box box2 = Box.createVerticalBox();

		final JRadioButton first = new JRadioButton("3.3 Volt");
		final JLabel text = new JLabel("Voltage (V) :", JLabel.CENTER);
		final JTextField displayA =new JTextField(6);

		final JRadioButton second = new JRadioButton("5 Volt");
		final JLabel text2 = new JLabel("Current (A) :", JLabel.CENTER);
		final JTextField displayB =new JTextField(6);
		
		// power button
		ImageIcon img = new ImageIcon("powerOFF.png");
		final JToggleButton power = new JToggleButton("Power Off",img);
		power.setBorder(BorderFactory.createEmptyBorder());
		power.setContentAreaFilled(false);
		
		power.addActionListener(new ActionListener()
		{
	           @Override
	           public void actionPerformed(ActionEvent evt) 
	           {
	               
	               if (power.isSelected())
	               {
	            	   	stopPower = false;
	            	   	Application.toggleSupply(1);				// turn on fix supply channel		
	            	   	
	            	   	power.setText("POWER ON");
      	               	power.setIcon(new ImageIcon("powerON.png"));
      	               	
      	               	first.setEnabled(true);
      	        		text.setEnabled(true);
      	        		displayA.setEnabled(true);
      	        		first.setSelected(true);
      	        		
      	        		second.setEnabled(true);
      	        		text2.setEnabled(true);
      	        		displayB.setEnabled(true);
      	        			
    	        		
		               Thread t = new Thread() 
		               {
		                   @Override
		                   public void run() 
		                   { 
		                	   while(true)
		                	   {
			                		if (stopPower) break;
			                		displayA.setText(new DecimalFormat("#.###").format(Application.getVoltage()));	// display voltage level
			                		displayB.setText(new DecimalFormat("#.###").format(Application.getCurrent()));	// display current level
			                			   	
			                		try {sleep(700);} 
			                		catch (InterruptedException ex) {}
		                	   }
		                   }
		               };
		               t.start();	// call back run()
	               }	               
	               else
	               {
	            	    stopPower = true;
	            	    Application.toggleSupply(0);	// turn off fix supply channel
	            	    displayA.setText(null);
	            	    displayB.setText(null);
	            	    
	            	    
	            	    power.setText("POWER Off");
			            power.setIcon(new ImageIcon("powerOFF.png"));
			            
			        	first.setEnabled(false);
		        		text.setEnabled(false);
		        		displayA.setEnabled(false);
		        		first.setSelected(true);
		        		
		        		second.setEnabled(false);
		        		text2.setEnabled(false);
		        		displayB.setEnabled(false);	   
	               }
	           }// end action perform
	     });// end actionListener
	  
		   panelNorth.add(power) ;
		
   
		// radio button
		displayA.setEditable(false);
		first.setMnemonic(KeyEvent.VK_A);
		box1.add(first);
		box1.add(text);
		box1.add(displayA);

		first.setEnabled(false);
		text.setEnabled(false);
		displayA.setEnabled(false);
		
		displayB.setEditable(false);
		second.setMnemonic(KeyEvent.VK_B);
		box2.add(second);
		box2.add(text2);
		box2.add(displayB);
		
		second.setEnabled(false);
		text2.setEnabled(false);
		displayB.setEnabled(false);
		
		ButtonGroup group = new ButtonGroup();
		group.add(first);
		group.add(second);
		
		 first.addChangeListener(new ChangeListener()
	       {
	           @Override
	           public void stateChanged(ChangeEvent event) 
	           {
	               if (first.isSelected())
	               {
	            	   Application.setVoltageSupply(3.3);
	               }
	           }
	       }); 
		 
		 second.addChangeListener(new ChangeListener()
	       {
	           @Override
	           public void stateChanged(ChangeEvent event) 
	           {
	               if (second.isSelected())
	               {
	            	   Application.setVoltageSupply(5);
	               }
	           }
	       });
		 

		panelNorth.add(box1);
		panelNorth.add(box2);
		

		//----------------------------------------------------------------------------------
		
		// All West Side --------------------------------------------------------------------------------------
		
		JPanel panelWest = new JPanel(new GridLayout(4,4,60,40));
		Border borderWest = BorderFactory.createTitledBorder("INPUT");
		panelWest.setBorder(borderWest);

		
		int num;
		/*	Set Panel
		 *	Note 	: 	pinLabel[0] = pin16
		 *			 	pinLabel[15] = pin31	*/
       for(num=0; num<16; num++)
       {	   	
	   	   	pinLabel[num].setText( "PIN"+ (num+16) );
	   	   	pinLabel[num].setHorizontalTextPosition(JLabel.CENTER);
	   	   	pinLabel[num].setVerticalTextPosition(JLabel.BOTTOM);
	   	   	panelWest.add(pinLabel[num]);
       }
		
       //-----------------------------------------------------------------------------------------------------
       

       // All East Side --------------------------------------------------------------------------------------
		JPanel panelEast = new JPanel(new GridLayout(4,4,50,80));
		Border borderEast = BorderFactory.createTitledBorder("OUTPUT SWITCH");
		panelEast.setBorder(borderEast);
		
		int count;
		
		// declare off/on text and attach string with button 
		for(count=0; count<16; count++)
		{
			off.add("PIN"+count+"\n[ Off ]");
			on.add("PIN"+count+"\n[ ON ]");
			button.add(new JToggleButton("<html>"+off.get(count).replaceAll("\\n", "<br>")+"</html>"));
			
			panelEast.add(button.get(count));	// add button to panel
		}
	

       // add action to all button
       for(count=0; count<button.size(); count++)
       {
    	   final int x = count;   // important
    	   button.get(count).addChangeListener(new ChangeListener()
    	   {

    		   @Override
    		   public void stateChanged(ChangeEvent event)
    		   {
    			   if(!disableButton)
    			   {
	    			   if (button.get(x).isSelected())
	                   {
	    				   	button.get(x).setText("<html>"+on.get(x).replaceAll("\\n", "<br>")+"</html>");
		                   	Application.setOutputByPin(x);
	                   } 
	                   else 
	                   {
	                	   	button.get(x).setText("<html>"+off.get(x).replaceAll("\\n", "<br>")+"</html>");
		                   	Application.setOutputByPin(x);
	                   }
    			   }
    		   }		   
    	   });   	    
       }// end for 
       
       //-----------------------------------------------------------------------------------------------------
       
       // All File Import-------------------------------------------------------------
       JPanel panelSouth = new JPanel(new FlowLayout(3,50,3));
       Border borderSouth = BorderFactory.createTitledBorder("File Import");
       panelSouth.setBorder(borderSouth);
     		
       final JLabel nameFile = new JLabel("File :", JLabel.CENTER);
     		
       
       open.addActionListener(new ActionListener()
	   {
		   @Override
		   public void actionPerformed(ActionEvent evt) 
		   {
			   	int rVal = c.showOpenDialog(window); 
	    		if (rVal == JFileChooser.APPROVE_OPTION) 		// if choose file, open new window
	    		{
		    		try
		    		{
		    			Application.readFile();					// read file
		    			Application.clearStorage();				// clear storage data every time when you open sub window
		    			open.setEnabled(false);					// disable read file button
		    			Application.OpenNewWindow();			// lastly, open sub window
		    		} 
		    		catch (Exception e1) {e1.printStackTrace();}    			
	    		}
		   }		   
	   });   	     
       
       
       filename.setEditable(false);
     		  
       panelSouth.add(nameFile);
       panelSouth.add(filename);
       panelSouth.add(open);

       //------------------------------------------------------------------------------------

        window.setSize(770, 680);
       	window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() 
        {
            public void windowClosing(WindowEvent ev) 
            {
            	Application.sayTschus();
            	System.exit(0);
            }
        });
        window.add(panelWest, BorderLayout.LINE_START);
		window.add(panelEast, BorderLayout.LINE_END);
		window.add(panelNorth, BorderLayout.PAGE_START);
		window.add(panelSouth, BorderLayout.PAGE_END);
		window.setJMenuBar(menubar);
		window.setResizable(false);
		window.setVisible(true);
	 
	}
	
	
	
	/*	
	 *	GUI SUB WINDOW 
	 *	
	 *	Important elements
	 *	1.	Quick mode check box	:	quick variable
	 *									call clearButton(), setEnableButton(false) and setOutputValue(currentSetOutput) function when action is performed.
	 *
	 * 	2.	Full mode check box 	:	full variable
	 * 									call setEnableButton(true) and setOutputManual(Application.decimalToBinary(Application.getOutputSetValue())) function.
	 *							
	 *	3.	Previous button			:	previousBut variable
	 *									call hwpStepBackward() function when action is performed.
	 *
	 *	4.	Run button				:	runBut variable
	 *									call hwpAll() function with performed in a new thread.
	 *
	 *	5.	Next button				:	nextBut variable
	 *									call hwpStepForward() function when action is performed.
	 *
	 *	6.	Clear button			:	clearBut variable
	 *									call clearStorage() with performed in a new thread.
	 *
	 *	7.	Save text button		:	saveTXTBut variable
	 *									call writeText() when action is performed.
	 *
	 *	8.	Save HTML button		:	saveHTMLBut variable
	 *									call writeHTML() when action is performed.	
	 */
	private static void OpenNewWindow() throws InterruptedException
	{
		final JFrame window2  = new JFrame();
		
		// Code for List-----------------------------------------------------------
		int MAX = tempRead.size();
		String[] x = new String[MAX];
		int stimmapNum = 1;
		
		for(int i = 0; i< MAX; i++)
		{
			String[] tmp = tempRead.get(i).split(" ");
			
			if(tmp[0].equals("log"))
			{
				x[i] = tmp[0]; 			
			}
			else if(tmp[0].equals("stimmap"))
			{
				x[i] = tmp[0] + stimmapNum;
				stimmapNum++;
			}
			else
			{
				x[i] = tempRead.get(i); 
			}
		}
		
		
		// Radio Box (quick and full mode)----------------------------------------------------------------
		final ButtonGroup group = new ButtonGroup();
		group.add(quick);
		group.add(full);
		final Box radioBox = Box.createVerticalBox();
		radioBox.setBorder(BorderFactory.createTitledBorder("Mode"));
		radioBox.add(quick);
		radioBox.add(Box.createRigidArea(new Dimension(0,10)));
		radioBox.add(full);
		
		
		quick.addChangeListener(new ChangeListener()
		{
           @Override
           public void stateChanged(ChangeEvent event) 
           {
        	   if (quick.isSelected())
               {   
        		   runMode = 0;	// set run mode to 0(quick mode)
        		   int currentSetOutput = 	Application.getOutputSetValue();
        		   
        		   try 
        		   {
        			 Application.clearButton();								// clear button first
        			 Application.setEnableButton(false);					// disable button in main window
        		   } 				
        		   catch (InterruptedException e) {e.printStackTrace();}				
	   				
            	   Application.setOutputValue(currentSetOutput);			// set current output value
               }
           }
       }); 
		
		
		full.addChangeListener(new ChangeListener()
       {
           @Override
           public void stateChanged(ChangeEvent event) 
           {
               if (full.isSelected())
               {
            	   
            	   runMode = 1;							// set run mode to 1(full mode)
	   				
              		try 
              		{
              			Application.setEnableButton(true);		// after select, enable all button in main window
              			// invoke JNI function that return current set value and click each button
						Application.setOutputManual(Application.decimalToBinary(Application.getOutputSetValue()));
					} catch (InterruptedException e) {e.printStackTrace();}
              		
               }
           }
       });
		
		// Control Box-------------------------------------------------------------------
		final Box runBox = Box.createHorizontalBox();
		previousBut.setVerticalTextPosition(AbstractButton.CENTER);
		previousBut.setHorizontalTextPosition(AbstractButton.LEADING);
		runBut.setHorizontalTextPosition(AbstractButton.CENTER);
		nextBut.setVerticalTextPosition(AbstractButton.CENTER);
		nextBut.setHorizontalTextPosition(AbstractButton.TRAILING);
		runBox.add(previousBut);
		runBox.add(Box.createRigidArea(new Dimension(0,5)));
		runBox.add(runBut);
		runBox.add(Box.createRigidArea(new Dimension(0,5)));
		runBox.add(nextBut);
		
		previousBut.addActionListener(new ActionListener()
	       {
				public void actionPerformed(ActionEvent e)
				{
	              
				   try 
				   {
					   Application.hwpStepBackward();
				   } 
				   catch (InterruptedException e1) {}
			
	           }
	       });
		
		
		runBut.addActionListener(new ActionListener()
	    {
			public void actionPerformed(ActionEvent e)
			{
				try 
		         {
					Application.clearStorage(); 		// clear data before run all again
		         } 
		         catch (InterruptedException e2) {}
						
				Thread all = new Thread()
				{   
					@Override
					public void run() 
					{ 
						try 
						{
							Application.hwpAll();
						} 
						catch (InterruptedException e1) {}
					}
				};
				all.start();
			}
		});
		
		
		nextBut.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try 
						   {
							   Application.hwpStepForward();
						   } 
						   catch (InterruptedException e1) {}
			}
		});
	
		
		// Radio Box + Control Box-----------------------------------------------------------
		final Box bigBox = Box.createHorizontalBox();
		bigBox.add(radioBox);
		bigBox.add(Box.createRigidArea(new Dimension(10,0)));
		bigBox.add(runBox);
		bigBox.add(Box.createRigidArea(new Dimension(0,10)));
		
		// Result Box
		scroll.setPreferredSize(new Dimension(30,30));
		data.setSize(30,30);
		final Box dataBox = Box.createVerticalBox();
		data.setEditable(false);
		dataBox.setBorder(BorderFactory.createTitledBorder("Result"));
		dataBox.add(scroll);	
			
		// Radio Box + Control Box + Result Box-----------------------------------------------
		final Box bigBox2 = Box.createVerticalBox();
		bigBox2.add(bigBox);
		bigBox2.add(Box.createRigidArea(new Dimension(0,10)));
		bigBox2.add(dataBox);
		
		// List Box-----------------------------------------------------------------------------
		list = new JList<String>(x);
		final JScrollPane scroll2 = new JScrollPane(list,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		list.setEnabled(false);
		list.setSize(10,20);
		final Box listBox= Box.createVerticalBox();
		listBox.setBorder(BorderFactory.createTitledBorder("List"));
		listBox.add(scroll2);

		// Save Box-----------------------------------------------------------------------------
		final Box saveBox = Box.createVerticalBox();
		saveBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 30));
		saveBox.add(clearBut);
		saveBox.add(Box.createRigidArea(new Dimension(0,10)));
		saveBox.add(saveTXTBut);
		saveBox.add(Box.createRigidArea(new Dimension(0,10)));
		saveBox.add(saveHTMLBut);
		
		clearBut.addActionListener(new ActionListener()
		{		 			 
              public void actionPerformed(ActionEvent e)
              {
            	 Thread clear = new Thread()
				   {   
					   @Override
					   public void run() 
					   { 
						    data.setText("");
						    try 
						   	{
						   		Application.clearStorage(); // clear data storage and button
						   	} 
						   	catch (InterruptedException e1) {}	
					   }
				   };
				   clear.start();
				   list.clearSelection();;
              }
		});
		
		saveTXTBut.addActionListener(new ActionListener() 
		{		 			 
              public void actionPerformed(ActionEvent e)
              { 			            	
            	  try 
            	  {
					Application.writeText();
            	  } 
            	  catch (Exception e1) {e1.printStackTrace();}
              }
          });
		
		saveHTMLBut.addActionListener(new ActionListener() 
		{		 			 
              public void actionPerformed(ActionEvent e)
              { 			            
            	  try 
            	  {
					Application.writeHtml();
            	  } 
            	  catch (Exception e1) {e1.printStackTrace();}
              }
          });

		// List Box + Save Box----------------------------------------------------
		final Box bigBox3 = Box.createVerticalBox();
		bigBox3.add(listBox);
		bigBox3.add(Box.createRigidArea(new Dimension(0,15)));
		bigBox3.add(saveBox);
		bigBox3.add(Box.createRigidArea(new Dimension(0,15)));

		//-----------------------------------------------------------------------
		
		window2.setSize(540, 500);
		window2.add(bigBox2, BorderLayout.LINE_START);
		window2.add(bigBox3, BorderLayout.LINE_END);
		window2.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		window2.addWindowListener(new WindowAdapter() 
        {
            public void windowClosing(WindowEvent ev) 
            {
            	intermidiateStop = true;			// trigger stop loop in hwpAll() function
            	open.setEnabled(true);
            	try 
            	{
            		Application.setEnableButton(true);	// enable button in main window
            		Application.clearButton();		
            		Application.clearStorage();
            	} 
            	catch (InterruptedException e) {e.printStackTrace();}
            	Application.setOutputValue(0);
            	window2.dispose();
            }
        });
        window2.setResizable(false);
        
        quick.setSelected(true);
		Application.clearButton();
		Application.setOutputValue(0);
		Application.setEnableButton(false);
		runMode = 0;
		window2.setVisible(true);
	}
	
	
	/*	
	 * 	This function executes all queries in the same time.
	 * 	Parameter	:	none
	 *	Return		: 	none
	 * 	Logic 		:	1.	If user ever ran this function once, call clearStorage() function first
	 * 					2. 	Looping call hwpProcess() using each query as the parameter
	 */
	private static void	hwpAll() throws InterruptedException
	{				
		if(flagRunAll)						// if user want to run again after run all once, we have to clear storage
		{
			Application.clearStorage();	
			flagRunAll = false;
		}
		
		
		for(int i=0; i<tempRead.size(); i++)			
		{
			if(intermidiateStop)			// if user closed the window or click the clear button immediately
			{
				intermidiateStop = false;		
				Application.clearStorage();
				break;						// stop this loop	
			}
			
			list.setSelectedIndex(indicatorList++);// set ���������͹ŧ
			TimeUnit.MILLISECONDS.sleep(20);
			Application.hwpProcess(tempRead.get(i));	// process each line of tempRead
			TimeUnit.MILLISECONDS.sleep(20);
			indicatorTempRead++;
		}
		

		flagRunAll = true;					// boolean for recognize program ever call this function
		Application.enableMenuButton(true);	
	}

	
	
	/*	
	 * This function executes previous query.
	 * 	Parameter	:	none
	 *	Return		: 	none
	 * 	Logic 		:	1. 	Check index out of bound
	 * 					2.	Rollback previous value in all storage
	 * 					3.	Display currently result in text area
	 */
	private static void hwpStepBackward() throws InterruptedException
	{				
		// protect index out of bound 
		if(indicatorTempRead == 0)
		{
			JOptionPane.showMessageDialog(null,"This is the first top query.","Warning",JOptionPane.WARNING_MESSAGE);
		}
		// rollback value regarding previous result in tempWrite
		else															
		{			
			TimeUnit.MILLISECONDS.sleep(10);
			
			String previousWrite[] = tempWrite.get(tempWrite.size()-1).split(" ");		// String stores previous result
			if(previousWrite[0].equals("Incorrect"))	
			{
				tempWrite.remove(tempWrite.size()-1);
				tempWrite.remove(tempWrite.size()-1);
			}
			else if(previousWrite[0].equals("error"))
			{
				tagOutput.remove(tagOutput.size()-1);
				tagInput.remove(tagInput.size()-1);
				tagExpectedInput.remove(tagExpectedInput.size()-1);		
				correctQuery--;
				
				tempWrite.remove(tempWrite.size()-1);
				tempWrite.remove(tempWrite.size()-1);
				tempWrite.remove(tempWrite.size()-1);
				tempWrite.remove(tempWrite.size()-1);
				tempWrite.remove(tempWrite.size()-1);
				
			}
			// ELSE IF previous query is log
			else							
			{
				logNumber--;
				if(logNumber == 0){	hasLog=false; }
				tagLog.remove(tagLog.size()-1);
				
				tempWrite.remove(tempWrite.size()-1);		
			}			
			
			
			
			// we need to set up the output regarding previous query and display result in test area
			if( runMode==1 )				
			{
				if(tagOutput.size() >= 1)
				{
					previousWrite = tempWrite.get(tempWrite.size()-1).split(" ");
					if(previousWrite[0].equals("error"))
					{
						if(tagOutput.get(tagOutput.size()-1).contains("!"))
						{
							String[] outputIndent = tagOutput.get(tagOutput.size()-1).split("!");		// remove tag log from storage of set output
							Application.setOutputManual(Application.removeSpace(outputIndent[0]));	
						}
						else
						{							
							Application.setOutputManual(Application.removeSpace(tagOutput.get(tagOutput.size()-1)));	
						}
					}
					else{	Application.clearButton();	}
				}
			}
			else
			{
				if(tagOutput.size() >= 1)
				{
					previousWrite = tempWrite.get(tempWrite.size()-1).split(" ");
					if(previousWrite[0].equals("error"))
					{
						if(tagOutput.get(tagOutput.size()-1).contains("!"))
						{
							String[] outputIndent = tagOutput.get(tagOutput.size()-1).split("!");		// remove tag log from storage of set output
							Application.setOutputQuick(Application.removeSpace(outputIndent[0]));	
						}
						else
						{
							
							Application.setOutputQuick(Application.removeSpace(tagOutput.get(tagOutput.size()-1)));	
						}
					}
					else{	Application.setOutputValue(0);	}
				}
			}						
			Application.showText();	
			
			
			
			indicatorTempRead--;					// decreasing prepare read line in tempRead			
			indicatorList -= 2;
			if(indicatorList<0)
			{
				list.clearSelection();
				indicatorList = 0;
			}
			else
			{
				list.setSelectedIndex(indicatorList);
				indicatorList++;
			}
		}
	}
	
	
	
	/*	
	 * 	This function executes next query.
	 * 	Parameter	:	none
	 *	Return		: 	none
	 * 	Logic 		:	1.	Check index out of bound
	 * 					2.	Move down indicator list in GUI
	 * 					3.	Call hwpProcess() function
	 */
	private static void hwpStepForward() throws InterruptedException
	{	
		
		// protect index out of bound
		if(indicatorTempRead == tempRead.size())
		{
			JOptionPane.showMessageDialog(null,"No more query in your file","Warning",JOptionPane.WARNING_MESSAGE);
		}
		else
		{
			list.setSelectedIndex(indicatorList++);		
			TimeUnit.MILLISECONDS.sleep(20);
			Application.hwpProcess(tempRead.get(indicatorTempRead++));					
			
			Application.enableMenuButton(true);	
		}
	}
	
	
	
	/*	
	 * 	The task of this method is mapped the string to its appropriate function.
	 * 	Parameter	:	a query string from reading file
	 *	Return		: 	none
	 * 	Logic 		:	1.	Split received string by space
	 * 					2.	If query is stimmap, call compare() function
	 * 						If query is log, keep log in storage and display in text area
	 * 					3.	Otherwise, record and display error message 
	 */
	private static void	hwpProcess(String aLine) throws InterruptedException
	{
		String[] words =  aLine.split(" ");			
		
		
		// stimmap command
		if( words[0].equals("stimmap") && words.length >= 3 && words[1].contains("_") && words[2].contains("|") )
		{
			String[] bits = words[1].split("_");				// String represent bit count
			String[] series = words[2].split("\\|");
			

			if( bits[1].equals("04") || bits[1].equals("4") )	//process for 4 bits here
			{			
				if( series[0].length() != 4 && series[1].length() != 4 )
				{
					Application.recordError(aLine, "Incorrect bit series");
				}
				else
				{
					Application.compare(4, series[0], series[1]);	
				}					
			}
			else if( bits[1].equals("08") || bits[1].equals("8") )	//process for 8 bits here
			{
				
				if( series[0].length() != 8 && series[1].length() != 8 )
				{
					Application.recordError(aLine, "Incorrect bit series");
				}
				else
				{
					Application.compare(8, series[0], series[1]);
				}
				
			}
			else if( bits[1].equals("16") )	//process for 16 bits here
			{
				
				if( series[0].length() != 16 && series[1].length() != 16 )
				{
					Application.recordError(aLine, "Incorrect bit series");
				}
				else
				{
					Application.compare(16, series[0], series[1]);
				}
			}
			else
			{
				Application.recordError(aLine, "Incorrect bit set");
			}				
		}
		// log query
		else if( words[0].equals("log") && words.length >= 2 )
		{
			if(!hasLog){	hasLog= true;	}
			else		{	logNumber++;		}
			String sentence = "";
			for(int j=1; j<words.length; j++)
			{
				sentence += words[j]+" ";
			}							
			tempWrite.add("\n"+sentence);
			tagLog.add(sentence);
			Application.showText();			
		}
		// incorrect query
		else
		{
			Application.recordError(aLine, "Incorrect command");
		}	
	}
	
	
	
	/*
	 * 	This method sets output pin value regarding the query, then compare the received input with expected input of query
	 * 	Parameter	:	count of bits(4-8-16), output string, expected input string
	 * 	Return		:	none
	 * 	Logic		:	1.	Set output regarding currently mode of program(quick or full)
	 * 					2.	Compare received input with expected input
	 * 					3.	Keep all result into all storage
	 */
	private static void compare(int bits, String outputStr, String expectInputStr) throws InterruptedException
	{
		// quick mode
		if( runMode==0 )								 		
		{
			Application.setOutputQuick(outputStr);		// set output by calculate value and invoke function directly
			
		}
		// full mode
		else if( runMode==1 )
		{
			Application.setOutputManual(outputStr);		// set output by click each button 
			
		}
			
		
		
		// Then comparing input from device with expected input
		String realInputStr = (inputPinValue+"").substring(16-bits);	// cast stringbuilder to string for comparing
		String errorBits ="";
		boolean delLast = false;
		int index;
		for(index=0; index<bits; index++)
		{
			if( expectInputStr.charAt(index) != '-' )
			{
				if( expectInputStr.charAt(index) != realInputStr.charAt(index))
				{
					errorBits += ((bits-1)-index) +",";
					delLast = true;
				}
			}
		}
		if(delLast)
		{
			errorBits = errorBits.substring(0,(errorBits.length()-1));
		}
		
		
		
		// store result to tagOutput, tagInput, tagExpectedInput, tempWrite
		String indentOutputStr = "";
		String indentRealInputStr = "";
		String indentExpectInputStr = "";
		for(index=0; index<outputStr.length(); index++)		// add indent for clearly look
		{
			if( index==4 || index==8 || index==12 )
			{
				indentOutputStr += " ";
				indentRealInputStr += " ";
				indentExpectInputStr += " ";
			}
			indentOutputStr += outputStr.charAt(index);
			indentRealInputStr += realInputStr.charAt(index);
			indentExpectInputStr += expectInputStr.charAt(index);
		}			
		tempWrite.add("\n"+(++correctQuery)+".");
		tempWrite.add("set output   : "+indentOutputStr);
		tempWrite.add("read input   : "+indentRealInputStr);
		tempWrite.add("expect input : "+indentExpectInputStr);
		tempWrite.add("error bits   : "+errorBits);
		Application.showText();			
		// attach logNumber with outputStr after first log was in storage
		String tag = ""; 
		if(hasLog)		
		{
			tag = "!"+logNumber; 	   
		}
		tagOutput.add(indentOutputStr+tag);
		tagInput.add(indentRealInputStr);
		tagExpectedInput.add(indentExpectInputStr);			
	}
	
	
	
	/*	
	 * 	This is a component function which remove the space out of a string
	 * 	Parameter	:	String with space
	 *	Return		: 	String without space
	 * 	Logic 		:	Split string into array and concatenate all of them
	 */
	private static String removeSpace(String spaceStr)
	{
		String[] partStr = spaceStr.split(" ");		
		String str="";
		for(int i=0; i<partStr.length; i++)
		{
			str += partStr[i];
		}
		return str;
	}
		
	
	
	/*	
	 * 	This is a component function which enable clearButton, saveTextButton, saveHtmlButton in sub GUI window
	 * 	Parameter	:	none
	 *	Return		: 	none
	 * 	Logic 		:	Set those 3 button to true
	 */
	private static void enableMenuButton(boolean b)
	{
		saveTXTBut.setEnabled(b);
		saveHTMLBut.setEnabled(b);
		clearBut.setEnabled(b);
	}
	

	
	/*	
	 * 	This is a component function which setting output by clicking each button
	 * 	Parameter	:	output string
	 *	Return		: 	none
	 * 	Logic 		:	1.	Call clearButton() function
	 * 					2.	Automatically select each button regarding output string
	 */	
	private static void setOutputManual(String outputStr) throws InterruptedException
	{
		//unclick all button
		Application.clearButton();
		TimeUnit.MILLISECONDS.sleep(100);
		
		
		//setting new output by automatic click button
		int lastPos = outputStr.length()-1;
		int realPos, index;
		for(index=0; index<outputStr.length(); index++)
		{
			realPos = lastPos-index;
			if(outputStr.charAt(index) == '1')
			{
				if(!button.get(realPos).isSelected())
				{
					button.get(realPos).doClick();
					TimeUnit.MILLISECONDS.sleep(50);
				}
			}
		}
	}
	
	
	
	/*	
	 * 	This is a component function which setting output by invoke setOutputValue() directly
	 * 	Parameter	:	output string
	 *	Return		: 	none
	 * 	Logic 		:	1.	Convert output string to decimal value
	 * 					2.	Then invoke setOutputValue() native method
	 */
	private static void setOutputQuick(String outputStr) throws InterruptedException
	{
		// calculate output from string directly
		int valueOutput = 0;
		int lastBitt = outputStr.length()-1;
		for(int i=lastBitt; i>=0; i--)		
		{
			if( Character.digit(outputStr.charAt(i),10) == 1 )
			{
				 valueOutput += (int)( Math.pow(2, lastBitt-i) );
			}
		}
		Application.setOutputValue(valueOutput);	// invoke setting output value	
		TimeUnit.MILLISECONDS.sleep(20);
	}

	
	
	/*
	 *	This method sets output by toggle(AND OPERATION) specific pin
	 *	Parameter	:	pin number
	 *	Return		:	none
	 *	Logic		:	1.	Toggle specific pin of outputPinValue 
	 *					2.	Convert binary string to decimal value
	 *					3.	Invoke setOutputValue() native function
	 */   
	private static void setOutputByPin(int pin)
   {
	   int fsOutput=0, i;

	   if(pin>=0 && pin<16)
		{
				// toggle specific pin
			   outputPinValue.setCharAt(15-pin,  Character.forDigit( Character.digit(outputPinValue.charAt(15-pin),10) ^ 1 , 10 ));
 
			   // calculate binary value to decimal value
			   for(i=15; i>=0; i--)
			   {
				   if( Character.digit(outputPinValue.charAt(i),10) == 1 )
				   {
					   fsOutput += (int)( Math.pow(2, 15-i) );
				   }
			   }

			   Application.setOutputValue(fsOutput);	// invoke setting output value
		}  
   }

    
	
	/*	
	 * 	This is a component function which store error message and original query to tempWrite variable
	 * 	Parameter	:	original message, error message
	 *	Return		: 	none
	 * 	Logic 		:	1.	Store
	 * 					2.	Call showText() function
	 */
	private static void recordError(String aLine, String error)
   {		
		tempWrite.add("\n"+aLine);
		tempWrite.add(error);
			
		Application.showText();
   }
	
	
	
	/*	
	 * 	This is a component function which represents result in text area.
	 * 	Parameter	:	none
	 *	Return		: 	none
	 * 	Logic 		:	1.	Looping to concatenate all string in tempWrite variable
	 * 					2.	Display result and adjust appropriate scroll bar
	 */
	private static void showText()
	{
		String tmp ="";
		for(int h=0; h<tempWrite.size(); h++)
		{
			tmp += tempWrite.get(h)+"\n";
		}
		
		data.setText(tmp);
		data.setCaretPosition(data.getDocument().getLength());
	}
   
   	
	
	/*
	 *	This function saves the result into TEXT file.
	 *	Parameter	:	none
	 *	Return		:	none
	 *	Logic		:	1.	After define name of text file, looping through tempWrite variable
	 *					2.	Copy each text from arrayList<String> and write to file
	 */
	private static void writeText() throws Exception 
   {	
		int returnVal = c.showSaveDialog(window);
   	 	String filePath = c.getSelectedFile().getAbsolutePath()+".txt";	 	 	
   
   	 	if (returnVal == JFileChooser.APPROVE_OPTION)  
	    {
			try
			{
				PrintWriter aFile = new PrintWriter(filePath); 
				for(int z=0; z<tempWrite.size(); z++)
				{
					aFile.println(tempWrite.get(z));
				}
				
				JOptionPane.showMessageDialog(null,c.getSelectedFile().getName()+".txt"+" "+"is Saved!"); 
				aFile.close();	
			}
			catch(Exception e){	e.printStackTrace(); }
		}		
	}
   
   
	
	/*
	 *	This function saves the result into HTML file.
	 *	Parameter	:	none
	 *	Return		:	none
	 *	Logic		:	1.	After define name of HTML file
	 *					2.	Copy all thing of prepared HTML template in to a string
	 *					3.	Insert necessary information from storages such as statistic, log, result
	 *					4.	Insert closing of HTML tag to eof
	 *					5.	Write processed string to new HTML file.
	 */
	private static void writeHtml() throws Exception 
	{
		String htmlString = "";
		//1. copy prepared html to string
		int returnVal = c.showSaveDialog(window);
		File htmlTemplateFile = new File("template.html");
		try 
		{
			htmlString = FileUtils.readFileToString(htmlTemplateFile);		
		}
		catch (IOException e1) {e1.printStackTrace();}
		
		
	   	if (returnVal == JFileChooser.APPROVE_OPTION)  
	    {
			//2. insert statistic 
			String nameSn = Application.getNameSN();
	   		String date = (new SimpleDateFormat("y/M/d").format(Calendar.getInstance().getTime()));
			String time = (new SimpleDateFormat("HH:mm a").format(Calendar.getInstance().getTime()));
			String hostname = "Unknown";
			try
			{	
				hostname = InetAddress.getLocalHost().getHostName();
			}
			catch (UnknownHostException ex){	System.out.println("Hostname can not be resolved");	}
			htmlString += "<div>Device : " +nameSn+ "</div><div>Computer's Name : " +hostname+ "</div><div>Date : " +date+ "</div><div>Time : " +time+ "</div>";
			
			
	   		//3. insert all row to string
			boolean headTable = true;
			int previousLog = -1;
			for(int round=0; round<tagOutput.size(); round++)
			{					
				// highlight error bit 	
				String finalRealInput ="";		
				String finalExpectInput ="";											
				for(int k=0; k<tagExpectedInput.get(round).length(); k++)
				{
					if(tagExpectedInput.get(round).charAt(k)=='-')	// don't care symbol don't highlight
					{
						finalRealInput += tagInput.get(round).charAt(k);
						finalExpectInput += tagExpectedInput.get(round).charAt(k);		
					}
					else
					{
						if( tagExpectedInput.get(round).charAt(k) == tagInput.get(round).charAt(k))
						{
							finalRealInput += tagInput.get(round).charAt(k);
							finalExpectInput += tagExpectedInput.get(round).charAt(k);	
						}
						else
						{
							finalRealInput += "<mark>"+tagInput.get(round).charAt(k)+"</mark>";
							finalExpectInput += "<mark>"+tagExpectedInput.get(round).charAt(k)+"</mark>";						
						}					
					}
				}// end highlight task
				
				
				// if there is any tag of log
				String[] seeLog = tagOutput.get(round).split("!");		
				if(seeLog.length == 2)	
				{
					int currentLog = Integer.parseInt(seeLog[1]);
					if(currentLog != previousLog)
					{
						htmlString += "\n<div><h2>" +tagLog.get(currentLog)+ "</h2></div>";
						previousLog = currentLog;
					}
				}
				
				
				
				if(headTable)
				{
					htmlString += "\n<table><tr><th>Round</th><th>Output</th> <th>Input recieved/expected</th></tr>";
					headTable = false;
				}				
				htmlString += "\n<tr><td rowspan =\"2\">" +(round+1)+ "</td><td rowspan =\"2\">" +seeLog[0]+ "</td><td>" +finalRealInput+ "</td></tr><tr><td>" +finalExpectInput+ "</td></tr>";		
				
				
				// check if end of file add closing table
				if((round+1) == tagOutput.size())	{	htmlString += "\n</table>";		}
				// if not, Is it a new log?, add closing table
				else
				{
					seeLog = tagOutput.get(round+1).split("!");
					if(Integer.parseInt(seeLog[1]) != previousLog)
					{
						htmlString += "\n</table>";
						headTable = true;
					}
				}				
				
			}// end insert all row loop
		
		
			
			//4. insert closing of html
			htmlString += "\n</body>\n</html>";
	    	
			
			
    		//5. try write processed string to new html
    		try 
    		{
    			File newHtmlFile = new File(c.getSelectedFile()+".html"); 
    			FileUtils.writeStringToFile(newHtmlFile, htmlString);
    			JOptionPane.showMessageDialog(null,c.getSelectedFile().getName()+".html"+" "+"is Saved!");
    		} 
    		catch (IOException e1) {e1.printStackTrace();}
	    }// end if approve	
	}

	
	
	/*	
	 * 	This function reads hwp file type and store in arrayList of string
	 * 	Parameter	:	none
	 *	Return		: 	none
	 * 	Logic 		:	After choosed file, looping scan and store in tempRead variable
	 */
	private static void readFile() throws FileNotFoundException, InterruptedException
	{
		filename.setText(c.getSelectedFile().getAbsolutePath());
		
		Scanner in = new Scanner(new FileReader(c.getSelectedFile().getAbsolutePath()));
		tempRead = new ArrayList<String>();
		String temp = "";
		// store file in arrayList of string
		try		
		{
			while(in.hasNextLine())
			{			
				temp = in.nextLine();
				if(temp != null && !temp.isEmpty())		// skip empty line
				{
					tempRead.add(temp);					
				}
			}
			in.close();
		}catch(Exception e)	{	System.out.println("UNSUCESSFUL READ FILE");	}
	}
	
	
	
	/*	
	 * 	This is a component function which clear all selected button0-15 on main window.
	 * 	Parameter	:	none
	 *	Return		: 	none
	 * 	Logic 		:	Looping through each button, if it was selected, clear it.
	 */
	private static void clearButton() throws InterruptedException
	{
		for(int num=0; num<button.size(); num++)
		{
			if(button.get(num).isSelected())
			{
				button.get(num).doClick();
				TimeUnit.MILLISECONDS.sleep(50);
			}
		}
	}
	
	
	
	/*	
	 * 	This is a component function which clear all data in storage and any component to begin.
	 * 	Parameter	:	none
	 *	Return		: 	none
	 * 	Logic 		:	Just set all necessary component as the beginning
	 */
	private static void clearStorage() throws InterruptedException
	{
		if(runMode == 1)
		{
			Application.clearButton();
		}
		else
		{
			Application.setOutputValue(0);
			outputPinValue = new StringBuilder("0000000000000000");
		}
		
		indicatorList = 0;								// reset all variable
		indicatorTempRead = 0;
		correctQuery = 0;
		logNumber = 0;
		hasLog = false;
		tempWrite = new ArrayList<String>();			// clear all storage
		tagOutput = new ArrayList<String>();	
		tagInput = new ArrayList<String>();	
		tagExpectedInput = new ArrayList<String>();	
		tagLog = new ArrayList<String>();
		
		data.setText("");								// clear text area
		list.clearSelection();							// clear highlight on GUI list
		Application.enableMenuButton(false);
		TimeUnit.MILLISECONDS.sleep(20);
		scroll.getVerticalScrollBar().setValue(0);
	}
	
	

	/*	
	 * 	This is a component function which convert decimal integer to binary string.
	 * 	Parameter	:	decimal integer
	 *	Return		: 	binary string
	 * 	Logic 		:	1.	Looping modulate a decimal integer and concatenate mod into temp string
	 * 					2.	Backward temp string and return the real binary string	 
	 */
	private static String decimalToBinary(int decimalNumber)
    {
	    String tempResult = "";
	    String binaryString = "";
	    
	    while(decimalNumber > 0)
	    {
	        int mod = decimalNumber % 2;
	        tempResult += mod;

	        decimalNumber /= 2;
	    }	    
	    for(int i=tempResult.length()-1; i>=0; i--)
	    {
	    	binaryString += tempResult.charAt(i);
	    }
	    
	    return binaryString;
    }
	
	
	
	/*	
	 * 	This is a component function which enable or disable button0-15 on main window
	 * 	Parameter	:	boolean true for enable those buttons, otherwise is disable
	 *	Return		: 	none
	 * 	Logic 		:	1.	Looping through each buttons, set it as parameter
 	 * 					2.	Set disableButton variable to protect actionPerform of button 
	 */
	private static void setEnableButton(boolean b) throws InterruptedException
	{
    	if(b==true)				
    	{
    		for(int c=0; c<16; c++)
	   		{
	   			button.get(c).setEnabled(b);		// enable button in main window
	   			TimeUnit.MILLISECONDS.sleep(50);
	   		}
    		disableButton = false;
    	}
    	else
    	{
    		disableButton = true;
    		for(int c=0; c<16; c++)
	   		{
	   			button.get(c).setEnabled(b);		// disable button in main window
	   			TimeUnit.MILLISECONDS.sleep(50);
	   		}
    	}
	}
	
	
		
}// end class