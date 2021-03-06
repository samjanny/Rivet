// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// Rivet Copyright (C) 2011 Ian Wraith
// This program comes with ABSOLUTELY NO WARRANTY

package org.e2k;

import java.awt.Color;
import javax.swing.JOptionPane;

public class CROWD36 extends MFSK {
	
	private int baudRate=40;
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	private long energyStartPoint;
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
	private boolean figureShift=false; 
	private int lineCount=0;
	private int correctionValue=0;
	private int highFreq=-1;
	private int lowFreq=5000;
	private final int TONES[]={1410,1450,1490,1530,1570,1610,1650,1690,1730,1770,1810,1850,1890,1930,1970,2010,2050,2090,2130,2170,2210,2250,2290,2330,2370,2410,2450,2490,2530,2570,2610,2650,2690,2730};
	private int toneCount[]=new int[34];
	private int toneLowCount;
	private int toneHighCount;
	private int syncHighTone=24;
	private final String DEBUGLETTERS[]={"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","0","1","2","3","4","5","6","7","8","9"};
		
	public CROWD36 (Rivet tapp,int baud)	{
		baudRate=baud;
		theApp=tapp;
	}
	
	public void setBaudRate(int baudRate) {
		this.baudRate = baudRate;
	}

	public int getBaudRate() {
		return baudRate;
	}

	public void setState(int state) {
		this.state = state;
	}

	public int getState() {
		return state;
	}
		
	public boolean decode (CircularDataBuffer circBuf,WaveData waveData)	{
		// Just starting
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()>11025)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nCROWD36 recordings must have\nbeen recorded at a sample rate\nof 11.025 KHz or less.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			// Check this is a mono recording
			if (waveData.getChannels()!=1)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\nmono WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			// Check this is a 16 bit WAV file
			if (waveData.getSampleSizeInBits()!=16)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\n16 bit WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			samplesPerSymbol=samplesPerSymbol(baudRate,waveData.getSampleRate());
			state=1;
			figureShift=false;
			highFreq=-1;
			lowFreq=5000;
			correctionValue=0;
			toneCountClear();
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			// Clear the energy buffer
			energyBuffer.setBufferCounter(0);
			// Clear the display side of things
			lineCount=0;
			theApp.setStatusLabel("Known Tone Hunt");
			return true;
		}
		// Hunting for known tones
		else if (state==1)	{
			String dout=syncToneHunt(circBuf,waveData);
			if (dout!=null)	{
				state=2;
				energyStartPoint=sampleCount;
				energyBuffer.setBufferCounter(0);
				theApp.setStatusLabel("Calculating Symbol Timing");
				theApp.writeLine(dout,Color.BLACK,theApp.italicFont);
				theApp.newLineWrite();
				String sinform="Sync High Tone is "+Integer.toString(syncHighTone);
				theApp.writeLine(sinform,Color.BLACK,theApp.italicFont);
				theApp.newLineWrite();
			}
		}
		
		// Set the symbol timing
		else if (state==2)	{
			final int lookAHEAD=1;
			// Obtain an average of the last few samples put through ABS
			double no=samplesPerSymbol/20.0;
			energyBuffer.addToCircBuffer(circBuf.getABSAverage(0,(int)no));
			// Gather a symbols worth of energy values
			if (energyBuffer.getBufferCounter()>(int)(samplesPerSymbol*lookAHEAD))	{
				// Now find the lowest energy value
				long perfectPoint=energyBuffer.returnLowestBin()+energyStartPoint+(int)samplesPerSymbol;
				// Calculate what the value of the symbol counter should be
				symbolCounter=(int)samplesPerSymbol-(perfectPoint-sampleCount);
				state=3;
				theApp.setStatusLabel("Symbol Timing Achieved");
				sampleCount++;
				symbolCounter++;
				return true;
			}
		}
		// Decode traffic
		else if (state==3)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol)	{
				symbolCounter=0;				
				int freq=crowd36Freq(circBuf,waveData,0);
				displayMessage(freq,waveData.isFromFile());
			}
		}
		sampleCount++;
		symbolCounter++;
		return true;				
	}
	
	private int crowd36Freq (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		
		// 8 KHz sampling
		if (waveData.getSampleRate()==8000.0)	{
			int freq=doCR36_8000FFT(circBuf,waveData,pos);
			freq=freq+correctionValue;
			return freq;
		}
		else if (waveData.getSampleRate()==11025.0)	{
			int freq=doCR36_11025FFT(circBuf,waveData,pos);
			freq=freq+correctionValue;
			return freq;
		}
		
		return -1;
	}
	
	private void displayMessage (int freq,boolean isFile)	{
		int tone=getTone(freq);
		String ch=getChar(tone);
		// Normal operation
		if (theApp.isDebug()==false)	{
			
			if (ch.equals("ls")) return;
			else if (ch.equals("fs")) return;
			
			if (ch.equals("cr"))	{
				lineCount=50;
			}
			else 	{
				theApp.writeChar(ch,Color.BLACK,theApp.boldFont);
				if (ch.length()>0) lineCount++;
			}	
			if (lineCount==80)	{
				theApp.newLineWrite();
				lineCount=0;
				return;
			}
			return;
		}
		else	{
			// Debug
			if (tone==-1) theApp.writeChar("*",Color.BLACK,theApp.boldFont);
			else theApp.writeChar(DEBUGLETTERS[tone],Color.BLACK,theApp.boldFont);
			lineCount++;
			if (lineCount==80)	{
				lineCount=0;
				theApp.newLineWrite();
			}
	        return;
		}
	}
	
	private String getChar(int tone)	{
		String out="";
		final String C36A[]={"Q" , "X" , "W" , "V" , "E" , "K" , " " , "B" , "R" , "J" , "<*10>" , "G" , "T" ,"F" , "<fs>" , "M" , "Y" , "C" , "cr" , "Z" , "U" , "L" , "<*22>" , "D" , "I" , "H" , "<ls>" , "S" , "O" , "N" , "<*30>" , "A" , "P" , "<*33>"};
		final String F36A[]={"1" , "\u0429" , "2" , "=" , "3" , "(" , " " , "?" , "4" , "\u042e" , "<*10>" , "8" , "5" ,"\u042d" , "<fs>" , "." , "6" , ":" , "cr" , "+" , "7" , ")" , "<*22>" , " " , "8" , "\u0449" , "<ls>" , ""  , "9" , "," , "<*30>" , "-"  , "0" , "<*33>"};
		
		//final String CRY36[]={"\u042f","\u044a","\u0412","\u04c1","\u0415","\u041a"," ","\u0411","\u0420","\u0419","<*10>","\u0490","\u0422","\u0424","<fs>","\u043c","\u042b","\u0426","cr","\u0417","\u0423","\u043b","<*22>","\u0414","\u0418","\u0425","ls","\u0421","\u041e","\041d","<*30>","\u0410","\u041f","<*33>"};
		
		//if (tone==16) figureShift=true;
		//else if (tone==28) figureShift=false;
		
		//figureShift=true;
				
		try	{
			
			if (tone<0)	{
				toneLowCount++;
				tone=0;
			}
			else if (tone>33)	{
				toneHighCount++;
				tone=33;
			}
			else toneCount[tone]++;
				
			if (figureShift==false) out=C36A[tone];
			else out=F36A[tone];
			
			if (out.equals("<ls>")) figureShift=false;
			else if (out.equals("<fs>")) figureShift=true;
			else if (out.equals("cr")) figureShift=false;
			
		}
		catch (Exception e)	{
			JOptionPane.showMessageDialog(null,e.toString(),"Rivet", JOptionPane.INFORMATION_MESSAGE);
			return "";
		}
		
		return out;
	}
	
	// Convert from a frequency to a tone number
	private int getTone (int freq)	{
		int a,index=-1,lowVal=999,dif;
		// Store the highest and lowest frequencies detected
		if (freq>highFreq) highFreq=freq;
		else if (freq<lowFreq) lowFreq=freq;
		// Match the frequency to a tone number
		for (a=0;a<TONES.length;a++)	{
			dif=Math.abs(TONES[a]-freq);
			if (dif<lowVal)	{
				lowVal=dif;
				index=a;
			}
		}
		
		if ((index==0)&&(lowVal>40)) return -1;
		else if ((index==33)&&(lowVal>40)) return 34;
		else return index;
	}
	
	
	// Hunt for known CROWD 36 tones
	private String syncToneHunt (CircularDataBuffer circBuf,WaveData waveData)	{
			int difference;
			// Get 4 symbols
			int freq1=crowd36Freq(circBuf,waveData,0);
			// Check this first tone isn't just noise
			if (getPercentageOfTotal()<5.0) return null;
			int freq2=crowd36Freq(circBuf,waveData,(int)samplesPerSymbol*1);
			// Check we have a high low
			if (freq2>freq1) return null;
			// Check there is more than 100 Hz of difference
			difference=freq1-freq2;
			if (difference<100) return null;
			int freq3=crowd36Freq(circBuf,waveData,(int)samplesPerSymbol*2);
			// Don't waste time carrying on if freq1 isn't the same as freq3
			if (freq1!=freq3) return null;
			int freq4=crowd36Freq(circBuf,waveData,(int)samplesPerSymbol*3);
			// Check 2 of the symbol frequencies are different
			if ((freq1!=freq3)||(freq2!=freq4)) return null;
			// Check that 2 of the symbol frequencies are the same
			if ((freq1==freq2)||(freq3==freq4)) return null;
			// Calculate the difference between the sync tones
			difference=freq1-freq2;
			correctionValue=TONES[syncHighTone]-freq1;
			String line=theApp.getTimeStamp()+" CROWD36 Sync Tones Found (Correcting by "+Integer.toString(correctionValue)+" Hz) sync tone difference "+Integer.toString(difference)+" Hz";
			return line;
		}
	
	public int getLineCount()	{
		return this.lineCount;
	}
	
	public String lowHighFreqs ()	{
		String line;
		line="Lowest frequency "+Integer.toString(lowFreq)+" Hz : Highest Frequency "+Integer.toString(highFreq)+" Hz";
		return line;
	}
	
	private void toneCountClear()	{
		int a;
		toneHighCount=0;
		toneLowCount=0;
		for (a=0;a<toneCount.length;a++)	{
			toneCount[a]=0;
		}
	}
	
	public void toneResults()	{
		int a;
		theApp.writeLine(("Low Tone Count "+Integer.toString(toneLowCount)),Color.BLACK,theApp.plainFont); ;
		for (a=0;a<toneCount.length;a++)	{
			String l="Tone #"+Integer.toString(a)+" "+Integer.toString(toneCount[a]);
			theApp.writeLine(l,Color.BLACK,theApp.plainFont); 
		}
		theApp.writeLine(("High Tone Count "+Integer.toString(toneHighCount)),Color.BLACK,theApp.plainFont); ;
		return;
	}

	public int getSyncHighTone() {
		return syncHighTone;
	}

	public void setSyncHighTone(int syncHighTone) {
		this.syncHighTone = syncHighTone;
	}
	
	


}
