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


import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.ProtectionSegment;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.IntUtils;
import com.net2plan.utils.Triple;
import com.net2plan.interfaces.networkDesign.Link;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;


import java.util.List;
import java.util.Map;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;

import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;

/**
 * @author Vasco Braz, Adolfo Oliveira
 * @version 2.0, May 2015
 */
public class Grooming_dedicated implements IAlgorithm
{
	
	
	 private NetworkLayer lowerLayer, upperLayer;
    @Override
   
    	
    	
    	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    	{
    	   		
    		lowerLayer=netPlan.getNetworkLayer(0);
    		upperLayer=netPlan.getNetworkLayer(1);
    		
    		/* Initialize some variables */
    		int N = netPlan.getNumberOfNodes();
    		int E = netPlan.getNumberOfLinks(lowerLayer);
    		int D = netPlan.getNumberOfDemands(lowerLayer);
    		
    				
    		netPlan.removeAllUnicastRoutingInformation();
    		netPlan.setRoutingType(RoutingType.SOURCE_ROUTING,upperLayer);
    		netPlan.setRoutingType(RoutingType.SOURCE_ROUTING,lowerLayer);
    		
    		
    		if (N == 0 || E == 0 || D == 0) throw new Net2PlanException("This algorithm requires a topology and a demand set");

    		String shortestPathType = algorithmParameters.get("shortestPathType");
    		String numberOfRoutes = algorithmParameters.get("numberofroutes");
    		
    		if (!shortestPathType.equalsIgnoreCase("hops") && !shortestPathType.equalsIgnoreCase("km")) 
    		{
    			throw new Net2PlanException("'shortestPathType' must be 'hops' or 'km'");
    		}
    		
    		
    		netPlan.removeAllRoutes(lowerLayer);
    		netPlan.removeAllProtectionSegments(lowerLayer);
    		netPlan.removeAllRoutes(upperLayer);
    		netPlan.removeAllProtectionSegments(upperLayer);
    		
    	    		

    		ArrayList<Long> linkIds = netPlan.getLinkIds(lowerLayer);
    		final DoubleMatrix1D linkCostVector = shortestPathType.equalsIgnoreCase("hops")? DoubleFactory1D.dense.make (E , 1.0) : netPlan.getVectorLinkLengthInKm();
    		ArrayList<Long> demandIds = netPlan.getDemandIds(lowerLayer);
    		long wavelengthCapacity = Long.parseLong(algorithmParameters.get("wavelengthCapacity"));
    		Route save=null;
    		String type = upperLayer.getName();	
    		
    		if(type != "Logical Topology Opaque" && type != "Logical Topology Transparent" && type != "Logical Topology Translucent")
    		throw new Net2PlanException ("Logical Topology Algorithm Required");	
    		
    		netPlan.addRoutesFromCandidatePathList(linkCostVector.toArray(),"K", numberOfRoutes); 
    		System.out.println(netPlan.getNumberOfRoutes());
    		
    		for(long links:linkIds)
			{
				netPlan.getLinkFromId(links).removeAllAttributes();
				netPlan.getLinkFromId(links).setCapacity(0);
			}
    		
    		
    		switch(type)
    		{
    			//region Opaque
    		
    			case "Logical Topology Opaque":   for (Demand d : netPlan.getDemands(lowerLayer))
    							{
    								
    								double demandTraffic = d.getOfferedTraffic();
    							    Node a = d.getIngressNode();
    								Node b = d.getEgressNode();
    								boolean odd=true;
    								int counter=0;
    								
    								Set<Route> droutes = d.getRoutes();
    								System.out.println(droutes.size());

    								for(Route c: droutes)
    									
    								{
    								counter++;	
    								boolean jump=false;
    								
    									if(odd)
    									{
    										c.setCarriedTraffic(d.getOfferedTraffic(), d.getOfferedTraffic());
    										save=c;
    										odd=false;
    										System.out.println("Roots");
    									}else
    									{
    										List<Link>  workingpath = save.getSeqLinksRealPath();
    										System.out.println("Protection");
    										
    										for(Link t:workingpath)
    										{
    											if(c.getSeqLinksRealPath().contains(t))	
    											{
    												jump=true;
    												break;
    											}
    										}

    										if(jump==false)
    											{
    												ProtectionSegment segment=netPlan.addProtectionSegment(c.getSeqLinksRealPath() , d.getOfferedTraffic() , null);
    												save.addProtectionSegment(segment);
    												odd=true;
    												break;
    											}

    										if(jump==true && counter == droutes.size())
    											{
    												ProtectionSegment segment=netPlan.addProtectionSegment(c.getSeqLinksRealPath() , d.getOfferedTraffic() , null);
    												save.addProtectionSegment(segment);
    												odd=true;
    												throw new Net2PlanException ("Number of routes is not enough");
    											}

    									}
    							
    								}

    									
    								//primaryPaths.add(primaryPath);
    								//if (c.getNumberOfHops() == 0) throw new Net2PlanException ("The network is not connected");
    								
    							}
    							netPlan.removeAllRoutesUnused(1);
    							
    							Link p;
    							for(long e:linkIds)
    							{
    								p=netPlan.getLinkFromId(e);
    								double sumTraffic = p.getCarriedTrafficNotIncludingProtectionSegments()+p.getReservedCapacityForProtection();
    								int nw = (int) Math.ceil(sumTraffic/wavelengthCapacity);
    								String numberWavelengths = String.valueOf(nw);
    								p.setCapacity(nw*wavelengthCapacity);
    								p.setAttribute("nW", numberWavelengths);
    								
    							}
    							break;
    						
    			case "Logical Topology Transparent":   for (Demand d : netPlan.getDemands(lowerLayer))
				{
					
					double demandTraffic = d.getOfferedTraffic();
				    Node a = d.getIngressNode();
					Node b = d.getEgressNode();
					boolean odd=true;
					int counter=0;
					
					
					
					//Pair<Set<Route>,Double> primaryPaths = d.computeShortestPathRoutes(linkCostVector.toArray());
					//System.out.println(primaryPaths.getFirst().size());
					
					Set<Route> droutes = d.getRoutes();
					System.out.println(droutes.size());
					
					
					for(Route c: droutes)
						
					{
					
					counter++;	
					boolean jump=false;
					
						if(odd)
						{
							c.setCarriedTraffic(d.getOfferedTraffic(), d.getOfferedTraffic());
							//netPlan.addRoute(d , d.getOfferedTraffic() , d.getOfferedTraffic() , c.getSeqLinksRealPath(), null); 
							save=c;
							odd=false;
							System.out.println("Roots");
						}else
						{
							List<Link>  workingpath = save.getSeqLinksRealPath();
							System.out.println("Protection");
							for(Link t:workingpath)
							{
							
							if(c.getSeqLinksRealPath().contains(t))	
							{
								jump=true;
								break;
							}
							
							}
							
						
							
							if(jump==false)
							{
							ProtectionSegment segment=netPlan.addProtectionSegment(c.getSeqLinksRealPath() , d.getOfferedTraffic() , null);
							save.addProtectionSegment(segment);
							odd=true;
							break;
							}

							if(jump==true && counter == droutes.size())
							{
							ProtectionSegment segment=netPlan.addProtectionSegment(c.getSeqLinksRealPath() , d.getOfferedTraffic() , null);
							save.addProtectionSegment(segment);
							odd=true;
							throw new Net2PlanException ("Number of routes is not enough");
							}

						}
				
					}

						
					//primaryPaths.add(primaryPath);
					//if (c.getNumberOfHops() == 0) throw new Net2PlanException ("The network is not connected");
					
				}
				netPlan.removeAllRoutesUnused(1);
				
				
				ArrayList<Long> tNodeIds = netPlan.getNodeIds(); 
				Node in;
				Node out;
				Set<Demand> groomDemand;
				Set<Route> groomRoute;
				Set<ProtectionSegment> protectRoutes;
				Route compare=null;
				ProtectionSegment compare1=null;
				List<Link> path;
				int nW=0;
				
				
				
				
				for (long tNodeId : tNodeIds)
				{
					
					in = netPlan.getNodeFromId(tNodeId);
					
					for (long tNodeId1 : tNodeIds)
					{
						
						if(tNodeId==tNodeId1)continue;
						
						out = netPlan.getNodeFromId(tNodeId1);
						double totaltraffic=0;
						
						groomDemand=netPlan.getNodePairDemands(in,out,false,lowerLayer);
						groomRoute=netPlan.getNodePairRoutes(in,out,false,lowerLayer);
						protectRoutes=netPlan.getNodePairProtectionSegments(in,out,false,lowerLayer);
						
						
				
						
						for(Route d:groomRoute)
						{
							totaltraffic = totaltraffic + d.getCarriedTraffic();
					    	compare=d;
					        					     			    	
						}
						
							path=compare.getSeqLinksRealPath();
							
							
							
							for (Link link:path)
							{
								String nw = link.getAttribute("nW");
								String n_W = null;
								nW=0;
								
								
								if(nw!=null)
								{
									
									
									nW=Integer.parseInt(nw);
									nW =(int) (nW+Math.ceil(totaltraffic/wavelengthCapacity));
									link.setAttribute("nW",String.valueOf(nW));
									
									
								}else
								{
									nW=(int)Math.ceil(totaltraffic/wavelengthCapacity);
									link.setAttribute("nW",String.valueOf(nW));
									
								}
								
								
							}
							
							
							
							//Protection Segments
							totaltraffic=0;
							
							for(ProtectionSegment protect:protectRoutes)
							{
								
						    	totaltraffic = totaltraffic + protect.getReservedCapacityForProtection();
						    	compare1 = protect;
						        
						     			    	
							}
							
								path=compare1.getSeqLinks();
								
								for (Link link:path)
								{
									String nw = link.getAttribute("nW");
									
									if(nw!=null)
									{
										
										nW=Integer.parseInt(nw);
										nW =(int) (nW+Math.ceil(totaltraffic/wavelengthCapacity));
										link.setAttribute("nW",String.valueOf(nW));
									}else
									{
										nW=(int)Math.ceil(totaltraffic/wavelengthCapacity);
										link.setAttribute("nW",String.valueOf(nW));
									}
									
									
									
									
								}
						
											
						
						
					}
					
	
				}
    		
				
				for(long e:linkIds)
				{
					p=netPlan.getLinkFromId(e);
					String n_w = p.getAttribute("nW");
					if(n_w==null) p.setCapacity(0);
					else
					{
						int numberWavelengths = Integer.parseInt(n_w);
						p.setCapacity(numberWavelengths*wavelengthCapacity);
					}
				
				}
				
				
					break;
    							
    		}
    	
    		
        return "Ok!";
    	}
    
    public List<Triple<String, String, String>> getParameters()
	{
		List<Triple<String, String, String>> parameters = new ArrayList<Triple<String, String, String>>();
		parameters.add(Triple.of("shortestPathType", "hops", "Each demand is routed according to the shortest path according to this type. Can be 'km' or 'hops'"));
		parameters.add(Triple.of("wavelengthCapacity", "80", "ODU0 Capacity per Wavelength"));
		parameters.add(Triple.of("numberofroutes", "10", "total number of routes per demand"));
		return parameters;
	}

	@Override
	public String getDescription()
	{
		return "Algorithm that creates routes and protection paths based on the shortestPath (hops or km) making sure they are bidirectional and according to the logical topology. Link capacity is based on the number of wavelengths necessary with a user defined wavelength traffic capacity.";
	}

}