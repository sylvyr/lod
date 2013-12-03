import java.net.*;
import java.io.*;
import java.lang.String.*;

class SWDServer {
String serverName = "SWD Server";
int maxConnections = 15;//No less than two.
int totalNpcs = 0;
int followSwitch = 0;
int maxLoginAttempts = 5;
ServerSocket servSock;
SWDPlayers[] player = new SWDPlayers[maxConnections];
SWDPlayers[] npc;
Socket[] sock = new Socket[maxConnections];
Thread[] sockt = new Thread[maxConnections];
Thread[] client = new Thread[maxConnections];
Thread npcloopz = new Thread();
PrintStream[] o = new PrintStream[maxConnections];
Thread listen;
boolean running = true;
//New Stuff
java.util.Random r = new java.util.Random();
int mapWidth = 50;
int mapHeight = 50;
int totalItems = 279;
int totalBitems = 52;
int totalMaps = 2;
SWDTile[][][] tile = new SWDTile[totalMaps+1][mapWidth][totalItems];
SWDTile[][][] ctile = new SWDTile[totalMaps+1][mapWidth][totalItems];
SWDMap[] map = new SWDMap[totalMaps+1];
SWDItem[] item = new SWDItem[totalItems];
SWDItem[] bitem = new SWDItem[totalBitems];
String[] floorName = new String[188];
boolean[] floorWalk = new boolean[188];
//End  New Stuff

	public static void main(String[] args)	{
		SWDServer Server = new SWDServer();
		Server.loadStuff();
		Server.loadNpcs();
		Server.startServer();
		Server.startNPC();
		//Server.checkTry();
	}
	
	/*public void checkTry(){
		SqlTest st = new SqlTest();
		System.out.println("Checking... " + st.checkPass("Sylvyr","hehgame"));
		st.createChar("ChickaBang","thepass",1,2,3,"1-2-3-4-5");
		System.out.println("COLOSR = " + st.getColors("ChickaBang"));
	}*/
	
	public void startServer(){
		try{
			servSock = new ServerSocket(8310);
			listen = new Server();
			listen.start();
			System.out.println(serverName + " up and running!");
		}catch(Exception e){
			System.out.println("startServer() error: " + e);
			e.printStackTrace();
		}
	}
	
	public void loadStuff(){
		setupItems();
		loadItemInfo();
		loadMap(1);
		loadMapConfig(1);
		loadMap(2);
		loadMapConfig(2);
	}
	
	public void createClient(int n){
		client[n] = new Client(n);
		player[n] = new SWDPlayers();		
	}
	
	public void sendAdminlist(int s){
		sendItOff(s, "15 Administrators: Adam, Brad.");
		sendItOff(s, "26 *Contact an administrator to report a bug or request help.");
	}
	
	public void sendItOff(int n, String s){
		o[n].println(s);
		o[n].flush();
		//System.out.println(s);
	}
	

	
	public void sendItAll(String s){
		for(int i = 1; i<maxConnections; i++)
			try{
				if(sock[i] != null && sock[i].isConnected()==true && player[i].state.equals("validated"))
				sendItOff(i, s);
			}catch(Exception e){}
	}
	
	public void sendItMap(int m, String what2send){
		String mapstring = map[m].players.replaceAll("##", " ");
		mapstring = mapstring.replaceAll("#", "");
		String[] newString = mapstring.split(" ");
		int x=0;
		boolean stumpy = true;
			while(stumpy){
				try{
					sendItOff(Integer.parseInt(newString[x]), what2send);
					x++;
				}catch(Exception e){stumpy=false;}
			}
	}
	
	public void sendItArea(int m, int x, int y, String what2send){
		String mapstring = map[m].players.replaceAll("##", " ");
		mapstring = mapstring.replaceAll("#", "");
		String[] newString = mapstring.split(" ");
		int xx=0;
		boolean stumpy = true;
			while(stumpy){
				try{
					if(player[Integer.parseInt(newString[xx])].xCoord > x -11 && player[Integer.parseInt(newString[xx])].xCoord < x +11 && player[Integer.parseInt(newString[xx])].yCoord > y - 11 && player[Integer.parseInt(newString[xx])].yCoord < y + 11)
					sendItOff(Integer.parseInt(newString[xx]), what2send);
					xx++;
				}catch(Exception e){stumpy=false;}
			}
	}
	
	public void logout(int n){
		saveChar(n);
		sendItAll("17 -!-" + player[n].name + " has logged out.");
		newPos(n,0,0,0);
	}
	
	public void saveChar(int s){
		String acc = "";
		acc = (acc + player[s].hand + "-" + player[s].wep + "-" + player[s].arm + "-" + player[s].hlm + "-" + player[s].acc);
		String inv = "" + player[s].inventory[1];
		for(int x = 2; x < 31; x++)
			inv = (inv + "-" + player[s].inventory[x]);
		SqlTest loginSql = new SqlTest();
		loginSql.saveChar(player[s].name,player[s].map, player[s].xCoord, player[s].yCoord, acc, inv);
	}
	
	public void killSock(int n){
		if(sock[n]!=null)
		if (sock[n].isConnected())try{sock[n].close();}catch(Exception e){System.out.println("ERROR CLOSING SOCKET");}
		sock[n] = null;
		o[n] = null;
		client[n] = null;
		player[n] = null;
		System.gc();
		System.out.println("Socket " + n + " closed.");
	}
	
	public void newChar(int s, String name, String pass){
		SqlTest loginSql = new SqlTest();
		player[s].state = "creating";
		player[s].name = name;
		String c = player[s].gender + "-" + player[s].cloth1 + "-" + player[s].cloth2 + "-" + player[s].cape + "-" + player[s].hair;
		loginSql.createChar(player[s].name, pass, player[s].map, player[s].xCoord, player[s].yCoord, c);
	}


	
	
	public void login(int s, String name){
		SqlTest loginSql = new SqlTest();
		System.out.println(player[s].state.equals("creating"));
		if (player[s].state.equals("creating")==false) sendItOff(s,"h");
		player[s].state="validated";
		player[s].name = name;
		//LoadPlayer
		String [] col = loginSql.getColors(player[s].name).split("-",5);
		player[s].gender = Integer.parseInt(col[0]);
		player[s].cloth1 = Integer.parseInt(col[1]);
		player[s].cloth2 = Integer.parseInt(col[2]);
		player[s].cape = Integer.parseInt(col[3]);
		player[s].hair = Integer.parseInt(col[4]);
		//player[s].xCoord = loginSql.getCoord("x",player[s].name);
		//player[s].yCoord = loginSql.getCoord("y",player[s].name);
		//player[s].map = loginSql.getMap(player[s].name);
		//newPos(s, loginSql.getMap(player[s].name), loginSql.getCoord("x",player[s].name), loginSql.getCoord("y",player[s].name));
		
		setInventory(s);
		setAcc(s);
		//FIX THIS	
		//sendItOff(s,"m " + player[s].map + " " + player[s].xCoord + " " + player[s].yCoord);
		//sendItAll("p " + player[s].xCoord + " " + player[s].yCoord + " " + player[s].facing + " " + player[s].gender + " " + player[s].cloth1 + " " + player[s].cloth2 + " " + player[s].cape + " " + player[s].hair);
		//sendItOff(s,"m " + player[s].map + " " + player[s].xCoord + " " + player[s].yCoord);
		//newPos(s,player[s].map,player[s].xCoord,player[s].yCoord);
		newPos(s, loginSql.getMap(player[s].name), loginSql.getCoord("x",player[s].name), loginSql.getCoord("y",player[s].name));
		//sendItOff(s,returnCmap(player[s].map));
		sendItAll("17 -!-" + player[s].name + " has logged in.");
		sendItOff(s,"*");
	}
	
	public void setInventory(int s){
		SqlTest loginSql = new SqlTest();
		String[] inv = loginSql.getInventory(player[s].name).split("-",30);
		for(int x = 1; x < 31; x++){
			player[s].inventory[x] = Integer.parseInt(inv[x-1]);
			sendItOff(s, "v " + player[s].inventory[x] + " " + x);
		}	
	}
	
	public void setAcc(int s){
		SqlTest loginSql = new SqlTest();
		String[] acc = loginSql.getAcc(player[s].name).split("-",5);
		player[s].hand = Integer.parseInt(acc[0]);
		player[s].wep = Integer.parseInt(acc[1]);
		player[s].arm = Integer.parseInt(acc[2]);
		player[s].hlm = Integer.parseInt(acc[3]);
		player[s].acc = Integer.parseInt(acc[4]);	
		sendItOff(s, "i " + player[s].hand);
		sendItOff(s, "w " + player[s].wep);
		sendItOff(s, "e " + player[s].hlm);
		sendItOff(s, "a " + player[s].acc);
		sendItOff(s, "r " + player[s].arm);	
	}
	
	public void loadNpcConfig(){
		int npcCount = 0;
		File npcFile = new File(".//config//npc.txt");
		BufferedReader input = null;
		String line = "";
		try{input = new BufferedReader(new FileReader(npcFile));}catch(Exception e){System.out.println("Cannot find npc.txt");}
		try{
			System.out.print("! - Loading NPC Count......");
			while(line!=null){
				try{line = input.readLine();}catch(Exception e){System.out.println("Error reading NPC input");}
				//System.out.println(line);
				if(line!=null && line.startsWith("#")==false){
					if(line.equals("<npc>")){totalNpcs++;}
				}	
			}
		}catch(Exception e){System.out.println("Error counting npcs");}
		System.out.println("TOTAL:" + totalNpcs);
		npc = new SWDPlayers[totalNpcs+1];
		try{input.close();}catch(Exception e){System.out.println("Error closing file." + e);}
		input = null;
		line = "";
		try{input = new BufferedReader(new FileReader(npcFile));}catch(Exception e){System.out.println("Cannot find npc.txt");}
		try{
			System.out.print("! - Loading NPCs......");
			while(line!=null){
				try{line = input.readLine();}catch(Exception e){System.out.println("Error reading NPC input");}
				if(line!=null && line.startsWith("#")==false){
					if(line.equals("<npc>")){
						npcCount++;
						npc[npcCount] = new SWDPlayers();
						while(line.equals("</npc>")==false){
							try{line = input.readLine();}catch(Exception e){System.out.println("Error reading NPC input");}
							String[] confsplit = line.split("=",2);
							if(confsplit[0].toLowerCase().startsWith("name")){
								npc[npcCount].name = confsplit[1];
							}else if(confsplit[0].toLowerCase().startsWith("maxhp")){
								npc[npcCount].maxhp = Integer.parseInt(confsplit[1].replaceAll(" ",""));
							}else if(confsplit[0].toLowerCase().startsWith("maxmp")){
								npc[npcCount].maxmp = Integer.parseInt(confsplit[1].replaceAll(" ",""));
							}else if(confsplit[0].toLowerCase().startsWith("gold")){
								npc[npcCount].gold = Integer.parseInt(confsplit[1].replaceAll(" ",""));
							}else if(confsplit[0].toLowerCase().startsWith("gender")){
								npc[npcCount].gender = Integer.parseInt(confsplit[1].replaceAll(" ",""));
							}else if(confsplit[0].toLowerCase().startsWith("cloth1")){
								npc[npcCount].cloth1 = Integer.parseInt(confsplit[1].replaceAll(" ",""));
							}else if(confsplit[0].toLowerCase().startsWith("cloth2")){
								npc[npcCount].cloth2 = Integer.parseInt(confsplit[1].replaceAll(" ",""));
							}else if(confsplit[0].toLowerCase().startsWith("cape")){
								npc[npcCount].cape = Integer.parseInt(confsplit[1].replaceAll(" ",""));
							}else if(confsplit[0].toLowerCase().startsWith("hair")){
								npc[npcCount].hair = Integer.parseInt(confsplit[1].replaceAll(" ",""));
							}else if(confsplit[0].toLowerCase().startsWith("mindef")){
								npc[npcCount].mindef = Integer.parseInt(confsplit[1].replaceAll(" ",""));
							}else if(confsplit[0].toLowerCase().startsWith("maxdef")){
								npc[npcCount].maxdef = Integer.parseInt(confsplit[1].replaceAll(" ",""));
							}else if(confsplit[0].toLowerCase().startsWith("minatk")){
								npc[npcCount].minatk = Integer.parseInt(confsplit[1].replaceAll(" ",""));
							}else if(confsplit[0].toLowerCase().startsWith("maxatk")){
								npc[npcCount].maxatk = Integer.parseInt(confsplit[1].replaceAll(" ",""));
							}else if(confsplit[0].toLowerCase().startsWith("talkingspeed")){
								npc[npcCount].talkingSpeed = Integer.parseInt(confsplit[1].replaceAll(" ",""));
							}else if(confsplit[0].toLowerCase().startsWith("walkingspeed")){
								npc[npcCount].walkingSpeed = Integer.parseInt(confsplit[1].replaceAll(" ",""));
							}else if(confsplit[0].toLowerCase().startsWith("talking")){
								if(confsplit[1].toLowerCase().replaceAll(" ","").equals("true")) npc[npcCount].talking = true;
							}else if(confsplit[0].toLowerCase().startsWith("walking")){
								if(confsplit[1].toLowerCase().replaceAll(" ","").equals("true")) npc[npcCount].walking = true;
							}else if(confsplit[0].toLowerCase().startsWith("following")){
								if(confsplit[1].toLowerCase().replaceAll(" ","").equals("true")) npc[npcCount].following = true;
							}else if(confsplit[0].toLowerCase().startsWith("enemy")){
								if(confsplit[1].toLowerCase().replaceAll(" ","").equals("true")) npc[npcCount].following = true;
							}else if(confsplit[0].toLowerCase().startsWith("drop")){
								npc[npcCount].drop = confsplit[1].toLowerCase().replaceAll(" ","");
							}else if(confsplit[0].toLowerCase().startsWith("randomtext0")){
								npc[npcCount].randomText[0] = confsplit[1];
							}else if(confsplit[0].toLowerCase().startsWith("randomtext1")){
								npc[npcCount].randomText[1] = confsplit[1];
							}else if(confsplit[0].toLowerCase().startsWith("randomtext2")){
								npc[npcCount].randomText[2] = confsplit[1];	
							}else if(confsplit[0].toLowerCase().startsWith("randomtext3")){
								npc[npcCount].randomText[3] = confsplit[1];
							}else if(confsplit[0].toLowerCase().startsWith("randomtext4")){
								npc[npcCount].randomText[4] = confsplit[1];	
							}
						}
					}
				}
			}
		}catch(Exception e){System.out.println("Error loading NPC Data");e.printStackTrace();}
		try{input.close();}catch(Exception e){System.out.println("Error closing file." + e);}
	}
	
	public void loadNpcs(){
		loadNpcConfig();
		placeNpc(1, 25, 26, 1);
		placeNpc(1, 26, 26, 2);
			//placeNpc(1, 27, 25, 1);

		
		/*
		for(int i = 0; i < totalNpcs; i++)
			npc[i] = new SWDPlayers();
		//Load NPCS Here
		
		npc[1].name = "NPC1";
		npc[1].facing = 1; 
		npc[1].gender = 2;
		npc[1].cloth1 = 3; 
		npc[1].cloth2 = 4;
		npc[1].cape = 5;
		npc[1].minatk = 0;
		npc[1].maxatk = 1;
		npc[1].mindef = 0;
		npc[1].maxdef = 1;
		npc[1].maxhp = 5;
		npc[1].curhp = 5;
		npc[1].maxmp = 5;
		npc[1].curmp = 5;
		npc[1].talking = true;
		npc[1].walking = true;
		npc[1].following = true;
		npc[1].walkingSpeed = 5;
		npc[1].talkingSpeed = 30;
		npc[1].randomText[0] = "OHAY";
		npc[1].randomText[1] = "I C U";
		npc[1].randomText[2] = "DO U CME?";
		npc[1].randomText[3] = "CNA YOU HEAR MEH?";
		npc[1].randomText[4] = "I CANNWALK! unfrezd!";
		//end loading npcs from config
			//placeNpc(1, 25, 25, 1);
			//placeNpc(1, 26, 25, 1);
			//placeNpc(1, 27, 25, 1);
		//System.out.println("Name> " + npc[1].name);
		//map[1].gnpc[1] = npc[1];
		//map[1].gnpc[1].xCoord = 25;
		//map[1].gnpc[1].yCoord = 25;
		*/
	}
	
	public void startNPC(){
		npcloopz = new npcLoop();
		npcloopz.start();
		System.out.println("NPC Loop started.");
	}
	
	public void destroyNpc(int m, int n){
		sendItAll("p " + map[m].gnpc[n].xCoord + " " + map[m].gnpc[n].yCoord + " " + 0 + " " + 0 + " " + 0 + " " + 0 + " " + 0 + " " + 0);
		tile[m][map[m].gnpc[n].xCoord][map[m].gnpc[n].yCoord].npc = 0;
		map[m].gnpc[n] = null;
		System.gc();
	}
	
	public void placeNpc(int m, int x, int y, int n){
	//placeNpc(map, xCoord, yCoord, npc#);
		for (int i = 1; i < 26; i++){
			if (map[m].gnpc[i]==null){
			System.out.println("Creating NPC: " + i);
				map[m].gnpc[i] = new SWDPlayers();
				map[m].gnpc[i].name = npc[n].name;
				map[m].gnpc[i].facing = npc[n].facing;
				map[m].gnpc[i].gender = npc[n].gender;
				map[m].gnpc[i].cloth1 = npc[n].cloth1;
				map[m].gnpc[i].cloth2 = npc[n].cloth2;
				map[m].gnpc[i].cape = npc[n].cape;
				map[m].gnpc[i].hair = npc[n].hair;
				map[m].gnpc[i].talking = npc[n].talking;
				map[m].gnpc[i].walking = npc[n].walking;
				map[m].gnpc[i].following = npc[n].following;
				map[m].gnpc[i].walkingSpeed = npc[n].walkingSpeed;
				map[m].gnpc[i].talkingSpeed = npc[n].talkingSpeed;
				map[m].gnpc[i].randomText[0] = npc[n].randomText[0];
				map[m].gnpc[i].randomText[1] = npc[n].randomText[1];
				map[m].gnpc[i].randomText[2] = npc[n].randomText[2];
				map[m].gnpc[i].randomText[3] = npc[n].randomText[3];
				map[m].gnpc[i].randomText[4] = npc[n].randomText[4];
				map[m].gnpc[i].xCoord = x;
				map[m].gnpc[i].yCoord = y;
				map[m].gnpc[i].map = m;
				map[m].gnpc[i].walkingExec = randomNum(0,map[m].gnpc[i].walkingSpeed);
				map[m].gnpc[i].talkingExec = randomNum(0,map[m].gnpc[i].talkingSpeed);
				map[m].totalnpcs++;
				newNpcPos(i, m, x, y);
				//tile[m][x][y].npc = i;
				break;
			}
		}
	}
	
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException(); 
	}
	
	public void socketStuff(String data, int s){
		char sw = data.charAt(0);
		String[] data2 = data.split(" ",2);
		
		switch(sw){
			case '%': parseInput(s, data2[1]); break;
			//sendItAll("0 " + player[s].name+": " + data2[1]);break;
			case 'm': char d = data2[1].charAt(0);
				movePlayer(s, d);
			break;
			case 'c': break;
			default: break;
		}
		
	}
	
	public void parseInput(int s, String data){
		SqlTest loginSql = new SqlTest();
		char sw = data.charAt(0);
		switch(sw){
			case'[':
				String[] split = data.split(" ",2);
				if(split[0].equals("[s"))playerShout(s, split[1]);
			break;
			case'-'://Do all - commands
				if(data.equals("-look"))look(s);
				else if(data.equals("-coords")){sendItOff(s,"0 " + player[s].map + ", " + player[s].xCoord + ", " + player[s].yCoord);}
				else if(data.equals("-rl")){
					switch(getFacingDirection(s)){
						case 'u': player[s].facing=11; break;
						case 'd': player[s].facing=10; break;
						case 'l': player[s].facing=9; break;
						case 'r': player[s].facing=12; break;
					}
					newPos(s, player[s].map, player[s].xCoord, player[s].yCoord);
				}else if(data.startsWith("-stats")){
					showStats(s);
				}else if(data.startsWith("-switch")){
					switchItem(s, data);
				}else if(data.equals("-get")){
					getItem(s);
				}else if(data.equals("-rr")){
					switch(getFacingDirection(s)){
						case 'u': player[s].facing=10; break;
						case 'd': player[s].facing=11; break;
						case 'l': player[s].facing=12; break;
						case 'r': player[s].facing=9; break;
					}
					newPos(s, player[s].map, player[s].xCoord, player[s].yCoord);
				}else if(data.equals("-adminlist")){
						sendAdminlist(s);
				}else if(data.equals("-sit")){
					sit(s);
				}else if(data.equals("-hit")){
					action(s);
				}else if(data.equals("-facing")){
					if(player[s].facing < 21){
						player[s].facing++;
						newPos(s,player[s].map,player[s].xCoord,player[s].yCoord);
					}else{
						player[s].facing=1;
						newPos(s,player[s].map,player[s].xCoord,player[s].yCoord);
					}
					sendItOff(s,"0 " + player[s].facing);
				}
				else sendItAll("0 " + player[s].name + ": " + data);
				break;
				
			case'@':
				if(player[s].name.equals("Adam")){
					if(data.toLowerCase().startsWith("@place "))
						placeItem(s, data);
					else if(data.toLowerCase().startsWith("@noclip"))
						if(player[s].clip)player[s].clip=false; else player[s].clip=true;
					else if(data.toLowerCase().startsWith("@what"))
						showWhat(s);
					else if(data.toLowerCase().startsWith("@echoallc "))
						echoAllColor(s, data);
					else if(data.toLowerCase().startsWith("@echomapc"))
						echoMapColor(s, data);
					else if(data.toLowerCase().startsWith("@exits"))
						sendItOff(s, "0 Exits, NSEW: " + map[player[s].map].nExit + ", " + map[player[s].map].sExit + ", " + map[player[s].map].eExit + ", " + map[player[s].map].wExit);	
					else if(data.toLowerCase().startsWith("@players"))
						sendItOff(s,"0 Players on this map: " + map[player[s].map].players);
					else if(data.toLowerCase().startsWith("@placenpc"))
						placeNpc(player[s].map, player[s].xCoord, player[s].yCoord, 1);
					else if(data.toLowerCase().startsWith("@npcdown"))
						moveNPC(1,1,'d');
					else if(data.toLowerCase().startsWith("@name"))
						map[1].gnpc[1].name = "NEWNAME";	
					else if(data.toLowerCase().startsWith("@totalnpcs"))
						sendItOff(s,"0 Total NPCs on this map: " + map[player[s].map].totalnpcs);
					else if(data.toLowerCase().startsWith("@welcome"))
						sendItOff(s,map[player[s].map].welcome);
					else if(data.toLowerCase().startsWith("@refresh")){
						sendItOff(s,returnCmap(player[s].map));
						sendItOff(s,"*");break;
					}	
					else if(data.toLowerCase().startsWith("@save"))
						saveChar(s);
					else sendItOff(s,"0 Bad command");
				}else{ playerTalk(s, data); break;}
				break;
			default: playerTalk(s, data); break;
		}
	}

	
	
	public char getFacingDirection(int s){
		char f = 'd';
		switch(player[s].facing){
			case 1: f='d';  break;
			case 2: f='d';	break;
			case 3: f='u';	break;
			case 4: f='u';	break;
			case 5: f='r';	break;
			case 6: f='r';	break;
			case 7: f='l';	break;
			case 8: f='l';	break;
			case 9: f='d';break;//Straight Down
			case 10: f='r';break;//Straight Right
			case 11: f='l';	break;//Straight Left
			case 12: f='u';break;//Straight up
			case 13:f='d';	break;//atk down
			case 14:f='d';	break;//atk down
			case 15:f='r';	break;//atk right
			case 16:f='r';	break;//atk right
			case 17:f='l';	break;//atk left
			case 18:f='l';	break;//atk left
			case 19:f='u';	break;//atk up
			case 20:f='u';	break;//atk up
			case 21:f='d'; break;//sitting
			default: f='d';break;
		}
		return(f);
	}
	
	public void placeItem(int s, String data){
		int it = 0; 
		String[] st; 
		try{
			st = data.split(" ", 2);
			it = Integer.parseInt(st[1]);
		}catch(Exception e){
			sendItOff(s, "0 Use: @place <#>");
		}
		
		int xx = player[s].xCoord;
		int yy = player[s].yCoord;
		switch(getFacingDirection(s)){
		 case 'u': yy--; break;
		 case 'd': yy++; break;
		 case 'l': xx--; break;
		 case 'r': xx++; break;
		}
	
		if((yy>=0) && (yy<mapHeight) && (xx>=0) && (xx<mapWidth)){
			if(it < totalItems){
				tile[player[s].map][xx][yy].item = it;
				sendItAll("!i " + xx + " " + yy + " " + it);
			}else{sendItOff(s, "0 Bad item.");}
		}
		sendItAll("*");
	}
	
	public void echoAllColor(int s, String data){
	int it = 0; 
		String[] st; 
		try{
			st = data.split(" ", 3);
			it = Integer.parseInt(st[1]);
			if(it>30){
				sendItOff(s,"0 Only colors 1-30");
			}else{
				if(st[2].equals("")==false)
				sendItAll(it + " " + st[2]);
			}
		}catch(Exception e){
			sendItOff(s, "0 Use: @echoallc <#>");
		}
	}

	public void echoMapColor(int s, String data){
	int it = 0; 
		String[] st; 
		try{
			st = data.split(" ", 3);
			it = Integer.parseInt(st[1]);
			if(it>30){
				sendItOff(s,"0 Only colors 1-30");
			}else{
				if(st[2].equals("")==false)
				sendItMap(player[s].map, it + " " + st[2]);
			}
		}catch(Exception e){
			sendItOff(s, "0 Use: @echoallc <#>");
		}
	}
	
	public void sit(int s){
		player[s].facing=21;
		newPos(s, player[s].map, player[s].xCoord, player[s].yCoord);
	}
	
	public void action(int s){
		switch(getFacingDirection(s)){
			case 'u': if((player[s].yCoord-1 > 0) && (tile[player[s].map][player[s].xCoord][player[s].yCoord-1].npc > 0) )
						attackNpc(s, tile[player[s].map][player[s].xCoord][player[s].yCoord-1].npc, player[s].map);
					if(player[s].altaction==0){player[s].facing=19; player[s].altaction=1;}else{player[s].facing=20; player[s].altaction=0;}
					break;
			case 'd': if((player[s].yCoord+1 < mapHeight) && (tile[player[s].map][player[s].xCoord][player[s].yCoord+1].npc > 0) )
						attackNpc(s, tile[player[s].map][player[s].xCoord][player[s].yCoord+1].npc, player[s].map);
					if(player[s].altaction==0){player[s].facing=13; player[s].altaction=1;}else{player[s].facing=14; player[s].altaction=0;} break;
			case 'l': if((player[s].xCoord-1 > 0) && (tile[player[s].map][player[s].xCoord-1][player[s].yCoord].npc > 0) )
						attackNpc(s, tile[player[s].map][player[s].xCoord-1][player[s].yCoord].npc, player[s].map);
				if(player[s].altaction==0){player[s].facing=17; player[s].altaction=1;}else{player[s].facing=18; player[s].altaction=0;} break;
			case 'r': if((player[s].xCoord+1 < mapWidth) && (tile[player[s].map][player[s].xCoord+1][player[s].yCoord].npc > 0) )
						attackNpc(s, tile[player[s].map][player[s].xCoord+1][player[s].yCoord].npc, player[s].map);
				if(player[s].altaction==0){player[s].facing=15; player[s].altaction=1;}else{player[s].facing=16; player[s].altaction=0;} break;
		}
		newPos(s, player[s].map, player[s].xCoord, player[s].yCoord);
	}
	
	public void attackNpc(int s1, int n, int m){
		int nroll = randomNum(map[m].gnpc[n].minatk, map[m].gnpc[n].maxatk+1);
		int proll = randomNum(player[s1].mindef, player[s1].maxdef+1);
		
		if (proll > nroll){
			int newroll = proll-nroll;
			map[m].gnpc[n].curhp = map[m].gnpc[n].curhp - (newroll);
			sendItOff(s1, "3 -!-" + player[s1].name + " hits " + map[m].gnpc[n].name + ". (" + newroll + ")");
		if(map[m].gnpc[n].curhp<=0){
			sendItOff(s1, "3 -!- You have killed " + map[m].gnpc[n].name + ".");
			destroyNpc(m,n);
		}
		}
	}
	
	public void showStats(int s){
		sendItOff(s, "0 HP: " + player[s].curhp + "/" + player[s].maxhp);
		sendItOff(s, "0 Attack: " + player[s].minatk + "/" + player[s].maxatk);
	}
	
	public void switchItem(int s, String data){
		String[] st = data.split(" ", 2);
		int slot = Integer.parseInt(st[1]);
		if(slot > 0 && slot < 31){
			int hand = player[s].hand;
			int inven = player[s].inventory[slot];
			player[s].inventory[slot] = hand;
			player[s].hand = inven;
			sendItOff(s, "i " + player[s].hand);
			sendItOff(s, "v " + player[s].inventory[slot] + " " + slot);
		}
	}
	
	public void getItem(int s){
		int hand = 0;
		int facing = 0;
		facing = player[s].facing;
		hand = player[s].hand;
		player[s].facing= 21;
		newPos(s, player[s].map, player[s].xCoord, player[s].yCoord);
		if(item[tile[player[s].map][player[s].xCoord][player[s].yCoord].item].getable || tile[player[s].map][player[s].xCoord][player[s].yCoord].item ==0){
			player[s].hand = tile[player[s].map][player[s].xCoord][player[s].yCoord].item;
			sendItOff(s, "i " + player[s].hand);
			//Send New Player Hand
			tile[player[s].map][player[s].xCoord][player[s].yCoord].item = hand;
			sendItAll("!i " + player[s].xCoord + " " + player[s].yCoord + " " + tile[player[s].map][player[s].xCoord][player[s].yCoord].item);
		}
		player[s].facing=facing;
		newPos(s, player[s].map, player[s].xCoord, player[s].yCoord);
	}
	
	public void playerTalk(int s, String data){
		sendItArea(player[s].map, player[s].xCoord, player[s].yCoord, "0 " + player[s].name + ": " + data);
	}
	
	public void playerShout(int s, String data){
		sendItMap(player[s].map, "10 " + player[s].name + " shouts: " + data);
	}	
	
	public void look(int s){
		int xx = player[s].xCoord;
		int yy = player[s].yCoord;
		switch(getFacingDirection(s)){
			case 'u': yy--; break;
			case 'd': yy++; break;
			case 'l': xx--; break;
			case 'r': xx++; break;
		}
	
		if((yy>(-1)) && (yy<mapHeight) && (xx>(-1)) && (xx<mapWidth))
			if(tile[player[s].map][xx][yy].player>0){
				sendItOff(s, "0 You see " + player[tile[player[s].map][xx][yy].player].name + ".");
				sendItOff(tile[player[s].map][xx][yy].player, "16 " + player[s].name + " is looking you over.");
			}else if(tile[player[s].map][xx][yy].npc>0){
				sendItOff(s, "0 You see " + map[player[s].map].gnpc[tile[player[s].map][xx][yy].npc].name + ". " + tile[player[s].map][xx][yy].npc);
			}else if (tile[player[s].map][xx][yy].readable.equals("")==false){
				if (tile[player[s].map][xx][yy].item>0)
				sendItOff(s, "0 There's something written on " + item[tile[player[s].map][xx][yy].item].name + ", it says:");
					sendItOff(s, tile[player[s].map][xx][yy].readable);
			}else if (tile[player[s].map][xx][yy].item>0)
				sendItOff(s, "0 You see " + item[tile[player[s].map][xx][yy].item].name + ".");
			else if (tile[player[s].map][xx][yy].bitem>0)
				sendItOff(s, "0 You see " + bitem[tile[player[s].map][xx][yy].bitem].name + ".");
			else sendItOff(s, "0 You see " + floorName[tile[player[s].map][xx][yy].floor] + ".");
	}
	
	public void showWhat(int s){
		int xx = player[s].xCoord;
		int yy = player[s].yCoord;
		switch(getFacingDirection(s)){
			case 'u': yy--; break;
			case 'd': yy++; break;
			case 'l': xx--; break;
			case 'r': xx++; break;
		}
	
		if((yy>(-1)) && (yy<mapHeight) && (xx>(-1)) && (xx<mapWidth))
			if(tile[player[s].map][xx][yy].player>0){
				sendItOff(s, "0 Player: " + tile[player[s].map][xx][yy].player + ".");
			}
			if(tile[player[s].map][xx][yy].npc>0){
				sendItOff(s, "0 NPC: " + tile[player[s].map][xx][yy].npc + ". ");
			}
			sendItOff(s, "0 Item: " + tile[player[s].map][xx][yy].item + ".");
			sendItOff(s, "0 Wall: " + tile[player[s].map][xx][yy].bitem + ".");
			sendItOff(s, "0 Floor: " + tile[player[s].map][xx][yy].floor + ".");
	}
	
	public void movePlayer(int s, char dir){
		switch(dir){
			case 'u': 	
				if(player[s].facing % 2 == 0)player[s].facing=3;else player[s].facing=4;
				if(player[s].yCoord > 0){
					if(player[s].clip){
						if((floorWalk[tile[player[s].map][player[s].xCoord][player[s].yCoord-1].floor]) && (item[tile[player[s].map][player[s].xCoord][player[s].yCoord-1].item].walkable==true) && (tile[player[s].map][player[s].xCoord][player[s].yCoord-1].bitem==0) && (tile[player[s].map][player[s].xCoord][player[s].yCoord-1].player==0)  && (tile[player[s].map][player[s].xCoord][player[s].yCoord-1].npc==0))
							newPos(s,player[s].map,player[s].xCoord,player[s].yCoord-1); 
						else
							newPos(s,player[s].map,player[s].xCoord,player[s].yCoord);
					}else newPos(s,player[s].map,player[s].xCoord,player[s].yCoord-1);
				}else if (player[s].yCoord==0 && map[player[s].map].nExit>0){
					newPos(s,map[player[s].map].nExit,player[s].xCoord,49); 
				}
			break;
			case 'd':
				if(player[s].facing % 2 == 0)player[s].facing=1;else player[s].facing=2;
				if(player[s].yCoord < mapHeight-1){
				if(player[s].clip){
				if((floorWalk[tile[player[s].map][player[s].xCoord][player[s].yCoord+1].floor]) && (item[tile[player[s].map][player[s].xCoord][player[s].yCoord+1].item].walkable==true)  && (tile[player[s].map][player[s].xCoord][player[s].yCoord+1].bitem==0) && (tile[player[s].map][player[s].xCoord][player[s].yCoord+1].player==0)  && (tile[player[s].map][player[s].xCoord][player[s].yCoord+1].npc==0))
				newPos(s,player[s].map,player[s].xCoord,player[s].yCoord+1); 
				else newPos(s,player[s].map,player[s].xCoord,player[s].yCoord); 
				}else newPos(s,player[s].map,player[s].xCoord,player[s].yCoord+1); 
				}else if (player[s].yCoord==mapHeight-1 && map[player[s].map].sExit>0){
					newPos(s,map[player[s].map].sExit,player[s].xCoord,0); 
				}
				
			break;
			case 'l':
				if(player[s].facing % 2 == 0)player[s].facing=7;else player[s].facing=8;
				if(player[s].xCoord > 0){ 
				if(player[s].clip){
				if((floorWalk[tile[player[s].map][player[s].xCoord-1][player[s].yCoord].floor]) && (item[tile[player[s].map][player[s].xCoord-1][player[s].yCoord].item].walkable==true)  && (tile[player[s].map][player[s].xCoord-1][player[s].yCoord].bitem==0) && (tile[player[s].map][player[s].xCoord-1][player[s].yCoord].player==0)  && (tile[player[s].map][player[s].xCoord-1][player[s].yCoord].npc==0))
				newPos(s,player[s].map,player[s].xCoord-1,player[s].yCoord);
				else newPos(s,player[s].map,player[s].xCoord,player[s].yCoord); 
				} else newPos(s,player[s].map,player[s].xCoord-1,player[s].yCoord);
				}else if (player[s].xCoord==0 && map[player[s].map].wExit>0){
					newPos(s,map[player[s].map].wExit,49,player[s].yCoord); 
				}
			break;
			case 'r':
				if(player[s].facing % 2 == 0)player[s].facing=5;else player[s].facing=6;
				if(player[s].xCoord < mapWidth-1){
					if(player[s].clip){
					if((floorWalk[tile[player[s].map][player[s].xCoord+1][player[s].yCoord].floor]) && (item[tile[player[s].map][player[s].xCoord+1][player[s].yCoord].item].walkable==true)  && (tile[player[s].map][player[s].xCoord+1][player[s].yCoord].bitem==0) && (tile[player[s].map][player[s].xCoord+1][player[s].yCoord].player==0) && (tile[player[s].map][player[s].xCoord+1][player[s].yCoord].npc==0)){
						newPos(s,player[s].map,player[s].xCoord+1,player[s].yCoord); 
					}else{ 
						newPos(s,player[s].map,player[s].xCoord,player[s].yCoord); 
					}
				} else newPos(s,player[s].map,player[s].xCoord+1,player[s].yCoord); 
				}else if(player[s].xCoord==mapWidth-1 && map[player[s].map].eExit > 0){
					newPos(s,map[player[s].map].eExit,0,player[s].yCoord); 
				}	
			break;	
		}
	}

	public void moveNPC(int s, int m, char dir){
		switch(dir){
			case 'u': 	
				if(map[m].gnpc[s].facing % 2 == 0)map[m].gnpc[s].facing=3;else map[m].gnpc[s].facing=4;
				if(map[m].gnpc[s].yCoord > 1) 
				if((floorWalk[tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord][map[m].gnpc[s].yCoord-1].floor]) && (item[tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord][map[m].gnpc[s].yCoord-1].item].walkable==true) && (tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord][map[m].gnpc[s].yCoord-1].bitem==0) && (tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord][map[m].gnpc[s].yCoord-1].player==0)  && (tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord][map[m].gnpc[s].yCoord-1].npc==0))
				newNpcPos(s,map[m].gnpc[s].map,map[m].gnpc[s].xCoord,map[m].gnpc[s].yCoord-1); 
				else newNpcPos(s,map[m].gnpc[s].map,map[m].gnpc[s].xCoord,map[m].gnpc[s].yCoord); 
			break;
			case 'd':
				if(map[m].gnpc[s].facing % 2 == 0)map[m].gnpc[s].facing=1;else map[m].gnpc[s].facing=2;
				if(map[m].gnpc[s].yCoord < mapHeight) 
				if((floorWalk[tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord][map[m].gnpc[s].yCoord+1].floor]) && (item[tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord][map[m].gnpc[s].yCoord+1].item].walkable==true)  && (tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord][map[m].gnpc[s].yCoord+1].bitem==0) && (tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord][map[m].gnpc[s].yCoord+1].player==0)  && (tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord][map[m].gnpc[s].yCoord+1].npc==0))
				newNpcPos(s,map[m].gnpc[s].map,map[m].gnpc[s].xCoord,map[m].gnpc[s].yCoord+1); 
				else newNpcPos(s,map[m].gnpc[s].map,map[m].gnpc[s].xCoord,map[m].gnpc[s].yCoord); 
			break;
			case 'l':
				if(map[m].gnpc[s].facing % 2 == 0)map[m].gnpc[s].facing=7;else map[m].gnpc[s].facing=8;
				if(map[m].gnpc[s].xCoord > 1) 
				if((floorWalk[tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord-1][map[m].gnpc[s].yCoord].floor]) && (item[tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord-1][map[m].gnpc[s].yCoord].item].walkable==true)  && (tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord-1][map[m].gnpc[s].yCoord].bitem==0) && (tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord-1][map[m].gnpc[s].yCoord].player==0)  && (tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord-1][map[m].gnpc[s].yCoord].npc==0))
				newNpcPos(s,map[m].gnpc[s].map,map[m].gnpc[s].xCoord-1,map[m].gnpc[s].yCoord);
				else newNpcPos(s,map[m].gnpc[s].map,map[m].gnpc[s].xCoord,map[m].gnpc[s].yCoord); 
			break;
			case 'r':
				if(map[m].gnpc[s].facing % 2 == 0)map[m].gnpc[s].facing=5;else map[m].gnpc[s].facing=6;
				if(map[m].gnpc[s].xCoord < mapWidth)
				if((floorWalk[tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord+1][map[m].gnpc[s].yCoord].floor]) && (item[tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord+1][map[m].gnpc[s].yCoord].item].walkable==true)  && (tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord+1][map[m].gnpc[s].yCoord].bitem==0) && (tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord+1][map[m].gnpc[s].yCoord].player==0) && (tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord+1][map[m].gnpc[s].yCoord].npc==0))
				newNpcPos(s,map[m].gnpc[s].map,map[m].gnpc[s].xCoord+1,map[m].gnpc[s].yCoord); 
				else newNpcPos(s,map[m].gnpc[s].map,map[m].gnpc[s].xCoord,map[m].gnpc[s].yCoord); 
			break;
		}
	}

	
	public void newPos(int s, int m, int x, int y){
		
		String tobesent="";
		int xOld = player[s].xCoord;
		int yOld = player[s].yCoord;
		int mOld = player[s].map;
		
		if(mOld != m){
			//Send Map Info
			//if(mOld!=0){
			map[mOld].players = map[mOld].players.replaceAll("#" + s + "#","");
			//}
			map[m].players = map[m].players + "#" + s + "#";
			sendItOff(s, map[m].welcome);
		}
		
		if (player[s].trans > 0){
			player[s].trans=0;
			sendItMap(mOld, "!i " + player[s].xCoord + " " + player[s].yCoord + " " + tile[player[s].map][player[s].xCoord][player[s].yCoord].item);
		}

		player[s].xCoord=x;
		player[s].yCoord=y;
		player[s].map=m;

		if (tile[m][x][y].player > 0 && tile[m][x][y].player != s){
			sendItMap(player[s].map, "21 " + player[s].name + " has splatted on " + player[tile[m][x][y].player].name);
			player[tile[m][x][y].player].trans = 170;
			sendItMap(player[s].map, "!i " + x + " " + y + " " + 170);
		}
		
		//player[s].xCoord=x;
		//player[s].yCoord=y;
		//player[s].map=m;
		tile[mOld][xOld][yOld].player = 0; 
		if(m!=0)tile[m][x][y].player=s;
		sendItMap(mOld, "p " + xOld + " " + yOld + " " + 0 + " " + 0 + " " + 0 + " " + 0 + " " + 0 + " " + 0);
		sendItOff(s,"m " + player[s].map + " " + player[s].xCoord + " " + player[s].yCoord);
		sendItMap(player[s].map, "p " + player[s].xCoord + " " + player[s].yCoord + " " + player[s].facing + " " + player[s].gender + " " + player[s].cloth1 + " " + player[s].cloth2 + " " + player[s].cape + " " + player[s].hair);
		
		
		if(mOld != m){
			sendItOff(s,returnCmap(player[s].map));
			sendItMap(mOld,"*");
		}	
		sendItMap(player[s].map, "*");
	}

	public void newNpcPos(int s, int m, int x, int y){
		//If the map is changing make the trans go away. :) 
		int xOld = map[m].gnpc[s].xCoord;
		int yOld = map[m].gnpc[s].yCoord;
		int mOld = map[m].gnpc[s].map;
		
		if (map[m].gnpc[s].trans > 0){
			map[m].gnpc[s].trans=0;
			sendItMap(m, "!i " + map[m].gnpc[s].xCoord + " " + map[m].gnpc[s].yCoord + " " + tile[map[m].gnpc[s].map][map[m].gnpc[s].xCoord][map[m].gnpc[s].yCoord].item);
		}
		if (tile[m][x][y].npc > 0 && tile[m][x][y].npc != s){
			//sendItAll("21 " + map[m].gnpc[s].name + " has splatted on " + player[tile[m][x][y].player].name);
			player[tile[m][x][y].player].trans = 170;
			sendItMap(m, "!i " + x + " " + y + " " + 170);
		}
		map[m].gnpc[s].xCoord=x;
		map[m].gnpc[s].yCoord=y;
		map[m].gnpc[s].map=m;
		tile[mOld][xOld][yOld].npc = 0; 
		if(m!=0)tile[m][x][y].npc=s;
		sendItMap(m, "p " + xOld + " " + yOld + " " + 0 + " " + 0 + " " + 0 + " " + 0 + " " + 0 + " " + 0);
		sendItMap(m, "p " + map[m].gnpc[s].xCoord + " " + map[m].gnpc[s].yCoord + " " + map[m].gnpc[s].facing + " " + map[m].gnpc[s].gender + " " + map[m].gnpc[s].cloth1 + " " + map[m].gnpc[s].cloth2 + " " + map[m].gnpc[s].cape + " " + map[m].gnpc[s].hair);
		//sendItOff(s,"m " + map[m].gnpc[s].map + " " + map[m].gnpc[s].xCoord + " " + map[m].gnpc[s].yCoord);
		sendItMap(m, "*");
		if(mOld != m){
			//Send Map Info
			//sendItOff(s, map[m].welcome);
		}
	}
	
		
class Server extends Thread {
	
	public void run(){
		while(running){
		int s = nextSocket();
		try{	
			sock[s] = servSock.accept();
			o[s] = new PrintStream(sock[s].getOutputStream());
			sock[s].setTcpNoDelay(true);
			System.out.println("Socket " + s + " connected.");
		}catch(Exception e){
			System.out.println("Error accepting socket: " + e);
		}
		sendItOff(s, "0 Connected to " + serverName + "!");
		sendItOff(s, "s");
		if(s==0){
			sendItOff(s, "0 Server full!");
			killSock(s);
		}else{
			createClient(s);
		}
			
		}
	}
	
	public int nextSocket(){
		int i;
		for(i=1; i<maxConnections; i++){
			if(sock[i]!=null)
			if(sock[i].isConnected()==false)sock[i]=null;
			if(sock[i]==null)break;
		}
		if(i==maxConnections)
			return(0);
		else
			return(i);
	}	
	
}	
class Client extends Thread{
SqlTest loginSql = new SqlTest();
BufferedReader i;
boolean opened=true;
int loginAttempts = 0;
int s;
	public Client(int i){
		s=i;
		start();
	}
	
	public void run(){
		try{
			i = new BufferedReader(new InputStreamReader(sock[s].getInputStream()));
		}catch(Exception e){
			System.out.println("Error creating Input Sream reader: " + e);
		}
		while(opened){
			try{
				String fromSock = i.readLine();
				if(fromSock != null){
					if(player[s].state.equals("validated")){//PLAYER PROCESSES HERE
						//System.out.println(fromSock);
						if(fromSock.equals("")==false)
						socketStuff(fromSock, s);
					}else if(player[s].state.equals("creating")) {
						String [] sp = fromSock.split(" ",7);
						if(sp[0].equals("") == false)
						if(sp[0].toLowerCase().equals("#")){
							sendItOff(s,"0 Creating player...");
							//sendItOff(s,"0 " + fromSock); 
							if(loginSql.checkPass(player[s].name,player[s].pass).equals("new")) {
								//sendItOff(s, "0 Colors: " + sp[1] + "-" + sp[2] + "-"  + sp[3] + "-"  + sp[4] + "-"  + sp[5]); 
								loginSql.createChar(player[s].name, player[s].pass, 1, 15, 15, sp[1] + "-" + sp[2] + "-"  + sp[3] + "-"  + sp[4] + "-"  + sp[5]);
								sendItOff(s, "17 Player: " + player[s].name + " has been created!");
								sendItOff(s, "k"); //Hide Create Player
								login(s, player[s].name);
							}else{
								sendItOff(s,"0 Oops... Looks like someone already created that character!");
								sendItOff(s,"0 Please restart SWD and try again!");
								opened=false;
							}
							
						}
					}else{//Check for 'connect' at beginning of string
						String [] sp = fromSock.split(" ");
						if(sp[0].equals("") == false)
						if(sp[0].toLowerCase().equals("connect")){
							try{
								if(loginSql.checkPass(sp[1],sp[2]).equals("true")) {
									login(s, sp[1]);
								}else if(loginSql.checkPass(sp[1],sp[2]).equals("new")) {//Create New Character
									//newChar(s, sp[1],sp[2]);
									sendItOff(s, "h");
									sendItOff(s, "n");
									player[s].state="creating";
									player[s].name=sp[1];
									player[s].pass=sp[2];
								}else{
									loginAttempts++;
									sendItOff(s,"0 Login attempt " + loginAttempts + "/" + maxLoginAttempts + " unsuccessful.");
									if(loginAttempts >= maxLoginAttempts)
										opened=false;
								}
							}catch(Exception e){
								//System.out.println("Connect Exception: " + e);
								//e.printStackTrace();
								sendItOff(s, "0 Enter a login name and password!");
							}
								
						}else{
							sendItOff(s,"0 Please connect first.");
						}
					}
				}else{
					opened=false;
				}
			}catch(Exception e){
				//System.out.println("Exception: " + e);
				//e.printStackTrace();
				opened=false;
			}
			
		}
		if(player[s].state.equals("validated"))
		logout(s);
		killSock(s);
	}	
}

public void loadMapConfig(int mapNumber){
	String name = "map" + mapNumber + ".ini";
	File mapFile = new File(".//maps//" + name);
	BufferedReader input = null;
	String line = "";
	Boolean reading=true;
	try{input = new BufferedReader(new FileReader(mapFile));}catch(Exception e){System.out.println("Cannot find map config file.");}
	try{
		System.out.print("! - Loading map config......");
		while(line!=null){
			try{line = input.readLine();}catch(Exception e){System.out.println("Error reading map config input");}
			if(line!=null){
				String[] lineSplit = line.split("=", 2);
				if(lineSplit[0].toLowerCase().equals("welcome")){
					map[mapNumber].welcome = lineSplit[1];
				}else if(lineSplit[0].toLowerCase().equals("read")){
					String[] leSplit = lineSplit[1].split(" ", 3);
					tile[mapNumber][Integer.parseInt(leSplit[0])][Integer.parseInt(leSplit[1])].readable = leSplit[2];
					//read=14 13 20 This is the fucking shop. You buy shit here. MOTHA FUKKAH! 
				}else if(lineSplit[0].toLowerCase().equals("north")){
					map[mapNumber].nExit = Integer.parseInt(lineSplit[1]);
				}else if(lineSplit[0].toLowerCase().equals("south")){
					map[mapNumber].sExit = Integer.parseInt(lineSplit[1]);
				}else if(lineSplit[0].toLowerCase().equals("east")){
					map[mapNumber].eExit = Integer.parseInt(lineSplit[1]);
				}else if(lineSplit[0].toLowerCase().equals("west")){
					map[mapNumber].wExit = Integer.parseInt(lineSplit[1]);
				}
			}
		}
		System.out.println("OK!");
	}catch(Exception e){System.out.println("Failed!"); er(e);}
	try{input.close();}catch(Exception e){System.out.println("Error closing " + name);}
}	

public void loadMap(int mapNumber){
	String name = "map" + mapNumber + ".swd";
	String type;
	int xC = 0;
	int yC = 0;
	int tN = 0;
	File mapFile = new File(".//maps//" + name);
	BufferedReader input = null;
	String line = "";
	Boolean reading=true;
	try{input = new BufferedReader(new FileReader(mapFile));}catch(Exception e){System.out.println("Cannot find map file: " + name);}
	try{
		System.out.print("! - Loading map......");
		//for(int x = 1; x < mapWidth; x++){for(int y = 1; y < mapHeight; y++){
			while(line!=null){
			try{line = input.readLine();}catch(Exception e){System.out.println("Error reading loadMap input");}
			if(line!=null){
			//System.out.println(line);
			String[] lineSplit = line.split(":", 2);
			type = lineSplit[0];
			if(type.equals("f")){
				String[] tileSplit = lineSplit[1].split(" ",2);
				String[] coordSplit = tileSplit[0].split(",",2);
				//System.out.println("Coordsplit: " + coordSplit[0] + " " + coordSplit[1]);
				tN = Integer.parseInt(tileSplit[1]);
				xC = Integer.parseInt(coordSplit[0]);
				yC = Integer.parseInt(coordSplit[1]);
				if((xC >= 0 && xC < mapWidth)&&(yC >= 0 && yC < mapHeight)){
					tile[mapNumber][xC][yC].floor = tN;
					ctile[mapNumber][xC][yC].floor = tN;
				}
			//System.out.println("Type: " + type + " Coords:" + xC + "," + yC + " Tile:" +tN);
			}else if(type.equals("i")){
				String[] tileSplit = lineSplit[1].split(" ",2);
				String[] coordSplit = tileSplit[0].split(",",2);
				tN = Integer.parseInt(tileSplit[1]);
				xC = Integer.parseInt(coordSplit[0]);
				yC = Integer.parseInt(coordSplit[1]);
				if((xC >= 0 && xC < mapWidth)&&(yC >= 0 && yC < mapHeight)){
					//System.out.println("ITEM: " + tN + " on tile: " + xC + ", " + yC);
					tile[mapNumber][xC][yC].item = tN;
					ctile[mapNumber][xC][yC].item = tN;
				}
			}else if(type.equals("b")){
				String[] tileSplit = lineSplit[1].split(" ",2);
				String[] coordSplit = tileSplit[0].split(",",2);
				tN = Integer.parseInt(tileSplit[1]);
				xC = Integer.parseInt(coordSplit[0]);
				yC = Integer.parseInt(coordSplit[1]);
				if((xC > 0 && xC < mapWidth)&&(yC > 0 && yC < mapHeight)){
					//System.out.println("ITEM: " + tN + " on tile: " + xC + ", " + yC);
					tile[mapNumber][xC][yC].bitem = tN;
					ctile[mapNumber][xC][yC].bitem = tN;
				}
			}else if(type.equals("dim")){
				//String[] coordSplit = lineSplit[1].split(",",2);
				//mapWidth = Integer.parseInt(coordSplit[0]);
				//mapHeight = Integer.parseInt(coordSplit[1]);
				//System.out.println("NEW DIMENSIONS: " + mapWidth + " by " + mapHeight);
			}
			//try{tileNUM = Integer.parseInt(line);}catch(Exception e){System.out.println("Error parsing tile info");}
			//try{tile[mapN][YO1][YO2].floor = tileNUM;}catch(Exception e){System.out.println("Error - dividing by 41" + e);}
		}
		}
	System.out.println("OK!");
	}catch(Exception e){System.out.println("Failed!"); er(e);}
	try{input.close();}catch(Exception e){System.out.println("Error closing " + name);}
}

	public String returnCmap(int mapNum){
		String holder = "";
		for(int x = 0; x < mapWidth; x++){
		for(int y = 0; y < mapHeight; y++){
		if(tile[mapNum][x][y].item != ctile[mapNum][x][y].item)
			holder = holder + "!i " + x + " " + y + " " + tile[mapNum][x][y].item + "\n";
		if(tile[mapNum][x][y].bitem != ctile[mapNum][x][y].bitem)
			holder = holder + "!b " + x + " " + y + " " + tile[mapNum][x][y].bitem + "\n";
		if(tile[mapNum][x][y].floor != ctile[mapNum][x][y].floor)
			holder= holder + "!f " + x + " " + y + " " + tile[mapNum][x][y].floor + "\n";
		if(tile[mapNum][x][y].player != ctile[mapNum][x][y].player)
			if(player[tile[mapNum][x][y].player].trans > 0)
				holder = holder + "!i " + x + " " + y + " " + player[tile[mapNum][x][y].player].trans + "\n";
			else
				holder = holder + "p " + x + " " + y + " " + player[tile[mapNum][x][y].player].facing + " " + player[tile[mapNum][x][y].player].gender + " " + player[tile[mapNum][x][y].player].cloth1 + " " + player[tile[mapNum][x][y].player].cloth2 + " " + player[tile[mapNum][x][y].player].cape + " " + player[tile[mapNum][x][y].player].hair + "\n";
		if(tile[mapNum][x][y].npc != ctile[mapNum][x][y].npc)
			holder = holder + "p " + x + " " + y + " " + map[mapNum].gnpc[tile[mapNum][x][y].npc].facing + " " + map[mapNum].gnpc[tile[mapNum][x][y].npc].gender + " " + map[mapNum].gnpc[tile[mapNum][x][y].npc].cloth1 + " " + map[mapNum].gnpc[tile[mapNum][x][y].npc].cloth2 + " " + map[mapNum].gnpc[tile[mapNum][x][y].npc].cape + " " + map[mapNum].gnpc[tile[mapNum][x][y].npc].hair + "\n";
			//holder= holder + "!p " + x + " " + y + " " + tile[mapNum][x][y].player;	
		}
		}
		//System.out.println("Sending: " + holder); 
		return(holder);
	}

	public void setupItems(){
	try{
		System.out.print("! - Initializing map......");
		for(int m = 0; m < totalMaps+1; m++)
		for(int x = 0; x < mapWidth; x++)
		for(int y = 0; y < mapHeight; y++)
		tile[m][x][y] = new SWDTile();
		System.out.println("OK!");
		System.out.print("! - Initializing Cmap......");
		for(int m = 0; m < totalMaps+1; m++)
		for(int x = 0; x < mapWidth; x++)
		for(int y = 0; y < mapHeight; y++)
		ctile[m][x][y] = new SWDTile();
		for(int m = 0; m < totalMaps+1; m++)
		map[m] = new SWDMap();
		System.out.println("OK!");
		System.out.print("! - Initializing items......");
		for(int x = 0; x < totalItems; x++)
		item[x] = new SWDItem();
		System.out.println("OK!");
		System.out.print("! - Initializing bitems.....");
		for(int x = 0; x < totalBitems; x++)
		bitem[x] = new SWDItem();
		System.out.println("OK!");
		
	}catch(Exception e){System.out.println("Failed!");er(e);}
	}


public void loadItemInfo(){
	String type;
	int xC = 0;
	int yC = 0;
	int ih = 0;
	int iy = 0; 
	int tN = 0;
	File mapFile = new File(".//config//iteminfo.txt");
	BufferedReader input = null;
	String line = "";
	try{input = new BufferedReader(new FileReader(mapFile));}catch(Exception e){System.out.println("Cannot find iteminfo.txt");}
	try{
		System.out.print("! - Loading items......");
		while(line!=null){
			try{line = input.readLine();}catch(Exception e){System.out.println("Error reading loadItemInfo input");}
			//System.out.println(line);
			if(line!=null){
			//System.out.println(line);
			String[] lineSplit = line.split(":", 2);
			type = lineSplit[0];
			if(type.equals("i")){
			String[] itemSplit = lineSplit[1].split(",",7);
			//System.out.println("Coordsplit: " + coordSplit[0] + " " + coordSplit[1]);
			tN = Integer.parseInt(itemSplit[0]);
			item[tN].x = Integer.parseInt(itemSplit[1]);
			item[tN].y = Integer.parseInt(itemSplit[2]);
			item[tN].w = Integer.parseInt(itemSplit[3]);
			item[tN].h = Integer.parseInt(itemSplit[4]);
			item[tN].xoff = Integer.parseInt(itemSplit[5]);
			item[tN].yoff = Integer.parseInt(itemSplit[6]);
			}else if(type.equals("b")){
			String[] itemSplit = lineSplit[1].split(",",7);
			//System.out.println("Coordsplit: " + coordSplit[0] + " " + coordSplit[1]);
			tN = Integer.parseInt(itemSplit[0]);
			bitem[tN].x = Integer.parseInt(itemSplit[1]);
			bitem[tN].y = Integer.parseInt(itemSplit[2]);
			bitem[tN].w = Integer.parseInt(itemSplit[3]);
			bitem[tN].h = Integer.parseInt(itemSplit[4]);
			bitem[tN].xoff = Integer.parseInt(itemSplit[5]);
			bitem[tN].yoff = Integer.parseInt(itemSplit[6]);
			}else if(type.equals("bn")){
			String[] itemSplit = lineSplit[1].split(",",2);
			tN = Integer.parseInt(itemSplit[0]);
			bitem[tN].name = itemSplit[1];
			}else if(type.equals("in")){
			String[] itemSplit = lineSplit[1].split(",",2);
			tN = Integer.parseInt(itemSplit[0]);
			item[tN].name = itemSplit[1];
			}else if(type.equals("id")){
			String[] itemSplit = lineSplit[1].split(",",2);
			tN = Integer.parseInt(itemSplit[0]);
			if(itemSplit[1].charAt(0)=='n')
			item[tN].walkable=false;
			if(itemSplit[1].charAt(1)=='y')
			item[tN].getable=true;
			}else if(type.equals("fw")){
			String[] itemSplit = lineSplit[1].split(",",2);
			tN = Integer.parseInt(itemSplit[0]);
			if(itemSplit[1].charAt(0)=='n')
			floorWalk[tN]=false;
			if(itemSplit[1].charAt(0)=='y')
			floorWalk[tN]=true;						
			}else if(type.equals("fn")){
			String[] itemSplit = lineSplit[1].split(",",2);
			tN = Integer.parseInt(itemSplit[0]);
			floorName[tN] = itemSplit[1];
			}				
			}
		}
	System.out.println("OK!");
	}catch(Exception e){System.out.println("Failed!"); er(e);}
	try{input.close();}catch(Exception e){System.out.println("Error closing iteminfo.txt");}
}

	public void randomTalk(int mapNum, int npcNum){
	//System.out.println("test");
		int randomCnt=0;
		for(int x = 0; x < 5; x++){
			//System.out.println(map[mapNum].gnpc[npcNum].random[x]);
			if(map[mapNum].gnpc[npcNum].randomText[x]!=null)
			randomCnt++;
		}
		if(randomCnt>0)
		sendItArea(mapNum, map[mapNum].gnpc[npcNum].xCoord, map[mapNum].gnpc[npcNum].yCoord, "0 " + map[mapNum].gnpc[npcNum].name + ": " + map[mapNum].gnpc[npcNum].randomText[randomNum(0, (randomCnt))]);
	}
	
	//public void npcFollow(int mapNum, int npcNum){
	//	SWDPlayers nme = map[mapNum].gnpc[npcNum];
	//	int focus = 0;
	//	for(int y = nme.yCoord -5; y < nme.yCoord + 6; y++)
	//	for(int x = nme.xCoord -5; x < nme.xCoord + 6; x++)
	//		if(tile[mapNum][x][y].player > 0) && 
	//}

	public void follow(int mapNum, int npcNum){
		SWDPlayers n = map[mapNum].gnpc[npcNum];
		int phold = 0;
		//int s = randomNum(0,2);
		String direction = "";
		switch(followSwitch){
		case 0:
		for(int l = n.xCoord-5; l<n.xCoord+6; l++)
		for(int m = n.yCoord-5; m<n.yCoord+6; m++)
		//if(l>0 && l <= mapWidth && m > 0 && m <= mapHeight)
		if(tile[mapNum][l][m].player>0)
		if(phold>0){
		if((java.lang.Math.abs(n.xCoord-player[phold].xCoord) + java.lang.Math.abs(n.yCoord-player[phold].yCoord)) > (java.lang.Math.abs(n.xCoord-player[tile[mapNum][l][m].player].xCoord) +	java.lang.Math.abs(n.yCoord-player[tile[mapNum][l][m].player].yCoord)))
		phold=tile[mapNum][l][m].player;
		}else{
		phold=tile[mapNum][l][m].player;}
		followSwitch=1;
		break;
		default:
		for(int l = n.xCoord+5; l>n.xCoord-6; l--)
		for(int m = n.yCoord+5; m>n.yCoord-6; m--)
		if(tile[mapNum][l][m].player>0)
		if(phold>0){
		if((java.lang.Math.abs(n.xCoord-player[phold].xCoord) + java.lang.Math.abs(n.yCoord-player[phold].yCoord)) >
		(java.lang.Math.abs(n.xCoord-player[tile[mapNum][l][m].player].xCoord) +
		java.lang.Math.abs(n.yCoord-player[tile[mapNum][l][m].player].yCoord)))
		phold=tile[mapNum][l][m].player;
		}else{
		phold=tile[mapNum][l][m].player;}
		followSwitch=0;
		break;
		}
		if(phold>0){
		if(n.yCoord>player[phold].yCoord)direction=direction+"u";
		else if (n.yCoord<player[phold].yCoord) direction=direction+"d";
		if(n.xCoord>player[phold].xCoord)direction=direction+"l";
		else if(n.xCoord<player[phold].xCoord)direction=direction+"r";

		if(direction.equals("u"))
			moveNPC(npcNum, mapNum, 'u');
		else if(direction.equals("d"))
			moveNPC(npcNum, mapNum, 'd');
		else if(direction.equals("l"))
			moveNPC(npcNum, mapNum, 'l');
		else if(direction.equals("r"))
			moveNPC(npcNum, mapNum, 'r');
		else if(direction.equals(""))
			randomWalk(mapNum, npcNum);
		else
			moveNPC(npcNum, mapNum, direction.charAt(randomNum(1,3)-1));
		}else randomWalk(mapNum, npcNum);
	}

	
	public void randomWalk(int mapNum, int npcNum){
		//System.out.println("test");
		int randomCnt=randomNum(0,4);

		switch (randomCnt) {
			case 0: moveNPC(npcNum, mapNum, 'u'); break;
			case 1: moveNPC(npcNum, mapNum, 'd'); break;
			case 2: moveNPC(npcNum, mapNum, 'l'); break;
			case 3: moveNPC(npcNum, mapNum, 'r'); break;
		}
		//sendItAll("0 " + map[mapNum].gnpc[npcNum].name + ": " + map[mapNum].gnpc[npcNum].randomText[randomNum(0, (randomCnt+1)-1)]);
	}	

	public int randomNum(int low, int high){
		try{return(low+r.nextInt(high-low));}catch(Exception e){System.out.println("randomNum error: " + e);e.printStackTrace();return(low+r.nextInt(high-low));}
	}
	

class npcLoop extends Thread{
	public void run(){
		while(running){
			for(int i = 1; i < totalMaps+1; i++){
				if(map[i]!=null){
					for(int ii = 1; ii < map[i].totalnpcs +1; ii++){//Random Talking
						if(map[i].gnpc[ii]!=null){
							if(map[i].gnpc[ii].talking){
								if(map[i].gnpc[ii].talkingSpeed*2==map[i].gnpc[ii].talkingExec){
									randomTalk(i,ii);
									map[i].gnpc[ii].talkingExec = 0;
								}else{
									map[i].gnpc[ii].talkingExec++;
								}
							}
							if(map[i].gnpc[ii].walking){
								if(map[i].gnpc[ii].walkingSpeed*2==map[i].gnpc[ii].walkingExec){
									if(map[i].gnpc[ii].following){
										follow(i,ii);
									}else{ 
										randomWalk(i,ii);
									}
									map[i].gnpc[ii].walkingExec = 0;
								}else{
									map[i].gnpc[ii].walkingExec++;
								}
							}
						}	
					}
				}
			}
			//Start Pause Sequence
			synchronized(this){try{this.wait(500);}catch(InterruptedException ignored){}}
			//End Pause Sequence
		}
	}
}


	public static void er(Exception e){
		System.out.println("Exception f0und: " + e);
		e.printStackTrace();
	}
	
}
