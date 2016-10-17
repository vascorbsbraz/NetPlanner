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
 * @version 2.0, May 2016
 */
public class logicalTopology implements IAlgorithm
{
	
	
	 private NetworkLayer lowerLayer, upperLayer;
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
    	
    	/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
        InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
    	
    	String logicalTopology = algorithmParameters.get("logicalTopology");
    	
    	final int N = netPlan.getNumberOfNodes();
        if (N == 0 ) throw new Net2PlanException("This algorithm requires a topology with nodes");
        
        if(netPlan.isMultilayer())
        {
        	 NetworkLayer l1= netPlan.getNetworkLayer(1);
        	 netPlan.removeNetworkLayer(l1);
        	
        }
        
         
       	 if (netPlan.isSingleLayer() && logicalTopology.equalsIgnoreCase("Opaque"))
         {		  
       		 	 	
       		 	  lowerLayer=netPlan.getNetworkLayerDefault();	
                  upperLayer = netPlan.addLayerFrom(lowerLayer);
                  /* Save the demands in the upper layer, and remove them from the lower layer */
                 netPlan.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING, upperLayer);
                 lowerLayer.setName("Physical Topology");
                 upperLayer.setName("Logical Topology Opaque");
                 upperLayer.setDescription("Opaque Logical Topology");
                 
                 //for (Demand d : netPlan.getDemands (lowerLayer)) netPlan.addDemand(d.getIngressNode() , d.getEgressNode() , d.getOfferedTraffic() , null , upperLayer);
                 
               
                 
         }
    	 
    	 if(netPlan.isSingleLayer() && logicalTopology.equalsIgnoreCase("Transparent"))
         {	
    		 
    		 
    		 this.lowerLayer = netPlan.getNetworkLayerDefault();
    		 lowerLayer.setName("Physical Topology");
             this.upperLayer = netPlan.addLayer("Logical Topology Transparent" , "Upper layer of the design" , "ODU" , "ODU" , null);
             /* Save the demands in the upper layer, and remove them from the lower layer */
             netPlan.removeAllLinks(upperLayer);
             
           
             
             for (Node i : netPlan.getNodes ()) for (Node j : netPlan.getNodes ()) if (i.getIndex() != j.getIndex())
            	                 {
            	                         
            	                         netPlan.addLink (i, j,0 , netPlan.getNodePairEuclideanDistance(i, j), 200000 , null , upperLayer);
               	                 }
                                
             //for (Demand d : netPlan.getDemands (lowerLayer)) netPlan.addDemand(d.getIngressNode() , d.getEgressNode() , d.getOfferedTraffic() , null , upperLayer);
             
         }
    	

     		
       
		//Check if there is no second layer
		//Opaque physical layer = logical layer
		//Add new layer logical topology transparent
        

            
        
    		
     /*   int index = 0;
		for (long nodeId: nodeIds)
		{
			 nodes[index] = (int)nodeId;
			 index++;
		}
*/
        /*switch(logicalTopology)
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
        }  */     
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
		description.append(NEW_LINE);
		description.append("This Algorithm creates the logical topology on another layer based on the type of transport mode chosen.");
	return description.toString();
    }

    @Override
    public List<Triple<String, String, String>> getParameters() 
    {
    	List<Triple<String, String, String>> parameters = new ArrayList<Triple<String, String, String>>();
    	parameters.add(Triple.of("logicalTopology", "", "Logical Topology type"));
    	//parameters.add(Triple.of("maximumOpticalReach", "", "Maximum Optical Reach in km"));
    	return parameters;   
    }
}