package org.example.duanparking.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import org.example.duanparking.model.ParkingSlot;

public interface ParkingInterface extends Remote {
   /*
   * Client gọi server sẽ trả lời
   */

   ArrayList<ParkingSlot> getSlot() throws RemoteException;
   void updateSlotStatus(String spotId, String status,String plateName, String owner, String arriveTime) throws RemoteException;
   ParkingSlot getUpdatedSlot(String spotId) throws RemoteException;
   void registerClient(ClientCallback client) throws RemoteException; // Đăng ký client
   void unregisterClient(ClientCallback client) throws RemoteException;
}
