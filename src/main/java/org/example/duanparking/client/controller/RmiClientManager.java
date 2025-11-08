package org.example.duanparking.client.controller;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.example.duanparking.client.ClientCallbackImpl;
import org.example.duanparking.common.*;

public class RmiClientManager {
    private static RmiClientManager instance;
    private ParkingInterface parkingInterface; 
    private ClientCallback clientCallback; 

    public RmiClientManager() {}

    public static synchronized RmiClientManager getInstance() {
        if (instance == null) {
            instance = new RmiClientManager();
        }
        return instance;
    }

    public void connect() {
        try{
            Registry registry = LocateRegistry.getRegistry("localhost",1099);
            parkingInterface = (ParkingInterface) registry.lookup("ParkingService"); 
            clientCallback = new ClientCallbackImpl(parkingInterface, null); 
        }catch(Exception e ){
            e.printStackTrace();
        } 
    }

    public ParkingInterface getParkingInterface() {
        return parkingInterface; 
    }

    public ClientCallback getClientCallBack() {
        return clientCallback; 
    }


    public void setGridManager(ParkingGridManager gridManager) {
      try{
          if (clientCallback != null) {
              clientCallback.setGridManager(gridManager);
          }
      }catch (RemoteException e){
          e.printStackTrace();
      }
    }


}
