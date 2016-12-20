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

import java.util.ArrayList;
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

/**
 * @author Adolfo Oliveira
 * @version 1.0, June 2015
 */
public class Logical_Topology_Algorithm implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
    	
        final int N = netPlan.getNumberOfNodes();
        if (N == 0 ) throw new Net2PlanException("This algorithm requires a topology with nodes");

        int nodes[] = new int[N];
        
        //Validate arguments
        netPlan.setLayerName(0, "Physical Topology");
        String logicalTopology = algorithmParameters.get("logicalTopology");
		if (!logicalTopology.equalsIgnoreCase("Opaque") && !logicalTopology.equalsIgnoreCase("Transparent") 
			&& !logicalTopology.equalsIgnoreCase("Translucent"))
		{
			throw new Net2PlanException("'logicalTopology' must be 'Opaque' , 'Transparent' or 'Translucent'");
		}
		
        Set<Long> nodeIds = netPlan.getNodeIds();
		Map<Long, Double> linkCosts = netPlan.getLinkLengthInKmMap(0);
		Map<Long, Pair<Long, Long>> linkTable = netPlan.getLinkMap(0); 
		
		Set<Long> demandIds = netPlan.getDemandIds(0);
		
		//Check if there is no second layer
		//Opaque physical layer = logical layer
		//Add new layer logical topology transparent
        boolean active = netPlan.isLayerActive(1);
        if(active!=true)
        {
        
        if (logicalTopology == "Opaque") 
        	{
        		netPlan.copyLayer(0);
        	}      
        if(logicalTopology != "Opaque") 
        	{
        		netPlan.addLayer(null, null, null, null, null); 
        	}
        }

        if(active==true)
        {
        	netPlan.removeAllLinks(1);
        	netPlan.removeAllDemands(1);
        }
    		
        int index = 0;
		for (long nodeId: nodeIds)
		{
			 nodes[index] = (int)nodeId;
			 index++;
		}

        switch(logicalTopology)
        {    					
        
        
        						
        	case "Opaque" :		 Set<Long> linkIds = netPlan.getLinkIds(0); 
    							 for(long linkId : linkIds)
    							 {
    								 double length = netPlan.getLinkLengthInKm(0,linkId);
    								 long destinationNode = netPlan.getLinkDestinationNode(0, linkId);
    								 long originNode = netPlan.getLinkOriginNode(0, linkId);
    								 netPlan.addLink(1,originNode, destinationNode, 0, length, null);
    							 }
    				        	netPlan.setLayerName(1, "Logical Topology " + logicalTopology);
    				        	netPlan.setLayerAttribute(1, "type", logicalTopology);
    				        	for(long demandId:demandIds)
    				        	{
    				        		long ingressNode = netPlan.getDemandIngressNode(0, demandId);
    				        		long egressNode = netPlan.getDemandEgressNode(0, demandId);
    				        		double offeredTraffic = netPlan.getDemandOfferedTraffic(0, demandId);
    				        		String attribute = netPlan.getDemandAttribute(0, demandId, "ODU");
    				        		long demandId1 = netPlan.addDemand(1,ingressNode,egressNode,offeredTraffic,null);
    				        		netPlan.setDemandAttribute(1, demandId1, "ODU", attribute);
    				        	}
        		break;
        	case "Transparent" : for(int i=0;i<N;i++)
        						 {
        							 for(int j=0;j<N;j++)
        							 {
        								 if(j!=i)
        								 {
            								 double length=0;
            								 List<Long> primaryPath = GraphUtils.getShortestPath(linkTable, i , j , linkCosts);
        									 for(int a=0;a<primaryPath.size();a++)
        									 {
        									 	length = length+netPlan.getLinkLengthInKm(0,primaryPath.get(a)); 
        									 }
        									 netPlan.addLink(1,nodes[i], nodes[j], 0, length, null);
        								 }
        							 }   			
        						 }
        			        	netPlan.setLayerName(1,"Logical Topology " + logicalTopology); 
    				        	netPlan.setLayerAttribute(1, "type", logicalTopology);
    				        	for(long demandId:demandIds)
    				        	{
    				        		long ingressNode = netPlan.getDemandIngressNode(0, demandId);
    				        		long egressNode = netPlan.getDemandEgressNode(0, demandId);
    				        		double offeredTraffic = netPlan.getDemandOfferedTraffic(0, demandId);
    				        		String attribute = netPlan.getDemandAttribute(0, demandId, "ODU");
    				        		long demandId1 = netPlan.addDemand(1,ingressNode,egressNode,offeredTraffic,null);
    				        		netPlan.setDemandAttribute(1, demandId1, "ODU", attribute);
    				        	}
        		break;
        	case "Translucent" : int maximumOpticalReach = Integer.parseInt(algorithmParameters.get("maximumOpticalReach"));
        						 for(int i=0;i<N;i++)
        						 {
        							 for(int j=0;j<N;j++)
        							 {
        								 if(j!=i)
        								 {
        									 double length=0;
        									 List<Long> primaryPath = GraphUtils.getShortestPath(linkTable, i , j , linkCosts);
        									 for(int a=0;a<primaryPath.size();a++)
        									 {
        										 length = length+netPlan.getLinkLengthInKm(0,primaryPath.get(a)); 
        									 }
        									 if(length<=maximumOpticalReach)
        									 {
        										 netPlan.addLink(1,nodes[i], nodes[j], 0, length, null); 
        									 } 	
        								 }
        							 }
        						 }
        						 netPlan.setLayerName(1,"Logical Topology " + logicalTopology + " - Maximum Optical Reach = "
        						 + maximumOpticalReach+ "km"); 
     				        	netPlan.setLayerAttribute(1, "type", logicalTopology);
    				        	netPlan.setLayerAttribute(1, "reach", ((Integer)maximumOpticalReach).toString());
    				        	for(long demandId:demandIds)
    				        	{
    				        		long ingressNode = netPlan.getDemandIngressNode(0, demandId);
    				        		long egressNode = netPlan.getDemandEgressNode(0, demandId);
    				        		double offeredTraffic = netPlan.getDemandOfferedTraffic(0, demandId);
    				        		String attribute = netPlan.getDemandAttribute(0, demandId, "ODU");
    				        		long demandId1 = netPlan.addDemand(1,ingressNode,egressNode,offeredTraffic,null);
    				        		netPlan.setDemandAttribute(1, demandId1, "ODU", attribute);
    				        	}
        		break;
        }       
        return "Ok!";
    }

	@Override
    public String getDescription()
    {
		StringBuilder description = new StringBuilder();
		String NEW_LINE = StringUtils.getLineSeparator();
		description.append("Logical Topology:"); description.append(NEW_LINE);	
		description.append("Opaque"); description.append(NEW_LINE);
		description.append("Transparent"); description.append(NEW_LINE);
		description.append("Translucent"); description.append(NEW_LINE);
		description.append(NEW_LINE);
		description.append("This Algorithm creates the logical topology on another layer based on the type of transport mode chosen.");
	return description.toString();
    }

    @Override
    public List<Triple<String, String, String>> getParameters() 
    {
    	List<Triple<String, String, String>> parameters = new ArrayList<Triple<String, String, String>>();
    	parameters.add(Triple.of("logicalTopology", "", "Logical Topology type"));
    	parameters.add(Triple.of("maximumOpticalReach", "", "Maximum Optical Reach in km"));
    	return parameters;   
    }
}