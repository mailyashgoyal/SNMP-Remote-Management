import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * @author 
 *
 */
public class Router234 {

	//Declaring the Variables
	static String  hostName  = null;
	static String  portNumber  = null;
	static String routerId = null;

	//Getting filr names
	static String configfile;
	static String config_rp;
	static String config_topo;

	static boolean hostInput = false;
	static int maxRouters = 0;
	static String sourceString = "C:\\Users\\Yash\\workspace\\TCP\\src\\";
	static HashMap<String, Socket> activeClients =null;

	/**
	 * Main class to check the input store the values and forward the data
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		//Get CommandLine Arguments :: Router Id, Configuration Files
		GetCommandLineArgs(args);

		//Read Files
		ArrayList<ArrayList<String>> configArray =  readConfig();
		ArrayList<RoutingTable> forwardingTable = new ArrayList<RoutingTable>();
		ArrayList<Connections> ConnectionPort = new ArrayList<Connections>();
		
		//create socket
		ServerSocket welcomeSocket = null;
		//Socket clientSocket = null;
		for(int i=0; i <configArray.size() ;i++)
		{
			//get port number and host name
			if(Integer.parseInt((configArray.get(i)).get(0)) == Integer.parseInt(routerId))
			{
				hostName = (configArray.get(i)).get(1);
				portNumber = (configArray.get(i)).get(2);
				try {
					welcomeSocket = new ServerSocket(Integer.parseInt(portNumber));
				} catch (IOException e) {

					e.printStackTrace();
				}
			}

			if(welcomeSocket != null)
			{
				// this will create socket from the file... 
				//as soon as the socket gets created for the first time for the file it exits.
				break;
			}
		}

		// In that we need to have all the host registered.
		// means allow for the connection between all the hosts connected to that router.
		// check n the file the number of hosts for that router and accept the connection.
		// and also store the multicast group name.

		try 
		{
			// fetch from router host connection file
			HashMap<String, Socket>  hostConnection = null; 

			while(true) 
			{
				System.out.println("Before accept");
				Socket connectionSocket = welcomeSocket.accept();
				System.out.println("After accept");
				
				//always have receive command open
				BufferedReader inputRecieved = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())); 
				String sentence = inputRecieved.readLine();
				System.out.println("Input Sentence: "+sentence);
				
				if(sentence!=null)
				{
					// till two spaces as every message has 2 spaces
					int firstSpace = sentence.indexOf(" ");
					String reqRecived =  sentence.substring(0, firstSpace);
					String remainingString = sentence.substring(firstSpace+1);
					int secSpace = remainingString.indexOf(" ");
					System.out.println(""+reqRecived);
					ArrayList<String> mgroupsOfRouter = GetMulticastGrouspOfRpRouter();
					if(reqRecived.equals("JOIN"))
					{
											
						//Join(myId, rpId, mgroup)
						// read the data...
						// id of router to join the group
						String myId = remainingString.substring(0,secSpace);
						String remainingString2 = remainingString.substring(secSpace+1);
						int thirdSpace = remainingString2.indexOf(" ");
						String rpId = remainingString2.substring(0,thirdSpace);
						System.out.println(""+rpId);
						String mGroup = remainingString2.substring(thirdSpace+1);
						
						//check from the table if it is a rpid itself 
						//and the mgroup s also the same as of the rpId
						if(routerId.equals(rpId))
						{
							// means the router is the rpId of the group
							//add the router to the join the group in rpid file.
							PrintWriter out = new PrintWriter(new BufferedWriter
									(new FileWriter(sourceString+"mgroup", true)));
							BufferedWriter br = new BufferedWriter(out);
							String outline= myId+" "+mGroup+"\n";
							System.out.println(""+outline);
							br.write(outline);
							br.close();
						}
						else
						{
							// the next router connection
							//if the router is not the rpid then pass it on.
							// perform a join send message
							//get the id of nex hop router so that we can have connection with that router
							String nexthopRouter =DjisktaAlgo(routerId,  getRpIdfromMGroupId( mGroup));
							hostConnection  = getConnectedRouterHostname(nexthopRouter);
							Socket sck = hostConnection.get(nexthopRouter);
							JoinSend(sck, mGroup);

							boolean flag = false; 
							for(int i=0; i < forwardingTable.size(); i ++)
							{
								if(forwardingTable.get(i).SendingId == "*" && forwardingTable.get(i).MGroup == mGroup)
								{
									forwardingTable.get(i).Nexthop.add(myId);
									flag = true;
								}
							}
							if(flag == false)
							{
								//create a forwarding table
								RoutingTable rt = new RoutingTable();
								rt.SendingId = "*";
								rt.MGroup = mGroup;
								rt.Nexthop = new ArrayList<String>();
								rt.Nexthop.add(myId);
								rt.Hosts = null;
								forwardingTable.add(rt);
							}
						}
					}
					else if(reqRecived.equals("PRUNE"))
					{
						//Prune(myId, rpId, mgroup)
						// id of router to prune from the group
						String myId = remainingString.substring(0,secSpace);
						String remainingString2 = remainingString.substring(secSpace+1);
						int thirdSpace = remainingString2.indexOf(" ");
						String rpId = remainingString2.substring(0,thirdSpace);
						String mGroup = remainingString2.substring(thirdSpace+1);

						//check from the table if it is a rpid itself 
						//and the mgroup s also the same as of the rpId
						int indexToBeRemoved = -1;
						for(int i=0; i < forwardingTable.size(); i ++)
						{
							if(forwardingTable.get(i).SendingId.equals("*") && forwardingTable.get(i).MGroup.equals(mGroup)
									&& forwardingTable.get(i).Hosts.size() == 1 && forwardingTable.get(i).Nexthop.size() == 1 
									&& forwardingTable.get(i).Hosts.get(0).equals(hostName))
							{
								indexToBeRemoved = i;
							}
						}
						if( indexToBeRemoved != -1)
						{
							forwardingTable.remove(indexToBeRemoved);
						}

						String inputtoberemoved = myId+" "+mGroup;
						// means the router is the rpId of the group							
						// remove from file
						FileReader in = new FileReader(sourceString+"mgroup");
						File filename = new File(sourceString+"mgroup");
						File tempFile = new File("myTempFile");
						BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
						BufferedReader reader = new BufferedReader(in);
						String sentence1="";
						while((
								sentence1=reader.readLine()) != null)
						{
							// trim newline when comparing with lineToRemove
							if(sentence1.equals(inputtoberemoved)) continue;
							writer.write(sentence1);
						}
						reader.close();
						writer.close();
						tempFile.delete();
						boolean successful = tempFile.renameTo(filename);

						if(!(routerId.equals(rpId)))
						{
							//forward it to next router
							String nexthopRouter = DjisktaAlgo(routerId,  getRpIdfromMGroupId( mGroup));
							hostConnection  = getConnectedRouterHostname(nexthopRouter);
							Socket sck = hostConnection.get(nexthopRouter);

							String outline = "PRUNE " + myId +" "+ rpId +" "+ mGroup+"\n";
							DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
							outToServer.writeBytes(outline);
						}
					}
					else if(reqRecived.equals("MCAST"))
					{
						System.out.println("Recd Mcast: "+routerId);
						//						3 things....
						//						1) check if the router id is rp or not
						//						if its rp then display all things to hosts and foward to connected routers
						//						2)if it is not an rp then check if it is coming fom rp or goint to rp
						//						3)if it is coming from rp or going towards rp
						//						4) check if it is source specific join

						//mcast(myId,srcId,mgroup,data)
						System.out.println("Inside MCAST");
						String myId = remainingString.substring(0,secSpace);
						String remainingString2 = remainingString.substring(secSpace+1);
						int thirdSpace = remainingString2.indexOf(" ");
						String srcId = remainingString2.substring(0,thirdSpace);
						String remainingString3 = remainingString2.substring(thirdSpace+1);
						int fourthSpace = remainingString3.indexOf(" ");
						String mGroup = remainingString3.substring(0,fourthSpace);
						String data =remainingString3.substring(fourthSpace+1);
						String rpId = getRpIdfromMGroupId(mGroup);

						System.out.println("Mcast Forwarding Table values start");
						for(int i =0;i < forwardingTable.size(); i ++ )
						{
							System.out.println(forwardingTable.get(i).SendingId);
							System.out.println(forwardingTable.get(i).MGroup);
							if(forwardingTable.get(i).Nexthop != null && forwardingTable.get(i).Nexthop.size() >0)
							{System.out.println(forwardingTable.get(i).Nexthop.get(0));}
							if(forwardingTable.get(i).Hosts != null && forwardingTable.get(i).Hosts.size() >0)
							{System.out.println(forwardingTable.get(i).Hosts);}

						}
						System.out.println("Mcast Forwarding Table values end");

						int getIndex = -1;
						String fromRP = null;
						for(int i =0 ; i <forwardingTable.size(); i ++)
						{
							// get all the  hosts as well as nect hops
							if(forwardingTable.get(i).SendingId.equals(srcId) && forwardingTable.get(i).MGroup.equals(mGroup))
							{
								getIndex = i ;
							}
						}
						if(getIndex == -1)
						{
							for(int i =0 ; i <forwardingTable.size(); i ++)
							{
								// get all the  hosts as well as nect hops
								if(forwardingTable.get(i).SendingId.equals("*") && forwardingTable.get(i).MGroup.equals(mGroup))
								{
									getIndex = i ;
									fromRP = "true";
									break;
								}
							}
						}
						System.out.println("Get index value"+getIndex);
						if(getIndex != -1)
						{
							// it has an entry in forwarding table
							if(forwardingTable.get(getIndex).Hosts != null && (forwardingTable.get(getIndex).Hosts).size() > 0)
							{
								for(int i = 0 ; i  <(forwardingTable.get(getIndex).Hosts).size() ; i++)
								{
									hostInput = true;
									Socket sck = null;
									for(int k =0 ; k < ConnectionPort.size();k++)
									{
										if(ConnectionPort.get(k).idFrom.equals((forwardingTable.get(getIndex).Hosts).get(i)))
										sck = ConnectionPort.get(i).sockId;
									}
									//hostConnection  = getConnectedRouterHostname(forwardingTable.get(getIndex).Hosts.get(i));
									//Socket sck = hostConnection.get(forwardingTable.get(getIndex).Hosts.get(i));
									System.out.println("Sending to host:");
									System.out.println(""+sck.getLocalAddress());
									System.out.println("Remote port"+sck.getPort());
									System.out.println("local port"+sck.getLocalPort());
									DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
									outToServer.writeBytes("MCAST " + routerId +" "+ srcId +" "+ mGroup + " "+ data+"\n");
									System.out.println("Sent to host:");

									hostInput = false;
								}
							}
							if(forwardingTable.get(getIndex).Nexthop != null && (forwardingTable.get(getIndex).Nexthop).size() > 0)
							{
								for(int i = 0 ; i  <(forwardingTable.get(getIndex).Nexthop).size() ; i++)
								{
									hostConnection  = getConnectedRouterHostname(forwardingTable.get(getIndex).Nexthop.get(i));
									Socket sck = hostConnection.get(forwardingTable.get(getIndex).Nexthop.get(i));

									DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
									outToServer.writeBytes("MCAST " + routerId +" "+ srcId +" "+ mGroup + " "+ data+"\n");
								}
							}
						}

						System.out.println("End Mcast Table values start");
						for(int i =0;i < forwardingTable.size(); i ++ )
						{
							System.out.println("SendID"+forwardingTable.get(i).SendingId);
							System.out.println("mgroup"+forwardingTable.get(i).MGroup);
							if(forwardingTable.get(i).Nexthop != null && forwardingTable.get(i).Nexthop.size() >0)
							{System.out.println("nexthop"+forwardingTable.get(i).Nexthop.get(0));}
							if(forwardingTable.get(i).Hosts != null && forwardingTable.get(i).Hosts.size() >0)
							{System.out.println("Host"+forwardingTable.get(i).Hosts.get(0));}
						}
						System.out.println(data);
						System.out.println("End mcast Table values end");
					}
					else if(reqRecived.equals("SSJOIN"))
					{
						//ssjoin(myId,srcId,mgroup)
						String myId = remainingString.substring(0,secSpace);
						String remainingString2 = remainingString.substring(secSpace+1);
						int thirdSpace = remainingString2.indexOf(" ");
						String srcId = remainingString2.substring(0,thirdSpace);
						String mGroup = remainingString2.substring(thirdSpace+1);

						// get the rpId from the file
						String rpId = getRpIdfromMGroupId(mGroup);
						if (!(rpId.equals(routerId)))
						{
							if(routerId.equals(srcId))
							{
								RoutingTable rt = new RoutingTable();
								rt.SendingId = srcId;
								rt.MGroup = mGroup;
								rt.Nexthop = null;
								rt.Hosts = new ArrayList<String>();
								// get the data fom register message
								for(int i=0;i<readMGroupHost().size();i++)
								{
									if(readMGroupHost().get(i).get(1).equals(mGroup))
									{
										rt.Hosts .add(readMGroupHost().get(i).get(0));
									}
									System.out.println(""+mgroupsOfRouter.get(i));
								}
								forwardingTable.add(rt);
							}
							else
							{
								// router will never recive ssJoin
								// forward the ssjoin message
								// and create a router table source specific entry
								RoutingTable rt = new RoutingTable();
								rt.SendingId = srcId;
								rt.MGroup = mGroup;
								rt.Nexthop = new ArrayList<String>();
								rt.Nexthop.add(routerId);
								rt.Hosts = null;
								forwardingTable.add(rt);
								String output = "SSJOIN "+ routerId+ " " +srcId+" "+ mGroup+"\n";

								String nexthopRouter = DjisktaAlgo(routerId,srcId);
								hostConnection  = getConnectedRouterHostname(nexthopRouter);
								Socket sck = hostConnection.get(nexthopRouter);
								DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
								outToServer.writeBytes(output);
							}
						}
					}
					else if(reqRecived.equals("REGISTER"))
					{
						//register(srcId, rpId,mgroup,data)
						String srcId = remainingString.substring(0,secSpace);
						String remainingString2 = remainingString.substring(secSpace+1);
						int thirdSpace = remainingString2.indexOf(" ");
						String rpId = remainingString2.substring(0,thirdSpace);
						String remainingString3 = remainingString2.substring(thirdSpace+1);
						String mGroup = remainingString3.substring(0,thirdSpace);
						String data =remainingString3.substring(thirdSpace);
						
						//create a table for host only when the router is rpid... 
						// for every other router the router needs to forward it as it is.
						if (routerId.equals(rpId))
						{
							PrintWriter out = new PrintWriter(new BufferedWriter
									(new FileWriter(sourceString+"mgroupfile+.txt", true)));
							BufferedWriter br = new BufferedWriter(out);
							String outline= hostName+" "+mGroup+"\n";
							br.write(outline);
							br.close();

							// then send SSJoin message
							RoutingTable rt = new RoutingTable();
							rt.SendingId = srcId;
							rt.MGroup = mGroup;
							// todo: HAVE TO SEE THIS ENTRY
							rt.Nexthop = new ArrayList<String>();
							rt.Nexthop.add(routerId);
							rt.Hosts = null;
							forwardingTable.add(rt);
							System.out.println("Table values start");
							for(int i =0;i < forwardingTable.size(); i ++ )
							{
								System.out.println(forwardingTable.get(i).SendingId);
								System.out.println(forwardingTable.get(i).MGroup);
								if(forwardingTable.get(i).Nexthop != null && forwardingTable.get(i).Nexthop.size() >0)
								{System.out.println(forwardingTable.get(i).Nexthop.get(0));}
								if(forwardingTable.get(i).Hosts != null && forwardingTable.get(i).Hosts.size() >0)
								{System.out.println(forwardingTable.get(i).Hosts);}

							}
							System.out.println("Table values end");

							String nexthopRouter =DjisktaAlgo(routerId,  getRpIdfromMGroupId( mGroup));
							hostConnection  = getConnectedRouterHostname(nexthopRouter);
							Socket sck = hostConnection.get(nexthopRouter);

							//sEND TWO THINGS both ssjoin and mcast
							String output = "SSJOIN "+ routerId+ " " +srcId+" "+ mGroup+"\n";
							DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
							outToServer.writeBytes(output);
							//sck.wait(1000);
							sck.close();

							hostConnection  = getConnectedRouterHostname(nexthopRouter);
							Socket sck1 = hostConnection.get(nexthopRouter);
							System.out.println("sending MCAST");
							String output1 = "MCAST " + routerId +" "+ srcId +" "+ mGroup + " "+ data+"\n";
							DataOutputStream outToServer1 = new DataOutputStream(sck1.getOutputStream());
							outToServer1.writeBytes(output1);
							System.out.println("MCAST SENT");
						}
						else
						{
							String nexthopRouter = DjisktaAlgo(routerId,  getRpIdfromMGroupId( mGroup));
							hostConnection  = getConnectedRouterHostname(nexthopRouter);
							Socket sck = hostConnection.get(nexthopRouter);

							String output = "REGISTER " +srcId+" "+ rpId +" "+ mGroup+ " "+ data+"\n";
							DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
							outToServer.writeBytes(output);

						}

						// keep this socket in thread and maintain all ins and out
					}
					else if (reqRecived.equals("LEAVE"))
					{
						//LEAVE <myID> <mgroup> 
						String myId = remainingString.substring(0,secSpace);
						String mgroup = remainingString.substring(secSpace+1);
						
						Connections cns = new Connections();
						cns.HostName = welcomeSocket.getLocalSocketAddress().toString() ;
						cns.PortNumber = ""+welcomeSocket.getLocalPort();
						cns.isFromHost = true ;
						cns.idFrom  = myId;
						cns.sockId = connectionSocket;
						ConnectionPort.add(cns);

						boolean flagContains  = false;
						boolean flag2 = false;
						ArrayList<ArrayList<String>> mgroupHostfile = readMGroupHost();
						for(int i=0 ; i < mgroupHostfile.size() ; i++)
						{
							if((mgroupHostfile.get(i)).get(0).equals(myId) && (mgroupHostfile.get(i)).get(1).equals(mgroup))
							{
								// it contains 
								flagContains = true;
							}
							if(!((mgroupHostfile.get(i)).get(0).equals(myId)) && (mgroupHostfile.get(i)).get(1).equals(mgroup))
							{
								// it is not the only one subscribed
								flag2 = true;
							}
						}
						if(flagContains == true && flag2 == true )
						{

							// remove the entry of host from mgroup table
							FileReader in = new FileReader(sourceString+"mgroup");
							File filename = new File(sourceString+"mgroup");
							File tempFile = new File("myTempFile");
							BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
							BufferedReader reader = new BufferedReader(in);
							String sentence1="";
							while((
									sentence1=reader.readLine()) != null)
							{

								// trim newline when comparing with lineToRemove
								if(sentence1.equals((myId + " " + mgroup) )) continue;
								writer.write(sentence1);
							}
							reader.close();
							writer.close();
							tempFile.delete();
							boolean successful = tempFile.renameTo(filename);
						}
						else if(flagContains == true && flag2 == false )
						{
							// send a prune message
							// append the id of router where message is sent get from config file also get the port number
							// append the request router id while sending
							String rpId = getRpIdfromMGroupId(mgroup);
							String outline = "PRUNE " +myId+" "+ rpId +" "+ mgroup+"\n";
							String nexthopId = DjisktaAlgo(routerId, mgroup);
							HashMap<String, Socket>  connect = getConnectedRouterHostname(nexthopId); 
							// get the route id frm djikstra
							DataOutputStream outToServer = new DataOutputStream(connect.get(routerId).getOutputStream());
							outToServer.writeBytes(outline);
						}
						else
						{
							// no need to do anything because the req is invalid
						}
					}
					else if(reqRecived.equals("SEND"))
					{
						//SEND <myID> <mgroup> <data>
						String myId = remainingString.substring(0,secSpace);
						String remainingString2 = remainingString.substring(secSpace+1);
						int thirdSpace = remainingString2.indexOf(" ");
						String mgroup = remainingString2.substring(0,thirdSpace);
						String data = remainingString2.substring(thirdSpace+1);

						Connections cns = new Connections();
						cns.HostName = welcomeSocket.getLocalSocketAddress().toString() ;
						cns.PortNumber = ""+welcomeSocket.getLocalPort();
						cns.isFromHost = true ;
						cns.idFrom  = myId;
						cns.sockId = connectionSocket;
						ConnectionPort.add(cns);
						
						String rpId = getRpIdfromMGroupId(mgroup);
						System.out.println(""+rpId);
						// router gets send from host
						// get the host specific socket
						for(int i=0;i<mgroupsOfRouter.size();i++)
							System.out.println(""+mgroupsOfRouter.get(i));

						if(mgroupsOfRouter.size() != 0 && mgroupsOfRouter.contains(mgroup))
						{
							System.out.println("mgroupsOfRouter.size() != 0 && mgroupsOfRouter.contains(mgroup)");
							//check the mgroups table if the entry exists else add the entry in mgroups of the router
							boolean flag  = false;
							ArrayList<ArrayList<String>> mgroupHostfile = readMGroupHost();
							for(int i=0 ; i < mgroupHostfile.size() ; i++)
							{
								if((mgroupHostfile.get(i)).get(0).equals(myId) && (mgroupHostfile.get(i)).get(1).equals(mgroup) )
								{
									System.out.println("(mgroupHostfile.get(i)).get(0).equals(myId) && (mgroupHostfile.get(i)).get(1).equals(mgroup)");
									// it contains 
									flag = true;
								}
							}
							if(flag == true)
							{
								System.out.println("flag == true");
								// send the mcast message
								// append the id of router where message is sent get from config file also get the port number
								// append the request router id while sending
								if(routerId.equals(rpId))
								{
									System.out.println("routerId == rpId");
									// read for the forwarding table all the next hop routers
									for (int i=0 ; i<forwardingTable.size();i++)
									{
										System.out.println("MCAST 1"+ forwardingTable.size()); 
										if(forwardingTable.get(i).SendingId.equals("*") && forwardingTable.get(i).MGroup.equals(mgroup)
												)
										{
											System.out.println("FTable entry: "+forwardingTable.get(i).SendingId+forwardingTable.get(i).MGroup+forwardingTable.get(i).Hosts);
											for(int j =0 ; j < forwardingTable.get(i).Hosts.size() ; j++)
											{
												hostConnection  = getConnectedRouterHostname(forwardingTable.get(i).Hosts.get(j));
												Socket sck = hostConnection.get(forwardingTable.get(i).Hosts.get(j));

												DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
												System.out.println("MCAST " + routerId +" "+ routerId +" "+ mgroup + " "+ data+"\n");
												outToServer.writeBytes("MCAST " + routerId +" "+ routerId +" "+ mgroup + " "+ data+"\n");
											}
											break;
										}										
									}
								}
								else
								{
									//forward it to next router
									String nexthopRouter =DjisktaAlgo(routerId,  getRpIdfromMGroupId( mgroup));
									hostConnection  = getConnectedRouterHostname(nexthopRouter);
									Socket sck = hostConnection.get(nexthopRouter);

									DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
									outToServer.writeBytes("MCAST " + routerId +" "+ routerId +" "+ mgroup + " "+ data+"\n");
								}
							}
							else
							{
								//create a table for host
								System.out.println("Flag false");
								PrintWriter out = new PrintWriter(new BufferedWriter
										(new FileWriter(sourceString+"mgroup", true)));
								BufferedWriter br = new BufferedWriter(out);
								String outline= ""+myId+" "+ mgroup+"\n";
								br.write(outline);
								br.close();

								//create a forwarding table
								RoutingTable rt = new RoutingTable();
								rt.SendingId = "*";
								rt.MGroup = mgroup;
								rt.Nexthop = new ArrayList<String>();
								rt.Nexthop.add(myId);
								rt.Hosts = null;
								forwardingTable.add(rt);

								String nexthopRouter = DjisktaAlgo(routerId,  getRpIdfromMGroupId( mgroup));
								hostConnection  = getConnectedRouterHostname(nexthopRouter);
								Socket sck = hostConnection.get(nexthopRouter);
								// will be send to next hop router
								DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
								outToServer.writeBytes("MCAST " + routerId +" "+ routerId +" "+ mgroup + " "+ data+"\n");
							}
						}
						else
						{
							System.out.println("Sending Register");
							// router is not a member of that group also means host is nt thr in the grp too then implement  register
							String nexthopRouter =DjisktaAlgo(routerId,  getRpIdfromMGroupId( mgroup));
							hostConnection  = getConnectedRouterHostname(nexthopRouter);
							Socket sck = hostConnection.get(nexthopRouter);

							String output = "REGISTER " +routerId+" "+ rpId +" "+ mgroup+ " "+data+"\n";
							DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
							outToServer.writeBytes(output);
						}
					}
					else if (reqRecived.equals("REPORT"))
					{
						//REPORT <myID> <mgroup> 
						System.out.print(" \n hostid.." +remainingString.substring(0,secSpace)+"..\n");
						String hostId = remainingString.substring(0,secSpace);
						System.out.print("\n mGroup.." + sentence.substring(secSpace)+"..\n");
						String mGroup = remainingString.substring(secSpace+1);
						System.out.println("Inside report");

						Connections cns = new Connections();
						cns.HostName = welcomeSocket.getLocalSocketAddress().toString() ;
						cns.PortNumber = ""+welcomeSocket.getLocalPort();
						cns.isFromHost = true ;
						cns.idFrom  = hostId;
						cns.sockId = connectionSocket;
						ConnectionPort.add(cns);
						
						// add entry in mgroups file is the host is not in the groupfile of the router
						int counter =0;
						boolean flag = false ; 
						//hostId="0";
						//mGroup="0";
						for(int i=0; i <readMGroupHost().size(); i++)
						{
							System.out.println("1HostId.."+hostId+"MGroup.."+mGroup+"..\n");
							if((readMGroupHost().get(i)).get(0).equals(hostId) && (readMGroupHost().get(i)).get(1).equals(mGroup))
							{
								System.out.println("2HostId.."+hostId+"MGroup.."+mGroup+"..\n");
								// do nothing
								flag = true;
							}
							else if((!(readMGroupHost().get(i)).get(0).equals(hostId)) && (readMGroupHost().get(i)).get(1).equals(mGroup))
							{
								System.out.println("3HostId.."+hostId+"MGroup.."+mGroup+"..\n");
								counter ++; 
								flag = true;
							}
							else if((!(readMGroupHost().get(i)).get(0).equals(hostId)) &&(!(readMGroupHost().get(i)).get(1).equals(mGroup)))
							{
								System.out.println("4HostId.."+hostId+"MGroup.."+mGroup+"..\n");
								flag = true;

								PrintWriter out = new PrintWriter(new BufferedWriter
										(new FileWriter(sourceString+"mgroup", true)));
								BufferedWriter br = new BufferedWriter(out);
								String outline= hostId +" "+mGroup+"\n";
								br.write(outline);
								br.close();

								// perform a join send message
								//get the id of nex hop router so that we can have connection with that router
								System.out.println("Entered REPORT");
								String nexthopRouter = DjisktaAlgo(routerId,  getRpIdfromMGroupId( mGroup));
								hostConnection  = getConnectedRouterHostname(nexthopRouter);
								Socket sck = hostConnection.get(nexthopRouter);
								JoinSend(sck, mGroup);
								break;
							}
						}
						if(counter == readMGroupHost().size() && flag == true)  
						{
							System.out.println("Counter == size and flag true, HostId.."+hostId+"MGroup.."+mGroup+"..");
							// check if id is pesent in the mgroupfile
							//add entry in router local file
							PrintWriter out = new PrintWriter(new BufferedWriter
									(new FileWriter(sourceString+"mgroup", true)));
							BufferedWriter br = new BufferedWriter(out);
							String outline= hostId +" "+mGroup+"\n";
							br.write(outline);
							br.close();
						}
						if(readMGroupHost().size() == 0 || flag == false)
						{
							if(readMGroupHost().size()==0 && flag == false)
							{
								PrintWriter out = new PrintWriter(new BufferedWriter
										(new FileWriter(sourceString+"mgroup", true)));
								BufferedWriter br = new BufferedWriter(out);
								String outline= hostId +" "+mGroup+"\n";
								br.write(outline);
								br.close();
								System.out.println("readMGroupHost().size()==0 && flag == false");

								if(routerId != getRpIdfromMGroupId( mGroup))
								{
									String nexthopRouter =DjisktaAlgo(routerId,  getRpIdfromMGroupId( mGroup));
									System.out.println("Dijkstra: "+nexthopRouter);
									hostConnection  = getConnectedRouterHostname(nexthopRouter);
									Socket sck = hostConnection.get(nexthopRouter);
									JoinSend(sck, mGroup);
								}
							}

							//create a forwarding table
							// get the data fom register message
							boolean flag2 = false; 
							for(int i=0; i < forwardingTable.size(); i ++)
							{
								if(forwardingTable.get(i).SendingId.equals("*") && forwardingTable.get(i).MGroup.equals(mGroup))
								{
									forwardingTable.get(i).Hosts.add(hostId);
									flag2 = true;
								}
							}
							if(flag2 == false)
							{
								System.out.println("RT Entry.."+hostId+".."+mGroup+"..");
								RoutingTable rt = new RoutingTable();
								rt.SendingId = "*";
								rt.MGroup = mGroup;
								rt.Nexthop = null;
								rt.Hosts = new ArrayList<String>();
								rt.Hosts.add(hostId);
								forwardingTable.add(rt);
							}
						}
					}
					else
					{
						System.out.print("Request is not proper" + reqRecived);
					}
				}
				else
				{
					System.out.print("Sentence is null");
				}

			}
		}
		catch(Exception e)
		{

		}
		//welcomeSocket.close();
	}

	/**
	 * Joins the socket
	 * @param clientSocket
	 * @param mgroupId
	 * @throws IOException
	 */
	static void JoinSend(Socket clientSocket,String mgroupId) throws IOException
	{
		// append the id of router where message is sent get from config file also get the port number
		// append the request router id while sending

		String rpId = getRpIdfromMGroupId(mgroupId); 
		System.out.println(" ");
		System.out.println("routerId"+ routerId+"..");
		System.out.println("rpId"+rpId+"..");
		System.out.println("mgroupId"+mgroupId+"..");
		System.out.println("");
		String sentence = "JOIN " +routerId+" "+ rpId +" "+ mgroupId+"\n";
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		outToServer.writeBytes(sentence);

	}

	/**
	 *  get the rp Id from the particular mgroup
	 * @param mGroupId
	 * @return
	 * @throws IOException
	 */
	static String getRpIdfromMGroupId(String mGroupId) throws IOException
	{
		ArrayList<ArrayList<String>> rpfile = readConfigRp();
		String rpId = null;
		// get the rpId from the file

		for(int i =0 ;i < rpfile.size();i++)
		{
			if((rpfile.get(i)).get(0).equals(mGroupId))
			{
				rpId =  (rpfile.get(i)).get(1);
				break;
			}
		}
		return rpId;
	}
	
	/**
	 * Method to check if the input provided by user is Integer or not
	 * @param input
	 * @return flag
	 */
	public static boolean isInteger(String input) 
	{
		//Handling number format exception if the number is not of type integer
		try 
		{
			Integer.parseInt(input);
			return true;
		} 
		//return true/false depending upon the user input
		catch (NumberFormatException e) 
		{ return false; }
	}

	/**
	 * Get connected router HostNames
	 * @param RouterId
	 * @return
	 * @throws IOException
	 */
	public static HashMap<String, Socket>  getConnectedRouterHostname(String RouterId) throws IOException
	{
		HashMap<String, Socket> socktsconnected = new HashMap<String,Socket>();
		// the client sockets should have port number as well as host name of all the connected routers
		ArrayList<ArrayList<String>> configArray =  readConfig();
		for(int i=0 ;i < configArray.size(); i++)
		{ 
			if((configArray.get(i)).get(0).equals(RouterId))
			{
				// means we have reached the correct router
				// get the clients port number and host name
				String hostName = 	(configArray.get(i)).get(1);
				String portNumber  = (configArray.get(i)).get(2);
				//				if(activeClients!=null  && activeClients.containsKey(RouterId))
				if(hostInput == true)
				{
					System.out.println("It is Host Input");
					socktsconnected.put(RouterId, activeClients.get(RouterId));
				}
				else
				{
					System.out.println(" Router connections input");
					activeClients =  new HashMap<String, Socket>();
					Socket clientSocket = new Socket(hostName, Integer.parseInt(portNumber));
					socktsconnected.put(RouterId, clientSocket);
					activeClients.put(RouterId, clientSocket);
				}
			}
		}
		return socktsconnected;
	}

	/**
	 * Get the connected Routers
	 * @param routerId
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<String> getConnectedRouter(String routerId) throws IOException
	{
		ArrayList<String> connectedRouters = new ArrayList<String>();
		ArrayList<String> allconnects = new ArrayList<String>();
		ArrayList<ArrayList<String>> topoArray  = readConfigTopo();

		for(int i=0 ; i< topoArray.size() ; i++)
		{
			if(i == Integer.parseInt(routerId))
			{
				allconnects = topoArray.get(i);
			}
		}

		for(Integer i=0 ; i< allconnects.size() ; i++)
		{
			// means router having direct connection
			if(Integer.parseInt(allconnects.get(i)) == 1)
			{
				connectedRouters.add(i.toString());
			}
		}
		return connectedRouters;
	}

	/**
	 * Read Config File
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<ArrayList<String>> readConfig() throws IOException
	{		
		ArrayList<ArrayList<String>> st = new ArrayList<ArrayList<String>>();
		FileInputStream fstream =null;
		try {fstream = new FileInputStream(sourceString+configfile);}
		catch (FileNotFoundException e)
		{e.printStackTrace();}
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine ;

		strLine = br.readLine();
		while (strLine != null)   
		{
			String ss = null;
			ArrayList<String> string = new ArrayList<String>();
			// get the substring and put in another array
			for (int i =0 ; i <3 ;i ++)
			{
				int spc = 0;
				spc = strLine.indexOf(" ");
				if(spc == -1)
				{
					ss = strLine.substring(0);
					string.add( ss);
				}
				else
				{
					ss = strLine.substring(0,spc);
					string.add( ss);
					strLine = strLine.substring((ss.length()+1));
				}
			}
			st.add(string);
			strLine = br.readLine();
		}
		br.close();
		fstream.close();
		return st;
	}

	/**
	 * Read config Topology File
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<ArrayList<String>> readConfigTopo() throws IOException
	{
		ArrayList<ArrayList<String>> st = new ArrayList<ArrayList<String>>();
		FileInputStream fstream =null;
		try {fstream = new FileInputStream(sourceString+config_topo);}
		catch (FileNotFoundException e)
		{e.printStackTrace();}
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine ;
		int k = 0;

		strLine = br.readLine();
		while (strLine != null)   
		{
			if(k == 0)
			{
				// first line tell about number of routers
				maxRouters = Integer.parseInt(strLine);
				k++;
			}
			else
			{
				String ss = null;
				ArrayList<String> string = new ArrayList<String>();
				// get the substring and put in another array
				for (int i =0 ; i <maxRouters ;i ++)
				{
					int spc = 0;
					spc = strLine.indexOf(" ");
					if(spc == -1)
					{
						ss = strLine.substring(0);
						string.add( ss);
					}
					else
					{
						ss = strLine.substring(0,spc);
						string.add( ss);
						strLine = strLine.substring((ss.length()+1));
					}
				}
				st.add(string);
				strLine = br.readLine();
			}
		}
		br.close();
		fstream.close();
		return st;
	}

	/**
	 * Read Config RP File
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<ArrayList<String>> readConfigRp() throws IOException
	{
		ArrayList<ArrayList<String>> st = new ArrayList<ArrayList<String>>();
		FileInputStream fstream =null;
		try {fstream = new FileInputStream(sourceString+config_rp);}
		catch (FileNotFoundException e)
		{e.printStackTrace();}
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine ;

		strLine = br.readLine();
		while (strLine != null)   
		{
			String ss = null;
			ArrayList<String> string = new ArrayList<String>();
			// get the substring and put in another array
			for (int i =0 ; i <3 ;i ++)
			{
				int spc = 0;
				spc = strLine.indexOf(" ");
				if(spc == -1)
				{
					ss = strLine.substring(0);
					string.add( ss);
				}
				else
				{
					ss = strLine.substring(0,spc);
					string.add( ss);
					strLine = strLine.substring((ss.length()+1));
				}
			}
			st.add(string);
			strLine = br.readLine();
		}
		br.close();
		fstream.close();
		return st;
	}

	/**
	 * Read hosts of router for mgroups
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<ArrayList<String>> readMGroupHost() throws IOException
	{
		ArrayList<ArrayList<String>> st = new ArrayList<ArrayList<String>>();
		FileInputStream fstream =null;
		try {fstream = new FileInputStream(sourceString+"mgroup");}
		catch (FileNotFoundException e)
		{e.printStackTrace();}
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine ;

		strLine = br.readLine();
		while (strLine != null)   
		{
			String ss = null;
			ArrayList<String> string = new ArrayList<String>();
			// get the substring and put in another array
			for (int i =0 ; i <2 ;i ++)
			{
				int spc = 0;
				spc = strLine.indexOf(" ");
				if(spc == -1)
				{
					ss = strLine.substring(0);
					string.add( ss);
				}
				else
				{
					ss = strLine.substring(0,spc);
					string.add( ss);
					strLine = strLine.substring((ss.length()+1));
				}
			}
			st.add(string);
			strLine = br.readLine();
		}
		br.close();
		fstream.close();
		return st;
	}

	/**
	 * Get command line Arguments
	 * @param args
	 */
	public static void GetCommandLineArgs(String[] args)
	{
		//Get the router Id from the command line
		try 
		{
			//Check if the router Id given by the user is of proper format or not, else again ask user input.
			if(isInteger(args[0]))
			{ 
				routerId = args[0];
			}
			else
			{ 
				System.out.println("Need valid argument: Router Id");
				System.exit(-1);
			}
		} 
		catch (ArrayIndexOutOfBoundsException e) 
		{
			System.out.println("Provide Router Id");
			System.exit(-1);
		}
		try 
		{
			configfile = args[1];
		} 
		catch (ArrayIndexOutOfBoundsException e) 
		{
			configfile = sourceString+"Config";
		}
		try 
		{
			config_rp = args[2];
		} 
		catch (ArrayIndexOutOfBoundsException e) 
		{
			config_rp = sourceString+"config_rp";
		}
		try 
		{
			config_topo = args[3];
		} 
		catch (ArrayIndexOutOfBoundsException e) 
		{
			config_topo = sourceString+"config_topo";
		}
	}

	/**
	 * Read Dijsktra Algorithm
	 * @param src
	 * @param dest
	 * @return
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public static String DjisktaAlgo(String src, String dest) throws NumberFormatException, IOException
	{
		Dijkstra dijkstra = new Dijkstra();
		System.out.println(src+" src dest"+dest +"router id"+routerId);
		Hashtable<String, String> nexthops = dijkstra.dijkstralgo(Integer.parseInt(routerId));

		String key = src+""+dest+"";
		System.out.println("key"+key);
		String nexthopRouter = nexthops.get(key);
		return nexthopRouter;
	}

	/**
	 * Get multicast group of RP router
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<String> GetMulticastGrouspOfRpRouter() throws IOException
	{
		ArrayList<String> mgroupsOfRouter = new ArrayList<String>();
		ArrayList<ArrayList<String>> rpArray  = readConfigRp();

		for(int i= 0; i < rpArray.size() ;i++)
		{
			if((rpArray.get(i)).get(1).equals(routerId))
			{
				mgroupsOfRouter.add((rpArray.get(i)).get(0));
			}
		}
		return mgroupsOfRouter;
	}
}

class RoutingTable
{
	public String SendingId;
	public String MGroup;
	public ArrayList<String> Nexthop;
	public ArrayList<String> Hosts;
	public RoutingTable()
	{
	}
}

class Connections
{
	public String PortNumber;
	public String HostName;
	public boolean isFromHost;
	public String idFrom;
	public Socket sockId; 
}


