import java.awt.CheckboxMenuItem;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import controlP5.ControlEvent;
import controlP5.ControlListener;
import controlP5.ControlP5;
import controlP5.Textfield;
import processing.awt.PSurfaceAWT;
import processing.core.PApplet;
import processing.serial.Serial;

public class Main extends PApplet {

	public static void main(String[] args) {
		PApplet.main("Main");
	}

	private int maxSalinity = 1024;
	private int maxPressure = 1024;
	private int verticalTickNumber = 8;
	private static int samplesPerMin = 120;
	
	private enum SAMPLE_DISPLAY_METHOD {
		POINTS, LINES, BAR
	}
	
	private SAMPLE_DISPLAY_METHOD displayMethod = SAMPLE_DISPLAY_METHOD.BAR;

	private static final int WIDTH = 1024;
	private static final int HEIGHT = 600;

	private Serial port;

	private PrintWriter output;

	private int unitSalinityWidth = 10;
	private int salinityOffset = 0;
	private int[] salinityValues = new int[samplesPerMin * 20];

	private int unitPressureWidth = 10;
	private int pressureOffset = 0;
	private int[] pressureValues = new int[samplesPerMin * 20];
	private int lastPressureValue = 0;
	private float pressureRatio = 1;
	private int currentTime = 0;
	
	private ControlP5 cp5;

	public void settings() {
		size(1024, 640);
	}
	
	public void setup() {
		cp5 = new ControlP5(this);
		Textfield tf = cp5.addTextfield("").setPosition(WIDTH / 4f-25, HEIGHT / 20f+7).setSize(100, 20).setAutoClear(true);
		tf.addListener(new ControlListener() {
			@Override
			public void controlEvent(ControlEvent arg0) {
				try{
					lastPressureValue = Integer.parseInt(tf.getText());
				}catch(Exception e){
					
				}
			}
		});
		MenuBar menu = new MenuBar();
		Menu fileMenu = new Menu("File");
		menu.add(fileMenu);
		Menu displayMenu = new Menu("Display");
		menu.add(displayMenu);
		MenuItem bar = new MenuItem("Bar Graph");
		displayMenu.add(bar);
		bar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				displayMethod = SAMPLE_DISPLAY_METHOD.BAR;
			}
		});
		MenuItem lines = new MenuItem("Line Graph");
		displayMenu.add(lines);
		lines.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				displayMethod = SAMPLE_DISPLAY_METHOD.LINES;
			}
		});
		MenuItem points = new MenuItem("Point Graph");
		displayMenu.add(points);
		points.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				displayMethod = SAMPLE_DISPLAY_METHOD.POINTS;
			}
		});
		MenuItem neww = new MenuItem("New");
		fileMenu.add(neww);
		neww.addActionListener(new ActionListener (){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				reset();
			}
		});
		MenuItem save = new MenuItem("Save");
		fileMenu.add(save);
		save.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				JFileChooser chooser = new JFileChooser();
				int returnVal = chooser.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					output = createWriter(chooser.getSelectedFile().getAbsolutePath());
					output.println("Pressure,Salinity");

					for (int i = 0; i < salinityValues.length; i++) {
						if (pressureValues[i] != 0 && salinityValues[i] != 0) {
							output.print(pressureValues[i] + ",");
							output.println(salinityValues[i]);
						}
					}

					output.flush();
					output.close();
				}
			}
		});
		
		MenuItem exit = new MenuItem("Exit");
		fileMenu.add(exit);
		exit.addActionListener(new ActionListener (){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				reset();
				System.exit(0);
			}
		});
		
		Menu comPort = new Menu("Com Port");
		menu.add(comPort);
		
		MenuItem update = new MenuItem ("Update List");
		comPort.add(update);
		comPort.addSeparator();
		update.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				comPort.removeAll();
				comPort.add(update);
				comPort.addSeparator();
				try {
					for(String s : Serial.list()) {
						MenuItem portMenu = new MenuItem(s);
						comPort.add(portMenu);
						portMenu.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								setPort(s);
							}
						});
					}
					comPort.addSeparator();
					MenuItem portMenu = new MenuItem("Random");
					comPort.add(portMenu);
					portMenu.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							setPort("Random");
						}
					});
					
					portMenu = new MenuItem("Noise");
					comPort.add(portMenu);
					portMenu.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							setPort("Noise");
						}
					});
					
				} catch (Exception ee) {
					ee.printStackTrace();
				}
			}
		});
		
		try {
			for(String s : Serial.list()) {
				MenuItem portMenu = new MenuItem(s);
				comPort.add(portMenu);
				portMenu.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						setPort(s);
					}
				});
			}
			comPort.addSeparator();
			MenuItem portMenu = new MenuItem("Random");
			comPort.add(portMenu);
			portMenu.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setPort("Random");
				}
			});
			
			portMenu = new MenuItem("Noise");
			comPort.add(portMenu);
			portMenu.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setPort("Noise");
				}
			});
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		Menu pressure = new Menu("Pressure");
		menu.add(pressure);
		MenuItem pressureItem = new MenuItem("Max Pressure");
		pressure.add(pressureItem);
		pressureItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try{
					maxPressure = Integer.parseInt(JOptionPane.showInputDialog("Enter the max pressure"));
				} catch(Exception ee){
					ee.printStackTrace();
				}
			}
		});
		
		MenuItem pressureRatioItem = new MenuItem("Pressure Ratio");
		pressure.add(pressureRatioItem);
		pressureRatioItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try{
					pressureRatio = Float.parseFloat(JOptionPane.showInputDialog("Enter the pressure ratio"));
				} catch(Exception ee){
					ee.printStackTrace();
				}
			}
		});
		
		Menu zoom = new Menu("Zoom");
		menu.add(zoom);
		
		MenuItem zoomPlus = new MenuItem ("Zoom+");
		zoom.add(zoomPlus);
		zoomPlus.setShortcut(new MenuShortcut(KeyEvent.VK_EQUALS));
		zoomPlus.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				unitPressureWidth = clamp(unitPressureWidth + 1, 2, 20);
				unitSalinityWidth = clamp(unitSalinityWidth + 1, 2, 20);
			}
		});
		MenuItem zoomMinus = new MenuItem ("Zoom-");
		zoomMinus.setShortcut(new MenuShortcut(KeyEvent.VK_MINUS));
		zoomMinus.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				unitPressureWidth = clamp(unitPressureWidth - 1, 2, 20);
				unitSalinityWidth = clamp(unitSalinityWidth - 1, 2, 20);
			}
		});
		zoom.add(zoomMinus);
		
		Menu options = new Menu("Options");
		menu.add(options);
		CheckboxMenuItem toggleAutoScroll = new CheckboxMenuItem("Toggle Autoscroll");
		toggleAutoScroll.setState(true);
		options.add(toggleAutoScroll);
		toggleAutoScroll.setShortcut(new MenuShortcut(KeyEvent.VK_T));
		toggleAutoScroll.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				autoscroll = toggleAutoScroll.getState();
			}
		});
		MenuItem samplesPerMinMenu = new MenuItem ("Samples Per Min");
		options.add(samplesPerMinMenu);
		samplesPerMinMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try{
					samplesPerMin = Integer.parseInt(JOptionPane.showInputDialog("Enter the pressure ratio"));
					reset();
				} catch(Exception ee){
					ee.printStackTrace();
				}
			}
		});
		
		PSurfaceAWT awtSurface = (PSurfaceAWT) surface;
		PSurfaceAWT.SmoothCanvas smoothCanvas = (PSurfaceAWT.SmoothCanvas) awtSurface.getNative();
		smoothCanvas.getFrame().setMenuBar(menu);
	}

	private boolean autoscroll = true;
	int s = 0;//Random
	float n = 0;//Noise Pressure
	float k = 0;//Noise Salinity
	boolean portReady = false;
	public void draw() {
		if(port != null && portReady) {
			while (port.available() > 0 && currentTime < salinityValues.length) {
				String str = port.readStringUntil('\n');
			    if (str != null) {
			      str = trim(str);
			      println(str);
			    } else break;
			    
			    if(str.equals("")) break;
//				String temp = port.readString();
//				if (temp.charAt(0) == 'S')
//					salinityValues[currentTime] = Integer.parseInt(temp.split("S")[1]);
				salinityValues[currentTime] = Integer.parseInt(str);
//				else if (temp.charAt(0) == 'P')lastPressureValue
//					pressureValues[currentTime] = Integer.parseInt(temp.split("P")[1]);
				pressureValues[currentTime++] = (int)(lastPressureValue * pressureRatio);
				
//				if(salinityValues[currentTime-1] > maxSalinity)
//					maxSalinity = salinityValues[currentTime-1];
				if(pressureValues[currentTime-1] > maxPressure)
					maxPressure = pressureValues[currentTime-1];
				
				if(currentTime > 400/unitSalinityWidth && autoscroll)
					salinityOffset = currentTime-400/unitSalinityWidth;
				
				if(currentTime > 400/unitPressureWidth && autoscroll)
					pressureOffset = currentTime-400/unitPressureWidth;
			}
		} else if(random){
			s++;
			if (s == 60*60/samplesPerMin) {
				s = 0;
				pressureValues[currentTime] = (int)(lastPressureValue * pressureRatio);
				salinityValues[currentTime++] = (int) random(1024);
				if (currentTime >= salinityValues.length)
					s = 60*60/samplesPerMin+1;
				
//				if(salinityValues[currentTime-1] > maxSalinity)
//					maxSalinity = salinityValues[currentTime-1];
				if(pressureValues[currentTime-1] > maxPressure)
					maxPressure = pressureValues[currentTime-1];
				
				if(currentTime > 400/unitSalinityWidth && autoscroll)
					salinityOffset = currentTime-400/unitSalinityWidth;
				
				if(currentTime > 400/unitPressureWidth && autoscroll)
					pressureOffset = currentTime-400/unitPressureWidth;
			}
		}  else if(noise){
			s++;
			if (s == 60*60/samplesPerMin) {
				s = 0;
				pressureValues[currentTime] = (int) (maxPressure*noise(n+=0.01));
				salinityValues[currentTime++] = (int) (maxSalinity*noise(k+=0.1));
				if (currentTime >= salinityValues.length)
					s = 60*60/samplesPerMin+1;
				
//				if(salinityValues[currentTime-1] > maxSalinity)
//					maxSalinity = salinityValues[currentTime-1];
				if(pressureValues[currentTime-1] > maxPressure)
					maxPressure = pressureValues[currentTime-1];
				
				if(currentTime > 400/unitSalinityWidth && autoscroll)
					salinityOffset = currentTime-400/unitSalinityWidth;
				
				if(currentTime > 400/unitPressureWidth && autoscroll)
					pressureOffset = currentTime-400/unitPressureWidth;
			}
		}

		background(0);
		text("Pressure", WIDTH / 4f, HEIGHT / 20f);
		text("Salinity", 3 * WIDTH / 4f, HEIGHT / 20f);
		
		for (int i = 0; i <= verticalTickNumber; i++) {
			text("" + (int) ((float) i * maxPressure / verticalTickNumber), 10, HEIGHT - (HEIGHT-15) * (float) i / (verticalTickNumber + 1) - 10);
			text("" + (int) ((float) i * maxSalinity / verticalTickNumber), 10 + WIDTH / 2, HEIGHT - (HEIGHT-15) * (float) i / (verticalTickNumber + 1) - 10);
		}

		fill(0,175,200);
		text ("Air", 40 + WIDTH / 2, HEIGHT - 10);
		text ("Water", 40 + WIDTH / 2, HEIGHT - (HEIGHT-15) * (float) 123 / (11f/10*maxSalinity) - 10);
		text ("1%", 40 + WIDTH / 2, HEIGHT - (HEIGHT-15) * (float) 463 / (11f/10*maxSalinity) - 10);
		text ("2%", 40 + WIDTH / 2, HEIGHT - (HEIGHT-15) * (float) 573 / (11f/10*maxSalinity) - 10);
		text ("3.5%", 40 + WIDTH / 2, HEIGHT - (HEIGHT-15) * (float) 610 / (11f/10*maxSalinity) - 10);
		
		fill(255);
//		if (unitPressureWidth >= 15)
//			for (int i = 0; i <= 400 / unitPressureWidth; i++)
//				text((i + 1) + pressureOffset, 80 + i * unitPressureWidth + 1, HEIGHT);
//		else
			for (int i = 0; i <= 400 / unitPressureWidth; i += (int) (400f / unitPressureWidth / 3))
				text((i + 1) + pressureOffset, 80 + i * unitPressureWidth + 1, HEIGHT+5);

//		if (unitSalinityWidth >= 15)
//			for (int i = 0; i <= 400 / unitSalinityWidth; i++)
//				text((i + 1) + salinityOffset, WIDTH / 2 + 80 + i * unitSalinityWidth + 1, HEIGHT);
//		else
			for (int i = 0; i <= 400 / unitSalinityWidth; i += (int) (400f / unitSalinityWidth / 3))
				text((i + 1) + salinityOffset, WIDTH / 2 + 80 + i * unitSalinityWidth + 1, HEIGHT+5);

		fill(100);
		stroke(0);
		rect(79, 65, 401, HEIGHT - 75);
		rect(WIDTH / 2 + 79, 65, 401, HEIGHT - 75);

		fill(255);
		stroke(255);

		switch(displayMethod){
			case BAR:
				stroke(0);
				colorMode(HSB, 255);
				for (int i = 0; i < 400 / unitPressureWidth; i++)
					if (i + pressureOffset < pressureValues.length)
						if(pressureValues[i + pressureOffset] != 0) {
							fill(map(i+pressureOffset, 0, pressureValues.length/samplesPerMin*10, 0, 255)%255, 255, 255);
							rect(80 + i * unitPressureWidth, HEIGHT - 10, unitPressureWidth, -((HEIGHT - 75) * (float) (pressureValues[i + pressureOffset]) / maxPressure));
						}
	
				for (int i = 0; i < 400 / unitSalinityWidth; i++)
					if (i + salinityOffset < salinityValues.length)
						if(salinityValues[i + salinityOffset] != 0) {
							fill(map(i+salinityOffset, 0, salinityValues.length/samplesPerMin*10, 0, 255)%255, 255, 255);
							rect(WIDTH / 2 + 80 + i * unitSalinityWidth, HEIGHT - 10, unitSalinityWidth, -((HEIGHT - 75) * (float) (salinityValues[i + salinityOffset]) / maxSalinity));
						}
				colorMode(RGB, 255);
				fill(255);
				stroke(255);
				break;
			case LINES:
				strokeWeight(2);
				for (int i = 0; i < 400 / unitPressureWidth; i++)
					if (i + pressureOffset < pressureValues.length) {
						if(i-1 >= 0 && pressureValues[i + pressureOffset] != 0)
							line(80 + i * unitPressureWidth, HEIGHT - 10 -((HEIGHT - 75) * (float) (pressureValues[i + pressureOffset]) / maxPressure), 80 + (i-1) * unitPressureWidth, HEIGHT - 10 -((HEIGHT - 75) * (float) (pressureValues[(i-1) + pressureOffset]) / maxPressure));
						if(pressureValues[i + pressureOffset] != 0)
							point(80 + i * unitPressureWidth, HEIGHT - 10 -((HEIGHT - 75) * (float) (pressureValues[i + pressureOffset]) / maxPressure));
					}
	
				for (int i = 0; i < 400 / unitSalinityWidth; i++)
					if (i + salinityOffset < salinityValues.length) {
						if(i-1 >= 0 && salinityValues[i + salinityOffset] != 0)
							line(WIDTH / 2 + 80 + i * unitSalinityWidth, HEIGHT - 10 -((HEIGHT - 75) * (float) (salinityValues[i + salinityOffset]) / maxSalinity), WIDTH / 2 + 80 + (i-1) * unitSalinityWidth, HEIGHT - 10 -((HEIGHT - 75) * (float) (salinityValues[(i-1) + salinityOffset]) / maxSalinity));
						if(salinityValues[i + salinityOffset] != 0)
							point(WIDTH / 2 + 80 + i * unitSalinityWidth, HEIGHT - 10 -((HEIGHT - 75) * (float) (salinityValues[i + salinityOffset]) / maxSalinity));
					}
				strokeWeight(1);
				break;
			case POINTS:
				strokeWeight(4);
				for (int i = 0; i < 400 / unitPressureWidth; i++)
					if (i + pressureOffset < pressureValues.length)
						if(pressureValues[i + pressureOffset] != 0)
							point(80 + i * unitPressureWidth, HEIGHT - 10 -((HEIGHT - 75) * (float) (pressureValues[i + pressureOffset]) / maxPressure));
	
				for (int i = 0; i < 400 / unitSalinityWidth; i++)
					if (i + salinityOffset < salinityValues.length)
						if(salinityValues[i + salinityOffset] != 0)
							point(WIDTH / 2 + 80 + i * unitSalinityWidth, HEIGHT - 10 -((HEIGHT - 75) * (float) (salinityValues[i + salinityOffset]) / maxSalinity));
				strokeWeight(1);
				break;
		}
	}
	
	private boolean random = false;
	private boolean noise = false;
	private void setPort(String p){
		portReady = false;
		reset();
		if(p.equals("Random")) {
			random = true;
			port = null;
			noise = false;
		} else if(p.equals("Noise")) {
			random = false;
			port = null;
			noise = true;
		} else {
			port = new Serial(this, p, 9600);
			while(port.available() <= 0)
				port.write(samplesPerMin);
			port.read();
			port.clear();
			random = false;
			noise = false;
			portReady = true;
		}
	}
	
	private void reset (){
		pressureOffset = 0;
		salinityOffset = 0;
		salinityValues = new int[samplesPerMin * 20];
		pressureValues = new int[samplesPerMin * 20];
		currentTime = 0;
		if(port != null)
			port.stop();
		port = null;
		random = false;
		noise = false;
		s=0;
	}

	private int mX;

	private boolean shiftDown = false;
	private boolean ctrlDown = false;
	public void keyPressed(){
		if(keyCode == KeyEvent.VK_SHIFT)
			shiftDown = true;
		if(keyCode == KeyEvent.VK_CONTROL)
			ctrlDown = true;
	}
	
	public void keyReleased(){
		if(keyCode == KeyEvent.VK_SHIFT)
			shiftDown = false;
		if(keyCode == KeyEvent.VK_CONTROL)
			ctrlDown = false;
	}
	
	public void mousePressed() {
		mX = mouseX;
	}

	public void mouseDragged() {
		
		int dX = 0;
		if(shiftDown)
			dX = (mX - mouseX)*10;
		else if (ctrlDown)
			dX = (mX - mouseX)*100;
		else
			dX = mX - mouseX;
		if (mouseX < WIDTH / 2) {
			pressureOffset += dX/2;
			pressureOffset = clamp(pressureOffset, 0, pressureValues.length);
		} else if (mouseX > WIDTH / 2) {
			salinityOffset += dX/2;
			salinityOffset = clamp(salinityOffset, 0, salinityValues.length);
		}
		mX = mouseX;
	}

	public void mouseWheel(processing.event.MouseEvent me) {
		if (mouseX < WIDTH / 2)
			unitPressureWidth = clamp(unitPressureWidth - me.getCount(), 2, 20);
		else
			unitSalinityWidth = clamp(unitSalinityWidth - me.getCount(), 2, 20);
	}

	private int clamp(int val, int min, int max) {
		return val < min ? min : val > max ? max : val;
	}

}