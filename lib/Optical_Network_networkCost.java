/*******************************************************************************
 * Copyright (c) 2013-2014 Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza - initial API and implementation
 ******************************************************************************/

//import cern.colt.matrix.tdouble.DoubleFactory1D;
//import cern.colt.matrix.tdouble.DoubleMatrix1D;

//import cern.colt.matrix.tdouble.DoubleFactory1D;
//import cern.colt.matrix.tdouble.DoubleMatrix1D;

import com.net2plan.interfaces.networkDesign.IReport;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.Node;
//import com.net2plan.libraries.GraphTheoryMetrics;
//import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.*;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Adolfo Oliveira, Vasco Braz
 * @version 2.0, May 2016
 */
public class Optical_Network_networkCost implements IReport
{

	private NetworkLayer lowerLayer, upperLayer;

	@Override


	public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
	{
		lowerLayer=netPlan.getNetworkLayer(0);
		upperLayer=netPlan.getNetworkLayer(1);

		/* Initialize some variables */
		int N = netPlan.getNumberOfNodes();
		int E = netPlan.getNumberOfLinks(lowerLayer);
		int D = netPlan.getNumberOfDemands(lowerLayer);

	

		ArrayList<Long> nodeIds = netPlan.getNodeIds();
		ArrayList<Long> linkIds = netPlan.getLinkIds(lowerLayer);

		int[] idLinks = new int[E];
		int index=0;
		

		double portsTotal = 0;

		String type = upperLayer.getName();	
		String transparent = "Logical Topology Transparent";
		String opaque = "Logical Topology Opaque";

		int portsTotalIn[] = new int[N];
		int portsTotalOut[] = new int[N];
		double portsTribIn[] = new double[N];
		double portsTribOut[] = new double[N];
		int portsLineIn[] = new int[N];
		int portsLineOut[] = new int[N];


		double numberAmplifiers = 0; 
		double opticalChannels[] = new double[E];

		String html;
		try { html = HTMLUtils.getHTMLFromURL(getClass().getResource("/main.html").toURI().toURL()); }
		catch(URISyntaxException | MalformedURLException e) { throw new RuntimeException(e); }

		// Per-link information
		StringBuilder linkInformationTable = new StringBuilder();	
		if (E > 0)
		{
			linkInformationTable.append("<table border='1'>"+"<tr><th><b> Node Pair </b></th> <th><b> Wavelengths forward </b></th> <th><b> Wavelengths backward </b></th>"
					+ "<th><b> Amplifiers forward </b></th> <th><b> Amplifiers backward </b></th> </tr>");
			for (long linkId : linkIds)
			{
				
				Link l=	netPlan.getLinkFromId(linkId);	
				Link inverse;
				int id = (int) linkId;
				String originNode = l.getOriginNode().getName();
				String destinationNode = l.getDestinationNode().getName();
				
				String originNode1;
				String destinationNode1;
			
				
				double length = l.getLengthInKm();
				if(l.getAttribute("nW") == null)
					{
						opticalChannels[netPlan.getLinkFromId(linkId).getIndex()] = 0;
					}
				else
				{
					opticalChannels[netPlan.getLinkFromId(linkId).getIndex()] = Double.parseDouble(l.getAttribute("nW"));

				}
				
				for (long linkId1 : linkIds)
				{
					
					Link l1=netPlan.getLinkFromId(linkId1);
					originNode1 = l1.getOriginNode().getName();
					destinationNode1 = l1.getDestinationNode().getName();
								
						
					if(originNode1==destinationNode && destinationNode1==originNode)
					{
						
						int amplifiersF=0;
						int amplifiersB=0;
						double nopticalChannels= 0;
						if(l1.getAttribute("nW")==null) nopticalChannels= 0;
						else nopticalChannels = Double.parseDouble(l1.getAttribute("nW"));

						if(nopticalChannels != 0)
						{
							amplifiersF = (int) (Math.ceil(length/Double.parseDouble(reportParameters.get("span")))-1);
							amplifiersB = (int) (Math.ceil(l1.getLengthInKm()/Double.parseDouble(reportParameters.get("span")))-1);
						}
						linkInformationTable.append(String.format("<tr><td>%s -» %s</td>  <td>%s</td> <td>%s</td> <td>%s</td> <td>%s</td> </tr>", 
								originNode, destinationNode,(int)opticalChannels[netPlan.getLinkFromId(linkId).getIndex()], (int)nopticalChannels, amplifiersF, amplifiersB));
					}
				}
				int nAmplifiers = 0;
				if(opticalChannels[netPlan.getLinkFromId(linkId).getIndex()] == 0) nAmplifiers=0;
				else nAmplifiers = (int) (Math.ceil(length/Double.parseDouble(reportParameters.get("span")))-1);

				numberAmplifiers += nAmplifiers;
			}
			linkInformationTable.append("</table>");
		}
		html = html.replaceFirst("#linkInformationTable#", linkInformationTable.toString());

		// Per-Node information
		StringBuilder nodeInformationTable = new StringBuilder();
		if (N > 0)
		{
			nodeInformationTable.append("<table border='1'>");
			nodeInformationTable.append("<tr> <th><b>Name</b></th> <th><b>Trib ports in</b></th> <th><b>Trib ports out</b></th>"
					+"<th><b>Line Ports in</b></th> <th><b>Line Ports out</b></th> <th><b>Total Ports in</b></th> <th><b>Total Ports out</b></th></tr>");

			List<Pair<Long,Long>> nodes = new ArrayList<Pair<Long,Long>> ();
			double trafficR[] = new double[netPlan.getNumberOfRoutes()];
			long link[] = new long[netPlan.getNumberOfRoutes()];
			long link1[] = new long[netPlan.getNumberOfRoutes()];
			int i=0;
			for(long routeId:netPlan.getRouteIds(lowerLayer))
			{
				boolean added = false;
				Route r = netPlan.getRouteFromId(routeId);
				Node in = r.getIngressNode();
				Node out = r.getEgressNode();

				Pair<Long, Long> nodePair = new Pair<Long,Long>(in.getId(), out.getId(), false);


				for(int j=0 ; j<i ; j++)
				{
					if(nodes.get(j).equals(nodePair))
					{
						trafficR[j] = trafficR[j]+r.getCarriedTraffic();
						added = true;
						break;
					}
				}
				if(added == true) continue;
				List<Link> Path = r.getSeqLinksRealPath();
				link[i] = Path.get(0).getId();
				link1[i] = Path.get(Path.size()-1).getId();
				nodes.add(i,nodePair);
				trafficR[i] = r.getCarriedTraffic();
				i++;
			} 	    

			for(long nodeId : nodeIds)
			{
				
				Node n = netPlan.getNodeFromId(nodeId);
				String name = n.getName();
				Set<Link>nodeLinksIncoming = n.getIncomingLinks(lowerLayer);
				Set<Link>nodeLinksOutgoing = n.getOutgoingLinks(lowerLayer);
				double portsLink[] = new double[nodeLinksOutgoing.size()];
				double portsLink1[] = new double[nodeLinksIncoming.size()];
				for(int j=0;j<nodes.size();j++)
				{
					Pair<Long,Long> nodePair = nodes.get(j);
					if(nodePair.getFirst()==(int)nodeId)
					{
						if(type.equals(transparent)) portsTribIn[netPlan.getNodeFromId(nodeId).getIndex()] += (int) Math.ceil(trafficR[j]/80);
						if(type.equals(opaque))
						{
							int a=0;
							for(Link nodeLink:nodeLinksOutgoing)
							{
								if(link[j]==nodeLink.getId())
								{
									portsLink[a] += trafficR[j]/80;
								}
								a++;
							}
						}
					}

					if(nodePair.getSecond()==(int)nodeId)
					{
						if(type.equals(transparent)) portsTribOut[netPlan.getNodeFromId(nodeId).getIndex()] += (int) Math.ceil(trafficR[j]/80);
						if(type.equals(opaque))
						{
							int a=0;
							for(Link nodeLink:nodeLinksIncoming)
							{
								if(link1[j]==nodeLink.getId())
								{
									portsLink1[a] += trafficR[j]/80;
								}
								a++;
							}
						}
					}	    		
				}
				if(type.equals(opaque))
				{
					for(int b=0;b<portsLink.length;b++)
					{
						portsLink[b] = (int)Math.ceil(portsLink[b]);
					}
					portsTribIn[netPlan.getNodeFromId(nodeId).getIndex()] = DoubleUtils.sum(portsLink);	
					for(int b=0;b<portsLink1.length;b++)
					{
						portsLink1[b] = (int)Math.ceil(portsLink1[b]);
					}
					portsTribOut[netPlan.getNodeFromId(nodeId).getIndex()] = DoubleUtils.sum(portsLink1);   			  			
				}
				portsTribIn[netPlan.getNodeFromId(nodeId).getIndex()] = (int) Math.ceil(portsTribIn[netPlan.getNodeFromId(nodeId).getIndex()]);
				portsTribOut[netPlan.getNodeFromId(nodeId).getIndex()] = (int) Math.ceil(portsTribOut[netPlan.getNodeFromId(nodeId).getIndex()]);

				for(Link linkId : nodeLinksIncoming)
				{
					int transponder = 0;
					String value = linkId.getAttribute("nW");
					if(netPlan.getAttribute("nW")==null) transponder = 0;
					else transponder = Integer.parseInt(value);
					portsLineIn[netPlan.getNodeFromId(nodeId).getIndex()] = portsLineIn[netPlan.getNodeFromId(nodeId).getIndex()]+transponder;;
					
					//System.out.println(portsLineIn[netPlan.getNodeFromId(nodeId).getIndex()]);
				}
					
				for(Link linkId : nodeLinksOutgoing)
				{
					int transponder = 0;
					String value = linkId.getAttribute("nW");
					if(linkId.getAttribute("nW")==null) transponder = 0;
					else transponder = Integer.parseInt(value);
					portsLineOut[netPlan.getNodeFromId(nodeId).getIndex()] = portsLineOut[netPlan.getNodeFromId(nodeId).getIndex()]+transponder;
					
				}	  
				portsLineIn=portsLineOut;
				portsTotalOut[netPlan.getNodeFromId(nodeId).getIndex()] = (int)portsTribOut[netPlan.getNodeFromId(nodeId).getIndex()]+portsLineOut[netPlan.getNodeFromId(nodeId).getIndex()];
				portsTotalIn[netPlan.getNodeFromId(nodeId).getIndex()] = (int)portsTribIn[netPlan.getNodeFromId(nodeId).getIndex()]+portsLineIn[netPlan.getNodeFromId(nodeId).getIndex()];
				nodeInformationTable.append(String.format("<tr> <td>%s</td> <td>%s</td> <td>%s</td> <td>%s</td> <td>%s</td> <td>%s</td> <td>%s</td> </tr>",name,
						(int)portsTribIn[netPlan.getNodeFromId(nodeId).getIndex()], (int)portsTribOut[netPlan.getNodeFromId(nodeId).getIndex()],portsLineIn[netPlan.getNodeFromId(nodeId).getIndex()],portsLineOut[netPlan.getNodeFromId(nodeId).getIndex()],portsTotalIn[netPlan.getNodeFromId(nodeId).getIndex()],
						portsTotalOut[netPlan.getNodeFromId(nodeId).getIndex()]));
			} 
			nodeInformationTable.append(String.format("<tr> <td>%s</td> <td>%s</td> <td>%s</td> <td>%s</td> <td>%s</td> <td>%s</td> <td>%s</td> </tr>","Total",
					(int)DoubleUtils.sum(portsTribIn), (int)DoubleUtils.sum(portsTribOut),(int)IntUtils.sum(portsLineIn),(int)IntUtils.sum(portsLineOut),
					(int)IntUtils.sum(portsTotalIn), (int)IntUtils.sum(portsTotalOut)));
			
			nodeInformationTable.append("</table>");	
		}
		portsTotal= Math.max(IntUtils.sum(portsTotalIn), IntUtils.sum(portsTotalOut));
		html = html.replaceFirst("#nodeInformationTable#", nodeInformationTable.toString()); 		

		// Network Cost
		StringBuilder costInformationTable = new StringBuilder();
		int nlink = 0;
		double capacity=0;
		double temp;
		boolean active = false;
		Link link=null;



		while(active == false && capacity==0)
		{
			link= netPlan.getLink(nlink, lowerLayer);
			temp = link.getCarriedTrafficIncludingProtectionSegments();
			if(temp>=0)
			{	
				active=true; 
			}
			else {
				active=false;
			}

			if(active==true)capacity=link.getCapacity();
			nlink++;
		}
		
		int nOpticalChannels = 0;
		for(long linkId:linkIds)
		{
			Link l2= netPlan.getLinkFromId(linkId);
			String nw = l2.getAttribute("nW");
			if(nw==null) nOpticalChannels = nOpticalChannels + 0;
			else nOpticalChannels = nOpticalChannels + Integer.parseInt(nw);
		}
		double Padd = DoubleUtils.sum(portsTribIn);
		double Tau = link.getCapacity()/Integer.parseInt(link.getAttribute("nW"))*1.25;
		double Cexc = (Double.parseDouble(reportParameters.get("EXC"))*N)+(Padd*2*Tau*Double.parseDouble(reportParameters.get("EXCPort")));
		double OLTCost = Double.parseDouble(reportParameters.get("OLT"))*E;
		double amplifiersCost = numberAmplifiers* Double.parseDouble(reportParameters.get("opticalAmplifier"));
		double transponderCost = Double.parseDouble(reportParameters.get("Transponder"))*Tau*nOpticalChannels;
		double Cl = OLTCost + amplifiersCost + transponderCost;
		double Coxc = (Double.parseDouble(reportParameters.get("OXC"))*N+(Double.parseDouble(reportParameters.get("OXCPort"))*portsTotal));
		if(type.equals(opaque))
		{
			Coxc = 0;
			Cexc = (Double.parseDouble(reportParameters.get("EXC"))*N)+(portsTotal*Tau*Double.parseDouble(reportParameters.get("EXCPort")));
		}
		if(type.equals(transparent))
		{
			Coxc = (Double.parseDouble(reportParameters.get("OXC"))*N+(Double.parseDouble(reportParameters.get("OXCPort"))*portsTotal));
			Cexc = (Double.parseDouble(reportParameters.get("EXC"))*N)+(Padd*2*Tau*Double.parseDouble(reportParameters.get("EXCPort")));
		}	


		costInformationTable.append("<table border='1'>");
		costInformationTable.append("<tr><th colspan=2><b>Category</b></th><th><b>Cost</b></th><th><b>Total</b></th></tr>");
		costInformationTable.append(String.format("<tr> <td rowspan=3><b>Link Cost</b></td> <td><b>OLT</b></td> <td>%s</td> <td rowspan=3>%s</td>",
				NumberFormat.getIntegerInstance().format(OLTCost),NumberFormat.getIntegerInstance().format(Cl)));
		costInformationTable.append(String.format("<tr> <td><b>Transponders</b></td> <td>%s</td>",NumberFormat.getIntegerInstance().format(transponderCost)));
		costInformationTable.append(String.format("<tr> <td><b>Amplifiers</b></td> <td>%s</td>",NumberFormat.getIntegerInstance().format(amplifiersCost)));
		costInformationTable.append(String.format("<tr> <td rowspan=2><b>Node Cost</b></td> <td><b>Electrical</b></td> <td>%s</td> <td rowspan=2>%s</td>",
				NumberFormat.getIntegerInstance().format(Cexc),NumberFormat.getIntegerInstance().format(Cexc+Coxc)));
		costInformationTable.append(String.format("<tr> <td><b>Optical</b></td> <td>%s</td>",NumberFormat.getIntegerInstance().format(Coxc)));
		costInformationTable.append(String.format("<tr> <th colspan=3><b>Total Network Cost</b></th> <td>%s</td>",NumberFormat.getIntegerInstance().format(Cl+Coxc+Cexc)));
		costInformationTable.append("</table>");

		html = html.replaceFirst("#costInformationTable#", costInformationTable.toString());

		return html;
	}

	@Override
	public String getDescription() { return "This report displays the number of optical channels, ports and calculates the network cost"; }

	@Override
	public String getTitle() { return "Network design report"; }

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		List<Triple<String, String, String>> reportParameters = new ArrayList<Triple<String, String, String>>();
		reportParameters.add(Triple.of("OLT", "15000", "OLT cost in euros"));
		reportParameters.add(Triple.of("Transponder", "5000", "Transponder cost in euros"));
		reportParameters.add(Triple.of("opticalAmplifier", "4000", "Optical amplifier cost in euros"));
		reportParameters.add(Triple.of("EXC", "10000", "EXC cost in euros"));
		reportParameters.add(Triple.of("OXC", "20000", "OXC cost in euros"));
		reportParameters.add(Triple.of("EXCPort", "1000", "EXC port cost in euros per GB/s"));
		reportParameters.add(Triple.of("OXCPort", "2500", "OXC port cost in euros"));
		reportParameters.add(Triple.of("span", "100", "Separation between regeneration stages in km"));
		return reportParameters;
	}
}