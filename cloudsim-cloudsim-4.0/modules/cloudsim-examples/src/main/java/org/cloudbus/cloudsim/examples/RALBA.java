/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */


package org.cloudbus.cloudsim.examples;

import java.lang.invoke.MethodHandles;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class RALBA {

	private static int WORKLOAD = 0;
	// 0 Synthetic
	// 1 Google like
	// 2 Synthetic + Google Like
	
	private static int SCHEDULER = 0;
	// 0 RALBA
	// 1 RS
	// 2 RR
	// 3 MCT
	
	
	/** The cloudlet list. */
	private static List<Cloudlet> cloudletList;

	/** The vmlist. */
	private static List<Vm> vmlist;
	
	public static int genRandom(int low, int high) {
		return new Random().nextInt(high + 1 - low) + low;
	}
	
	private static List<Vm> createVMAdvance(int userId, int vms, int mipslow, int mipshigh) {

		//Creates a container to store VMs. This list is passed to the broker later
		LinkedList<Vm> list = new LinkedList<Vm>();

		//VM Parameters
		long size = 10000; //image size (MB)
		int ram = 512; //vm memory (MB)
		int mips = RALBA.genRandom(mipslow, mipshigh);
		long bw = 1000;
		int pesNumber = 1; //number of cpus
		String vmm = "Xen"; //VMM name

		//create VMs
		Vm[] vm = new Vm[vms];

		for(int i=0;i<vms;i++){
			vm[i] = new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			//for creating a VM with a space shared scheduling policy for cloudlets:
			//vm[i] = Vm(i, userId, mips, pesNumber, ram, bw, size, priority, vmm, new CloudletSchedulerSpaceShared());

			list.add(vm[i]);
		}

		return list;
	}

	private static List<Vm> createVMAdvance(int userId, int vms, int mips) {
		return createVMAdvance(userId, vms, mips, mips);
	}
	
	private static List<Vm> createVM(int userId, int vms) {
		return createVMAdvance(userId, vms, 800, 1100);
	}
	
	private static List<Vm> createExperimentVM(int userId) {

		//Creates a container to store VMs. This list is passed to the broker later
		LinkedList<Vm> list = new LinkedList<Vm>();
		
		LinkedList<Integer> vmc = new LinkedList<>();
		int ll[] = {7, 6,6,6,6,6,6};
		int lz[] = {500,750,1000,1250,1500,1750,4000};
		
		for(int i = 0; i < ll.length; i++)
		{
			for(int j = 0; j < ll[i]; j++) {
				vmc.add(lz[i]);
			}
		}

		//VM Parameters
		long size = 10000; //image size (MB)
		int ram = 512; //vm memory (MB)
		long bw = 1000;
		int pesNumber = 1; //number of cpus
		String vmm = "Xen"; //VMM name

		//create VMs
		Vm[] vm = new Vm[vmc.size()];

		for(int i=0;i<vmc.size();i++){
			vm[i] = new Vm(i, userId, vmc.get(i), pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			//for creating a VM with a space shared scheduling policy for cloudlets:
			//vm[i] = Vm(i, userId, mips, pesNumber, ram, bw, size, priority, vmm, new CloudletSchedulerSpaceShared());

			list.add(vm[i]);
		}

		return list;
	}


	private static List<Cloudlet> createCloudletAdvance(int userId, int cloudlets, int milow, int mihigh){
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		//cloudlet parameters
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for(int i=0;i<cloudlets;i++){
			long length = milow + (i * ( (mihigh - milow) / (cloudlets - 1) ) );
			cloudlet[i] = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}

		return list;
	}
	
	private static List<Cloudlet> createCloudletAdvance(int userId, int cloudlets, int mi){
		return createCloudletAdvance(userId, cloudlets, mi, mi);
	}
	
	
	private static List<Cloudlet> createCloudlet(int userId, int cloudlets){
		return createCloudletAdvance(userId, cloudlets, 900, 1000);
	}
	
	private static List<Cloudlet> createSyntheticCloudlet(int userId){
		
		LinkedList<Integer> clc = new LinkedList<>();
		int ll[] = {60, 5, 10, 5};
		int lz[][] = {{800,1200}, {1800,2500}, {7000, 10000}, {30000, 45000}};
		
		for(int i = 0; i < ll.length; i++)
		{
			for(int j = 0; j < ll[i]; j++) {
				int milow = lz[i][0];
				int mihigh = lz[i][1];
				clc.add(milow + (j * ( (mihigh - milow) / (ll[i] - 1) ) ));
			}
		}
		
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		//cloudlet parameters
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[clc.size()];

		for(int i=0;i<clc.size();i++){
			long length = clc.get(i);
			cloudlet[i] = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}

		return list;
	}
	
	private static List<Cloudlet> createGoogleCloudlet(int userId){
		
		LinkedList<Integer> clc = new LinkedList<>();
		int ll[] = {20, 40, 30, 4, 6};
		int lz[][] = {{15000, 55000}, {59000, 99000}, {101000, 135000}, {150000, 337500}, {525000, 900000}};
		
		for(int i = 0; i < ll.length; i++)
		{
			for(int j = 0; j < ll[i]; j++) {
				int milow = lz[i][0];
				int mihigh = lz[i][1];
				clc.add(milow + (j * ( (mihigh - milow) / (ll[i] - 1) ) ));
			}
		}
		
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		//cloudlet parameters
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[clc.size()];

		for(int i=0;i<clc.size();i++){
			long length = clc.get(i);
			cloudlet[i] = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}

		return list;
	}
	
	private static HashMap<Integer, Double> getVmCrMap(List<Vm> vmlist) {
		HashMap<Integer, Double> vmcrmap = new HashMap<>();
		double total = 0;
		for(Vm vm: vmlist) {
			total += vm.getMips();
		}
		for(Vm vm: vmlist) {
			vmcrmap.put(vm.getId(), vm.getMips() / total);
		}
		return vmcrmap;
	}
	
	private static HashMap<Integer, Double> getVmShare(HashMap<Integer, Double> vmcrmap, long cloudletLength){
		HashMap<Integer, Double> vmshare = new HashMap<>();
		for(Entry<Integer, Double> vm: vmcrmap.entrySet()) {
			vmshare.put(vm.getKey(), vm.getValue() * cloudletLength);
		}
		return vmshare;
	}
	
	private static long getCloudletMISum(List<Cloudlet> cloudlist) {
		long sum = 0;
		
		for(Cloudlet cl: cloudlist)
			sum += cl.getCloudletTotalLength();
		
		return sum;
	}
	
	private static Integer getLargestVmShare(HashMap<Integer, Double> vmshare) {
		Double largest_vmshare = null;
		Integer id = null;
		
		for(Entry<Integer, Double> vm: vmshare.entrySet()) {
			if(Objects.isNull(largest_vmshare) || vm.getValue() > largest_vmshare) {
				largest_vmshare = vm.getValue();
				id = vm.getKey();
			}
		}
		return id;
	}
	
	private static Cloudlet getSmallestCloudlet(HashMap<Integer,Cloudlet> remainingCloudlet) {
		Cloudlet smallest_cl = null;
		for(Cloudlet cl: remainingCloudlet.values()) {
			if(Objects.isNull(smallest_cl) || cl.getCloudletLength() < smallest_cl.getCloudletLength()) {
				smallest_cl = cl;
			}
		}
		return smallest_cl;
	}
	
	private static Cloudlet getLargestCloudletToAssignVM(Double largest_vmshare, HashMap<Integer,Cloudlet> remainingCloudlet) {
		Cloudlet largest_cl = null;
		for(Cloudlet cl: remainingCloudlet.values()) {
			if(cl.getCloudletLength() < largest_vmshare) {
				if(Objects.isNull(largest_cl) || cl.getCloudletLength() > largest_cl.getCloudletLength()) {
					largest_cl = cl;
				}
			}
		}
		
		return largest_cl;
	}
	
	private static Cloudlet getLargestCloudlet(HashMap<Integer,Cloudlet> remainingCloudlet) {
		Cloudlet largest_cl = null;
		for(Cloudlet cl: remainingCloudlet.values()) {
			if(Objects.isNull(largest_cl) || cl.getCloudletLength() > largest_cl.getCloudletLength()) {
				largest_cl = cl;
			}
		
		}
		
		return largest_cl;
	}
	
	private static HashMap<Integer, Double> getVmCt(List<Vm> vmList, List<Cloudlet> cloudletList){
		HashMap<Integer, Double> vm_ct = new HashMap<>();
		for(Vm vm : vmList) {
			double sum = 0;
			for(Cloudlet cl : cloudletList) {
				if(cl.getVmId() == vm.getId())
					sum += (cl.getCloudletLength() / vm.getMips());
			}
			vm_ct.put(vm.getId(), sum);
		}
		return vm_ct;
	}
	
	private static HashMap<Integer, Double> getCloudletCtPerVm(Cloudlet cl, List<Vm> vmList, HashMap<Integer, Double> vm_ct) {
		HashMap<Integer, Double> cloudlet_ct = new HashMap<>();
		
		for(Vm vm : vmList) {
			double sum = (cl.getCloudletLength() / vm.getMips()) + vm_ct.get(vm.getId());
			cloudlet_ct.put(vm.getId(), sum);
		}
		
		return cloudlet_ct;
	}
	
	private static Vm getVMwithEFT(HashMap<Integer, Double> cloudlet_ct_per_vm, List<Vm> vmList) {
		
		Double min = 0.0;
		if(vmList.size() == 0) return null;
		
		Vm vm = vmList.get(0);
		min = cloudlet_ct_per_vm.get(vm.getId());
		
		for(int i = 1; i < cloudlet_ct_per_vm.size(); i++) {
			if(cloudlet_ct_per_vm.get(vmList.get(i).getId()) < min) {
				vm = vmList.get(i);
				min = cloudlet_ct_per_vm.get(vmList.get(i).getId());
			}
		}
		
		return vm;
	}
	
	private static Double getAverageMakeSpan(HashMap<Integer, Double> vm_ct) {
		
		Double sum = 0.0;
		
		for(Double e: vm_ct.values()) {
			sum += e;
		}
		
		return sum / vm_ct.size();
	}
	
	private static HashMap<Integer, Double> getVmMakeSpan(List<Cloudlet> cloudletlist){
		HashMap<Integer, Double> vm_makespan = new HashMap<>();
		
		for(Cloudlet cl : cloudletlist) {
			if(cl.getVmId() != -1) {
				if(!vm_makespan.containsKey(cl.getVmId()) || cl.getFinishTime() > vm_makespan.get(cl.getVmId())) {
					vm_makespan.put(cl.getVmId(), cl.getFinishTime());
				}
			}
		}
		
		return vm_makespan;
	}
	
	private static Vm getVmWithMaxMakeSpan(HashMap<Integer, Double> vm_ct, List<Vm> vmlist) {
		
		Vm max = null;
		
		for(Vm vm : vmlist) {
			if(Objects.isNull(max) || vm_ct.get(vm.getId()) > vm_ct.get(max.getId()) ) {
				max = vm;
			}
		}
		
		return max;
		
	}
	
	private static Vm getVmWithMinMakeSpan(HashMap<Integer, Double> vm_ct, List<Vm> vmlist) {
		
		Vm min = null;
		
		for(Vm vm : vmlist) {
			if(Objects.isNull(min) || vm_ct.get(vm.getId()) < vm_ct.get(min.getId()) ) {
				min = vm;
			}
		}
		
		return min;
		
	}
	

	////////////////////////// STATIC METHODS ///////////////////////

	/**
	 * Creates main() to run this example
	 */
	public static void main(String[] args) {
		Log.printLine("Starting "+MethodHandles.lookup().lookupClass().getSimpleName()+"...");

		try {
			// First step: Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 1;   // number of grid users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;  // mean trace events

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag);

			// Second step: Create Datacenters
			//Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
			@SuppressWarnings("unused")
			Datacenter datacenter0 = createDatacenter("Datacenter_0");

			//Third step: Create Broker
			DatacenterBroker broker = createBroker();
			int brokerId = broker.getId();

			//Fourth step: Create VMs and Cloudlets and send them to broker
			//vmlist = createVM(brokerId,30); //creating 20 vms
			//cloudletList = createCloudlet(brokerId,2000); // creating 40 cloudlets
			
			vmlist = createExperimentVM(brokerId);
			if(WORKLOAD == 0)
				cloudletList = createSyntheticCloudlet(brokerId);
			else if (WORKLOAD == 1)
				cloudletList = createGoogleCloudlet(brokerId);
			else {
				cloudletList = createSyntheticCloudlet(brokerId);
				cloudletList.addAll(createGoogleCloudlet(brokerId));
			}
						
			broker.submitVmList(vmlist);
			broker.submitCloudletList(cloudletList);
			
			
			// CODE STARTS HERE
			
			// FILL
			if(SCHEDULER == 0) {
				HashMap<Integer, Double> vmcrmap = getVmCrMap(vmlist);
				Long cloudletLength = getCloudletMISum(cloudletList);
				HashMap<Integer, Double> vmshare = getVmShare(vmcrmap, cloudletLength);
				HashMap<Integer, Cloudlet> remainingCloudlet = new HashMap<>();
				HashMap<Integer, Integer> clAssignedToVM = new HashMap<>();
				
				for(Cloudlet cl: cloudletList)
					remainingCloudlet.put(cl.getCloudletId(), cl);
				
				while(true) {
					
					Cloudlet cl = getSmallestCloudlet(remainingCloudlet);
					
					if(Objects.isNull(cl))
						break;
					
					int largest_vmshare_id = getLargestVmShare(vmshare);
					
					if(!(cl.getCloudletLength() >= vmshare.get(largest_vmshare_id)))
						break;
					
					Cloudlet clToAssign = getLargestCloudletToAssignVM(vmshare.get(largest_vmshare_id), remainingCloudlet);
					
					vmshare.put(largest_vmshare_id, vmshare.get(largest_vmshare_id) - cl.getCloudletLength());
					
					remainingCloudlet.remove(clToAssign.getCloudletId());
					
					broker.bindCloudletToVm(clToAssign.getCloudletId(), largest_vmshare_id);
					
					clAssignedToVM.put(clToAssign.getCloudletId(), largest_vmshare_id);
					
					if(remainingCloudlet.size() == 0)
						break;
				}
				
				// SPILL
				
				while(remainingCloudlet.size() != 0) {
					
					Cloudlet cl = getLargestCloudlet(remainingCloudlet);
					
					HashMap<Integer, Double> vm_ct = getVmCt(vmlist, cloudletList);
					HashMap<Integer, Double> cloudlet_ct_per_vm = getCloudletCtPerVm(cl, vmlist, vm_ct);
					
					Vm vmwitheft = getVMwithEFT(cloudlet_ct_per_vm, vmlist);
									
					remainingCloudlet.remove(cl.getCloudletId());
					
					broker.bindCloudletToVm(cl.getCloudletId(), vmwitheft.getId());
					
					clAssignedToVM.put(cl.getCloudletId(), vmwitheft.getId());
					
				}
			}
			
			// CODE ENDS HERE FOR SCHEDULER, RESUMED LATER
			
			
			// RANDOM
			else if (SCHEDULER == 1) {
				for(int i = 0; i < cloudletList.size(); i++) {
					int vmid = genRandom(0, vmlist.size() - 1);
					broker.bindCloudletToVm(cloudletList.get(i).getCloudletId(), vmlist.get(vmid).getId());
				}
			}
			
			// Round Robin
			else if (SCHEDULER == 2) {
				int vmid = 0;
				for(int i = 0; i < cloudletList.size(); i++) {
					broker.bindCloudletToVm(cloudletList.get(i).getCloudletId(), vmlist.get(vmid).getId());
					vmid = (vmid + 1) % vmlist.size();
				}
			}
			
			// Minimum Completion Time
			else {
				for(int i = 0; i < cloudletList.size(); i++) {
					HashMap<Integer, Double> vm_ct = getVmCt(vmlist, cloudletList);
					int vmid = getVmWithMinMakeSpan(vm_ct, vmlist).getId();
					broker.bindCloudletToVm(cloudletList.get(i).getCloudletId(), vmlist.get(vmid).getId());
				}
			}
			
			// Fifth step: Starts the simulation
			CloudSim.startSimulation();

			// Final step: Print results when simulation is over
			List<Cloudlet> newList = broker.getCloudletReceivedList();

			CloudSim.stopSimulation();

			printCloudletList(newList);
			
			// CODE RESUMES HERE
			
			// Metrics
			
			HashMap<Integer, Double> vm_ct = getVmCt(vmlist, cloudletList);
			HashMap<Integer, Double> vm_makespan = getVmMakeSpan(cloudletList);
			Vm vmWitHighestMakeSpan = getVmWithMaxMakeSpan(vm_ct, vmlist);
			Double average_makespan = getAverageMakeSpan(vm_ct);
			Double makespan = vm_ct.get(vmWitHighestMakeSpan.getId());
			Double throughput = newList.size() / makespan;
			Double arur = average_makespan / makespan;
			
			System.out.println(String.format("Makespan: %f", makespan));
			System.out.println(String.format("Throughput: %f", throughput));
			System.out.println(String.format("ARUR: %f", arur));
			
			System.out.println(String.format("%f\n%f\n%f", makespan, throughput, arur));
			
			
			// CODE ENDS HERE

			Log.printLine(MethodHandles.lookup().lookupClass().getSimpleName() + " finished!");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}
	}

	private static Datacenter createDatacenter(String name){

		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store one or more
		//    Machines
		List<Host> hostList = new ArrayList<Host>();

		// 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
		//    create a list to store these PEs before creating
		//    a Machine.
		List<Pe> peList1 = new ArrayList<Pe>();

		int mips = 4000;

		// 3. Create PEs and add these into the list.
		//for a quad-core machine, a list of 4 PEs is required:
		peList1.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
		peList1.add(new Pe(1, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(2, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(3, new PeProvisionerSimple(mips)));
		
		//Another list, for a dual-core machine
		List<Pe> peList2 = new ArrayList<Pe>();

		peList2.add(new Pe(0, new PeProvisionerSimple(mips)));
		peList2.add(new Pe(1, new PeProvisionerSimple(mips)));

		//4. Create Hosts with its id and list of PEs and add them to the list of machines
		int hostId = 0;
		int ram = 16384; //host memory (MB)
		long storage = 1000000; //host storage
		int bw = 10000;
		
		for(; hostId < 4; hostId++) {
			hostList.add(
    			new Host(
    				hostId,
    				new RamProvisionerSimple(ram),
    				new BwProvisionerSimple(bw),
    				storage,
    				peList1,
    				new VmSchedulerTimeShared(peList2)
	    		)
    		); // This is our first machine
		}
		
		for(; hostId < 26; hostId++) {
		hostList.add(
    			new Host(
    				hostId,
    				new RamProvisionerSimple(ram),
    				new BwProvisionerSimple(bw),
    				storage,
    				peList1,
    				new VmSchedulerTimeShared(peList1)
    			)
    		); // This is our first machine
		}


		//To create a host with a space-shared allocation policy for PEs to VMs:
		//hostList.add(
    	//		new Host(
    	//			hostId,
    	//			new CpuProvisionerSimple(peList1),
    	//			new RamProvisionerSimple(ram),
    	//			new BwProvisionerSimple(bw),
    	//			storage,
    	//			new VmSchedulerSpaceShared(peList1)
    	//		)
    	//	);

		//To create a host with a oportunistic space-shared allocation policy for PEs to VMs:
		//hostList.add(
    	//		new Host(
    	//			hostId,
    	//			new CpuProvisionerSimple(peList1),
    	//			new RamProvisionerSimple(ram),
    	//			new BwProvisionerSimple(bw),
    	//			storage,
    	//			new VmSchedulerOportunisticSpaceShared(peList1)
    	//		)
    	//	);


		// 5. Create a DatacenterCharacteristics object that stores the
		//    properties of a data center: architecture, OS, list of
		//    Machines, allocation policy: time- or space-shared, time zone
		//    and its price (G$/Pe time unit).
		String arch = "x86";      // system architecture
		String os = "Linux";          // operating system
		String vmm = "Xen";
		double time_zone = 10.0;         // time zone this resource located
		double cost = 3.0;              // the cost of using processing in this resource
		double costPerMem = 0.05;		// the cost of using memory in this resource
		double costPerStorage = 0.1;	// the cost of using storage in this resource
		double costPerBw = 0.1;			// the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>();	//we are not adding SAN devices by now

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);


		// 6. Finally, we need to create a PowerDatacenter object.
		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	//We strongly encourage users to develop their own broker policies, to submit vms and cloudlets according
	//to the specific rules of the simulated scenario
	private static DatacenterBroker createBroker(){

		DatacenterBroker broker = null;
		try {
			broker = new DatacenterBroker("Broker");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	/**
	 * Prints the Cloudlet objects
	 * @param list  list of Cloudlets
	 */
	private static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "    ";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
				"Data center ID" + indent + "VM ID" + indent + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
				Log.print("SUCCESS");

				Log.printLine( indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
						indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
						indent + indent + dft.format(cloudlet.getExecStartTime())+ indent + indent + indent + dft.format(cloudlet.getFinishTime()));
			}
		}

	}
}
